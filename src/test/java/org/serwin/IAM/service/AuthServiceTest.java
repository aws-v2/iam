package org.serwin.IAM.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.UserRepository;
import org.serwin.iam.service.AuthService;
import org.serwin.iam.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UUID testUserId;
    private String testEmail;
    private String testPassword;
    private String testPasswordHash;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testPassword = "password123";
        testPasswordHash = "hashedPassword";
        testToken = "jwt.token.here";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail(testEmail);
        testUser.setPasswordHash(testPasswordHash);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO(testEmail, testPassword);
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn(testPasswordHash);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(testUserId);
            return savedUser;
        });
        when(jwtUtil.generateToken(testUserId)).thenReturn(testToken);

        // Act
        AuthResponseDTO response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(testToken);
        assertThat(response.message()).isEqualTo("User registered successfully");

        verify(userRepository).existsByEmail(testEmail);
        verify(passwordEncoder).encode(testPassword);
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when email already exists during registration")
    void register_EmailAlreadyExists() {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO(testEmail, testPassword);
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");

        verify(userRepository).existsByEmail(testEmail);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void login_Success() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(testEmail, testPassword);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, testPasswordHash)).thenReturn(true);
        when(jwtUtil.generateToken(testUserId)).thenReturn(testToken);

        // Act
        AuthResponseDTO response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(testToken);
        assertThat(response.message()).isEqualTo("Login successful");

        verify(userRepository).findByEmail(testEmail);
        verify(passwordEncoder).matches(testPassword, testPasswordHash);
        verify(jwtUtil).generateToken(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void login_UserNotFound() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(testEmail, testPassword);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");

        verify(userRepository).findByEmail(testEmail);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw exception when password is invalid during login")
    void login_InvalidPassword() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(testEmail, "wrongPassword");
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testPasswordHash)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");

        verify(userRepository).findByEmail(testEmail);
        verify(passwordEncoder).matches("wrongPassword", testPasswordHash);
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should successfully get user from valid token")
    void getUserFromToken_Success() {
        // Arrange
        String tokenWithBearer = "Bearer " + testToken;
        when(jwtUtil.validateTokenAndGetUserId(testToken)).thenReturn(testUserId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        User result = authService.getUserFromToken(tokenWithBearer);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getEmail()).isEqualTo(testEmail);

        verify(jwtUtil).validateTokenAndGetUserId(testToken);
        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName("Should successfully get user from token without Bearer prefix")
    void getUserFromToken_WithoutBearer() {
        // Arrange
        when(jwtUtil.validateTokenAndGetUserId(testToken)).thenReturn(testUserId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        User result = authService.getUserFromToken(testToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);

        verify(jwtUtil).validateTokenAndGetUserId(testToken);
        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when user not found for valid token")
    void getUserFromToken_UserNotFound() {
        // Arrange
        when(jwtUtil.validateTokenAndGetUserId(testToken)).thenReturn(testUserId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.getUserFromToken(testToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");

        verify(jwtUtil).validateTokenAndGetUserId(testToken);
        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void getUserFromToken_NullToken() {
        // Arrange
        when(jwtUtil.validateTokenAndGetUserId(null)).thenReturn(testUserId);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        User result = authService.getUserFromToken(null);

        // Assert
        assertThat(result).isNotNull();
        verify(jwtUtil).validateTokenAndGetUserId(null);
    }

    @Test
    @DisplayName("Should verify password encoding during registration")
    void register_PasswordEncodingVerification() {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO(testEmail, testPassword);
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn(testPasswordHash);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getEmail()).isEqualTo(testEmail);
            assertThat(savedUser.getPasswordHash()).isEqualTo(testPasswordHash);
            savedUser.setId(testUserId);
            return savedUser;
        });
        when(jwtUtil.generateToken(testUserId)).thenReturn(testToken);

        // Act
        authService.register(request);

        // Assert
        verify(passwordEncoder).encode(testPassword);
    }

    @Test
    @DisplayName("Should verify JWT token is generated with correct user ID on login")
    void login_TokenGenerationWithCorrectUserId() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(testEmail, testPassword);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, testPasswordHash)).thenReturn(true);
        when(jwtUtil.generateToken(testUserId)).thenReturn(testToken);

        // Act
        authService.login(request);

        // Assert
        verify(jwtUtil).generateToken(testUserId);
    }
}
