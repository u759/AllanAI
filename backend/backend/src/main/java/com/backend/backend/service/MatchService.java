package com.backend.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.backend.backend.model.MatchDocument;
import com.backend.backend.model.MatchStatus;
import com.backend.backend.repository.MatchRepository;

@Service
public class MatchService {

    private final MatchRepository repository;

    public MatchService(MatchRepository repository) {
        this.repository = repository;
    }

    public MatchDocument createMatch(String matchId, String originalFilename, String videoPath) {
        MatchDocument match = new MatchDocument();
        match.setId(matchId);
        match.setCreatedAt(Instant.now());
        match.setStatus(MatchStatus.UPLOADED);
        match.setOriginalFilename(originalFilename);
        match.setVideoPath(videoPath);
        return repository.save(match);
    }

    public MatchDocument getById(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Match not found: " + id));
    }

    public List<MatchDocument> listMatches() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public MatchDocument save(MatchDocument match) {
        return repository.save(match);
    }

    public void updateStatus(String id, MatchStatus status) {
        MatchDocument match = getById(id);
        match.setStatus(status);
        if (status == MatchStatus.PROCESSING) {
            match.setProcessedAt(null);
        }
        repository.save(match);
    }

    public void deleteMatch(MatchDocument match) {
        repository.delete(match);
    }
}
