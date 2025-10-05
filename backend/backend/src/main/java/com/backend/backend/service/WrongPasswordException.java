package com.backend.backend.service;

public class WrongPasswordException extends UserServiceException {

    public WrongPasswordException() {
        super("Current password is incorrect");
    }
}
