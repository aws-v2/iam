package org.serwin.iam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.domain.Policy;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.domain.PolicyAttachment;
import org.serwin.iam.domain.ProcessedRequest;
import org.serwin.iam.repository.PolicyAttachmentRepository;
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
    private final PolicyAttachmentRepository attachmentRepository;
    private final ProcessedRequestRepository processedRequestRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public Policy registerPolicy(PolicyCreateEvent event) {
        // 1. Idempotency Check
        if (processedRequestRepository.existsById(event.request_id())) {
            log.info("Request already processed: {}", event.request_id());
            return policyRepository.findByAccountIdAndName(event.account_id(), event.policy_name())
                    .orElse(null);
        }

        // 2. Validate Payload (Basic)
        if (event.account_id() == null || event.policy_name() == null || event.principal_arn() == null) {
            throw new IllegalArgumentException("Missing required fields");
        }

        // 3. Validate ARN (Simple regex check)
        if (!event.principal_arn().startsWith("arn:serw:")) {
            throw new IllegalArgumentException("Invalid principal ARN format");
        }

        // 4. Enforce Uniqueness
        if (policyRepository.existsByAccountIdAndName(event.account_id(), event.policy_name())) {
            throw new IllegalStateException("duplicate_policy_name");
        }

        // 5. Create Policy
        Policy policy = new Policy();
        policy.setId(UUID.randomUUID());
        policy.setAccountId(event.account_id());
        policy.setName(event.policy_name());
        // Use ObjectMapper for proper JSON serialization
        try {
            policy.setPolicyDocument(objectMapper.writeValueAsString(event.policy_document()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid policy document format", e);
        }
        policy.setCreatedBy(event.created_by());
        policy.setCreatedAt(OffsetDateTime.now());

        policy = policyRepository.save(policy);

        // 6. Create Attachment
        PolicyAttachment attachment = new PolicyAttachment();
        attachment.setPolicy(policy);
        attachment.setPrincipalArn(event.principal_arn());
        attachment.setAttachedAt(OffsetDateTime.now());
        attachmentRepository.save(attachment);

        // 7. Mark Processed
        ProcessedRequest pr = new ProcessedRequest();
        pr.setRequestId(event.request_id());
        pr.setProcessedAt(OffsetDateTime.now());
        processedRequestRepository.save(pr);

        log.info("Policy registered via event: id={}, name={}", policy.getId(), policy.getName());
        return policy;
    }

    @Transactional
    public Policy updatePolicy(PolicyCreateEvent event) {
        Policy policy = policyRepository.findByAccountIdAndName(event.account_id(), event.policy_name())
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        try {
            policy.setPolicyDocument(objectMapper.writeValueAsString(event.policy_document()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid policy document format", e);
        }
        policy.setUpdatedAt(OffsetDateTime.now());

        return policyRepository.save(policy);
    }

    @Transactional
    public void deletePolicy(String accountId, String name) {
        if (!policyRepository.existsByAccountIdAndName(accountId, name)) {
            throw new IllegalArgumentException("Policy not found");
        }
        policyRepository.deleteByAccountIdAndName(accountId, name);
    }

    @Transactional(readOnly = true)
    public Policy getPolicyByAccountAndName(String accountId, String name) {
        return policyRepository.findByAccountIdAndName(accountId, name)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));
    }

    public PolicyResponseDTO createPolicy(User user, CreatePolicyDTO dto) {
        Policy policy = new Policy();
        policy.setName(dto.name());
        // In a real app, validate JSON structure here
        policy.setPolicyDocument(dto.policyDocument());
        policy.setUser(user);

        policy = policyRepository.save(policy);
        log.info("Policy created: id={}, name={}, userId={}", policy.getId(), policy.getName(), user.getId());

        return new PolicyResponseDTO(policy.getId(), policy.getName(), policy.getPolicyDocument());
    }

    @Transactional(readOnly = true)
    public List<PolicyResponseDTO> listPolicies(User user) {
        return policyRepository.findByUserId(user.getId()).stream()
                .map(p -> new PolicyResponseDTO(p.getId(), p.getName(), p.getPolicyDocument()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PolicyResponseDTO getPolicy(User user, UUID policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        if (!policy.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized: You don't own this policy");
        }

        return new PolicyResponseDTO(policy.getId(), policy.getName(), policy.getPolicyDocument());
    }

    @Transactional
    public void deletePolicy(User user, UUID policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        if (!policy.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized: You don't own this policy");
        }

        policyRepository.delete(policy);
        log.info("Policy deleted: id={}, name={}, userId={}", policyId, policy.getName(), user.getId());
    }
}
