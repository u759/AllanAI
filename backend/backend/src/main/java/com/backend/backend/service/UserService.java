package com.backend.backend.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.backend.backend.dto.AuthResponse;
import com.backend.backend.dto.ChangePasswordRequest;
import com.backend.backend.dto.SignInRequest;
import com.backend.backend.dto.SignUpRequest;
import com.backend.backend.dto.UpdateProfileRequest;
import com.backend.backend.dto.UserProfileResponse;
import com.backend.backend.model.UserDocument;
import com.backend.backend.repository.UserRepository;
import com.backend.backend.security.JwtService;
import com.backend.backend.security.JwtToken;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse registerUser(SignUpRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());

        validateUsernameAvailability(username, null);
        validateEmailAvailability(email, null);

        UserDocument user = new UserDocument();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserDocument saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    public AuthResponse authenticate(SignInRequest request) {
        String username = normalizeUsername(request.username());
        UserDocument user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new WrongPasswordException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String newUsername = normalizeUsername(request.username());
        String newEmail = normalizeEmail(request.email());

        validateUsernameAvailability(newUsername, userId);
        validateEmailAvailability(newEmail, userId);

        user.setUsername(newUsername);
        user.setEmail(newEmail);
        user.setUpdatedAt(Instant.now());

        UserDocument saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserProfileResponse getProfile(String userId) {
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toResponse(user);
    }

    private AuthResponse buildAuthResponse(UserDocument user) {
        JwtToken token = jwtService.generateToken(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), toResponse(user));
    }

    private void validateUsernameAvailability(String username, String currentUserId) {
        userRepository.findByUsername(username).ifPresent(existing -> {
            if (currentUserId == null || !existing.getId().equals(currentUserId)) {
                throw new UserAlreadyExistsException("Username already taken");
            }
        });
    }

    private void validateEmailAvailability(String email, String currentUserId) {
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (currentUserId == null || !existing.getId().equals(currentUserId)) {
                throw new UserAlreadyExistsException("Email already in use");
            }
        });
    }

    private String normalizeUsername(String username) {
        String trimmed = username == null ? null : username.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return trimmed;
    }

    private String normalizeEmail(String email) {
        String trimmed = email == null ? null : email.trim().toLowerCase();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        return trimmed;
    }

    private UserProfileResponse toResponse(UserDocument user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
