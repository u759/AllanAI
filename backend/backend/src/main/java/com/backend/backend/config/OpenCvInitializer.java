package com.backend.backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import nu.pattern.OpenCV;

@Component
public class OpenCvInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCvInitializer.class);

    @PostConstruct
    public void loadLibrary() {
        try {
            OpenCV.loadLocally();
            LOGGER.info("OpenCV native libraries loaded successfully");
        } catch (UnsatisfiedLinkError error) {
            LOGGER.error("Failed to load OpenCV native libraries", error);
            throw error;
        }
    }
}
