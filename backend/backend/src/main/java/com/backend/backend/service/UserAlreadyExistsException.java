package com.backend.backend.service;

public class UserAlreadyExistsException extends UserServiceException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
