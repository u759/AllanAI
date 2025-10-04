package com.backend.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.backend.backend.config.VideoStorageProperties;

@Service
public class VideoStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoStorageService.class);
    private final Path rootLocation;

    public VideoStorageService(VideoStorageProperties properties) {
        this.rootLocation = properties.getVideoDirectory().toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize video storage directory", e);
        }
    }

    public String store(MultipartFile file, String matchId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "match.mp4";
        }
        String cleanedName = StringUtils.cleanPath(originalName);
        String safeFilename = cleanedName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        Path matchDirectory = rootLocation.resolve(matchId);
        try {
            Files.createDirectories(matchDirectory);
            Path destinationFile = matchDirectory.resolve(safeFilename).normalize();
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Stored video {} for match {}", destinationFile, matchId);
            return destinationFile.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store video file", ex);
        }
    }

    public Resource loadAsResource(String storedPath) {
        try {
            Path path = resolvePath(storedPath);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Video file not found: " + storedPath);
            }
            return resource;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read video file", e);
        }
    }

    public Path resolvePath(String storedPath) {
        Path path = Paths.get(storedPath);
        if (!path.isAbsolute()) {
            path = rootLocation.resolve(path).normalize();
        }
        return path;
    }

    public void delete(String storedPath) {
        try {
            Files.deleteIfExists(resolvePath(storedPath));
        } catch (IOException e) {
            LOGGER.warn("Failed to delete video file {}", storedPath, e);
        }
    }

    public void deleteMatchDirectory(String matchId) {
        Path matchDirectory = rootLocation.resolve(matchId);
        try {
            FileSystemUtils.deleteRecursively(matchDirectory);
        } catch (IOException e) {
            LOGGER.warn("Failed to remove match directory {}", matchDirectory, e);
        }
    }
}
