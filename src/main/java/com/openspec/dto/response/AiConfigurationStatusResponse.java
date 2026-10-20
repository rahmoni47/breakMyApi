package com.openspec.dto.response;

import com.openspec.enums.AiProvider;
import com.openspec.enums.OpenSourceAiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI configuration status response. Sensitive API keys are never exposed in responses.")
public class AiConfigurationStatusResponse {

    @Schema(description = "Whether an AI provider is currently configured and ready", example = "true")
    private boolean configured;

    @Schema(description = "Configured open-source AI provider", example = "GROQ")
    private AiProvider provider;

    @Schema(description = "Configured open-source AI model selected from the allowed enum options", example = "LLAMA_3_3_70B")
    private OpenSourceAiModel model;

    @Schema(description = "Custom endpoint URL, if configured", example = "https://api.groq.com/openai/v1")
    private String apiEndpoint;

    @Schema(description = "Indicates whether a valid API key has been supplied without exposing the secret", example = "true")
    private boolean apiKeyConfigured;

    @Schema(description = "Timestamp when the AI configuration was last updated", example = "2026-10-20T12:00:00Z")
    private Instant updatedAt;

    @Schema(description = "Status message or notes regarding the AI configuration", example = "Open-source AI provider configuration is active")
    private String message;
}
