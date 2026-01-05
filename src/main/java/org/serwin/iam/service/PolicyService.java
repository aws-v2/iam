package org.serwin.iam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serwin.iam.domain.Policy;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;

    @Transactional
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
