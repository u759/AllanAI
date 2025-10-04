package com.backend.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "allanai.model")
public class ModelProperties {

    private boolean enabled = false;
    private Path workingDirectory = Paths.get("..");
    private List<String> command = new ArrayList<>();
    private Path weightsPath = Paths.get("runs/train/weights/best.pt");
    private Path outputDirectory = Paths.get("storage/model-output");
    private String resultFileName = "{matchId}-inference.json";
    private double confidenceThreshold = 0.35;
    private int preEventFrames = 4;
    private int postEventFrames = 12;
    private double fallbackFps = 120.0;
    private Duration commandTimeout = Duration.ofMinutes(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public List<String> getCommand() {
        return command;
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }

    public Path getWeightsPath() {
        return weightsPath;
    }

    public void setWeightsPath(Path weightsPath) {
        this.weightsPath = weightsPath;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getResultFileName() {
        return resultFileName;
    }

    public void setResultFileName(String resultFileName) {
        this.resultFileName = resultFileName;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public int getPreEventFrames() {
        return preEventFrames;
    }

    public void setPreEventFrames(int preEventFrames) {
        this.preEventFrames = preEventFrames;
    }

    public int getPostEventFrames() {
        return postEventFrames;
    }

    public void setPostEventFrames(int postEventFrames) {
        this.postEventFrames = postEventFrames;
    }

    public double getFallbackFps() {
        return fallbackFps;
    }

    public void setFallbackFps(double fallbackFps) {
        this.fallbackFps = fallbackFps;
    }

    public Duration getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(Duration commandTimeout) {
        this.commandTimeout = commandTimeout;
    }
}
