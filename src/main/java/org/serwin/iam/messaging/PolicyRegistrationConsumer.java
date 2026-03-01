package org.serwin.iam.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.dto.DTOs.PolicyCreateEvent;
import org.serwin.iam.dto.DTOs.PolicyResponseEvent;
import org.serwin.iam.dto.DTOs.PolicyUpdateEvent;
import org.serwin.iam.dto.DTOs.PolicyResponseDTO;
import org.serwin.iam.service.PolicyService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyRegistrationConsumer {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final PolicyService policyService;
    private final Publisher publisher;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:dev}")
    private String env;

    @PostConstruct
    public void start() throws Exception {
        io.nats.client.Dispatcher dispatcher = natsConnection.createDispatcher();

        PushSubscribeOptions pso = PushSubscribeOptions.builder()
                .durable("iam-policy-registrar-v3")
                .build();

        jetStream.subscribe(env + ".*.v1.policy.*", "iam-policy-workers", dispatcher, this::handleMessage, true, pso);

        log.info("NATS JetStream Consumer started for full Policy CRUD");
    }

    private void handleMessage(Message msg) {
        String subject = msg.getSubject();

        String[] parts = subject.split("\\.");
        if (parts.length < 5) {
            log.warn("Invalid subject format: {}", subject);
            msg.term();
            return;
        }

        String env = parts[0];
        String service = parts[1]; // <env>.<service>.<version>.<domain>.<action>
        String action = parts[4];

        if ("iam".equals(service)) {
            msg.ack();
            return;
        }

        log.info("Received inbound event on {}: {}", subject, new String(msg.getData()));

        try {
            JsonNode rootNode = objectMapper.readTree(msg.getData());
            String requestId = rootNode.hasNonNull("request_id") ? rootNode.get("request_id").asText()
                    : UUID.randomUUID().toString();

            if (requestId.trim().isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }

            switch (action) {
                case "create":
                    handleCreate(env, msg.getData(), requestId, msg);
                    break;
                case "update":
                    handleUpdate(env, msg.getData(), requestId, msg);
                    break;
                case "delete":
                    handleDelete(env, rootNode, requestId, msg);
                    break;
                case "get":
                    handleGet(env, rootNode, requestId, msg);
                    break;
                default:
                    log.warn("Unknown action: {}", action);
                    msg.term();
            }
        } catch (Exception e) {
            log.error("Failed to unmarshal message: ", e);
            msg.term();
        }
    }

    private void handleCreate(String env, byte[] data, String requestId, Message msg) {
        try {
            PolicyCreateEvent event = objectMapper.readValue(data, PolicyCreateEvent.class);
            PolicyResponseDTO policy = policyService.createPolicy(event);
            PolicyResponseEvent response = new PolicyResponseEvent(requestId, "success",
                    policy.id().toString(), policy, "Policy created successfully", null);
            publisher.publishResponse(env + ".iam.v1.policy.created", response);
            msg.ack();
        } catch (Exception e) {
            handleError(env, requestId, e, msg);
        }
    }

    private void handleUpdate(String env, byte[] data, String requestId, Message msg) {
        try {
            PolicyUpdateEvent event = objectMapper.readValue(data, PolicyUpdateEvent.class);
            PolicyResponseDTO policy = policyService.updatePolicy(event);
            PolicyResponseEvent response = new PolicyResponseEvent(requestId, "success",
                    policy.id().toString(), policy, "Policy updated successfully", null);
            publisher.publishResponse(env + ".iam.v1.policy.updated", response);
            msg.ack();
        } catch (Exception e) {
            handleError(env, requestId, e, msg);
        }
    }

    private void handleDelete(String env, JsonNode node, String requestId, Message msg) {
        try {
            String principalId = node.path("principal_id").asText(null);
            String resourceType = node.path("resource_type").asText(null);
            String resourceId = node.path("resource_id").asText(null);
            String action = node.path("action").asText(null);

            if (principalId == null || resourceType == null || resourceId == null || action == null) {
                throw new IllegalArgumentException("Missing fields for delete");
            }

            policyService.deletePolicy(requestId, principalId, resourceType, resourceId, action);
            PolicyResponseEvent response = new PolicyResponseEvent(requestId, "success",
                    null, null, "Policy deleted successfully", null);
            publisher.publishResponse(env + ".iam.v1.policy.deleted", response);
            msg.ack();
        } catch (Exception e) {
            handleError(env, requestId, e, msg);
        }
    }

    private void handleGet(String env, JsonNode node, String requestId, Message msg) {
        try {
            String principalId = node.path("principal_id").asText(null);
            if (principalId == null || principalId.isEmpty()) {
                throw new IllegalArgumentException("Missing principal_id for get");
            }

            List<PolicyResponseDTO> policies = policyService.getPoliciesByPrincipal(principalId);
            PolicyResponseEvent response = new PolicyResponseEvent(requestId, "success",
                    null, policies, "Policies retrieved successfully", null);
            publisher.publishResponse(env + ".iam.v1.policy.found", response);
            msg.ack();
        } catch (Exception e) {
            handleError(env, requestId, e, msg);
        }
    }

    private void handleError(String env, String requestId, Exception e, Message msg) {
        log.error("Error processing event: ", e);
        PolicyResponseEvent response = new PolicyResponseEvent(requestId, "failure", null, null, null, e.getMessage());
        publisher.publishResponse(env + ".iam.v1.policy.error", response);
        msg.ack(); // Ack error as we handled it by sending error response
    }
}
