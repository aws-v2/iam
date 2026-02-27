package org.serwin.iam.dto;

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
                        @NotBlank(message = "Policy name is required") String name,

                        @NotBlank(message = "Policy document is required") String policyDocument) {
        }

        public record PolicyResponseDTO(UUID id, String name, String policyDocument) {
        }

        public record ValidationRequestDTO(String accessKeyId, String secretAccessKey) {
        }

        public record ValidationResponse(boolean valid, String userId, List<String> policies) {
        }

        public record PolicyCreateEvent(
                        String request_id,
                        String account_id,
                        String principal_arn,
                        String policy_name,
                        Object policy_document,
                        String created_by,
                        String timestamp) {
        }

        public record PolicyResponseEvent(
                        String request_id,
                        String status,
                        String policy_id,
                        String message,
                        String error) {
        }
}
