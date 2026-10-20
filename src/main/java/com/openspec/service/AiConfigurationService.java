package com.openspec.service;

import com.openspec.dto.request.AiConfigurationRequest;
import com.openspec.dto.response.AiConfigurationStatusResponse;

/**
 * Service contract for managing AI provider settings.
 * In this API contract phase, methods return contract stubs without external AI connections or persistence.
 */
public interface AiConfigurationService {

    /**
     * Updates the AI configuration with provider, model, and credentials.
     *
     * @param request configuration parameters
     * @return updated status without exposing secrets
     */
    AiConfigurationStatusResponse updateConfiguration(AiConfigurationRequest request);

    /**
     * Retrieves current AI configuration status.
     *
     * @return current status without exposing secrets
     */
    AiConfigurationStatusResponse getConfigurationStatus();
}
