package com.backend.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.backend.backend.repository.MatchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for MatchController API endpoints.
 * Tests the full flow: upload video -> process -> retrieve data
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MatchRepository matchRepository;

    private static final String TEST_VIDEO_PATH = "storage/videos/test_2.mp4";

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        matchRepository.deleteAll();
    }

    @Test
    void testUploadMatch_Success() throws Exception {
        // Load test video file
        Path videoPath = Paths.get(TEST_VIDEO_PATH);
        byte[] videoContent = Files.readAllBytes(videoPath);

        MockMultipartFile videoFile = new MockMultipartFile(
            "video",
            "test_2.mp4",
            "video/mp4",
            videoContent
        );

        // Upload video
        MvcResult result = mockMvc.perform(multipart("/api/matches/upload")
                .file(videoFile))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.matchId").exists())
            .andExpect(jsonPath("$.status").value("PROCESSING"))
            .andExpect(header().exists("Location"))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        String matchId = jsonNode.get("matchId").asText();

        assertThat(matchId).isNotNull();
        assertThat(matchId).isNotEmpty();
    }

    @Test
    void testListMatches_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/matches"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testFullMatchFlow() throws Exception {
        // Step 1: Upload video
        Path videoPath = Paths.get(TEST_VIDEO_PATH);
        byte[] videoContent = Files.readAllBytes(videoPath);

        MockMultipartFile videoFile = new MockMultipartFile(
            "video",
            "test_2.mp4",
            "video/mp4",
            videoContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/matches/upload")
                .file(videoFile))
            .andExpect(status().isAccepted())
            .andReturn();

        String uploadResponse = uploadResult.getResponse().getContentAsString();
        JsonNode uploadJson = objectMapper.readTree(uploadResponse);
        String matchId = uploadJson.get("matchId").asText();

        // Step 2: List matches - should contain 1 match
        mockMvc.perform(get("/api/matches"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(matchId));

        // Step 3: Get match details
        mockMvc.perform(get("/api/matches/{id}", matchId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(matchId))
            .andExpect(jsonPath("$.originalFilename").value("test_2.mp4"))
            .andExpect(jsonPath("$.status").exists());

        // Step 4: Get match status
        mockMvc.perform(get("/api/matches/{id}/status", matchId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(matchId))
            .andExpect(jsonPath("$.status").exists());

        // Step 5: Get statistics (may be null if processing not complete)
        mockMvc.perform(get("/api/matches/{id}/statistics", matchId))
            .andExpect(status().isOk());

        // Step 6: Get events (may be empty if processing not complete)
        mockMvc.perform(get("/api/matches/{id}/events", matchId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        // Step 7: Get highlights
        mockMvc.perform(get("/api/matches/{id}/highlights", matchId))
            .andExpect(status().isOk());

        // Step 8: Stream video
        mockMvc.perform(get("/api/matches/{id}/video", matchId))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Type"))
            .andExpect(header().string("Content-Disposition", "inline; filename=\"test_2.mp4\""));

        // Step 9: Delete match
        mockMvc.perform(delete("/api/matches/{id}", matchId))
            .andExpect(status().isNoContent());

        // Step 10: Verify deletion - should return 404
        mockMvc.perform(get("/api/matches/{id}", matchId))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetMatch_NotFound() throws Exception {
        String nonExistentId = "non-existent-id";

        mockMvc.perform(get("/api/matches/{id}", nonExistentId))
            .andExpect(status().isNotFound());
    }

    @Test
    void testUploadMatch_InvalidFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "video",
            "empty.mp4",
            "video/mp4",
            new byte[0]
        );

        // This should fail due to empty file validation
        mockMvc.perform(multipart("/api/matches/upload")
                .file(emptyFile))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void testGetStatistics_MatchExists() throws Exception {
        // Upload a match first
        Path videoPath = Paths.get(TEST_VIDEO_PATH);
        byte[] videoContent = Files.readAllBytes(videoPath);

        MockMultipartFile videoFile = new MockMultipartFile(
            "video",
            "test_2.mp4",
            "video/mp4",
            videoContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/matches/upload")
                .file(videoFile))
            .andExpect(status().isAccepted())
            .andReturn();

        String uploadResponse = uploadResult.getResponse().getContentAsString();
        JsonNode uploadJson = objectMapper.readTree(uploadResponse);
        String matchId = uploadJson.get("matchId").asText();

        // Get statistics
        mockMvc.perform(get("/api/matches/{id}/statistics", matchId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testGetEvents_MatchExists() throws Exception {
        // Upload a match first
        Path videoPath = Paths.get(TEST_VIDEO_PATH);
        byte[] videoContent = Files.readAllBytes(videoPath);

        MockMultipartFile videoFile = new MockMultipartFile(
            "video",
            "test_2.mp4",
            "video/mp4",
            videoContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/matches/upload")
                .file(videoFile))
            .andExpect(status().isAccepted())
            .andReturn();

        String uploadResponse = uploadResult.getResponse().getContentAsString();
        JsonNode uploadJson = objectMapper.readTree(uploadResponse);
        String matchId = uploadJson.get("matchId").asText();

        // Get events - should return empty array initially
        mockMvc.perform(get("/api/matches/{id}/events", matchId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testDeleteMatch_Success() throws Exception {
        // Upload a match first
        Path videoPath = Paths.get(TEST_VIDEO_PATH);
        byte[] videoContent = Files.readAllBytes(videoPath);

        MockMultipartFile videoFile = new MockMultipartFile(
            "video",
            "test_2.mp4",
            "video/mp4",
            videoContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/matches/upload")
                .file(videoFile))
            .andExpect(status().isAccepted())
            .andReturn();

        String uploadResponse = uploadResult.getResponse().getContentAsString();
        JsonNode uploadJson = objectMapper.readTree(uploadResponse);
        String matchId = uploadJson.get("matchId").asText();

        // Delete the match
        mockMvc.perform(delete("/api/matches/{id}", matchId))
            .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/matches/{id}", matchId))
            .andExpect(status().isNotFound());
    }
}
