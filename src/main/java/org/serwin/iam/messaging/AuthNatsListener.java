package org.serwin.iam.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.domain.AccessKey;
import org.serwin.iam.dto.DTOs.ValidationRequestDTO;
import org.serwin.iam.dto.DTOs.ValidationResponse;
import org.serwin.iam.repository.AccessKeyRepository;
import org.serwin.iam.repository.PolicyRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthNatsListener {

    private final Connection natsConnection;
    private final AccessKeyRepository accessKeyRepository;
    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            try {
                handleMessage(msg);
            } catch (Exception e) {
                log.error("Error handling NATS message", e);
            }
        });
    }

    private void handleMessage(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            ValidationRequestDTO request = objectMapper.readValue(json, ValidationRequestDTO.class);
            log.info("Message recieved for access key: "+request.secretAccessKey());

            ValidationResponse response = validateKey(request.accessKeyId(), request.secretAccessKey());
            String responseJson = objectMapper.writeValueAsString(response);

            natsConnection.publish(msg.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to process validation request", e);
        }
    }

    private ValidationResponse validateKey(String accessKeyId, String secretAccessKey) {
        log.info("Validating keyId: {}", accessKeyId);
        Optional<AccessKey> keyOpt = accessKeyRepository.findByAccessKeyId(accessKeyId);

        if (keyOpt.isEmpty()) {
            log.warn("Key validation failed: Access key not found - {}", accessKeyId);
            return new ValidationResponse(false, null, null);
        }

        AccessKey key = keyOpt.get();
        if (key.getStatus() != AccessKey.Status.ACTIVE) {
            log.warn("Key validation failed: Access key is {} - {}", key.getStatus(), accessKeyId);
            return new ValidationResponse(false, null, null);
        }

        // Verify the secret access key matches
        if (!secretAccessKey.equals(key.getSecretAccessKey())) {
            log.warn("Key validation failed: Secret key mismatch for access key - {}", accessKeyId);
            return new ValidationResponse(false, null, null);
        }

        List<String> policies = policyRepository.findByUserId(key.getUser().getId()).stream()
                .map(p -> p.getPolicyDocument())
                .collect(Collectors.toList());

        log.info("Key validation successful: keyId={}, userId={}, policiesCount={}", 
                accessKeyId, key.getUser().getId(), policies.size());
        return new ValidationResponse(
                true,
                key.getUser().getId().toString(),
                policies);
    }
}
