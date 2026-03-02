package com.nalepa.demo.example04;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class CustomFixedCoreAndMaxPoolSize {

    static class ExecutorAwareLinkedBlockingQueue extends LinkedBlockingQueue<Runnable> {
        private ThreadPoolExecutor executor;

        ExecutorAwareLinkedBlockingQueue(int size) {
            super(size);
        }

        public void setExecutor(ThreadPoolExecutor executor) {
            this.executor = executor;
        }

        @Override
        public boolean offer(Runnable runnable) {
            if (executor == null) {
                throw new IllegalStateException("Executor is null. Use method setExecutor to set it before using this queue.");
            }

            // If there are fewer threads than the maximum pool size,
            // we want to create new threads instead of putting tasks in the queue.
            if (executor.getPoolSize() < executor.getMaximumPoolSize()) {
                return false;
            }
            return super.offer(runnable);
        }
    }

}
