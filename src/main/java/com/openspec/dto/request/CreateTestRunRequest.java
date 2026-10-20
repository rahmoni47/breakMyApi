package com.openspec.dto.request;

import com.openspec.enums.TestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to initiate a new test run")
public class CreateTestRunRequest {

    @NotBlank(message = "Test run name is required")
    @Schema(
        description = "Descriptive name for the test run",
        example = "Payment Service Regression Suite",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @NotBlank(message = "OpenAPI document is required")
    @Schema(
        description = "Target OpenAPI/Swagger specification in JSON or YAML string format",
        example = "{\"openapi\": \"3.0.3\", \"info\": {\"title\": \"Payment Gateway API\", \"version\": \"2.1.0\"}, \"paths\": {\"/v1/charges\": {\"post\": {\"summary\": \"Create charge\"}}}}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String openApiDocument;

    @NotNull(message = "Test type is required")
    @Schema(
        description = "Depth and scope profile for testing",
        example = "FAST",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private TestType testType;
}
