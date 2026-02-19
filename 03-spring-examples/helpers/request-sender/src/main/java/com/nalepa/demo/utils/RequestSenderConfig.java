package com.nalepa.demo.utils;

public interface RequestSenderConfig {
    int numberOfRequestInBatch();

    public static class CpuCountRequestConfig implements RequestSenderConfig {
        public static final CpuCountRequestConfig INSTANCE = new CpuCountRequestConfig();

        @Override
        public int numberOfRequestInBatch() {
            return Runtime.getRuntime().availableProcessors();
        }
    }

    public static class UserProvidedRequestConfig implements RequestSenderConfig {

        private final int count;

        public UserProvidedRequestConfig(int count) {
            if (count > 200) {
                throw new IllegalArgumentException("Count of request in batch cannot be bigger than 200, because of thread pool size");
            }
            this.count = count;
        }

        @Override
        public int numberOfRequestInBatch() {
            return count;
        }
    }
}

