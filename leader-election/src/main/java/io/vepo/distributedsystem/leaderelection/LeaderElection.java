package io.vepo.distributedsystem.leaderelection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

public class LeaderElection {

    public static void main(String[] args) throws Exception {
        try (var controller = new ClusterController(UUID.randomUUID().toString(), "224.0.0.1", 4445)) {
            controller.join();
            while (true) {
                var maybeCommand = controller.getCommand(Duration.ofSeconds(10));
                if (maybeCommand.isPresent()) {
                    System.out.println("Command: " + maybeCommand.get());
                } else {
                    Thread.sleep(500);
                }
            }
        }
    }
}
