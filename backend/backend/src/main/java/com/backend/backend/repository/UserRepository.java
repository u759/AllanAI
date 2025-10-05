package com.backend.backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.backend.backend.model.UserDocument;

public interface UserRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByUsername(String username);

    Optional<UserDocument> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
