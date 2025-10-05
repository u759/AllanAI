package com.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(@NotBlank @Size(min = 3, max = 50) String username,
                             @NotBlank @Email String email,
                             @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters.") String password) {
}
