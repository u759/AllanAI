package com.backend.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "allanai.processing")
public class ProcessingProperties {

    private int maxFrameSamples = 1200;
    private double motionThreshold = 18.0;
    private Threads threads = new Threads();

    public int getMaxFrameSamples() {
        return maxFrameSamples;
    }

    public void setMaxFrameSamples(int maxFrameSamples) {
        this.maxFrameSamples = maxFrameSamples;
    }

    public double getMotionThreshold() {
        return motionThreshold;
    }

    public void setMotionThreshold(double motionThreshold) {
        this.motionThreshold = motionThreshold;
    }

    public Threads getThreads() {
        return threads;
    }

    public void setThreads(Threads threads) {
        this.threads = threads;
    }

    public static class Threads {
        private int core = 2;
        private int max = 4;
        private int queueCapacity = 32;

        public int getCore() {
            return core;
        }

        public void setCore(int core) {
            this.core = core;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }
}
