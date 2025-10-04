package com.backend.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "allanai.processing")
public class ProcessingProperties {

    private int maxFrameSamples = 1200;

    public int getMaxFrameSamples() {
        return maxFrameSamples;
    }

    public void setMaxFrameSamples(int maxFrameSamples) {
        this.maxFrameSamples = maxFrameSamples;
    }
}
