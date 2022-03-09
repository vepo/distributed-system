package io.vepo.distributedsystem.leaderelection;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClusterController implements AutoCloseable {
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
                        Command.loadCommand(data)
                                .ifPresent(command -> {
                                    synchronized (commandBuffer) {
                                        commandBuffer.offer(command);
                                        commandBuffer.notifyAll();
                                    }
                                });
                    }
                } catch (IOException | InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private AtomicBoolean running = new AtomicBoolean(true);
    private Duration timeout;
    private DatagramSocket outputSocket;
    private InetAddress address;
    private int port;
    private MulticastSocket inputSocket;
    private ExecutorService threadPool;
    private Queue<Command> commandBuffer;
    private InetSocketAddress group;
    private String id;
    private long pid;

    public ClusterController(String id, String host, int port) throws IOException {
        this.id = id;
        this.pid = ProcessHandle.current().pid();
        var rand = new Random();
        this.timeout = Duration.ofSeconds(5)
                .plus(Duration.ofMillis(rand.nextInt(500) + 250));

        this.address = InetAddress.getByName(host);
        this.port = port;

        outputSocket = new DatagramSocket();
        outputSocket.setBroadcast(true);

        inputSocket = new MulticastSocket(port);
        group = new InetSocketAddress(address, port);
        var networkInterface = NetworkInterface.networkInterfaces()
                .filter(net -> {
                    try {
                        return net.supportsMulticast();
                    } catch (SocketException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("No Multicast interface"));
        inputSocket.joinGroup(group, networkInterface);

        commandBuffer = new LinkedList<>();
        threadPool = Executors.newFixedThreadPool(1);
        threadPool.submit(new ConsumeCommands());
        System.out.println("Cluster created!");
    }

    public void join() throws IOException {
        System.out.println("Sending Join Command");
        sendCommand(new JoinCommand(id, pid));
    }

    public Optional<Command> getCommand(Duration timeout) throws InterruptedException {
        long start = System.nanoTime();
        do {
            synchronized (commandBuffer) {
                if (!commandBuffer.isEmpty()) {
                    return Optional.of(commandBuffer.poll());
                } else {
                    commandBuffer.wait(500);
                }
            }
        } while (System.nanoTime() - start < timeout.toNanos());
        return Optional.empty();
    }

    public void sendCommand(Command command) throws IOException {
        var buf = command.getBytes();
        var packet = new DatagramPacket(buf, buf.length, address, port);
        outputSocket.send(packet);
    }

    @Override
    public void close() throws Exception {
        running.set(false);
        threadPool.awaitTermination(500, TimeUnit.MILLISECONDS);
        threadPool.shutdownNow();

        if (Objects.nonNull(outputSocket) && !outputSocket.isClosed()) {
            outputSocket.close();
            outputSocket = null;
        }

        if (Objects.nonNull(inputSocket) && !inputSocket.isClosed()) {
            inputSocket.leaveGroup(address);
            inputSocket.close();
            inputSocket = null;
        }
    }
}
