package com.abadeksvp.bankingsimulator.cqrs.query;

public record QueryBusConfig(int threadPoolSize, int shutdownTimeoutSeconds) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int threadPoolSize = Runtime.getRuntime().availableProcessors();
        private int shutdownTimeoutSeconds = 30;

        public Builder threadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        public Builder shutdownTimeoutSeconds(int shutdownTimeoutSeconds) {
            this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
            return this;
        }

        public QueryBusConfig build() {
            return new QueryBusConfig(threadPoolSize, shutdownTimeoutSeconds);
        }
    }
}
