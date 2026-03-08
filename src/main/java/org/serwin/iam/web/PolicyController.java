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
            @Valid @RequestBody CreatePolicyDTO dto) {
        User user = userRepository.findById(userId).orElseThrow();
        // adapt the REST payload to the Event structure our Service expects
        PolicyCreateEvent event = new PolicyCreateEvent(
                UUID.randomUUID().toString(), // fake requestId for REST
                user.getId().toString(),
                dto.principalId(),
                dto.resourceType(),
                dto.resourceId(),
                dto.action(),
                user.getId().toString());
        return ResponseEntity.ok(policyService.createPolicy(event));
    }

    @GetMapping("/principal/{principalId}")
    public ResponseEntity<List<PolicyResponseDTO>> listPoliciesByPrincipal(@PathVariable String principalId) {
        return ResponseEntity.ok(policyService.getPoliciesByPrincipal(principalId));
    }

    @PutMapping("/{policyId}")
    public ResponseEntity<PolicyResponseDTO> updatePolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody CreatePolicyDTO dto) {
        PolicyUpdateEvent event = new PolicyUpdateEvent(
                UUID.randomUUID().toString(),
                policyId.toString(),
                dto.resourceType(),
                dto.resourceId(),
                dto.action());
        return ResponseEntity.ok(policyService.updatePolicy(event));
    }

    @DeleteMapping("/principal/{principalId}")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable String principalId,
            @RequestParam String resourceType,
            @RequestParam String resourceId,
            @RequestParam String action) {
        policyService.deletePolicy(UUID.randomUUID().toString(), principalId);
        return ResponseEntity.noContent().build();
    }
}
