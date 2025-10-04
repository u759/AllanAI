package com.backend.backend.service;

import com.backend.backend.config.ModelProperties;
import com.backend.backend.service.model.ModelInferenceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ModelEventDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelEventDetectionService.class);

    private final ModelProperties properties;
    private final ObjectMapper objectMapper;

    public ModelEventDetectionService(ModelProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<ModelInferenceResult> detect(String matchId, Path videoPath) {
        if (!properties.isEnabled()) {
            LOGGER.debug("Research model integration disabled; falling back to heuristic processing");
            return Optional.empty();
        }

        try {
            Path outputDirectory = properties.getOutputDirectory().resolve(matchId);
            Files.createDirectories(outputDirectory);

            Map<String, String> placeholders = createPlaceholders(matchId, videoPath, outputDirectory);
            runExternalCommand(placeholders);

            Path resultFile = outputDirectory.resolve(resolvePlaceholders(properties.getResultFileName(), placeholders));
            if (!Files.exists(resultFile)) {
                LOGGER.warn("Model command completed but result file {} is missing", resultFile);
                return Optional.empty();
            }

            ModelInferenceResult rawResult = objectMapper.readValue(resultFile.toFile(), ModelInferenceResult.class);
            ModelInferenceResult normalized = rawResult.normalize(properties);
            LOGGER.info("Loaded {} events and {} shots from research model output for match {}", normalized.getEvents().size(), normalized.getShots().size(), matchId);
            return Optional.of(normalized);
        } catch (IOException ex) {
            LOGGER.error("Failed to execute research model pipeline", ex);
            return Optional.empty();
        }
    }

    private Map<String, String> createPlaceholders(String matchId, Path videoPath, Path outputDirectory) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("matchId", matchId);
        placeholders.put("video", videoPath.toAbsolutePath().toString());
        placeholders.put("outputDir", outputDirectory.toAbsolutePath().toString());
        placeholders.put("weights", properties.getWeightsPath().toAbsolutePath().toString());
        placeholders.put("confidence", Double.toString(properties.getConfidenceThreshold()));
        return placeholders;
    }

    private void runExternalCommand(Map<String, String> placeholders) throws IOException {
        List<String> commandTemplate = properties.getCommand();
        if (commandTemplate == null || commandTemplate.isEmpty()) {
            LOGGER.debug("No external command configured; expecting inference JSON to exist already");
            return;
        }

        List<String> resolvedCommand = new ArrayList<>();
        for (String token : commandTemplate) {
            resolvedCommand.add(resolvePlaceholders(token, placeholders));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(resolvedCommand);
        processBuilder.directory(properties.getWorkingDirectory().toFile());
        processBuilder.redirectErrorStream(true);
        LOGGER.info("Executing research model command: {} (cwd={})", String.join(" ", resolvedCommand), properties.getWorkingDirectory());

        Process process = processBuilder.start();
        Duration timeout = properties.getCommandTimeout();
        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IOException("Model command interrupted", interruptedException);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException(String.format(Locale.US, "Model command timed out after %d seconds", timeout.toSeconds()));
        }

        int exitCode = process.exitValue();
        String output = captureOutput(process);
        if (exitCode != 0) {
            throw new IOException("Model command failed with exit code " + exitCode + " and output:\n" + output);
        }

        if (!output.isBlank()) {
            LOGGER.debug("Research model output:\n{}", output);
        }
    }

    private String captureOutput(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private String resolvePlaceholders(String token, Map<String, String> placeholders) {
        String resolved = token;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return resolved;
    }
}
