package com.openspec.core.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Unified Spring AI implementation of {@link AiTestingClient}.
 * Communicates with AI models via Spring AI's {@link ChatClient} abstraction.
 */
@Component
public class SpringAiTestingClient implements AiTestingClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiTestingClient.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            You are openSpec AI, an expert automated API testing and vulnerability discovery agent.
            Your task is to analyze OpenAPI specifications and conduct systematic, automated API test runs.

            CORE SAFETY RULES:
            1. Use the provided OpenAPI document as your sole source of truth.
            2. Never invent undocumented endpoints, parameters, paths, or authentication credentials.
            3. In FAST testing mode, focus on rapid schema conformance, expected status codes, required parameters, and standard response formats.
            4. Never generate commands attempting to contact unauthorized external systems or localhost.

            COMMUNICATION PROTOCOL:
            1. You MUST communicate exclusively by returning valid, parseable JSON conforming strictly to the requested schema.
            2. Do NOT include markdown formatting outside the JSON, commentary, or conversational filler.
            3. Return only the raw JSON object.
            """;

    @Autowired
    public SpringAiTestingClient(@Autowired(required = false) ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public TestBatchMessage generateTestBatch(TestContext context) {
        String userPrompt = buildGenerateBatchPrompt(context);

        String rawResponse = executeChat(userPrompt);
        return parseResponse(rawResponse, TestBatchMessage.class);
    }

    @Override
    public BatchAnalysisMessage analyzeBatchResults(BatchExecutionResultsMessage executionResults, TestContext context) {
        String userPrompt = buildAnalysisPrompt(executionResults, context);

        String rawResponse = executeChat(userPrompt);
        return parseResponse(rawResponse, BatchAnalysisMessage.class);
    }

    private String executeChat(String userPrompt) {
        if (chatClient == null) {
            throw new IllegalStateException("ChatClient is not configured. Please supply an AI provider configuration or Mock ChatClient.");
        }

        try {
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.error("AI Provider communication failure: {}", ex.getMessage());
            throw new RuntimeException("Failed to communicate with AI model: " + ex.getMessage(), ex);
        }
    }

    private String buildGenerateBatchPrompt(TestContext context) {
        int nextSeq = context.getCurrentBatchSequence() + 1;
        return String.format("""
                TASK: Generate test batch sequence #%d for test run '%s' (Test Type: %s).

                TARGET BASE URL: %s

                OPENAPI SPECIFICATION:
                %s

                INSTRUCTIONS:
                1. Propose up to %d test commands for this batch.
                2. Every command must include:
                   - commandId: unique string (e.g. CMD-%03d)
                   - order: 1-indexed execution order
                   - name: human-readable short name
                   - description: explanation for display
                   - purpose: testing objective (e.g. "Verify 200 OK on health check")
                   - request: method, path, headers, queryParams, body (if needed)
                   - expected: statusCodes list, contentType
                   - timeoutMs: recommended timeout (e.g. 5000)
                3. Return a JSON message of type "TEST_BATCH" with fields:
                   - type: "TEST_BATCH"
                   - testRunId: "%s"
                   - testId: "%s"
                   - testName: "%s"
                   - testType: "%s"
                   - description: summary of this test batch
                   - batch: {"batchId": "BATCH-%d", "sequence": %d, "isLastBatch": false}
                   - commands: [...]
                   - nextAction: "EXECUTE_BATCH"
                """,
                nextSeq, context.getTestRunId(), context.getTestType(),
                context.getTargetBaseUrl() != null ? context.getTargetBaseUrl() : "https://api.example.com",
                context.getOpenApiDocument(),
                context.getMaxCommandsPerBatch(),
                nextSeq * 10,
                context.getTestRunId(),
                context.getTestId() != null ? context.getTestId() : "FAST-TEST-01",
                context.getTestName() != null ? context.getTestName() : "Fast API Conformance Suite",
                context.getTestType(),
                nextSeq, nextSeq
        );
    }

    private String buildAnalysisPrompt(BatchExecutionResultsMessage executionResults, TestContext context) {
        try {
            String resultsJson = objectMapper.writeValueAsString(executionResults);
            return String.format("""
                    TASK: Analyze execution results for batch %s of test run '%s'.

                    EXECUTION RESULTS:
                    %s

                    INSTRUCTIONS:
                    1. Review each command execution result against the OpenAPI contract and expectations.
                    2. Check status codes, response headers, and response bodies.
                    3. Report findings with severity (INFO, LOW, MEDIUM, HIGH, CRITICAL), title, description, and evidence.
                    4. Determine the next action:
                       - "EXECUTE_BATCH": if more tests are needed (also supply "nextBatch" object)
                       - "FINISH_TESTING": if FAST testing objectives have been satisfied
                       - "ABORT_TESTING": if a critical blocking defect occurred
                    5. Return a JSON message of type "BATCH_ANALYSIS" with fields:
                       - type: "BATCH_ANALYSIS"
                       - testRunId: "%s"
                       - testId: "%s"
                       - batchId: "%s"
                       - status: "PASSED" | "FAILED" | "WARNING"
                       - report: {"summary": "...", "findings": [...], "commandResults": [...]}
                       - nextAction: "EXECUTE_BATCH" | "FINISH_TESTING" | "ABORT_TESTING"
                       - nextBatch: (only if nextAction is EXECUTE_BATCH)
                    """,
                    executionResults.getBatchId(), context.getTestRunId(),
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
            log.error("Failed to parse AI JSON response: {}. Raw was: {}", ex.getMessage(), rawResponse);
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
