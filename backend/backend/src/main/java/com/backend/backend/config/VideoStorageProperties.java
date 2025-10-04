package com.backend.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "allanai.storage")
public class VideoStorageProperties {

    private Path videoDirectory = Paths.get("storage/videos");

    public Path getVideoDirectory() {
        return videoDirectory;
    }

    public void setVideoDirectory(Path videoDirectory) {
        this.videoDirectory = videoDirectory;
    }
}
