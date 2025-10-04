package com.backend.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.backend.backend.model.MatchDocument;

public interface MatchRepository extends MongoRepository<MatchDocument, String> {
}
