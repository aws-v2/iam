package org.serwin.iam.exception;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        String error,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponseDTO of(String error, String message) {
        return new ErrorResponseDTO(error, message, LocalDateTime.now());
    }
}
