package com.backend.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.backend.backend.dto.EventResponse;
import com.backend.backend.dto.HighlightsResponse;
import com.backend.backend.dto.MatchDetailsResponse;
import com.backend.backend.dto.MatchStatisticsResponse;
import com.backend.backend.dto.MatchStatusResponse;
import com.backend.backend.dto.MatchSummaryResponse;
import com.backend.backend.dto.MatchUploadResponse;
import com.backend.backend.mapper.MatchMapper;
import com.backend.backend.model.MatchDocument;
import com.backend.backend.model.MatchStatus;
import com.backend.backend.service.MatchProcessingService;
import com.backend.backend.service.MatchService;
import com.backend.backend.service.VideoStorageService;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchController.class);

    private final MatchService matchService;
    private final MatchProcessingService matchProcessingService;
    private final VideoStorageService videoStorageService;

    public MatchController(MatchService matchService,
                           MatchProcessingService matchProcessingService,
                           VideoStorageService videoStorageService) {
        this.matchService = matchService;
        this.matchProcessingService = matchProcessingService;
        this.videoStorageService = videoStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MatchUploadResponse> uploadMatch(@RequestParam("video") MultipartFile video,
                                                           UriComponentsBuilder uriBuilder) {
        String matchId = UUID.randomUUID().toString();
        String storedPath = videoStorageService.store(video, matchId);
        MatchDocument match = matchService.createMatch(matchId, video.getOriginalFilename(), storedPath);
        matchProcessingService.processAsync(match.getId());
        URI location = uriBuilder.path("/api/matches/{id}").buildAndExpand(match.getId()).toUri();
        MatchUploadResponse response = new MatchUploadResponse(match.getId(), MatchStatus.PROCESSING);
        return ResponseEntity.accepted().location(location).body(response);
    }

    @GetMapping
    public List<MatchSummaryResponse> listMatches() {
        return matchService.listMatches().stream().map(MatchMapper::toSummary).toList();
    }

    @GetMapping("/{id}")
    public MatchDetailsResponse getMatch(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        return MatchMapper.toDetails(match);
    }

    @GetMapping("/{id}/status")
    public MatchStatusResponse getStatus(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        return new MatchStatusResponse(match.getId(), match.getStatus(), match.getProcessedAt());
    }

    @GetMapping("/{id}/statistics")
    public MatchStatisticsResponse getStatistics(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        return MatchMapper.toStatistics(match.getStatistics());
    }

    @GetMapping("/{id}/events")
    public List<EventResponse> getEvents(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        if (match.getEvents() == null) {
            return List.of();
        }
        return match.getEvents().stream().map(MatchMapper::toEvent).toList();
    }

    @GetMapping("/{id}/highlights")
    public HighlightsResponse getHighlights(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        return MatchMapper.toHighlights(match.getHighlights());
    }

    @GetMapping("/{id}/video")
    public ResponseEntity<Resource> streamVideo(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        Resource video = videoStorageService.loadAsResource(match.getVideoPath());
        MediaType mediaType = MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + match.getOriginalFilename() + "\"")
            .body(video);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        if (match.getVideoPath() != null) {
            videoStorageService.delete(match.getVideoPath());
            videoStorageService.deleteMatchDirectory(id);
        }
        matchService.deleteMatch(match);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException exception) {
        LOGGER.warn("API error: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
