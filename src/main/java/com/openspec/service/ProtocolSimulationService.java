package com.openspec.service;

import com.openspec.core.protocol.dto.ActualRequestDto;
import com.openspec.core.protocol.dto.BatchAnalysisReportDto;
import com.openspec.core.protocol.dto.CommandAnalysisResultDto;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.CommandValidationEvaluationDto;
import com.openspec.core.protocol.dto.FinalReportDto;
import com.openspec.core.protocol.dto.TestBatchMetadata;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestCommandExpectedDto;
import com.openspec.core.protocol.dto.TestCommandRequestDto;
import com.openspec.core.protocol.dto.TestFindingDto;
import com.openspec.core.protocol.enums.AnalysisStatus;
import com.openspec.core.protocol.enums.BatchExecutionStatus;
import com.openspec.core.protocol.enums.CommandStatus;
import com.openspec.core.protocol.enums.FindingSeverity;
import com.openspec.core.protocol.enums.NextAction;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.ProtocolMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import com.openspec.core.protocol.messages.TestFailedMessage;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.enums.TestRunStatus;
import com.openspec.enums.TestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service generating realistic simulated protocol messages and real-time SSE streams
 * based on the openSpec internal testing protocol.
 * <p>
 * Designed for frontend developers to inspect sample payloads and develop UI components
 * without needing a live AI engine or target API.
 */
@Service
public class ProtocolSimulationService {

    private static final Logger log = LoggerFactory.getLogger(ProtocolSimulationService.class);

    public static final UUID SIMULATION_TEST_RUN_ID = UUID.fromString("e3b0c442-98fc-1c14-9afb-f4c8996fb924");
    public static final String SIMULATION_TEST_ID = "ecommerce-checkout-flow";
    public static final String SIMULATION_TEST_NAME = "E-Commerce Core Flow & Boundary Verification";

    /**
     * Generates a simulated {@link TestBatchMessage} representing commands proposed by the AI.
     *
     * @param sequence batch sequence number (1 for baseline checks, 2 for boundary/fuzzing)
     * @return simulated TestBatchMessage
     */
    public TestBatchMessage getSimulatedTestBatch(int sequence) {
        if (sequence <= 1) {
            return buildBatch1Commands();
        } else {
            return buildBatch2Commands();
        }
    }

    /**
     * Generates a simulated {@link BatchExecutionResultsMessage} representing raw HTTP results
     * collected by the backend executor.
     *
     * @param sequence batch sequence number (1 for baseline checks, 2 for boundary/fuzzing)
     * @return simulated BatchExecutionResultsMessage
     */
    public BatchExecutionResultsMessage getSimulatedExecutionResults(int sequence) {
        if (sequence <= 1) {
            return buildBatch1ExecutionResults();
        } else {
            return buildBatch2ExecutionResults();
        }
    }

    /**
     * Generates a simulated {@link BatchAnalysisMessage} representing the AI's analysis
     * of executed commands, verdicts, findings, and next actions.
     *
     * @param sequence batch sequence number (1 for baseline checks, 2 for boundary/fuzzing)
     * @return simulated BatchAnalysisMessage
     */
    public BatchAnalysisMessage getSimulatedBatchAnalysis(int sequence) {
        if (sequence <= 1) {
            return buildBatch1Analysis();
        } else {
            return buildBatch2Analysis();
        }
    }

    /**
     * Generates a simulated {@link TestRunCompletedMessage} representing final test completion
     * with metrics and authoritative GitHub Flavored Markdown report.
     *
     * @return simulated TestRunCompletedMessage
     */
    public TestRunCompletedMessage getSimulatedTestRunCompleted() {
        String markdownReport = """
                # openSpec Automated API Test Report

                ## 1. Executive Summary
                - **Test Run ID:** `e3b0c442-98fc-1c14-9afb-f4c8996fb924`
                - **Test Name:** E-Commerce Core Flow & Boundary Verification
                - **Target Spec:** OpenAPI 3.0.3 (E-Commerce Storefront API)
                - **Overall Status:** `COMPLETED` (Defects Identified)
                - **Total Commands Executed:** 4
                - **Passed:** 3 | **Failed:** 1 | **Skipped:** 0
                - **Success Rate:** 75.0%
                - **Total Findings:** 2 (1 HIGH, 0 MEDIUM, 1 LOW, 0 INFO)

                ---

                ## 2. Findings Summary

                | ID | Severity | Title | Endpoint | Related Command |
                |---|---|---|---|---|
                | `find-001` | **HIGH** | Unhandled 500 Internal Server Error on Negative Order Quantity | `POST /v1/orders` | `cmd-003` |
                | `find-002` | **LOW** | Missing Standard Security Headers on Error Responses | `POST /v1/orders` | `cmd-003` |

                ---

                ## 3. Detailed Findings & Recommendations

                ### [find-001] HIGH: Unhandled 500 Internal Server Error on Negative Order Quantity
                - **Endpoint:** `POST /v1/orders`
                - **Root Cause:** Passing a negative quantity value (`-5`) in the order payload triggers an unhandled `java.lang.IllegalArgumentException` in `OrderService.calculateTotal`, causing the server to return HTTP 500 instead of HTTP 400 Bad Request or HTTP 422 Unprocessable Entity.
                - **Evidence:**
                  ```json
                  {
                    "timestamp": "2026-10-20T14:20:00Z",
                    "status": 500,
                    "error": "Internal Server Error",
                    "message": "java.lang.IllegalArgumentException: Total items cannot be negative",
                    "path": "/v1/orders"
                  }
                  ```
                - **Remediation:** Add Jakarta Bean Validation annotations (`@Min(value = 1, message = "Quantity must be at least 1")`) to the `OrderItemDto` request model.

                ### [find-002] LOW: Missing Standard Security Headers on Error Responses
                - **Endpoint:** `POST /v1/orders`
                - **Description:** Error responses do not emit standard OWASP security headers (`X-Content-Type-Options: nosniff`, `Content-Security-Policy`).
                - **Remediation:** Configure Spring Security or a global servlet filter to append security headers across all error dispatches.

                ---

                ## 4. Execution Audit Trail

                | Command ID | Name | Method | Path | Status Code | Duration | Verdict |
                |---|---|---|---|---|---|---|
                | `cmd-001` | List Active Products Catalog | `GET` | `/v1/products` | 200 | 42ms | **PASSED** |
                | `cmd-002` | Create Standard Customer Order | `POST` | `/v1/orders` | 201 | 115ms | **PASSED** |
                | `cmd-003` | Negative Order Quantity Probe | `POST` | `/v1/orders` | 500 | 230ms | **FAILED** |
                | `cmd-004` | Query Parameter SQL Injection Probe | `GET` | `/v1/products` | 400 | 18ms | **PASSED** |

                *Report generated automatically by openSpec Core Engine.*
                """;

        return TestRunCompletedMessage.builder()
                .testRunId(SIMULATION_TEST_RUN_ID)
                .status(TestRunStatus.COMPLETED)
                .summary("Automated test run completed. Executed 4 commands across 2 iterative batches: 3 passed, 1 failed (75% success rate). Identified 2 findings (1 HIGH, 1 LOW).")
                .finalReport(FinalReportDto.builder()
                        .format("markdown")
                        .content(markdownReport)
                        .build())
                .build();
    }

    /**
     * Generates a simulated {@link TestFailedMessage} representing error aborts.
     *
     * @return simulated TestFailedMessage
     */
    public TestFailedMessage getSimulatedTestFailed() {
        return TestFailedMessage.builder()
                .testRunId(SIMULATION_TEST_RUN_ID)
                .reason("TARGET_HOST_UNREACHABLE")
                .details("Connection refused when attempting to connect to target server at https://api.internal-stage.example.com after 3 retries (ConnectException: Connection refused: no further information).")
                .build();
    }

    /**
     * Returns the complete chronological flow of protocol messages for a full test run.
     *
     * @return ordered list of protocol messages
     */
    public List<ProtocolMessage> getSimulatedFullFlow() {
        List<ProtocolMessage> flow = new ArrayList<>();
        flow.add(buildBatch1Commands());
        flow.add(buildBatch1ExecutionResults());
        flow.add(buildBatch1Analysis());
        flow.add(buildBatch2Commands());
        flow.add(buildBatch2ExecutionResults());
        flow.add(buildBatch2Analysis());
        flow.add(getSimulatedTestRunCompleted());
        return flow;
    }

    /**
     * Streams simulated protocol messages asynchronously over an {@link SseEmitter}.
     *
     * @param intervalMs interval in milliseconds between successive messages
     * @return active SseEmitter
     */
    public SseEmitter streamSimulation(long intervalMs) {
        long effectiveInterval = intervalMs > 0 ? intervalMs : 1000L;
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        List<ProtocolMessage> flow = getSimulatedFullFlow();

        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < flow.size(); i++) {
                    ProtocolMessage msg = flow.get(i);
                    SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                            .id("sim-evt-" + (i + 1))
                            .name(msg.getType().name())
                            .data(msg);
                    emitter.send(eventBuilder);

                    if (i < flow.size() - 1) {
                        Thread.sleep(effectiveInterval);
                    }
                }
                emitter.complete();
            } catch (IOException | IllegalStateException e) {
                log.info("Client disconnected from simulation SSE stream: {}", e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("Unexpected error in simulation SSE stream", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // --- Private Builders for Batch 1 and Batch 2 ---

    private TestBatchMessage buildBatch1Commands() {
        TestCommandDto cmd1 = TestCommandDto.builder()
                .commandId("cmd-001")
                .order(1)
                .name("List Active Products Catalog")
                .description("Fetch available product inventory with category filter")
                .purpose("Verify endpoint returns HTTP 200 with JSON items array")
                .request(TestCommandRequestDto.builder()
                        .method("GET")
                        .path("/v1/products")
                        .queryParams(Map.of("category", "electronics", "limit", "10"))
                        .headers(Map.of("Accept", "application/json"))
                        .build())
                .expected(TestCommandExpectedDto.builder()
                        .statusCodes(List.of(200))
                        .contentType("application/json")
                        .build())
                .timeoutMs(5000L)
                .build();

        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("customerId", "cust-9481");
        orderBody.put("currency", "USD");
        orderBody.put("items", List.of(Map.of("productId", "prod-101", "quantity", 2, "price", 49.99)));

        TestCommandDto cmd2 = TestCommandDto.builder()
                .commandId("cmd-002")
                .order(2)
                .name("Create Standard Customer Order")
                .description("Place an order for verified active products")
                .purpose("Verify successful order placement returns HTTP 201 Created")
                .request(TestCommandRequestDto.builder()
                        .method("POST")
                        .path("/v1/orders")
                        .headers(Map.of("Content-Type", "application/json", "Accept", "application/json"))
                        .body(orderBody)
                        .build())
                .expected(TestCommandExpectedDto.builder()
                        .statusCodes(List.of(200, 201))
                        .contentType("application/json")
                        .build())
                .timeoutMs(5000L)
                .build();

        return TestBatchMessage.builder()
                .testRunId(SIMULATION_TEST_RUN_ID)
                .testId(SIMULATION_TEST_ID)
                .testName(SIMULATION_TEST_NAME)
                .testType(TestType.FAST)
                .description("Batch 1: Baseline verification of product catalog discovery and standard order creation")
                .batch(TestBatchMetadata.builder()
                        .batchId("batch-001")
                        .sequence(1)
                        .isLastBatch(false)
                        .build())
                .commands(List.of(cmd1, cmd2))
                .nextAction(NextAction.EXECUTE_BATCH)
                .build();
    }

    private BatchExecutionResultsMessage buildBatch1ExecutionResults() {
        CommandExecutionResultDto res1 = CommandExecutionResultDto.builder()
                .commandId("cmd-001")
                .order(1)
                .name("List Active Products Catalog")
                .actualRequest(ActualRequestDto.builder()
                        .method("GET")
                        .resolvedUrl("https://api.example.com/v1/products?category=electronics&limit=10")
                        .headers(Map.of("Accept", "application/json", "User-Agent", "openSpec-agent/1.0"))
                        .build())
                .statusCode(200)
                .durationMs(42L)
                .responseHeaders(Map.of(
                        "content-type", "application/json;charset=UTF-8",
                        "x-request-id", "req-001-9872",
                        "x-cache", "MISS"
                ))
                .responseBody("{\"items\":[{\"id\":\"prod-101\",\"name\":\"Wireless Noise-Canceling Headphones\",\"stock\":45,\"price\":49.99}],\"total\":1}")
                .backendValidation(CommandValidationEvaluationDto.builder()
                        .statusPassed(true)
                        .contentTypePassed(true)
                        .evidence("HTTP 200 matches expected [200]; Content-Type application/json matches")
                        .build())
                .build();

        CommandExecutionResultDto res2 = CommandExecutionResultDto.builder()
                .commandId("cmd-002")
                .order(2)
                .name("Create Standard Customer Order")
                .actualRequest(ActualRequestDto.builder()
                        .method("POST")
                        .resolvedUrl("https://api.example.com/v1/orders")
                        .headers(Map.of("Content-Type", "application/json", "Accept", "application/json", "User-Agent", "openSpec-agent/1.0"))
                        .body("{\"customerId\":\"cust-9481\",\"currency\":\"USD\",\"items\":[{\"productId\":\"prod-101\",\"quantity\":2,\"price\":49.99}]}")
                        .build())
                .statusCode(201)
                .durationMs(115L)
                .responseHeaders(Map.of(
                        "content-type", "application/json;charset=UTF-8",
                        "location", "/v1/orders/ord-5582",
                        "x-request-id", "req-002-3481"
                ))
                .responseBody("{\"orderId\":\"ord-5582\",\"status\":\"CONFIRMED\",\"totalAmount\":99.98,\"currency\":\"USD\"}")
                .backendValidation(CommandValidationEvaluationDto.builder()
                        .statusPassed(true)
                        .contentTypePassed(true)
                        .evidence("HTTP 201 matches expected [200, 201]; Content-Type application/json matches")
                        .build())
                .build();

        return BatchExecutionResultsMessage.builder()
                .testRunId(SIMULATION_TEST_RUN_ID)
                .testId(SIMULATION_TEST_ID)
                .batchId("batch-001")
                .batchStatus(BatchExecutionStatus.COMPLETED)
                .executionResults(List.of(res1, res2))
                .build();
    }

    private BatchAnalysisMessage buildBatch1Analysis() {
        CommandAnalysisResultDto cr1 = CommandAnalysisResultDto.builder()
                .commandId("cmd-001")
                .status(CommandStatus.PASSED)
                .explanation("Product catalog responded within 42ms with status 200 OK and valid JSON array.")
                .evidence("Status: 200, items array size: 1")
                .build();

        CommandAnalysisResultDto cr2 = CommandAnalysisResultDto.builder()
                .commandId("cmd-002")
                .status(CommandStatus.PASSED)
                .explanation("Standard order placement succeeded with HTTP 201 Created and confirmed orderId.")
                .evidence("Status: 201, orderId: ord-5582")
                .build();

        BatchAnalysisReportDto report = BatchAnalysisReportDto.builder()
                .summary("Batch 1 completed successfully. Both baseline product listing and order placement returned conforming HTTP statuses and schemas.")
                .commandResults(List.of(cr1, cr2))
                .findings(List.of())
                .build();

        return BatchAnalysisMessage.builder()
                .testRunId(SIMULATION_TEST_RUN_ID)
                .testId(SIMULATION_TEST_ID)
                .batchId("batch-001")
                .status(AnalysisStatus.PASSED)
                .report(report)
                .nextAction(NextAction.NEXT_TEST)
                .nextBatch(buildBatch2Commands())
                .build();
    }

    private TestBatchMessage buildBatch2Commands() {
        Map<String, Object> invalidOrderBody = new LinkedHashMap<>();
        invalidOrderBody.put("customerId", "cust-9481");
        invalidOrderBody.put("currency", "USD");
        invalidOrderBody.put("items", List.of(Map.of("productId", "prod-101", "quantity", -5, "price", 49.99)));

        TestCommandDto cmd3 = TestCommandDto.builder()
                .commandId("cmd-003")
                .order(1)
                .name("Negative Order Quantity Validation Probe")
                .description("Submit negative quantity (-5) to test input sanitization and boundary constraints")
                .purpose("Verify server rejects negative items with client error (400 Bad Request or 422 Unprocessable)")
                .request(TestCommandRequestDto.builder()
                        .method("POST")
                        .path("/v1/orders")
                        .headers(Map.of("Content-Type", "application/json", "Accept", "application/json"))
                        .body(invalidOrderBody)
                        .build())
                .expected(TestCommandExpectedDto.builder()
                        .statusCodes(List.of(400, 422))
                        .contentType("application/json")
                        .build())
                .timeoutMs(5000L)
                .build();

        TestCommandDto cmd4 = TestCommandDto.builder()
                .commandId("cmd-004")
                .order(2)
                .name("Query Parameter SQL Injection Probe")
                .description("Inject SQL metacharacters into the category query parameter")
                .purpose("Ensure unexpected characters in category filter are handled safely without DB syntax errors")
                .request(TestCommandRequestDto.builder()
                        .method("GET")
                        .path("/v1/products")
                        .queryParams(Map.of("category", "electronics' OR 1=1--"))
                        .headers(Map.of("Accept", "application/json"))
                        .build())
                .expected(TestCommandExpectedDto.builder()
                        .statusCodes(List.of(200, 400))
                        .contentType("application/json")
                        .build())
                .timeoutMs(5000L)
                .build();

        return TestBatchMessage.builder()
                .testRunId(SIMULATION_TEST_RUN_ID)
                .testId(SIMULATION_TEST_ID)
                .testName(SIMULATION_TEST_NAME)
                .testType(TestType.FAST)
                .description("Batch 2: Boundary, negative input, and injection resilience probing")
                .batch(TestBatchMetadata.builder()
                        .batchId("batch-002")
                        .sequence(2)
                        .isLastBatch(true)
                        .build())
                .commands(List.of(cmd3, cmd4))
                .nextAction(NextAction.EXECUTE_BATCH)
                .build();
    }

    private BatchExecutionResultsMessage buildBatch2ExecutionResults() {
        CommandExecutionResultDto res3 = CommandExecutionResultDto.builder()
                .commandId("cmd-003")
                .order(1)
                .name("Negative Order Quantity Validation Probe")
                .actualRequest(ActualRequestDto.builder()
                        .method("POST")
                        .resolvedUrl("https://api.example.com/v1/orders")
                        .headers(Map.of("Content-Type", "application/json", "Accept", "application/json", "User-Agent", "openSpec-agent/1.0"))
                        .body("{\"customerId\":\"cust-9481\",\"currency\":\"USD\",\"items\":[{\"productId\":\"prod-101\",\"quantity\":-5,\"price\":49.99}]}")
                        .build())
                .statusCode(500)
                .durationMs(230L)
                .responseHeaders(Map.of(
                        "content-type", "application/json;charset=UTF-8",
                        "x-request-id", "req-003-8812"
                ))
                .responseBody("{\"timestamp\":\"2026-10-20T14:20:00Z\",\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"java.lang.IllegalArgumentException: Total items cannot be negative\",\"path\":\"/v1/orders\"}")
                .backendValidation(CommandValidationEvaluationDto.builder()
                        .statusPassed(false)
                        .contentTypePassed(true)
                        .evidence("Expected HTTP status in [400, 422] but received 500 Internal Server Error")
                        .build())
                .build();

        CommandExecutionResultDto res4 = CommandExecutionResultDto.builder()
                .commandId("cmd-004")
                .order(2)
                .name("Query Parameter SQL Injection Probe")
                .actualRequest(ActualRequestDto.builder()
                        .method("GET")
                        .resolvedUrl("https://api.example.com/v1/products?category=electronics%27%20OR%201%3D1--")
                        .headers(Map.of("Accept", "application/json", "User-Agent", "openSpec-agent/1.0"))
                        .build())
                .statusCode(400)
                .durationMs(18L)
                .responseHeaders(Map.of(
                        "content-type", "application/json;charset=UTF-8",
                        "x-request-id", "req-004-1290"
                ))
                .responseBody("{\"error\":\"INVALID_FILTER\",\"message\":\"Category filter contains prohibited characters\"}")
                .backendValidation(CommandValidationEvaluationDto.builder()
                        .statusPassed(true)
                        .contentTypePassed(true)
                        .evidence("HTTP 400 matches expected [200, 400]; Content-Type application/json matches")
                        .build())
                .build();

        return BatchExecutionResultsMessage.builder()
                .testRunId(SIMULATION_TEST_RUN_ID)
                .testId(SIMULATION_TEST_ID)
                .batchId("batch-002")
                .batchStatus(BatchExecutionStatus.COMPLETED)
                .executionResults(List.of(res3, res4))
                .build();
    }

    private BatchAnalysisMessage buildBatch2Analysis() {
        CommandAnalysisResultDto cr3 = CommandAnalysisResultDto.builder()
                .commandId("cmd-003")
                .status(CommandStatus.FAILED)
                .explanation("Submitting a negative quantity crashed the server with HTTP 500 instead of a standard 400/422 validation response.")
                .evidence("HTTP 500 Internal Server Error: java.lang.IllegalArgumentException: Total items cannot be negative")
                .build();

        CommandAnalysisResultDto cr4 = CommandAnalysisResultDto.builder()
                .commandId("cmd-004")
                .status(CommandStatus.PASSED)
                .explanation("SQL injection probe rejected properly with HTTP 400 Bad Request.")
                .evidence("HTTP 400 Bad Request with sanitized error response")
                .build();

        TestFindingDto f1 = TestFindingDto.builder()
                .id("find-001")
                .severity(FindingSeverity.HIGH)
                .title("Unhandled 500 Internal Server Error on Negative Order Quantity")
                .description("Sending a negative order quantity (-5) to POST /v1/orders causes an unhandled java.lang.IllegalArgumentException yielding HTTP 500 instead of a standard 400 Bad Request validation response.")
                .relatedCommandId("cmd-003")
                .evidence("HTTP 500 Internal Server Error; message: Total items cannot be negative")
                .build();

        TestFindingDto f2 = TestFindingDto.builder()
                .id("find-002")
                .severity(FindingSeverity.LOW)
                .title("Missing Standard Security Headers on Error Responses")
                .description("HTTP 500 and 400 error responses lack X-Content-Type-Options: nosniff and Content-Security-Policy headers.")
                .relatedCommandId("cmd-003")
                .evidence("Response headers do not include X-Content-Type-Options")
                .build();

        BatchAnalysisReportDto report = BatchAnalysisReportDto.builder()
                .summary("Batch 2 identified a severe robustness defect: submitting a negative quantity triggers an unhandled 500 Internal Server Error with internal exception details leaked.")
                .commandResults(List.of(cr3, cr4))
                .findings(List.of(f1, f2))
                .build();

        return BatchAnalysisMessage.builder()
                .testRunId(SIMULATION_TEST_RUN_ID)
                .testId(SIMULATION_TEST_ID)
                .batchId("batch-002")
                .status(AnalysisStatus.FAILED)
                .report(report)
                .nextAction(NextAction.FINISH_TESTING)
                .nextBatch(null)
                .build();
    }
}
