package com.openspec.core.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Unified Spring AI implementation of {@link AiTestingClient}.
 * Communicates with Google AI Studio Gemini models via Spring AI's {@link ChatClient} abstraction.
 */
@Component
public class SpringAiTestingClient implements AiTestingClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiTestingClient.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.model:gemini-2.5-flash-lite}")
    private String primaryModel;

    @Value("${gemini.api.fallback-model:gemini-3.5-flash-lite}")
    private String fallbackModel;

    private static final String SYSTEM_PROMPT = """
            You are openSpec AI, an elite automated API test engineering and vulnerability discovery agent.

            ### OVERVIEW & ROLE:
            Your mission is to perform systematic, automated API testing against a provided OpenAPI/Swagger specification
            using an iterative, feedback-driven dialogue loop with the openSpec execution engine.

            ### HOW OPENSPEC WORKS & YOUR ITERATIVE WORKFLOW:
            The testing session progresses through structured phases:
            1. PHASE 1 - TEST PLANNING & COMMAND GENERATION (TEST_BATCH):
               - You analyze the OpenAPI specification: paths, HTTP methods, required query parameters, headers, request bodies, and expected status codes.
               - You produce an ordered batch of executable HTTP test commands adhering to the TEST_BATCH protocol schema.
               - Every command defines the exact method, path, headers, query parameters, payload, and expected responses.
            2. PHASE 2 - BACKEND EXECUTION & EVIDENCE GATHERING:
               - The openSpec backend engine safely executes your test commands against the live target server.
               - The engine records the actual HTTP status code, response headers, response bodies, durations, and validation verdicts.
               - It bundles this empirical evidence into a BATCH_EXECUTION_RESULTS message and sends it back to you.
            3. PHASE 3 - ANALYSIS, DISCOVERY & NEXT ACTION (BATCH_ANALYSIS):
               - You analyze the returned execution results against the OpenAPI contract and expectations.
               - You assign command verdicts (PASSED, FAILED, ERROR) with clear explanations.
               - You document any unexpected behavior, contract deviations, or vulnerabilities as severity-ranked findings (CRITICAL, HIGH, MEDIUM, LOW, INFO).
               - You decide the next action:
                 * "EXECUTE_BATCH": if more testing scenarios or deeper boundary exploration are needed.
                 * "FINISH_TESTING": if testing objectives have been successfully achieved.
                 * "ABORT_TESTING": if a blocking, catastrophic failure occurs.
            4. PHASE 4 - AUTHORITATIVE REPORT SYNTHESIS:
               - The engine aggregates all evidence and generates an audit-ready Markdown report.

            ### FAST MODE GUIDELINES:
            When testType is "FAST":
            - Focus on rapid sanity checks, schema conformance, required parameter validation, valid status codes (2xx), and basic error handling (4xx).
            - Generate 2 to 5 high-impact, non-destructive test commands for the initial batch.
            - Verify primary resource discovery and retrieval (GET).
            - Verify primary creation or update endpoints with valid required fields.
            - Test one boundary or invalid input case to verify proper client error handling.

            ### CORE SAFETY RULES:
            1. Use the provided OpenAPI document as your sole source of truth.
            2. Never invent undocumented endpoints or external hosts.
            3. Target paths must start with a leading slash (e.g. "/v1/products").
            4. Never generate commands attempting to contact unauthorized external URLs.

            ### COMMUNICATION PROTOCOL:
            1. You MUST communicate exclusively by returning valid, parseable JSON conforming strictly to the requested schema.
            2. Do NOT wrap the JSON in commentary or conversational filler.
            3. Return only the raw JSON object.
            """;

    @Autowired
    public SpringAiTestingClient(@Autowired(required = false) ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public TestBatchMessage generateTestBatch(TestContext context) {
        String userPrompt = buildGenerateBatchPrompt(context);
        log.info("Dispatching initial test generation prompt to Gemini for test run: {}", context.getTestRunId());

        String rawResponse = executeChat(userPrompt);
        return parseResponse(rawResponse, TestBatchMessage.class);
    }

    @Override
    public BatchAnalysisMessage analyzeBatchResults(BatchExecutionResultsMessage executionResults, TestContext context) {
        String userPrompt = buildAnalysisPrompt(executionResults, context);
        log.info("Dispatching batch analysis prompt to Gemini for batch: {}", executionResults.getBatchId());

        String rawResponse = executeChat(userPrompt);
        return parseResponse(rawResponse, BatchAnalysisMessage.class);
    }

    private String executeChat(String userPrompt) {
        if (chatClient == null) {
            throw new IllegalStateException("ChatClient is not configured. Please ensure gemini.api.key is set in application.properties.");
        }

        try {
            return callModel(userPrompt, primaryModel);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            if (msg.contains("404") || msg.contains("no longer available") || msg.contains("NOT_FOUND")) {
                log.warn("Primary model '{}' unavailable ({}), falling back to '{}'", primaryModel, msg, fallbackModel);
                return callModel(userPrompt, fallbackModel);
            }
            log.error("AI Provider communication failure: {}", ex.getMessage());
            throw new RuntimeException("Failed to communicate with AI model: " + ex.getMessage(), ex);
        }
    }

    private String callModel(String userPrompt, String model) {
        String content = null;
        try {
            var spec = chatClient.prompt();
            if (model != null && !model.isBlank()) {
                spec = spec.options(OpenAiChatOptions.builder().withModel(model).build());
            }
            content = spec.system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.debug("Error calling model with options: {}", ex.getMessage());
        }

        if (content == null) {
            // Fallback for tests or setups without ChatOptions override
            content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
        }

        return content;
    }

    private String buildGenerateBatchPrompt(TestContext context) {
        int nextSeq = context.getCurrentBatchSequence() + 1;
        int maxCmds = Math.min(context.getMaxCommandsPerBatch(), 5);

        return String.format("""
                === TEST SESSION INITIALIZATION ===
                TEST RUN ID: %s
                TEST NAME: %s
                EXECUTION PROFILE: %s
                TARGET BASE URL: %s
                CURRENT BATCH SEQUENCE: #%d
                MAX COMMANDS FOR THIS BATCH: %d

                === OPENAPI SPECIFICATION TO TEST ===
                %s

                === INSTRUCTIONS FOR THIS BATCH ===
                As openSpec AI, analyze the OpenAPI specification above and plan Batch #%d.
                1. Select %d high-priority endpoints to verify for %s test depth.
                2. Formulate realistic HTTP request headers (e.g. {"Accept": "application/json"}), query parameters, and JSON payloads matching the schema.
                3. Set expected status codes (e.g. [200] for GET, [200, 201] for POST, [400, 422] for invalid inputs).
                4. Set nextAction to "EXECUTE_BATCH".

                === REQUIRED JSON RESPONSE FORMAT ===
                Return a single JSON object strictly following this structure:
                {
                  "type": "TEST_BATCH",
                  "testRunId": "%s",
                  "testId": "%s",
                  "testName": "%s",
                  "testType": "%s",
                  "description": "Brief explanation of this batch's test goals",
                  "batch": {
                    "batchId": "BATCH-%03d",
                    "sequence": %d,
                    "isLastBatch": false
                  },
                  "commands": [
                    {
                      "commandId": "CMD-001",
                      "order": 1,
                      "name": "Human-readable test name",
                      "description": "Details about what this command tests",
                      "purpose": "Specific verification goal (e.g. 'Verify catalog listing returns 200 OK with items')",
                      "request": {
                        "method": "GET",
                        "path": "/path/from/openapi",
                        "headers": {"Accept": "application/json"},
                        "queryParams": {},
                        "body": null
                      },
                      "expected": {
                        "statusCodes": [200],
                        "contentType": "application/json"
                      },
                      "timeoutMs": 5000
                    }
                  ],
                  "nextAction": "EXECUTE_BATCH"
                }
                """,
                context.getTestRunId(),
                context.getTestName() != null ? context.getTestName() : "Fast API Conformance Suite",
                context.getTestType(),
                context.getTargetBaseUrl() != null ? context.getTargetBaseUrl() : "https://api.example.com",
                nextSeq,
                maxCmds,
                context.getOpenApiDocument(),
                nextSeq,
                maxCmds,
                context.getTestType(),
                context.getTestRunId(),
                context.getTestId() != null ? context.getTestId() : "FAST-" + context.getTestRunId().toString().substring(0, 8),
                context.getTestName() != null ? context.getTestName() : "Fast API Conformance Suite",
                context.getTestType(),
                nextSeq,
                nextSeq
        );
    }

    private String buildAnalysisPrompt(BatchExecutionResultsMessage executionResults, TestContext context) {
        try {
            String resultsJson = objectMapper.writeValueAsString(executionResults);
            return String.format("""
                    === BATCH EXECUTION ANALYSIS ===
                    TEST RUN ID: %s
                    BATCH ID: %s
                    TEST PROFILE: %s

                    === EMPIRICAL EXECUTION RESULTS RETURNED BY ENGINE ===
                    %s

                    === INSTRUCTIONS FOR ANALYSIS ===
                    1. Evaluate each command result against expectations and the OpenAPI specification.
                    2. Check HTTP status code, response headers, response body, and duration.
                    3. If any discrepancies, unexpected 5xx crashes, schema mismatches, or missing security headers occurred, create finding objects.
                    4. Assign command verdicts: "PASSED", "FAILED", or "ERROR".
                    5. Decide nextAction:
                       - "FINISH_TESTING": if FAST testing objectives are satisfied.
                       - "EXECUTE_BATCH": if follow-up commands are required (provide "nextBatch").
                       - "ABORT_TESTING": if fatal error prevents further execution.

                    === REQUIRED JSON RESPONSE FORMAT ===
                    Return a single JSON object strictly matching:
                    {
                      "type": "BATCH_ANALYSIS",
                      "testRunId": "%s",
                      "testId": "%s",
                      "batchId": "%s",
                      "status": "PASSED" or "FAILED" or "WARNING",
                      "report": {
                        "summary": "High-level analytical summary of this batch",
                        "commandResults": [
                          {
                            "commandId": "CMD-001",
                            "status": "PASSED" or "FAILED",
                            "explanation": "Why this test passed or failed",
                            "evidence": "Observed status or response snippet"
                          }
                        ],
                        "findings": [
                          {
                            "id": "FIND-001",
                            "severity": "HIGH" or "MEDIUM" or "LOW" or "INFO",
                            "title": "Short title of the issue",
                            "description": "Full explanation of the defect or vulnerability",
                            "relatedCommandId": "CMD-001",
                            "evidence": "HTTP 500 received instead of 400"
                          }
                        ]
                      },
                      "nextAction": "FINISH_TESTING" or "EXECUTE_BATCH" or "ABORT_TESTING",
                      "nextBatch": null
                    }
                    """,
                    context.getTestRunId(),
                    executionResults.getBatchId(),
                    context.getTestType(),
                    resultsJson,
                    context.getTestRunId(),
                    executionResults.getTestId(),
                    executionResults.getBatchId()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize execution results: " + e.getMessage(), e);
        }
    }

    private <T> T parseResponse(String rawResponse, Class<T> targetClass) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("AI returned empty or null response");
        }

        String cleanedJson = extractJson(rawResponse);
        try {
            return objectMapper.readValue(cleanedJson, targetClass);
        } catch (Exception ex) {
            log.error("Failed to parse AI JSON response: {}. Raw was:\n{}", ex.getMessage(), rawResponse);
            throw new IllegalStateException("AI returned invalid JSON protocol response: " + ex.getMessage(), ex);
        }
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        // Check for markdown code fences ```json ... ```
        if (trimmed.contains("```json")) {
            int start = trimmed.indexOf("```json") + 7;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        } else if (trimmed.contains("```")) {
            int start = trimmed.indexOf("```") + 3;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        // Find enclosing '{' and '}'
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }
}
