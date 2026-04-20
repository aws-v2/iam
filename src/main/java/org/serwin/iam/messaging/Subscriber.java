package org.serwin.iam.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.dto.InstanceTokenRequest;
import org.serwin.iam.dto.InstanceTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Subscriber {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    @Value("${nats.subject.prefix}")
    private String subjectPrefix;

    @PostConstruct
    public void init() {
        String subject = subjectPrefix + ".iam.token.generate";

        Dispatcher dispatcher = natsConnection.createDispatcher(this::handleMessage);
        dispatcher.subscribe(subject);

        log.info("[NATS] Subscribed to subject={}", subject);
    }

    private void handleMessage(Message msg) {
        String subject = msg.getSubject();
        String replyTo = msg.getReplyTo();

        try {
            InstanceTokenRequest request =
                objectMapper.readValue(msg.getData(), InstanceTokenRequest.class);

            log.info("[NATS] [REQUEST] subject={} user_id={} instance_id={}",
                    subject, request.getUserId(), request.getInstanceId());

            // 🔑 Generate token (replace with real logic)
            String token = generateToken(request);

            InstanceTokenResponse response = new InstanceTokenResponse(token, null);

            byte[] responseBytes = objectMapper.writeValueAsBytes(response);

            // ✅ Respond directly using reply subject
            if (replyTo != null) {
                natsConnection.publish(replyTo, responseBytes);
            } else {
                log.warn("[NATS] No reply subject set for request");
            }

        } catch (Exception e) {
            log.error("[NATS] Failed to process token request", e);

            try {
                InstanceTokenResponse errorResponse =
                    new InstanceTokenResponse(null, e.getMessage());

                if (msg.getReplyTo() != null) {
                    natsConnection.publish(
                        msg.getReplyTo(),
                        objectMapper.writeValueAsBytes(errorResponse)
                    );
                }
            } catch (Exception ex) {
                log.error("[NATS] Failed to send error response", ex);
            }
        }
    }

    private String generateToken(InstanceTokenRequest request) {
        // ⚠️ Replace with JWT or IAM logic
        return "token-for-" + request.getInstanceId();
    }
}