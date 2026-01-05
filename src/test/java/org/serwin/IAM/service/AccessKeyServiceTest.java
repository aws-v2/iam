package org.serwin.IAM.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serwin.iam.domain.AccessKey;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.AccessKeyRepository;
import org.serwin.iam.service.AccessKeyService;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessKeyService Unit Tests")
class AccessKeyServiceTest {

    @Mock
    private AccessKeyRepository accessKeyRepository;

    @InjectMocks
    private AccessKeyService accessKeyService;

    private User testUser;
    private User otherUser;
    private AccessKey testAccessKey;
    private UUID testUserId;
    private UUID otherUserId;
    private String testAccessKeyId;
    private String testSecretKey;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        testAccessKeyId = "AKIA1234567890123456";
        testSecretKey = "abcdefghijklmnopqrstuvwxyz1234567890ABCD";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hash");
        testUser.setCreatedAt(LocalDateTime.now());

        otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash("hash");
        otherUser.setCreatedAt(LocalDateTime.now());

        testAccessKey = new AccessKey();
        testAccessKey.setAccessKeyId(testAccessKeyId);
        testAccessKey.setSecretAccessKey(testSecretKey);
        testAccessKey.setStatus(AccessKey.Status.ACTIVE);
        testAccessKey.setUser(testUser);
        testAccessKey.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully retrieve all keys for a user")
    void getKeys_Success() {
        // Arrange
        AccessKey key2 = new AccessKey();
        key2.setAccessKeyId("AKIA9876543210987654");
        key2.setSecretAccessKey("zyxwvutsrqponmlkjihgfedcba0987654321");
        key2.setStatus(AccessKey.Status.INACTIVE);
        key2.setUser(testUser);
        key2.setCreatedAt(LocalDateTime.now().minusDays(1));

        List<AccessKey> keys = Arrays.asList(testAccessKey, key2);
        when(accessKeyRepository.findByUserId(testUserId)).thenReturn(keys);

        // Act
        List<AccessKeyResponseDTO> result = accessKeyService.getKeys(testUser);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).accessKeyId()).isEqualTo(testAccessKeyId);
        assertThat(result.get(0).status()).isEqualTo("ACTIVE");
        assertThat(result.get(1).accessKeyId()).isEqualTo("AKIA9876543210987654");
        assertThat(result.get(1).status()).isEqualTo("INACTIVE");

        verify(accessKeyRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should return empty list when user has no keys")
    void getKeys_NoKeys() {
        // Arrange
        when(accessKeyRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

        // Act
        List<AccessKeyResponseDTO> result = accessKeyService.getKeys(testUser);

        // Assert
        assertThat(result).isEmpty();
        verify(accessKeyRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should successfully create a new access key")
    void createKey_Success() {
        // Arrange
        when(accessKeyRepository.findByAccessKeyId(anyString())).thenReturn(Optional.empty());
        when(accessKeyRepository.save(any(AccessKey.class))).thenAnswer(invocation -> {
            AccessKey savedKey = invocation.getArgument(0);
            savedKey.setCreatedAt(LocalDateTime.now());
            return savedKey;
        });

        // Act
        AccessKeyCreatedDTO result = accessKeyService.createKey(testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.accessKeyId()).startsWith("AKIA");
        assertThat(result.accessKeyId()).hasSize(20);
        assertThat(result.secretAccessKey()).hasSize(40);
        assertThat(result.status()).isEqualTo("ACTIVE");

        verify(accessKeyRepository).save(argThat(key ->
                key.getAccessKeyId().startsWith("AKIA") &&
                key.getAccessKeyId().length() == 20 &&
                key.getSecretAccessKey().length() == 40 &&
                key.getStatus() == AccessKey.Status.ACTIVE &&
                key.getUser().equals(testUser)
        ));
    }

    @Test
    @DisplayName("Should generate unique access key ID when collision occurs")
    void createKey_HandleCollision() {
        // Arrange
        AccessKey existingKey = new AccessKey();
        existingKey.setAccessKeyId("AKIA0000000000000000");

        // First call returns existing key (collision), second call returns empty (unique)
        when(accessKeyRepository.findByAccessKeyId(anyString()))
                .thenReturn(Optional.of(existingKey))
                .thenReturn(Optional.empty());
        when(accessKeyRepository.save(any(AccessKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccessKeyCreatedDTO result = accessKeyService.createKey(testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.accessKeyId()).startsWith("AKIA");
        verify(accessKeyRepository, atLeast(2)).findByAccessKeyId(anyString());
    }

    @Test
    @DisplayName("Should successfully delete own access key")
    void deleteKey_Success() {
        // Arrange
        when(accessKeyRepository.findByAccessKeyId(testAccessKeyId)).thenReturn(Optional.of(testAccessKey));

        // Act
        accessKeyService.deleteKey(testUser, testAccessKeyId);

        // Assert
        verify(accessKeyRepository).findByAccessKeyId(testAccessKeyId);
        verify(accessKeyRepository).delete(testAccessKey);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent key")
    void deleteKey_KeyNotFound() {
        // Arrange
        when(accessKeyRepository.findByAccessKeyId(testAccessKeyId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accessKeyService.deleteKey(testUser, testAccessKeyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access key not found");

        verify(accessKeyRepository).findByAccessKeyId(testAccessKeyId);
        verify(accessKeyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when deleting another user's key")
    void deleteKey_Unauthorized() {
        // Arrange
        testAccessKey.setUser(otherUser);
        when(accessKeyRepository.findByAccessKeyId(testAccessKeyId)).thenReturn(Optional.of(testAccessKey));

        // Act & Assert
        assertThatThrownBy(() -> accessKeyService.deleteKey(testUser, testAccessKeyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unauthorized: You don't own this access key");

        verify(accessKeyRepository).findByAccessKeyId(testAccessKeyId);
        verify(accessKeyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should successfully update own key status to INACTIVE")
    void updateKeyStatus_ToInactive() {
        // Arrange
        when(accessKeyRepository.findByAccessKeyId(testAccessKeyId)).thenReturn(Optional.of(testAccessKey));
        when(accessKeyRepository.save(any(AccessKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accessKeyService.updateKeyStatus(testUser, testAccessKeyId, AccessKey.Status.INACTIVE);

        // Assert
        assertThat(testAccessKey.getStatus()).isEqualTo(AccessKey.Status.INACTIVE);
        verify(accessKeyRepository).findByAccessKeyId(testAccessKeyId);
        verify(accessKeyRepository).save(testAccessKey);
    }

    @Test
    @DisplayName("Should successfully update own key status to ACTIVE")
    void updateKeyStatus_ToActive() {
        // Arrange
        testAccessKey.setStatus(AccessKey.Status.INACTIVE);
        when(accessKeyRepository.findByAccessKeyId(testAccessKeyId)).thenReturn(Optional.of(testAccessKey));
        when(accessKeyRepository.save(any(AccessKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accessKeyService.updateKeyStatus(testUser, testAccessKeyId, AccessKey.Status.ACTIVE);

        // Assert
        assertThat(testAccessKey.getStatus()).isEqualTo(AccessKey.Status.ACTIVE);
        verify(accessKeyRepository).save(testAccessKey);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent key status")
    void updateKeyStatus_KeyNotFound() {
        // Arrange
        when(accessKeyRepository.findByAccessKeyId(testAccessKeyId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accessKeyService.updateKeyStatus(testUser, testAccessKeyId, AccessKey.Status.INACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access key not found");

        verify(accessKeyRepository).findByAccessKeyId(testAccessKeyId);
        verify(accessKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when updating another user's key status")
    void updateKeyStatus_Unauthorized() {
        // Arrange
        testAccessKey.setUser(otherUser);
        when(accessKeyRepository.findByAccessKeyId(testAccessKeyId)).thenReturn(Optional.of(testAccessKey));

        // Act & Assert
        assertThatThrownBy(() -> accessKeyService.updateKeyStatus(testUser, testAccessKeyId, AccessKey.Status.INACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unauthorized: You don't own this access key");

        verify(accessKeyRepository).findByAccessKeyId(testAccessKeyId);
        verify(accessKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate access key ID with correct format")
    void createKey_AccessKeyIdFormat() {
        // Arrange
        when(accessKeyRepository.findByAccessKeyId(anyString())).thenReturn(Optional.empty());
        when(accessKeyRepository.save(any(AccessKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccessKeyCreatedDTO result = accessKeyService.createKey(testUser);

        // Assert
        assertThat(result.accessKeyId())
                .startsWith("AKIA")
                .hasSize(20)
                .matches("AKIA\\d{16}");
    }

    @Test
    @DisplayName("Should generate secret key with correct length")
    void createKey_SecretKeyFormat() {
        // Arrange
        when(accessKeyRepository.findByAccessKeyId(anyString())).thenReturn(Optional.empty());
        when(accessKeyRepository.save(any(AccessKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccessKeyCreatedDTO result = accessKeyService.createKey(testUser);

        // Assert
        assertThat(result.secretAccessKey()).hasSize(40);
    }

    @Test
    @DisplayName("Should verify created keys contain timestamps")
    void getKeys_ContainsTimestamp() {
        // Arrange
        when(accessKeyRepository.findByUserId(testUserId)).thenReturn(Collections.singletonList(testAccessKey));

        // Act
        List<AccessKeyResponseDTO> result = accessKeyService.getKeys(testUser);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).createdAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create multiple unique keys for the same user")
    void createKey_MultipleKeysAreUnique() {
        // Arrange
        when(accessKeyRepository.findByAccessKeyId(anyString())).thenReturn(Optional.empty());
        when(accessKeyRepository.save(any(AccessKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccessKeyCreatedDTO key1 = accessKeyService.createKey(testUser);
        AccessKeyCreatedDTO key2 = accessKeyService.createKey(testUser);
        AccessKeyCreatedDTO key3 = accessKeyService.createKey(testUser);

        // Assert
        assertThat(key1.accessKeyId()).isNotEqualTo(key2.accessKeyId());
        assertThat(key1.accessKeyId()).isNotEqualTo(key3.accessKeyId());
        assertThat(key2.accessKeyId()).isNotEqualTo(key3.accessKeyId());

        assertThat(key1.secretAccessKey()).isNotEqualTo(key2.secretAccessKey());
        assertThat(key1.secretAccessKey()).isNotEqualTo(key3.secretAccessKey());
        assertThat(key2.secretAccessKey()).isNotEqualTo(key3.secretAccessKey());
    }
}
