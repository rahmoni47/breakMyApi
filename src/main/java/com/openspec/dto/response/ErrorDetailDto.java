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
@Schema(description = "Specific validation or contextual error detail")
public class ErrorDetailDto {

    @Schema(description = "Field name or target property triggering the error", example = "openApiDocument")
    private String field;

    @Schema(description = "Description of the error condition", example = "OpenAPI document content is required")
    private String message;
}
