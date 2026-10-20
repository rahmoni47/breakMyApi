package com.openspec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspec.dto.request.AiConfigurationRequest;
import com.openspec.dto.request.CreateTestRunRequest;
import com.openspec.dto.request.ValidateOpenApiRequest;
import com.openspec.enums.AiProvider;
import com.openspec.enums.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID TEST_RUN_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Test
    @DisplayName("Swagger OpenAPI documentation is accessible at /v3/api-docs")
    void testOpenApiDocsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("openSpec Backend API"))
                .andExpect(jsonPath("$.paths['/api/v1/ai-configuration']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ai-configuration/status']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tests/validate']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tests']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tests/{testRunId}/events']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tests/{testRunId}/report']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tests/{testRunId}/logs']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tests/{testRunId}/logs/download']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tests/{testRunId}/stop']").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/ai-configuration updates config and does not expose API key")
    void testUpdateAiConfiguration() throws Exception {
        AiConfigurationRequest request = AiConfigurationRequest.builder()
                .provider(AiProvider.GROQ)
                .model(com.openspec.enums.OpenSourceAiModel.LLAMA_3_3_70B)
                .apiKey("gsk_secret-test-key-12345")
                .apiEndpoint("https://api.groq.com/openai/v1")
                .build();

        mockMvc.perform(put("/api/v1/ai-configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.provider").value("GROQ"))
                .andExpect(jsonPath("$.model").value("LLAMA_3_3_70B"))
                .andExpect(jsonPath("$.apiKeyConfigured").value(true))
                .andExpect(content().string(not(containsString("gsk_secret-test-key-12345"))));
    }

    @Test
    @DisplayName("PUT /api/v1/ai-configuration rejects model not in the OpenSourceAiModel enum")
    void testRejectInvalidModel() throws Exception {
        String invalidPayload = """
                {
                    "provider": "GROQ",
                    "model": "NON_EXISTENT_PROPRIETARY_MODEL",
                    "apiKey": "gsk_12345"
                }
                """;

        mockMvc.perform(put("/api/v1/ai-configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
    }

    @Test
    @DisplayName("GET /api/v1/ai-configuration/status returns status without API key")
    void testGetAiConfigurationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/ai-configuration/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.provider").exists())
                .andExpect(jsonPath("$.model").exists())
                .andExpect(jsonPath("$.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.apiKey").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/tests/validate validates OpenAPI specification")
    void testValidateOpenApiDocument() throws Exception {
        ValidateOpenApiRequest request = ValidateOpenApiRequest.builder()
                .openApiDocument("""
                        {
                          "openapi": "3.0.3",
                          "info": {
                            "title": "Test API",
                            "version": "1.0.0"
                          },
                          "paths": {
                            "/charges": {
                              "post": {
                                "summary": "Create charge",
                                "responses": {
                                  "201": {
                                    "description": "Created"
                                  }
                                }
                              }
                            }
                          }
                        }
                        """)
                .build();

        mockMvc.perform(post("/api/v1/tests/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.detectedVersion").value("3.0.3"))
                .andExpect(jsonPath("$.endpointsCount").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/tests/validate returns valid=false for malformed document")
    void testValidateOpenApiDocumentMalformed() throws Exception {
        ValidateOpenApiRequest request = ValidateOpenApiRequest.builder()
                .openApiDocument("{ this is not valid json or yaml }")
                .build();

        mockMvc.perform(post("/api/v1/tests/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.endpointsCount").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/tests creates a test run with 202 Accepted")
    void testCreateTestRun() throws Exception {
        CreateTestRunRequest request = CreateTestRunRequest.builder()
                .name("Payment Gateway Test")
                .openApiDocument("{\"openapi\": \"3.0.3\"}")
                .testType(TestType.FAST)
                .build();

        mockMvc.perform(post("/api/v1/tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.testRunId").exists())
                .andExpect(jsonPath("$.name").value("Payment Gateway Test"))
                .andExpect(jsonPath("$.testType").value("FAST"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/tests/{testRunId}/events returns text/event-stream")
    void testSubscribeToEvents() throws Exception {
        mockMvc.perform(get("/api/v1/tests/{testRunId}/events", TEST_RUN_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    @DisplayName("GET /api/v1/tests returns paginated history")
    void testGetTestRunHistory() throws Exception {
        mockMvc.perform(get("/api/v1/tests")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].testRunId").value(TEST_RUN_ID.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/tests/{testRunId}/report returns text/markdown")
    void testGetMarkdownReport() throws Exception {
        mockMvc.perform(get("/api/v1/tests/{testRunId}/report", TEST_RUN_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/markdown"))
                .andExpect(content().string(containsString("# openSpec Test Run Report")));
    }

    @Test
    @DisplayName("GET /api/v1/tests/{testRunId}/logs returns text/plain")
    void testGetLogs() throws Exception {
        mockMvc.perform(get("/api/v1/tests/{testRunId}/logs", TEST_RUN_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("[openSpec] Initializing test run")));
    }

    @Test
    @DisplayName("GET /api/v1/tests/{testRunId}/logs/download returns attachment header")
    void testDownloadLogs() throws Exception {
        mockMvc.perform(get("/api/v1/tests/{testRunId}/logs/download", TEST_RUN_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test-run-" + TEST_RUN_ID + ".log\""));
    }

    @Test
    @DisplayName("POST /api/v1/tests/{testRunId}/stop cancels run with 202 Accepted")
    void testStopTestRun() throws Exception {
        mockMvc.perform(post("/api/v1/tests/{testRunId}/stop", TEST_RUN_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.testRunId").value(TEST_RUN_ID.toString()))
                .andExpect(jsonPath("$.status").value("STOPPED"));
    }

    @Test
    @DisplayName("Validation failure returns 400 with structured ErrorResponse")
    void testValidationFailure() throws Exception {
        CreateTestRunRequest invalidRequest = CreateTestRunRequest.builder()
                .name("") // blank
                .openApiDocument(null) // null
                .testType(null) // null
                .build();

        mockMvc.perform(post("/api/v1/tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/v1/tests"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[*].field", hasItem("name")));
    }
}
