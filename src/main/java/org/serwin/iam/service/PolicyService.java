package org.serwin.iam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.domain.Policy;
import org.serwin.iam.domain.ProcessedRequest;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.PolicyRepository;
import org.serwin.iam.repository.ProcessedRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final ProcessedRequestRepository processedRequestRepository;

    private static final List<String> ALLOWED_TYPES = List.of("s3", "rds", "ec2", "lambda");

    private void validateResourceTypeAndAction(String resourceType, String action) {
        if (!ALLOWED_TYPES.contains(resourceType)) {
            throw new IllegalArgumentException("Invalid resourceType: " + resourceType);
        }
        boolean validAction = false;
        switch (resourceType) {
            case "s3":
                validAction = List.of("PutObject", "GetObject", "DeleteObject").contains(action);
                break;
            case "rds":
                validAction = List.of("UpdateDatabase", "ReadDatabase").contains(action);
                break;
            case "ec2":
                validAction = List.of("StartInstances", "StopInstances", "RunInstances").contains(action);
                break;
            case "lambda":
                validAction = List.of("InvokeFunction", "CreateFunction").contains(action);
                break;
        }
        if (!validAction) {
            throw new IllegalArgumentException("Invalid action " + action + " for resourceType " + resourceType);
        }
    }

    private void checkIdempotency(String requestId) {
        if (requestId != null && processedRequestRepository.existsById(requestId)) {
            throw new IllegalStateException("idempotent_request_processed");
        }
    }

    private void markProcessed(String requestId) {
        if (requestId != null) {
            ProcessedRequest pr = new ProcessedRequest();
            pr.setRequestId(requestId);
            pr.setProcessedAt(OffsetDateTime.now());
            processedRequestRepository.save(pr);
        }
    }

    @Transactional
    public PolicyResponseDTO createPolicy(PolicyCreateEvent event) {
        if (event.principal_id() == null || event.resource_type() == null || event.resource_id() == null
                || event.action() == null) {
            log.error("Missing required fields. Received: principalId={}, resourceType={}, resourceId={}, action={}",
                    event.principal_id(), event.resource_type(), event.resource_id(), event.action());
            throw new IllegalArgumentException("Missing required valid fields");
        }
        validateResourceTypeAndAction(event.resource_type(), event.action());

        checkIdempotency(event.request_id());

        if (policyRepository.existsByPrincipalIdAndResourceTypeAndResourceIdAndAction(
                event.principal_id(), event.resource_type(), event.resource_id(), event.action())) {
            throw new IllegalStateException("duplicate_policy");
        }

        Policy policy = new Policy();
        policy.setAccountId(event.account_id() == null ? "system" : event.account_id());
        policy.setPrincipalId(event.principal_id());
        policy.setResourceType(event.resource_type());
        policy.setResourceId(event.resource_id());
        policy.setAction(event.action());
        policy.setCreatedBy(event.created_by() == null ? "system" : event.created_by());
        policy.setCreatedAt(OffsetDateTime.now());

        policy = policyRepository.save(policy);

        markProcessed(event.request_id());

        log.info("Policy registered: id={}", policy.getId());
        return mapToDTO(policy);
    }

    @Transactional
    public PolicyResponseDTO updatePolicy(PolicyUpdateEvent event) {
        if (event.resource_type() == null || event.resource_id() == null || event.action() == null) {
            throw new IllegalArgumentException("Missing required valid fields");
        }
        validateResourceTypeAndAction(event.resource_type(), event.action());

        checkIdempotency(event.request_id());

        Policy policy = policyRepository.findById(UUID.fromString(event.policy_id()))
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        policy.setResourceType(event.resource_type());
        policy.setResourceId(event.resource_id());
        policy.setAction(event.action());
        policy.setUpdatedAt(OffsetDateTime.now());

        policy = policyRepository.save(policy);

        markProcessed(event.request_id());

        log.info("Policy updated: id={}", policy.getId());
        return mapToDTO(policy);
    }

    @Transactional
    public void deletePolicy(String requestId, String principalId, String resourceType, String resourceId,
            String action) {
        checkIdempotency(requestId);

        if (!policyRepository.existsByPrincipalIdAndResourceTypeAndResourceIdAndAction(
                principalId, resourceType, resourceId, action)) {
            throw new IllegalArgumentException("Policy not found");
        }
        policyRepository.deleteByPrincipalIdAndResourceTypeAndResourceIdAndAction(
                principalId, resourceType, resourceId, action);

        markProcessed(requestId);
        log.info("Policy deleted for principal={}", principalId);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponseDTO> getPoliciesByPrincipal(String principalId) {
        return policyRepository.findByPrincipalId(principalId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private PolicyResponseDTO mapToDTO(Policy policy) {
        return new PolicyResponseDTO(
                policy.getId(),
                policy.getAccountId(),
                policy.getPrincipalId(),
                policy.getResourceType(),
                policy.getResourceId(),
                policy.getAction(),
                policy.getCreatedBy(),
                policy.getCreatedAt());
    }
}
