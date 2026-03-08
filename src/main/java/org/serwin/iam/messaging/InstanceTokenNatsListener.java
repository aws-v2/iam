package org.serwin.iam.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.dto.DTOs.InstanceTokenRequest;
import org.serwin.iam.dto.DTOs.InstanceTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class InstanceTokenNatsListener {

    private static final String SUBJECT = "dev.iam.v1.token.generate";
    private static final long TOKEN_EXPIRATION_MS = 30 * 60 * 1000 * 10; // 30 minutes

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final Key signingKey;

    public InstanceTokenNatsListener(
            Connection natsConnection,
            ObjectMapper objectMapper,
            @Value("${instance.token.secret}") String secret) {
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;
        this.signingKey = Keys.hmacShaKeyFor(hexToBytes(secret));
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @PostConstruct
    public void init() {
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            try {
                handleTokenRequest(msg);
            } catch (Exception e) {
                log.error("[IAM] Error handling instance token request", e);
                replyError(msg, "internal error");
            }
        });

        dispatcher.subscribe(SUBJECT);
        log.info("[IAM] Subscribed to NATS subject: {}", SUBJECT);
    }

    private void handleTokenRequest(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            InstanceTokenRequest request = objectMapper.readValue(json, InstanceTokenRequest.class);

            log.info("[IAM] Token request received user_id={} instance_id={}",
                    request.user_id(), request.instance_id());

            if (request.instance_id() == null || request.user_id() == null) {
                log.warn("[IAM] Token request missing required fields");
                replyError(msg, "missing instance_id or user_id");
                return;
            }

            // Generate JWT with instance-scoped claims
            String token = generateInstanceToken(request.instance_id(), request.user_id());

            InstanceTokenResponse response = new InstanceTokenResponse(token, "");
            String responseJson = objectMapper.writeValueAsString(response);

            natsConnection.publish(msg.getReplyTo(), responseJson.getBytes(StandardCharsets.UTF_8));
            log.info("[IAM] Generated instance token for instance_id={}", request.instance_id());

        } catch (Exception e) {
            log.error("[IAM] Failed to process instance token request", e);
            replyError(msg, "failed to generate token");
        }
    }

    private String generateInstanceToken(String instanceId, String userId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject("instance:" + instanceId)
                .claim("user_id", userId)
                .claim("role", "instance")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TOKEN_EXPIRATION_MS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private void replyError(Message msg, String errorMessage) {
        try {
            InstanceTokenResponse errorResponse = new InstanceTokenResponse("", errorMessage);
            String json = objectMapper.writeValueAsString(errorResponse);
            natsConnection.publish(msg.getReplyTo(), json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("[IAM] Failed to send error response", e);
        }
    }
}
