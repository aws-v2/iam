package org.serwin.iam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class DTOs {

        public record RegisterRequestDTO(
                        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,

                        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password) {
        }

        public record LoginRequestDTO(
                        @NotBlank(message = "Email is required") String email,

                        @NotBlank(message = "Password is required") String password) {
        }

        public record AuthResponseDTO(String token, String message, String userId) {
        }

        public record AccessKeyResponseDTO(String accessKeyId, String status, String createdAt) {
        }

        public record AccessKeyCreatedDTO(String accessKeyId, String secretAccessKey, String status) {
        }

        public record UpdateKeyStatusDTO(
                        @NotBlank(message = "Status is required") String status) {
        }

        public record CreatePolicyDTO(
                        @NotBlank(message = "Principal ID is required") String principalId,
                        @NotBlank(message = "Resource Type is required") String resourceType,
                        @NotBlank(message = "Resource ID is required") String resourceId,
                        @NotBlank(message = "Action is required") String action) {
        }

        public record PolicyResponseDTO(
                        UUID id,
                        String accountId,
                        String principalId,
                        String resourceType,
                        String resourceId,
                        String action,
                        String createdBy,
                        java.time.OffsetDateTime createdAt) {
        }

        public record ValidationRequestDTO(String accessKeyId, String secretAccessKey) {
        }

        public record ValidationResponse(boolean valid, String userId, List<String> policies) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PolicyCreateEvent(
                        String request_id,
                        String account_id,
                        String principal_id,
                        String resource_type,
                        String resource_id,
                        String action,
                        String created_by) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PolicyUpdateEvent(
                        String request_id,
                        String policy_id,
                        String resource_type,
                        String resource_id,
                        String action) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PolicyResponseEvent(
                        String request_id,
                        String status,
                        String policy_id, // optional, only if created/found
                        Object policy, // optional, return PolicyResponseDTO or list of them
                        String message,
                        String error) {
        }
}
