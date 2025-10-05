package com.backend.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.backend.backend.dto.EventResponse;
import com.backend.backend.dto.MatchDetailsResponse;
import com.backend.backend.dto.MatchStatisticsResponse;
import com.backend.backend.dto.MatchStatusResponse;
import com.backend.backend.dto.MatchUploadResponse;
import com.backend.backend.model.MatchStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MatchPipelineIntegrationTest {

    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath().getParent();
    private static final Path TEST_VIDEO_DIR = PROJECT_ROOT.resolve("build/test-videos");
    private static final Path TEST_MODEL_OUTPUT_DIR = PROJECT_ROOT.resolve("build/test-model-output");
    private static final Path WEIGHTS_PATH = resolveWeightsPath();
    private static final Path TEST_VIDEO = resolveTestVideo();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.database", () -> "backend_test_" + UUID.randomUUID());
        registry.add("allanai.model.output-directory", () -> TEST_MODEL_OUTPUT_DIR.toString());
        registry.add("allanai.storage.video-directory", () -> TEST_VIDEO_DIR.toString());
        registry.add("allanai.model.working-directory", () -> PROJECT_ROOT.toString());
        registry.add("allanai.model.weights-path", () -> WEIGHTS_PATH.toString());
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void cleanOutputDirectories() throws IOException {
        FileSystemUtils.deleteRecursively(TEST_VIDEO_DIR);
        FileSystemUtils.deleteRecursively(TEST_MODEL_OUTPUT_DIR);
    }

    @Test
    void endToEndPipelineProcessesMatchAndStoresResults() throws Exception {
        Assumptions.assumeTrue(TEST_VIDEO != null && Files.exists(TEST_VIDEO),
            "Set ALLANAI_TEST_VIDEO environment variable to a real match video path.");
        Assumptions.assumeTrue(Files.exists(WEIGHTS_PATH),
            () -> "Model weights not found at " + WEIGHTS_PATH + ". Provide YOLO weights for integration test.");

        byte[] videoBytes = Files.readAllBytes(TEST_VIDEO);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("video", new org.springframework.core.io.ByteArrayResource(videoBytes) {
            @Override
            public String getFilename() {
                return TEST_VIDEO.getFileName().toString();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(parts, headers);

        ResponseEntity<MatchUploadResponse> uploadResponse = restTemplate.postForEntity(
            "/api/matches/upload",
            request,
            MatchUploadResponse.class
        );

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    MatchUploadResponse body = Objects.requireNonNull(uploadResponse.getBody(), "upload response body");
    String matchId = body.matchId();
        assertThat(matchId).isNotBlank();

        MatchStatusResponse finalStatus = waitForCompletion(matchId);
        assertThat(finalStatus.status()).isEqualTo(MatchStatus.COMPLETE);

        MatchDetailsResponse details = restTemplate.getForObject("/api/matches/{id}", MatchDetailsResponse.class, matchId);
        assertThat(details).isNotNull();

        MatchStatisticsResponse statistics = details.statistics();
        assertThat(statistics).isNotNull();
        assertThat(statistics.player1Score()).isGreaterThanOrEqualTo(1);
        assertThat(statistics.totalRallies()).isGreaterThanOrEqualTo(1);

        ResponseEntity<EventResponse[]> eventsResponse = restTemplate.getForEntity(
            "/api/matches/{id}/events",
            EventResponse[].class,
            matchId);
        assertThat(eventsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    EventResponse[] eventsBody = Objects.requireNonNull(eventsResponse.getBody(), "events body");
    assertThat(eventsBody).isNotEmpty();
    EventResponse firstEvent = eventsBody[0];
        assertThat(firstEvent.type()).isNotNull();
        assertThat(firstEvent.metadata()).isNotNull();
        assertThat(firstEvent.metadata().detections()).isNotEmpty();
    }

    private MatchStatusResponse waitForCompletion(String matchId) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(60);
        MatchStatusResponse status;
        while (true) {
            status = restTemplate.getForObject("/api/matches/{id}/status", MatchStatusResponse.class, matchId);
            assertThat(status).as("status response").isNotNull();
            if (status.status() == MatchStatus.COMPLETE) {
                return status;
            }
            if (status.status() == MatchStatus.FAILED) {
                fail("Match processing failed");
            }
            if (Instant.now().isAfter(deadline)) {
                fail("Timed out waiting for match to complete");
            }
            Thread.sleep(1000);
        }
    }

    private static Path resolveWeightsPath() {
        String override = System.getenv("YOLO_MODEL_WEIGHTS");
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        return PROJECT_ROOT.resolve("backend").resolve("best.pt").toAbsolutePath();
    }

    private static Path resolveTestVideo() {
        String override = System.getenv("ALLANAI_TEST_VIDEO");
        if (override == null || override.isBlank()) {
            return null;
        }
        return Paths.get(override).toAbsolutePath();
    }
}
