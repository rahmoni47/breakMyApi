package com.openspec.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of OpenAPI document validation")
public class OpenApiValidationResponse {

    @Schema(description = "Whether the OpenAPI specification conforms to schema standards", example = "true")
    private boolean valid;

    @Schema(description = "List of validation errors found in the specification")
    private List<OpenApiValidationErrorDto> errors;

    @Schema(description = "List of non-critical warnings or recommendations", example = "[\"Summary missing on POST /v1/charges\"]")
    private List<String> warnings;

    @Schema(description = "OpenAPI / Swagger version detected in the document", example = "3.0.3")
    private String detectedVersion;

    @Schema(description = "Total number of API endpoint operations discovered", example = "15")
    private Integer endpointsCount;

    @Schema(description = "High-level validation summary message", example = "OpenAPI document is valid and ready for test generation")
    private String message;
}
