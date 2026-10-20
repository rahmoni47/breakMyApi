package com.openspec.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response returned when an HTTP operation fails")
public class ErrorResponse {

    @Schema(description = "Machine-readable error classification code", example = "VALIDATION_FAILED")
    private String code;

    @Schema(description = "Human-readable description of the error", example = "The request payload failed validation")
    private String message;

    @Schema(description = "Timestamp when the error occurred", example = "2026-10-20T12:00:00Z")
    private Instant timestamp;

    @Schema(description = "URI path of the failed request", example = "/api/v1/tests")
    private String path;

    @Schema(description = "Detailed list of contextual or field-level validation errors")
    private List<ErrorDetailDto> details;
}
