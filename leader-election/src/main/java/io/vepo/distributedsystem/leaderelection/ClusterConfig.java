package io.vepo.distributedsystem.leaderelection;

import java.time.Duration;
import java.util.Objects;

public record ClusterConfig(String id, String multicastIp, int multicastPort, Duration timeout) {
    public static class ClusterConfigBuilder {
        private String id;
        private Duration timeout;
        private String multicastIp;
        private Integer multicastPort;

        private ClusterConfigBuilder() {
        }

        public ClusterConfigBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ClusterConfigBuilder multicastIp(String multicastIp) {
            this.multicastIp = multicastIp;
            return this;
        }

        public ClusterConfigBuilder multicastPort(int multicastPort) {
            this.multicastPort = multicastPort;
            return this;
        }

        public ClusterConfigBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public ClusterConfig build() {
            Objects.requireNonNull(id, "id must not be null or empty!");
            if (id.isBlank()) {
                throw new IllegalArgumentException("id must not be null or empty!");
            }

            Objects.requireNonNull(multicastIp, "multicastIp must not be null or empty!");
            if (multicastIp.isBlank()) {
                throw new IllegalArgumentException("multicastIp must not be null or empty!");
            }

            Objects.requireNonNull(multicastPort, "multicastPort must not be null!");

            Objects.requireNonNull(timeout, "timeout must not be null!");

            return new ClusterConfig(id, multicastIp, multicastPort, timeout);
        }
    }

    public static ClusterConfigBuilder builder() {
        return new ClusterConfigBuilder();
    }
}
