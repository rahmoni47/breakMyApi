package com.openspec.controller;

import com.openspec.dto.request.AiConfigurationRequest;
import com.openspec.dto.response.AiConfigurationStatusResponse;
import com.openspec.dto.response.ErrorResponse;
import com.openspec.service.AiConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing endpoints for AI provider configuration.
 * Defines the API contract for updating and reading AI configuration status.
 */
@RestController
@RequestMapping("/api/v1/ai-configuration")
@RequiredArgsConstructor
@Tag(name = "AI Configuration", description = "Endpoints for configuring LLM inference providers and checking status")
public class AiConfigurationController {

        private final AiConfigurationService aiConfigurationService;

        @Operation(summary = "Configure AI provider settings", description = "Registers or updates the AI model provider, model checkpoint name, API key, and optional custom endpoint. "
                        + "Note: In this API contract phase, credentials are not validated against remote providers or persisted.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "AI configuration registered successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AiConfigurationStatusResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Validation error on request fields", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<AiConfigurationStatusResponse> updateConfiguration(
                        @Valid @RequestBody AiConfigurationRequest request) {
                AiConfigurationStatusResponse response = aiConfigurationService.updateConfiguration(request);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get AI provider configuration status", description = "Returns current AI provider setup status, including active provider and model name. "
                        + "IMPORTANT: The API key is NEVER exposed in the status response.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "AI configuration status retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AiConfigurationStatusResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<AiConfigurationStatusResponse> getConfigurationStatus() {
                AiConfigurationStatusResponse response = aiConfigurationService.getConfigurationStatus();
                return ResponseEntity.ok(response);
        }
}
