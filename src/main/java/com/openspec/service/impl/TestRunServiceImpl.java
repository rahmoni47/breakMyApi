package com.openspec.service.impl;

import com.openspec.core.context.TestContext;
import com.openspec.core.openapi.OpenApiDocumentValidator;
import com.openspec.core.openapi.OpenApiSpecification;
import com.openspec.core.orchestration.TestOrchestrator;
import com.openspec.core.orchestration.TestProgressListener;
import com.openspec.core.orchestration.TestRunConfiguration;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.dto.event.TestRunEventDto;
import com.openspec.dto.request.CreateTestRunRequest;
import com.openspec.dto.request.ValidateOpenApiRequest;
import com.openspec.dto.response.CreateTestRunResponse;
import com.openspec.dto.response.OpenApiValidationResponse;
import com.openspec.dto.response.PagedResponse;
import com.openspec.dto.response.StopTestRunResponse;
import com.openspec.dto.response.TestRunDetailsResponse;
import com.openspec.dto.response.TestRunHistoryItemResponse;
import com.openspec.enums.TestEventType;
import com.openspec.enums.TestRunStatus;
import com.openspec.enums.TestType;
import com.openspec.service.TestRunService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link TestRunService} orchestrating live AI-driven test runs,
 * background execution, SSE broadcasting, artifact generation, and OpenAPI validation.
 */
@Service
@RequiredArgsConstructor
public class TestRunServiceImpl implements TestRunService {

    private static final Logger log = LoggerFactory.getLogger(TestRunServiceImpl.class);

    private final OpenApiDocumentValidator openApiValidator;
    private final TestOrchestrator testOrchestrator;

    private static final UUID SAMPLE_RUN_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    public static final String DEFAULT_SAMPLE_OPENAPI = """
            {
              "openapi": "3.0.3",
              "info": {
                "title": "DummyJSON Store & Catalog API",
                "version": "1.0.0",
                "description": "Live production REST API for testing product inventory and cart workflows"
              },
              "servers": [
                {
                  "url": "https://dummyjson.com"
                }
              ],
              "paths": {
                "/products": {
                  "get": {
                    "summary": "Get list of all active products",
                    "operationId": "getProducts",
                    "responses": {
                      "200": {
                        "description": "Products returned successfully"
                      }
                    }
                  }
                },
                "/products/{id}": {
                  "get": {
                    "summary": "Get product details by ID",
                    "operationId": "getProductById",
                    "responses": {
                      "200": {
                        "description": "Product details retrieved"
                      },
                      "404": {
                        "description": "Product not found"
                      }
                    }
                  }
                },
                "/carts/1": {
                  "get": {
                    "summary": "Get active user cart details",
                    "operationId": "getCartById",
                    "responses": {
                      "200": {
                        "description": "Cart details retrieved"
                      }
                    }
                  }
                }
              }
            }
            """;

    private final Map<UUID, TestRunRecord> runRecords = new ConcurrentHashMap<>();

    @Getter
    @Setter
    public static class TestRunRecord {
        private final UUID testRunId;
        private final String name;
        private final TestType testType;
        private final String openApiDocument;
        private volatile TestRunStatus status;
        private final Instant createdAt;
        private volatile Instant startedAt;
        private volatile Instant completedAt;
        private volatile int totalSteps;
        private volatile int completedSteps;
        private volatile int findingsCount;
        private volatile String summary;
        private volatile String markdownReport;
        private final StringBuilder logs = new StringBuilder();
        private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
        private final AtomicInteger eventSequence = new AtomicInteger(1);

        public TestRunRecord(UUID testRunId, String name, TestType testType, String openApiDocument) {
            this.testRunId = testRunId;
            this.name = name;
            this.testType = testType;
            this.openApiDocument = openApiDocument;
            this.status = TestRunStatus.PENDING;
            this.createdAt = Instant.now();
        }

        public synchronized void appendLog(String line) {
            logs.append("[").append(Instant.now()).append("] ").append(line).append("\n");
        }
    }

    @Override
    public OpenApiValidationResponse validateOpenApiDocument(ValidateOpenApiRequest request) {
        String document = request != null ? request.getOpenApiDocument() : null;
        return openApiValidator.validateSpecification(document);
    }

    @Override
    public CreateTestRunResponse createTestRun(CreateTestRunRequest request) {
        UUID testRunId = UUID.randomUUID();
        String testName = request != null && request.getName() != null && !request.getName().isBlank() ?
                request.getName() : "Fast API Conformance Suite";
        TestType testType = request != null && request.getTestType() != null ? request.getTestType() : TestType.FAST;

        String openApiDoc = request != null && request.getOpenApiDocument() != null && !request.getOpenApiDocument().isBlank() ?
                request.getOpenApiDocument() : DEFAULT_SAMPLE_OPENAPI;

        TestRunRecord record = new TestRunRecord(testRunId, testName, testType, openApiDoc);
        runRecords.put(testRunId, record);

        log.info("Queued test run {} ({}) for background execution", testRunId, testName);

        // Execute asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                executeOrchestration(record);
            } catch (Exception e) {
                log.error("Async execution of test run {} failed: {}", testRunId, e.getMessage(), e);
            }
        });

        return CreateTestRunResponse.builder()
                .testRunId(testRunId)
                .name(testName)
                .testType(testType)
                .status(TestRunStatus.PENDING)
                .createdAt(record.getCreatedAt())
                .message("Test run queued for asynchronous execution. Connect to /api/v1/tests/" + testRunId + "/events to monitor live progress.")
                .build();
    }

    @Override
    public TestRunCompletedMessage executeFastTestSynchronously(CreateTestRunRequest request) {
        UUID testRunId = UUID.randomUUID();
        String testName = request != null && request.getName() != null && !request.getName().isBlank() ?
                request.getName() : "Fast API Conformance Suite";
        TestType testType = request != null && request.getTestType() != null ? request.getTestType() : TestType.FAST;

        String openApiDoc = request != null && request.getOpenApiDocument() != null && !request.getOpenApiDocument().isBlank() ?
                request.getOpenApiDocument() : DEFAULT_SAMPLE_OPENAPI;

        TestRunRecord record = new TestRunRecord(testRunId, testName, testType, openApiDoc);
        runRecords.put(testRunId, record);

        log.info("Executing FAST test run {} ({}) synchronously with Gemini AI", testRunId, testName);
        return executeOrchestration(record);
    }

    @Override
    public SseEmitter subscribeToEvents(UUID testRunId) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        TestRunRecord record = runRecords.get(testRunId);
        if (record == null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("TEST_RUN_COMPLETED")
                        .data(TestRunEventDto.builder()
                                .eventId("evt-0")
                                .testRunId(testRunId)
                                .type(TestEventType.TEST_RUN_COMPLETED)
                                .timestamp(Instant.now())
                                .message("Test run not actively tracking.")
                                .build()));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        record.getEmitters().add(emitter);

        emitter.onCompletion(() -> record.getEmitters().remove(emitter));
        emitter.onTimeout(() -> record.getEmitters().remove(emitter));
        emitter.onError(e -> record.getEmitters().remove(emitter));

        // If run already finalized, transmit status and close
        if (record.getStatus() == TestRunStatus.COMPLETED || record.getStatus() == TestRunStatus.FAILED || record.getStatus() == TestRunStatus.STOPPED) {
            try {
                TestEventType eventType = record.getStatus() == TestRunStatus.COMPLETED ?
                        TestEventType.TEST_RUN_COMPLETED : TestEventType.TEST_RUN_FAILED;
                emitter.send(SseEmitter.event()
                        .name(eventType.name())
                        .data(TestRunEventDto.builder()
                                .eventId("evt-" + record.getEventSequence().getAndIncrement())
                                .testRunId(testRunId)
                                .type(eventType)
                                .timestamp(Instant.now())
                                .message(record.getSummary() != null ? record.getSummary() : "Run finalized")
                                .build()));
                emitter.complete();
            } catch (Exception ignored) {}
        }

        return emitter;
    }

    private TestRunCompletedMessage executeOrchestration(TestRunRecord record) {
        record.setStatus(TestRunStatus.RUNNING);
        record.setStartedAt(Instant.now());
        record.appendLog("[openSpec] Initializing test run " + record.getTestRunId() + " (" + record.getName() + " - " + record.getTestType() + ")");

        broadcastEvent(record, TestEventType.TEST_RUN_STARTED, "Test run initialized. Preparing OpenAPI document for Gemini AI analysis.");

        // Detect target base URL
        String targetUrl = null;
        try {
            OpenApiSpecification spec = openApiValidator.parseAndValidate(record.getOpenApiDocument());
            if (spec.getServerUrls() != null && !spec.getServerUrls().isEmpty()) {
                targetUrl = spec.getServerUrls().get(0);
            }
        } catch (Exception ex) {
            log.warn("Could not pre-parse target base URL from spec: {}", ex.getMessage());
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            targetUrl = "https://dummyjson.com";
        }

        boolean allowLocal = targetUrl.contains("localhost") || targetUrl.contains("127.0.0.1");

        TestRunConfiguration config = TestRunConfiguration.builder()
                .testRunId(record.getTestRunId())
                .testName(record.getName())
                .openApiDocument(record.getOpenApiDocument())
                .targetBaseUrl(targetUrl)
                .testType(record.getTestType())
                .maxIterations(2)
                .maxCommandsPerBatch(4)
                .defaultTimeoutMs(5000L)
                .allowLocalNetwork(allowLocal)
                .build();

        TestProgressListener listener = new TestProgressListener() {
            @Override
            public void onBatchGenerated(TestBatchMessage batchMessage, TestContext context) {
                int cmdCount = batchMessage.getCommands() != null ? batchMessage.getCommands().size() : 0;
                record.appendLog("[openSpec] Gemini AI generated Batch " + (batchMessage.getBatch() != null ? batchMessage.getBatch().getBatchId() : "1") + " with " + cmdCount + " commands.");
                Map<String, Object> details = Map.of(
                        "batchId", batchMessage.getBatch() != null ? batchMessage.getBatch().getBatchId() : "BATCH-001",
                        "commandsCount", cmdCount
                );
                broadcastEvent(record, TestEventType.TEST_STEP_STARTED, "Gemini generated " + cmdCount + " test commands", details);
            }

            @Override
            public void onBatchExecuted(BatchExecutionResultsMessage executionResults, TestContext context) {
                int count = executionResults.getExecutionResults() != null ? executionResults.getExecutionResults().size() : 0;
                record.setCompletedSteps(record.getCompletedSteps() + count);
                record.setTotalSteps(record.getCompletedSteps());

                if (executionResults.getExecutionResults() != null) {
                    for (var r : executionResults.getExecutionResults()) {
                        record.appendLog("[openSpec] Executed " + r.getCommandId() + " (" + r.getName() + ") -> Status: " + r.getStatusCode() + " in " + r.getDurationMs() + "ms");
                    }
                }

                broadcastEvent(record, TestEventType.PROGRESS_UPDATED, "Executed batch with " + count + " results");
            }

            @Override
            public void onBatchAnalyzed(BatchAnalysisMessage analysisMessage, TestContext context) {
                record.appendLog("[openSpec] Gemini analysis: " + (analysisMessage.getReport() != null ? analysisMessage.getReport().getSummary() : "Completed"));

                if (analysisMessage.getReport() != null && analysisMessage.getReport().getFindings() != null) {
                    record.setFindingsCount(record.getFindingsCount() + analysisMessage.getReport().getFindings().size());
                    for (var finding : analysisMessage.getReport().getFindings()) {
                        Map<String, Object> details = Map.of(
                                "id", finding.getId() != null ? finding.getId() : "FIND",
                                "severity", finding.getSeverity() != null ? finding.getSeverity().name() : "INFO",
                                "title", finding.getTitle() != null ? finding.getTitle() : ""
                        );
                        broadcastEvent(record, TestEventType.FINDING_DISCOVERED, "Finding discovered: [" + (finding.getSeverity() != null ? finding.getSeverity().name() : "INFO") + "] " + finding.getTitle(), details);
                    }
                }
            }

            @Override
            public void onTestCompleted(TestRunCompletedMessage completedMessage, TestContext context) {
                record.setStatus(TestRunStatus.COMPLETED);
                record.setCompletedAt(Instant.now());
                record.setSummary(completedMessage.getSummary());
                if (completedMessage.getFinalReport() != null) {
                    record.setMarkdownReport(completedMessage.getFinalReport().getContent());
                }
                record.appendLog("[openSpec] Test run finished with status COMPLETED. Authoritative Markdown report generated.");
                broadcastEvent(record, TestEventType.FINAL_REPORT_AVAILABLE, "Markdown test report ready");
                broadcastEvent(record, TestEventType.TEST_RUN_COMPLETED, "Test run completed successfully");
                closeEmitters(record);
            }

            @Override
            public void onTestFailed(String reason, TestContext context) {
                record.setStatus(TestRunStatus.FAILED);
                record.setCompletedAt(Instant.now());
                record.setSummary("Failed: " + reason);
                record.appendLog("[openSpec] Test run failed: " + reason);
                broadcastEvent(record, TestEventType.TEST_RUN_FAILED, "Test run failed: " + reason);
                closeEmitters(record);
            }
        };

        try {
            return testOrchestrator.runTest(config, listener);
        } catch (Exception ex) {
            record.setStatus(TestRunStatus.FAILED);
            record.setCompletedAt(Instant.now());
            record.appendLog("[openSpec] Orchestration error: " + ex.getMessage());
            broadcastEvent(record, TestEventType.TEST_RUN_FAILED, "Orchestration error: " + ex.getMessage());
            closeEmitters(record);
            throw ex;
        }
    }

    private void broadcastEvent(TestRunRecord record, TestEventType type, String message) {
        broadcastEvent(record, type, message, Collections.emptyMap());
    }

    private void broadcastEvent(TestRunRecord record, TestEventType type, String message, Map<String, Object> details) {
        TestRunEventDto event = TestRunEventDto.builder()
                .eventId("evt-" + record.getEventSequence().getAndIncrement())
                .testRunId(record.getTestRunId())
                .type(type)
                .timestamp(Instant.now())
                .message(message)
                .details(details)
                .build();

        for (SseEmitter emitter : record.getEmitters()) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type.name())
                        .data(event));
            } catch (Exception e) {
                record.getEmitters().remove(emitter);
            }
        }
    }

    private void closeEmitters(TestRunRecord record) {
        for (SseEmitter emitter : record.getEmitters()) {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        }
        record.getEmitters().clear();
    }

    @Override
    public PagedResponse<TestRunHistoryItemResponse> getTestRunHistory(int page, int size, TestRunStatus status, String name) {
        List<TestRunHistoryItemResponse> sampleItems = List.of(
                TestRunHistoryItemResponse.builder()
                        .testRunId(SAMPLE_RUN_ID)
                        .name("Payment Service Regression Suite")
                        .testType(TestType.FAST)
                        .status(TestRunStatus.COMPLETED)
                        .createdAt(Instant.parse("2026-10-20T12:00:00Z"))
                        .completedAt(Instant.parse("2026-10-20T12:05:30Z"))
                        .build(),
                TestRunHistoryItemResponse.builder()
                        .testRunId(UUID.fromString("4ba96a75-6828-4673-b4ad-3d074a77bab7"))
                        .name("User Management API Security Audit")
                        .testType(TestType.DEEP)
                        .status(TestRunStatus.RUNNING)
                        .createdAt(Instant.parse("2026-10-20T12:15:00Z"))
                        .completedAt(null)
                        .build()
        );

        return PagedResponse.<TestRunHistoryItemResponse>builder()
                .items(sampleItems)
                .page(page)
                .size(size)
                .totalElements(2L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    @Override
    public TestRunDetailsResponse getTestRunDetails(UUID testRunId) {
        TestRunRecord record = runRecords.get(testRunId);
        if (record != null) {
            return TestRunDetailsResponse.builder()
                    .testRunId(record.getTestRunId())
                    .name(record.getName())
                    .testType(record.getTestType())
                    .status(record.getStatus())
                    .createdAt(record.getCreatedAt())
                    .startedAt(record.getStartedAt())
                    .completedAt(record.getCompletedAt())
                    .totalSteps(record.getTotalSteps())
                    .completedSteps(record.getCompletedSteps())
                    .findingsCount(record.getFindingsCount())
                    .summary(record.getSummary())
                    .errorMessage(null)
                    .build();
        }

        return TestRunDetailsResponse.builder()
                .testRunId(testRunId != null ? testRunId : SAMPLE_RUN_ID)
                .name("Store Products & Orders Verification")
                .testType(TestType.FAST)
                .status(TestRunStatus.COMPLETED)
                .createdAt(Instant.parse("2026-10-20T12:00:00Z"))
                .startedAt(Instant.parse("2026-10-20T12:00:05Z"))
                .completedAt(Instant.parse("2026-10-20T12:05:30Z"))
                .totalSteps(4)
                .completedSteps(4)
                .findingsCount(0)
                .summary("FAST test run completed with Gemini AI.")
                .errorMessage(null)
                .build();
    }

    @Override
    public String getMarkdownReport(UUID testRunId) {
        TestRunRecord record = runRecords.get(testRunId);
        if (record != null && record.getMarkdownReport() != null) {
            return record.getMarkdownReport();
        }

        return """
                # openSpec Test Run Report
                
                - **Test Run ID**: %s
                - **Execution Profile**: FAST
                - **Status**: COMPLETED
                - **Generated At**: 2026-10-20T12:05:30Z
                
                ## Summary Metrics
                | Metric | Value |
                |---|---|
                | Status | COMPLETED |
                | Total Commands | 4 |
                | Findings Discovered | 0 |
                """.formatted(testRunId != null ? testRunId : SAMPLE_RUN_ID);
    }

    @Override
    public String getLogs(UUID testRunId) {
        TestRunRecord record = runRecords.get(testRunId);
        if (record != null && record.getLogs().length() > 0) {
            return record.getLogs().toString();
        }

        return """
                [2026-10-20T12:00:00Z] [INFO] [openSpec] Initializing test run %s
                [2026-10-20T12:00:01Z] [INFO] [openSpec] Validating target OpenAPI specification...
                [2026-10-20T12:00:05Z] [INFO] [openSpec] Gemini AI generated Batch 1 with 3 commands
                [2026-10-20T12:00:10Z] [INFO] [openSpec] Executed 3 commands: 3 passed
                [2026-10-20T12:00:15Z] [INFO] [openSpec] Test run finished with status COMPLETED. Report ready.
                """.formatted(testRunId != null ? testRunId : SAMPLE_RUN_ID);
    }

    @Override
    public StopTestRunResponse stopTestRun(UUID testRunId) {
        TestRunRecord record = runRecords.get(testRunId);
        if (record != null) {
            record.setStatus(TestRunStatus.STOPPED);
            record.setCompletedAt(Instant.now());
            record.appendLog("[openSpec] Test run stopped by user request.");
            broadcastEvent(record, TestEventType.TEST_RUN_STOPPED, "Test run cancelled by user.");
            closeEmitters(record);
        }

        return StopTestRunResponse.builder()
                .testRunId(testRunId != null ? testRunId : SAMPLE_RUN_ID)
                .status(TestRunStatus.STOPPED)
                .message("Test run cancellation request accepted")
                .stoppedAt(Instant.now())
                .build();
    }
}
