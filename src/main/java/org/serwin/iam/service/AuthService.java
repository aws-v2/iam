package org.serwin.iam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.UserRepository;
import org.serwin.iam.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: Email already in use - {}", request.email());
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        userRepository.save(user);
        log.info("User registered successfully: {}", request.email());

        String token = jwtUtil.generateToken(user.getId());
        return new AuthResponseDTO(token, "User registered successfully", user.getId().toString());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed: Invalid credentials for email - {}", request.email());
                    return new IllegalArgumentException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for email - {}", request.email());
            throw new IllegalArgumentException("Invalid credentials");
        }

        log.info("Login successful: {}", request.email());
        String token = jwtUtil.generateToken(user.getId());
        return new AuthResponseDTO(token, "Login successful", user.getId().toString());
    }

    public User getUserFromToken(String token) {
        // Simple helper to bridge raw token -> User entity for controllers
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        var userId = jwtUtil.validateTokenAndGetUserId(token);
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
