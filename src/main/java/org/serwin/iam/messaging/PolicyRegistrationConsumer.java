package org.serwin.iam.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStream;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.domain.Policy;
import org.serwin.iam.dto.DTOs.PolicyCreateEvent;
import org.serwin.iam.dto.DTOs.PolicyResponseEvent;
import org.serwin.iam.service.PolicyService;
import org.springframework.stereotype.Component;

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
                .durable("iam-policy-registrar-v2")
                .build();

        // Subscribe to all policy CRUD actions for all services using a queue group
        jetStream.subscribe(env + ".*.v1.policy.*", "iam-policy-workers", dispatcher, this::handleMessage, true, pso);

        log.info("NATS JetStream Consumer started for full Policy CRUD");
    }

    private void handleMessage(Message msg) {
        String subject = msg.getSubject();
        log.info("Received message on {}: {}", subject, new String(msg.getData()));

        String[] parts = subject.split("\\.");
        if (parts.length < 5) {
            log.warn("Invalid subject format: {}", subject);
            msg.term();
            return;
        }

        String env = parts[0];
        String action = parts[4]; // <env>.<service>.<version>.<domain>.<action>

        try {
            PolicyCreateEvent event = objectMapper.readValue(msg.getData(), PolicyCreateEvent.class);

            switch (action) {
                case "create":
                    handleCreate(env, event, msg);
                    break;
                case "update":
                    handleUpdate(env, event, msg);
                    break;
                case "delete":
                    handleDelete(env, event, msg);
                    break;
                case "get":
                    handleGet(env, event, msg);
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

    private void handleCreate(String env, PolicyCreateEvent event, Message msg) {
        try {
            Policy policy = policyService.registerPolicy(event);
            PolicyResponseEvent response = new PolicyResponseEvent(event.request_id(), "success",
                    policy.getId().toString(), "Policy created successfully", null);
            publisher.publishResponse(env + ".iam.v1.policy.created", response);
            msg.ack();
        } catch (Exception e) {
            handleError(env, event.request_id(), e, msg);
        }
    }

    private void handleUpdate(String env, PolicyCreateEvent event, Message msg) {
        try {
            Policy policy = policyService.updatePolicy(event);
            PolicyResponseEvent response = new PolicyResponseEvent(event.request_id(), "success",
                    policy.getId().toString(), "Policy updated successfully", null);
            publisher.publishResponse(env + ".iam.v1.policy.updated", response);
            msg.ack();
        } catch (Exception e) {
            handleError(env, event.request_id(), e, msg);
        }
    }

    private void handleDelete(String env, PolicyCreateEvent event, Message msg) {
        try {
            policyService.deletePolicy(event.account_id(), event.policy_name());
            PolicyResponseEvent response = new PolicyResponseEvent(event.request_id(), "success",
                    null, "Policy deleted successfully", null);
            publisher.publishResponse(env + ".iam.v1.policy.deleted", response);
            msg.ack();
        } catch (Exception e) {
            handleError(env, event.request_id(), e, msg);
        }
    }

    private void handleGet(String env, PolicyCreateEvent event, Message msg) {
        try {
            Policy policy = policyService.getPolicyByAccountAndName(event.account_id(), event.policy_name());
            PolicyResponseEvent response = new PolicyResponseEvent(event.request_id(), "success",
                    policy.getId().toString(), policy.getPolicyDocument(), null);
            publisher.publishResponse(env + ".iam.v1.policy.found", response);
            msg.ack();
        } catch (Exception e) {
            handleError(env, event.request_id(), e, msg);
        }
    }

    private void handleError(String env, String requestId, Exception e, Message msg) {
        log.error("Error processing event: ", e);
        PolicyResponseEvent response = new PolicyResponseEvent(requestId, "failure", null, null, e.getMessage());
        publisher.publishResponse(env + ".iam.v1.policy.error", response);
        msg.ack(); // Ack error as we handled it by sending error response
    }
}
