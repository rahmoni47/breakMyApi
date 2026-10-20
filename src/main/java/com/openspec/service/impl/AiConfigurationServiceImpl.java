package com.openspec.service.impl;

import com.openspec.dto.request.AiConfigurationRequest;
import com.openspec.dto.response.AiConfigurationStatusResponse;
import com.openspec.enums.AiProvider;
import com.openspec.enums.OpenSourceAiModel;
import com.openspec.service.AiConfigurationService;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Stub implementation of {@link AiConfigurationService} for API contract specification and frontend handoff.
 * Constrained strictly to open-source AI model providers and curated models.
 */
@Service
public class AiConfigurationServiceImpl implements AiConfigurationService {

    @Override
    public AiConfigurationStatusResponse updateConfiguration(AiConfigurationRequest request) {
        return AiConfigurationStatusResponse.builder()
                .configured(true)
                .provider(request.getProvider())
                .model(request.getModel())
                .apiEndpoint(request.getApiEndpoint() != null ? request.getApiEndpoint() : "https://api.groq.com/openai/v1")
                .apiKeyConfigured(request.getApiKey() != null && !request.getApiKey().isBlank())
                .updatedAt(Instant.now())
                .message("Open-source AI provider and model configuration successfully registered (contract stub)")
                .build();
    }

    @Override
    public AiConfigurationStatusResponse getConfigurationStatus() {
        return AiConfigurationStatusResponse.builder()
                .configured(true)
                .provider(AiProvider.GROQ)
                .model(OpenSourceAiModel.LLAMA_3_3_70B)
                .apiEndpoint("https://api.groq.com/openai/v1")
                .apiKeyConfigured(true)
                .updatedAt(Instant.parse("2026-10-20T12:00:00Z"))
                .message("Open-source AI provider configuration is active (contract stub)")
                .build();
    }
}
