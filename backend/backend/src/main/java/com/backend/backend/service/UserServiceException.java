package com.backend.backend.service;

public abstract class UserServiceException extends RuntimeException {

    public UserServiceException(String message) {
        super(message);
    }
}
