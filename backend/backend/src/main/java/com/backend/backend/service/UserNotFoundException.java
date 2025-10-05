package com.backend.backend.service;

public class UserNotFoundException extends UserServiceException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
