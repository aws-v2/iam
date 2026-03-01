package org.serwin.iam.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.dto.DTOs.PolicyResponseEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Publisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    public void publishResponse(String subject, PolicyResponseEvent event) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(event);
            natsConnection.publish(subject, data);
            log.info("Published response to {}: request_id={}, status={}", subject, event.request_id(), event.status());
        } catch (Exception e) {
            log.error("Failed to publish response to " + subject, e);
        }
    }
}
