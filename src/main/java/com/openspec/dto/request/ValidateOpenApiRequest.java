package com.openspec.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload containing the OpenAPI/Swagger specification to validate")
public class ValidateOpenApiRequest {

    @NotBlank(message = "OpenAPI document content is required")
    @Schema(
        description = "OpenAPI specification content serialized as a JSON or YAML string",
        example = "{\"openapi\": \"3.0.3\", \"info\": {\"title\": \"Sample API\", \"version\": \"1.0.0\"}, \"paths\": {\"/users\": {\"get\": {\"summary\": \"List users\", \"responses\": {\"200\": {\"description\": \"OK\"}}}}}}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String openApiDocument;
}
