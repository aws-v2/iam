package org.serwin.iam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.domain.AccessKey;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.AccessKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessKeyService {

    private final AccessKeyRepository accessKeyRepository;

    @Transactional(readOnly = true)
    public List<AccessKeyResponseDTO> getKeys(User user) {
        return accessKeyRepository.findByUserId(user.getId()).stream()
                .map(key -> new AccessKeyResponseDTO(
                        key.getAccessKeyId(),
                        key.getStatus().name(),
                        key.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public AccessKeyCreatedDTO createKey(User user) {
        String accessKeyId = generateAccessKeyId();
        String secretKey = generateSecretKey();

        AccessKey key = new AccessKey();
        key.setAccessKeyId(accessKeyId);
        key.setSecretAccessKey(secretKey);
        key.setStatus(AccessKey.Status.ACTIVE);
        key.setUser(user);

        accessKeyRepository.save(key);
        log.info("Access key created for user: {} (keyId: {})", user.getId(), accessKeyId);

        return new AccessKeyCreatedDTO(accessKeyId, secretKey, "ACTIVE");
    }

    private String generateAccessKeyId() {
        // "AKIA" + 16 numeric digits = 20 chars
        // Check for uniqueness to prevent collisions
        String keyId;
        do {
            StringBuilder sb = new StringBuilder("AKIA");
            SecureRandom random = new SecureRandom();
            for (int i = 0; i < 16; i++) {
                sb.append(random.nextInt(10));
            }
            keyId = sb.toString();
        } while (accessKeyRepository.findByAccessKeyId(keyId).isPresent());

        return keyId;
    }

    @Transactional
    public void deleteKey(User user, String accessKeyId) {
        AccessKey key = accessKeyRepository.findByAccessKeyId(accessKeyId)
                .orElseThrow(() -> new IllegalArgumentException("Access key not found"));

        if (!key.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized: You don't own this access key");
        }

        accessKeyRepository.delete(key);
        log.info("Access key deleted: keyId={}, userId={}", accessKeyId, user.getId());
    }

    @Transactional
    public void updateKeyStatus(User user, String accessKeyId, AccessKey.Status newStatus) {
        AccessKey key = accessKeyRepository.findByAccessKeyId(accessKeyId)
                .orElseThrow(() -> new IllegalArgumentException("Access key not found"));

        if (!key.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized: You don't own this access key");
        }

        key.setStatus(newStatus);
        accessKeyRepository.save(key);
        log.info("Access key status updated: keyId={}, userId={}, newStatus={}", accessKeyId, user.getId(), newStatus);
    }

    private String generateSecretKey() {
        // 40 char string. Using URL safe base64 of random bytes.
        byte[] bytes = new byte[32]; // 32 bytes -> ~43 chars base64. Truncate to 40.
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 40);
    }
}
