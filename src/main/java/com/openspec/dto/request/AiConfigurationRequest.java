package com.openspec.dto.request;

import com.openspec.enums.AiProvider;
import com.openspec.enums.OpenSourceAiModel;
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
@Schema(description = "Request payload to configure open-source AI model provider settings")
public class AiConfigurationRequest {

    @NotNull(message = "AI provider is required")
    @Schema(
        description = "Target open-source AI inference provider selected from supported enum options",
        example = "GROQ",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private AiProvider provider;

    @NotNull(message = "Open-source AI model is required")
    @Schema(
        description = "Target open-source model chosen strictly from the supported enum options",
        example = "LLAMA_3_3_70B",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OpenSourceAiModel model;

    @NotBlank(message = "API key is required")
    @Schema(
        description = "API authentication key or secret token for the chosen provider",
        example = "gsk_abc123xyz789...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String apiKey;

    @Schema(
        description = "Optional custom or self-hosted API base endpoint URL (e.g., for Ollama or private proxy)",
        example = "https://api.groq.com/openai/v1"
    )
    private String apiEndpoint;
}
