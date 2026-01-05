package org.serwin.iam.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.UserRepository;
import org.serwin.iam.service.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<PolicyResponseDTO> createPolicy(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreatePolicyDTO dto
    ) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(policyService.createPolicy(user, dto));
    }

    @GetMapping
    public ResponseEntity<List<PolicyResponseDTO>> listPolicies(@AuthenticationPrincipal UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(policyService.listPolicies(user));
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyResponseDTO> getPolicy(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID policyId
    ) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(policyService.getPolicy(user, policyId));
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> deletePolicy(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID policyId
    ) {
        User user = userRepository.findById(userId).orElseThrow();
        policyService.deletePolicy(user, policyId);
        return ResponseEntity.noContent().build();
    }
}
