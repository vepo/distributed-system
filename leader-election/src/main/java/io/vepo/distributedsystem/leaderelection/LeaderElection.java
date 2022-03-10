package io.vepo.distributedsystem.leaderelection;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaderElection {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting leader election...");
        var rand = new Random();
        var config = ClusterConfig.builder()
                .id(UUID.randomUUID().toString())
                .multicastIp("224.0.0.1")
                .multicastPort(4445)
                .timeout(Duration.ofSeconds(5)
                        .plus(Duration.ofMillis((long) rand.nextInt(500) + 250)))
                .build();
        try (var controller = new ClusterController(config)) {
            controller.joinCluster();
        }
        logger.info("Leader election ended!");
    }
}
