package com.backend.backend.service;

public class InvalidCredentialsException extends UserServiceException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
