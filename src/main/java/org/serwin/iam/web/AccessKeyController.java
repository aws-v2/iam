package org.serwin.iam.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serwin.iam.domain.AccessKey;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.UserRepository;
import org.serwin.iam.service.AccessKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class AccessKeyController {

    private final AccessKeyService accessKeyService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AccessKeyResponseDTO>> getKeys(@AuthenticationPrincipal UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(accessKeyService.getKeys(user));
    }

    @PostMapping
    public ResponseEntity<AccessKeyCreatedDTO> createKey(@AuthenticationPrincipal UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(accessKeyService.createKey(user));
    }

    @DeleteMapping("/{accessKeyId}")
    public ResponseEntity<Void> deleteKey(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String accessKeyId
    ) {
        User user = userRepository.findById(userId).orElseThrow();
        accessKeyService.deleteKey(user, accessKeyId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{accessKeyId}/status")
    public ResponseEntity<Void> updateKeyStatus(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String accessKeyId,
            @Valid @RequestBody UpdateKeyStatusDTO dto
    ) {
        User user = userRepository.findById(userId).orElseThrow();
        AccessKey.Status newStatus = AccessKey.Status.valueOf(dto.status().toUpperCase());
        accessKeyService.updateKeyStatus(user, accessKeyId, newStatus);
        return ResponseEntity.ok().build();
    }
}
