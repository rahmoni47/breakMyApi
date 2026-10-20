package com.openspec.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed validation error found in the OpenAPI document")
public class OpenApiValidationErrorDto {

    @Schema(description = "JSON Pointer or path expression indicating error location", example = "/paths/~1charges/post/responses")
    private String path;

    @Schema(description = "Human-readable explanation of the validation failure", example = "Missing default or 2XX response definition")
    private String message;

    @Schema(description = "Severity level of the validation issue", example = "ERROR", allowableValues = {"ERROR", "WARNING", "INFO"})
    private String severity;
}
