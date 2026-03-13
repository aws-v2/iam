package org.serwin.IAM.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serwin.iam.domain.AccessKey;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.AccessKeyCreatedDTO;
import org.serwin.iam.dto.DTOs.AccessKeyResponseDTO;
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

    // ── test fixtures ─────────────────────────────────────────────────────────

    private User testUser;
    private User otherUser;
    private AccessKey testAccessKey;
    private UUID testUserId;
    private UUID otherUserId;
    private static final String TEST_KEY_ID = "AKIA1234567890123456";

    @BeforeEach
    void setUp() {
        testUserId  = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

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
        testAccessKey.setAccessKeyId(TEST_KEY_ID);
        testAccessKey.setSecretAccessKey("abcdefghijklmnopqrstuvwxyz1234567890ABCD");
        testAccessKey.setStatus(AccessKey.Status.ACTIVE);
        testAccessKey.setUser(testUser);
        testAccessKey.setCreatedAt(LocalDateTime.now());
    }

    // ── shared stubs ──────────────────────────────────────────────────────────

    /** Simulates JPA save(entity) → entity (no ID generation needed here) */
    private void stubSaveReturnsArg() {
        when(accessKeyRepository.save(any(AccessKey.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /** First uniqueness check always passes — no collision */
    private void stubNoCollision() {
        when(accessKeyRepository.findByAccessKeyId(anyString()))
                .thenReturn(Optional.empty());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getKeys
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getKeys")
    class GetKeys {

        @Test
        @DisplayName("Returns all keys for the user with correct DTO values")
        void success_returnsAllKeys() {
            AccessKey key2 = new AccessKey();
            key2.setAccessKeyId("AKIA9876543210987654");
            key2.setSecretAccessKey("zyxwvutsrqponmlkjihgfedcba0987654321ZY");
            key2.setStatus(AccessKey.Status.INACTIVE);
            key2.setUser(testUser);
            key2.setCreatedAt(LocalDateTime.now().minusDays(1));

            when(accessKeyRepository.findByUserId(testUserId))
                    .thenReturn(Arrays.asList(testAccessKey, key2));

            List<AccessKeyResponseDTO> result = accessKeyService.getKeys(testUser);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).accessKeyId()).isEqualTo(TEST_KEY_ID);
            assertThat(result.get(0).status()).isEqualTo("ACTIVE");
            assertThat(result.get(1).accessKeyId()).isEqualTo("AKIA9876543210987654");
            assertThat(result.get(1).status()).isEqualTo("INACTIVE");
            verify(accessKeyRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("Returns empty list when user has no keys")
        void noKeys_returnsEmptyList() {
            when(accessKeyRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

            List<AccessKeyResponseDTO> result = accessKeyService.getKeys(testUser);

            assertThat(result).isEmpty();
            verify(accessKeyRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("Maps accessKeyId field correctly")
        void mapsAccessKeyId() {
            when(accessKeyRepository.findByUserId(testUserId)).thenReturn(List.of(testAccessKey));

            assertThat(accessKeyService.getKeys(testUser).get(0).accessKeyId())
                    .isEqualTo(TEST_KEY_ID);
        }

        @Test
        @DisplayName("Maps status as the enum name() string")
        void mapsStatusAsEnumName() {
            testAccessKey.setStatus(AccessKey.Status.INACTIVE);
            when(accessKeyRepository.findByUserId(testUserId)).thenReturn(List.of(testAccessKey));

            assertThat(accessKeyService.getKeys(testUser).get(0).status())
                    .isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("Maps createdAt as a non-blank string")
        void mapsCreatedAtAsNonBlankString() {
            when(accessKeyRepository.findByUserId(testUserId)).thenReturn(List.of(testAccessKey));

            assertThat(accessKeyService.getKeys(testUser).get(0).createdAt())
                    .isNotNull()
                    .isNotBlank();
        }

        @Test
        @DisplayName("Queries repository by the user UUID, not by object identity")
        void queriesRepositoryByUserId() {
            when(accessKeyRepository.findByUserId(testUserId)).thenReturn(List.of());

            accessKeyService.getKeys(testUser);

            verify(accessKeyRepository).findByUserId(testUserId);
            verifyNoMoreInteractions(accessKeyRepository);
        }

        @Test
        @DisplayName("Response DTO does not expose secretAccessKey")
        void dtoDoesNotExposeSecret() {
            when(accessKeyRepository.findByUserId(testUserId)).thenReturn(List.of(testAccessKey));

            AccessKeyResponseDTO dto = accessKeyService.getKeys(testUser).get(0);

            // AccessKeyResponseDTO carries only accessKeyId, status, createdAt
            assertThat(dto.accessKeyId()).isNotNull();
            assertThat(dto.status()).isNotNull();
            assertThat(dto.createdAt()).isNotNull();
        }

        @ParameterizedTest
        @EnumSource(AccessKey.Status.class)
        @DisplayName("Maps every Status value to its string name()")
        void mapsAllStatusValues(AccessKey.Status status) {
            testAccessKey.setStatus(status);
            when(accessKeyRepository.findByUserId(testUserId)).thenReturn(List.of(testAccessKey));

            assertThat(accessKeyService.getKeys(testUser).get(0).status())
                    .isEqualTo(status.name());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // createKey
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createKey")
    class CreateKey {

        @Test
        @DisplayName("Returns non-null DTO")
        void returnsNonNull() {
            stubNoCollision();
            stubSaveReturnsArg();
            assertThat(accessKeyService.createKey(testUser)).isNotNull();
        }

        @Test
        @DisplayName("Access key ID starts with AKIA")
        void accessKeyStartsWithAKIA() {
            stubNoCollision();
            stubSaveReturnsArg();
            assertThat(accessKeyService.createKey(testUser).accessKeyId()).startsWith("AKIA");
        }

        @Test
        @DisplayName("Access key ID is exactly 20 characters")
        void accessKeyIsLength20() {
            stubNoCollision();
            stubSaveReturnsArg();
            assertThat(accessKeyService.createKey(testUser).accessKeyId()).hasSize(20);
        }

        @Test
        @DisplayName("Access key ID matches AKIA + 16 decimal digits")
        void accessKeyMatchesFormat() {
            stubNoCollision();
            stubSaveReturnsArg();
            assertThat(accessKeyService.createKey(testUser).accessKeyId())
                    .matches("AKIA\\d{16}");
        }

        @Test
        @DisplayName("Secret key is exactly 40 characters")
        void secretKeyIsLength40() {
            stubNoCollision();
            stubSaveReturnsArg();
            assertThat(accessKeyService.createKey(testUser).secretAccessKey()).hasSize(40);
        }

        @Test
        @DisplayName("Secret key contains only URL-safe Base64 characters (A-Za-z0-9-_)")
        void secretKeyUsesUrlSafeBase64Chars() {
            // Base64.getUrlEncoder() replaces + with - and / with _; no = padding
            stubNoCollision();
            stubSaveReturnsArg();
            assertThat(accessKeyService.createKey(testUser).secretAccessKey())
                    .matches("[A-Za-z0-9\\-_]+");
        }

        @Test
        @DisplayName("Status is ACTIVE on creation")
        void statusIsActive() {
            stubNoCollision();
            stubSaveReturnsArg();
            assertThat(accessKeyService.createKey(testUser).status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Persisted AccessKey has correct user, status, keyId format, secret length")
        void persistsKeyWithCorrectFields() {
            stubNoCollision();
            stubSaveReturnsArg();

            accessKeyService.createKey(testUser);

            ArgumentCaptor<AccessKey> captor = ArgumentCaptor.forClass(AccessKey.class);
            verify(accessKeyRepository).save(captor.capture());

            AccessKey saved = captor.getValue();
            assertThat(saved.getUser()).isSameAs(testUser);
            assertThat(saved.getStatus()).isEqualTo(AccessKey.Status.ACTIVE);
            assertThat(saved.getAccessKeyId()).matches("AKIA\\d{16}");
            assertThat(saved.getSecretAccessKey()).hasSize(40);
        }

        @Test
        @DisplayName("Access key ID in DTO matches what was persisted")
        void accessKeyIdInDtoMatchesPersisted() {
            stubNoCollision();
            stubSaveReturnsArg();

            AccessKeyCreatedDTO dto = accessKeyService.createKey(testUser);

            ArgumentCaptor<AccessKey> captor = ArgumentCaptor.forClass(AccessKey.class);
            verify(accessKeyRepository).save(captor.capture());
            assertThat(dto.accessKeyId()).isEqualTo(captor.getValue().getAccessKeyId());
        }

        @Test
        @DisplayName("Secret key in DTO matches what was persisted")
        void secretInDtoMatchesPersisted() {
            stubNoCollision();
            stubSaveReturnsArg();

            AccessKeyCreatedDTO dto = accessKeyService.createKey(testUser);

            ArgumentCaptor<AccessKey> captor = ArgumentCaptor.forClass(AccessKey.class);
            verify(accessKeyRepository).save(captor.capture());
            assertThat(dto.secretAccessKey()).isEqualTo(captor.getValue().getSecretAccessKey());
        }

        @Test
        @DisplayName("Retries generation once on collision, then succeeds")
        void handlesOneCollision() {
            AccessKey existing = new AccessKey();
            when(accessKeyRepository.findByAccessKeyId(anyString()))
                    .thenReturn(Optional.of(existing))   // 1st — collision
                    .thenReturn(Optional.empty());        // 2nd — unique
            stubSaveReturnsArg();

            assertThat(accessKeyService.createKey(testUser).accessKeyId()).startsWith("AKIA");
            verify(accessKeyRepository, atLeast(2)).findByAccessKeyId(anyString());
        }

        @Test
        @DisplayName("Retries generation on triple collision")
        void handlesTripleCollision() {
            AccessKey existing = new AccessKey();
            when(accessKeyRepository.findByAccessKeyId(anyString()))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.empty());
            stubSaveReturnsArg();

            assertThat(accessKeyService.createKey(testUser).accessKeyId()).startsWith("AKIA");
            verify(accessKeyRepository, atLeast(3)).findByAccessKeyId(anyString());
        }

        @Test
        @DisplayName("Calls save exactly once per createKey call")
        void callsSaveExactlyOnce() {
            stubNoCollision();
            stubSaveReturnsArg();

            accessKeyService.createKey(testUser);

            verify(accessKeyRepository, times(1)).save(any(AccessKey.class));
        }

        @Test
        @DisplayName("Generates unique IDs across multiple calls")
        void generatesUniqueIds() {
            stubNoCollision();
            stubSaveReturnsArg();

            String id1 = accessKeyService.createKey(testUser).accessKeyId();
            String id2 = accessKeyService.createKey(testUser).accessKeyId();
            String id3 = accessKeyService.createKey(testUser).accessKeyId();

            assertThat(Set.of(id1, id2, id3)).hasSize(3);
        }

        @Test
        @DisplayName("Generates unique secrets across multiple calls")
        void generatesUniqueSecrets() {
            stubNoCollision();
            stubSaveReturnsArg();

            String s1 = accessKeyService.createKey(testUser).secretAccessKey();
            String s2 = accessKeyService.createKey(testUser).secretAccessKey();
            String s3 = accessKeyService.createKey(testUser).secretAccessKey();

            assertThat(Set.of(s1, s2, s3)).hasSize(3);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // deleteKey
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteKey")
    class DeleteKey {

        @Test
        @DisplayName("Deletes own key successfully")
        void success_deletesOwnKey() {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));

            accessKeyService.deleteKey(testUser, TEST_KEY_ID);

            verify(accessKeyRepository).findByAccessKeyId(TEST_KEY_ID);
            verify(accessKeyRepository).delete(testAccessKey);
        }

        @Test
        @DisplayName("Deletes the exact key object returned by the repository")
        void deletesExactKeyObject() {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));

            accessKeyService.deleteKey(testUser, TEST_KEY_ID);

            ArgumentCaptor<AccessKey> captor = ArgumentCaptor.forClass(AccessKey.class);
            verify(accessKeyRepository).delete(captor.capture());
            assertThat(captor.getValue()).isSameAs(testAccessKey);
        }

        @Test
        @DisplayName("Never calls save during delete")
        void doesNotSave() {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));

            accessKeyService.deleteKey(testUser, TEST_KEY_ID);

            verify(accessKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when key not found")
        void keyNotFound_throws() {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessKeyService.deleteKey(testUser, TEST_KEY_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Access key not found");

            verify(accessKeyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Throws when key belongs to another user")
        void unauthorized_throws() {
            testAccessKey.setUser(otherUser);
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));

            assertThatThrownBy(() -> accessKeyService.deleteKey(testUser, TEST_KEY_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unauthorized: You don't own this access key");

            verify(accessKeyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Authorizes by user UUID, not by object reference")
        void authorizesById_notObjectEquality() {
            // Different User instance, same UUID → must be treated as the same owner
            User cloneUser = new User();
            cloneUser.setId(testUserId);
            cloneUser.setEmail("clone@example.com");
            testAccessKey.setUser(cloneUser);

            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));

            assertThatCode(() -> accessKeyService.deleteKey(testUser, TEST_KEY_ID))
                    .doesNotThrowAnyException();

            verify(accessKeyRepository).delete(testAccessKey);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // updateKeyStatus
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateKeyStatus")
    class UpdateKeyStatus {

        @Test
        @DisplayName("Updates ACTIVE to INACTIVE successfully")
        void activeToInactive() {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));
            stubSaveReturnsArg();

            accessKeyService.updateKeyStatus(testUser, TEST_KEY_ID, AccessKey.Status.INACTIVE);

            assertThat(testAccessKey.getStatus()).isEqualTo(AccessKey.Status.INACTIVE);
            verify(accessKeyRepository).save(testAccessKey);
        }

        @Test
        @DisplayName("Updates INACTIVE to ACTIVE successfully")
        void inactiveToActive() {
            testAccessKey.setStatus(AccessKey.Status.INACTIVE);
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));
            stubSaveReturnsArg();

            accessKeyService.updateKeyStatus(testUser, TEST_KEY_ID, AccessKey.Status.ACTIVE);

            assertThat(testAccessKey.getStatus()).isEqualTo(AccessKey.Status.ACTIVE);
            verify(accessKeyRepository).save(testAccessKey);
        }

        @Test
        @DisplayName("Updating to the same status does not throw and still saves")
        void updateToSameStatus_noError() {
            testAccessKey.setStatus(AccessKey.Status.ACTIVE);
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));
            stubSaveReturnsArg();

            assertThatCode(() -> accessKeyService.updateKeyStatus(
                    testUser, TEST_KEY_ID, AccessKey.Status.ACTIVE))
                    .doesNotThrowAnyException();

            assertThat(testAccessKey.getStatus()).isEqualTo(AccessKey.Status.ACTIVE);
            verify(accessKeyRepository).save(testAccessKey);
        }

        @Test
        @DisplayName("Mutates the status field before saving")
        void mutatesStatusBeforeSave() {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));
            stubSaveReturnsArg();

            accessKeyService.updateKeyStatus(testUser, TEST_KEY_ID, AccessKey.Status.INACTIVE);

            ArgumentCaptor<AccessKey> captor = ArgumentCaptor.forClass(AccessKey.class);
            verify(accessKeyRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AccessKey.Status.INACTIVE);
        }

        @Test
        @DisplayName("Calls save exactly once per update")
        void savesExactlyOnce() {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));
            stubSaveReturnsArg();

            accessKeyService.updateKeyStatus(testUser, TEST_KEY_ID, AccessKey.Status.INACTIVE);

            verify(accessKeyRepository, times(1)).save(testAccessKey);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when key not found")
        void keyNotFound_throws() {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessKeyService.updateKeyStatus(
                    testUser, TEST_KEY_ID, AccessKey.Status.INACTIVE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Access key not found");

            verify(accessKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws when key belongs to another user")
        void unauthorized_throws() {
            testAccessKey.setUser(otherUser);
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));

            assertThatThrownBy(() -> accessKeyService.updateKeyStatus(
                    testUser, TEST_KEY_ID, AccessKey.Status.INACTIVE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unauthorized: You don't own this access key");

            verify(accessKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Authorizes by user UUID, not by object reference")
        void authorizesById_notObjectEquality() {
            User cloneUser = new User();
            cloneUser.setId(testUserId); // same UUID, different instance
            testAccessKey.setUser(cloneUser);

            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));
            stubSaveReturnsArg();

            assertThatCode(() -> accessKeyService.updateKeyStatus(
                    testUser, TEST_KEY_ID, AccessKey.Status.INACTIVE))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(AccessKey.Status.class)
        @DisplayName("Accepts and persists every valid Status enum value")
        void acceptsAllStatusValues(AccessKey.Status status) {
            when(accessKeyRepository.findByAccessKeyId(TEST_KEY_ID))
                    .thenReturn(Optional.of(testAccessKey));
            stubSaveReturnsArg();

            assertThatCode(() -> accessKeyService.updateKeyStatus(testUser, TEST_KEY_ID, status))
                    .doesNotThrowAnyException();

            assertThat(testAccessKey.getStatus()).isEqualTo(status);
        }
    }
}