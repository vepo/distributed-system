package io.vepo.distributedsystem.leaderelection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vepo.distributedsystem.leaderelection.commands.AcceptLeaderCommand;
import io.vepo.distributedsystem.leaderelection.commands.ApplyForLeaderCommand;
import io.vepo.distributedsystem.leaderelection.commands.Command;
import io.vepo.distributedsystem.leaderelection.commands.JoinCommand;
import io.vepo.distributedsystem.leaderelection.commands.LeaderElectedCommand;

public class ClusterController implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ClusterController.class);

    public enum LeaderStatus {
        NO_LEADER, WAITING_ELECTION_RESULT, ELECTED, LEADER_DEAD
    }

    private class ConsumeCommands implements Runnable {
        protected byte[] buf = new byte[256];

        @Override
        public void run() {
            while (running.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    inputSocket.receive(packet);
                    if (packet.getLength() > 0) {
                        var data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                        Command.loadCommand(data).ifPresent(command -> {
                            logger.info("Command received: {}", command);
                            if (command instanceof JoinCommand joinCommand) {
                                brokers.put(joinCommand.id(),
                                        new Broker(joinCommand.id(), joinCommand.hostname(), joinCommand.startup()));
                            } else if (command instanceof ApplyForLeaderCommand applyCommand) {
                                if (isWaitingElectionResults() && !config.id().equals(applyCommand.id())) {
                                    acceptLeader(applyCommand.id());
                                }
                            } else if (command instanceof AcceptLeaderCommand acceptLeaderCommand) {
                                if (isWaitingElectionResults() && config.id().equals(acceptLeaderCommand.leaderId())) {
                                    acceptedVotes.add(acceptLeaderCommand.brokerId());

                                    if (acceptedVotes.equals(brokers.keySet().stream()
                                            .filter(id -> !id.equals(config.id())).collect(Collectors.toSet()))) {
                                        wonElection();
                                    }
                                }
                            } else if (command instanceof LeaderElectedCommand leaderElectedCommand &&
                                    !leaderElectedCommand.leaderId().equals(config.id())) {
                                logger.info("Process won the election! {}", leaderElectedCommand);
                                leaderSelected(leaderElectedCommand.leaderId());
                            }
                        });
                    }
                } catch (IOException ex) {
                    logger.error("Error!", ex);
                }
            }
        }

    }

    private AtomicBoolean running = new AtomicBoolean(true);
    private DatagramSocket outputSocket;
    private InetAddress address;
    private MulticastSocket inputSocket;
    private ExecutorService threadPool;
    private InetSocketAddress group;
    private long pid;
    private NetworkInterface networkInterface;
    private long startTimestamp;
    private ClusterConfig config;
    private Broker leader;
    private String hostName;
    private Map<String, Broker> brokers;
    private Map<String, Long> heartbeats;
    private LeaderStatus leaderStatus;
    private Set<String> acceptedVotes;

    public ClusterController(ClusterConfig config) throws IOException {
        try {
            this.config = config;
            this.startTimestamp = System.nanoTime();
            this.pid = ProcessHandle.current().pid();

            this.address = InetAddress.getByName(config.multicastIp());
            this.hostName = InetAddress.getLocalHost().getHostName();

            this.outputSocket = new DatagramSocket();
            this.outputSocket.setBroadcast(true);

            this.inputSocket = new MulticastSocket(config.multicastPort());
            this.group = new InetSocketAddress(address, config.multicastPort());
            this.networkInterface = NetworkInterface.networkInterfaces().filter(net -> {
                try {
                    return net.supportsMulticast();
                } catch (SocketException e) {
                    return false;
                }
            }).findFirst().orElseThrow(() -> new UnsupportedOperationException("No Multicast interface"));
            this.inputSocket.joinGroup(group, networkInterface);

            this.acceptedVotes = Collections.synchronizedSet(new HashSet<>());
            this.brokers = Collections.synchronizedMap(new HashMap<>());
            this.heartbeats = Collections.synchronizedMap(new HashMap<>());
            this.leader = null;
            this.leaderStatus = LeaderStatus.NO_LEADER;
            this.threadPool = Executors.newFixedThreadPool(1);
            this.threadPool.submit(new ConsumeCommands());

        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Could not resolve hostname!", ex);
        }
    }

    private boolean isWaitingElectionResults() {
        return Objects.isNull(leader) && (leaderStatus == LeaderStatus.NO_LEADER
                || leaderStatus == LeaderStatus.WAITING_ELECTION_RESULT);
    }

    private void acceptLeader(String leaderId) {
        sendCommand(new AcceptLeaderCommand(config.id(), leaderId));
    }

    private void leaderSelected(String leaderId) {
        leaderStatus = LeaderStatus.ELECTED;
        leader = brokers.get(leaderId);
    }

    private void wonElection() {
        leaderStatus = LeaderStatus.ELECTED;
        sendCommand(new LeaderElectedCommand(config.id()));
        leader = new Broker(config.id(), hostName, startTimestamp);
    }

    private void applyForLeader() {
        sendCommand(new ApplyForLeaderCommand(config.id()));
        leaderStatus = LeaderStatus.WAITING_ELECTION_RESULT;
    }

    public void joinCluster() {
        sendCommand(new JoinCommand(config.id(), hostName, pid, startTimestamp));
        while (running.get()) {
            try {
                Thread.sleep(config.timeout().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (leaderStatus == LeaderStatus.NO_LEADER
                    && brokers.values().stream().filter(broker -> !broker.id().equals(config.id())).count() > 0L) {
                applyForLeader();
            } else if (leaderStatus == LeaderStatus.NO_LEADER) {
                wonElection();
            }
        }
    }

    public void sendCommand(Command command) {
        logger.info("Sending command: {}", command);
        try {
            var buf = command.getBytes();
            var packet = new DatagramPacket(buf, buf.length, group);
            outputSocket.send(packet);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not send command to cluster!", ex);
        }
    }

    @Override
    public void close() {
        try {
            this.running.set(false);
            this.threadPool.awaitTermination(500, TimeUnit.MILLISECONDS);
            this.threadPool.shutdownNow();

            if (Objects.nonNull(this.outputSocket) && !this.outputSocket.isClosed()) {
                this.outputSocket.close();
                this.outputSocket = null;
            }

            if (Objects.nonNull(this.inputSocket) && !this.inputSocket.isClosed()) {
                this.inputSocket.leaveGroup(this.group, this.networkInterface);
                this.inputSocket.close();
                this.inputSocket = null;
            }
        } catch (InterruptedException | IOException ex) {
            throw new IllegalStateException("Error stopping...", ex);
        }
    }
}
