package com.openspec.core.orchestration;

import com.openspec.core.ai.AiTestingClient;
import com.openspec.core.context.TestContext;
import com.openspec.core.executor.TestCommandExecutor;
import com.openspec.core.openapi.OpenApiDocumentValidator;
import com.openspec.core.protocol.dto.ActualRequestDto;
import com.openspec.core.protocol.dto.BatchAnalysisReportDto;
import com.openspec.core.protocol.dto.CommandAnalysisResultDto;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.CommandValidationEvaluationDto;
import com.openspec.core.protocol.dto.TestBatchMetadata;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestCommandExpectedDto;
import com.openspec.core.protocol.dto.TestCommandRequestDto;
import com.openspec.core.protocol.dto.TestFindingDto;
import com.openspec.core.protocol.enums.AnalysisStatus;
import com.openspec.core.protocol.enums.CommandStatus;
import com.openspec.core.protocol.enums.FindingSeverity;
import com.openspec.core.protocol.enums.NextAction;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.core.report.TestReportGenerator;
import com.openspec.core.validator.TestCommandValidator;
import com.openspec.enums.TestRunStatus;
import com.openspec.enums.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestOrchestratorLifecycleTests {

    private static final String SAMPLE_OPENAPI = """
            {
              "openapi": "3.0.3",
              "info": {
                "title": "Inventory Management API",
                "version": "1.0.0"
              },
              "servers": [
                {
                  "url": "https://api.inventory.com/v1"
                }
              ],
              "paths": {
                "/items": {
                  "get": {
                    "summary": "List inventory items",
                    "responses": {
                      "200": { "description": "OK" }
                    }
                  },
                  "post": {
                    "summary": "Add item",
                    "responses": {
                      "201": { "description": "Created" }
                    }
                  }
                }
              }
            }
            """;

    @Test
    @DisplayName("Complete iterative FAST lifecycle: Batch 1 -> Execute -> Analyze -> Batch 2 -> Execute -> Finish & Report")
    void testCompleteIterativeTestingLifecycle() {
        AiTestingClient aiClient = mock(AiTestingClient.class);
        TestCommandExecutor executor = mock(TestCommandExecutor.class);

        UUID testRunId = UUID.randomUUID();

        // Batch 1 definition
        TestBatchMessage batch1 = TestBatchMessage.builder()
                .testRunId(testRunId)
                .testId("FAST-01")
                .testName("Inventory Fast Conformance")
                .testType(TestType.FAST)
                .batch(TestBatchMetadata.builder().batchId("BATCH-01").sequence(1).isLastBatch(false).build())
                .commands(List.of(
                        TestCommandDto.builder()
                                .commandId("CMD-01")
                                .order(1)
                                .name("List Items")
                                .request(TestCommandRequestDto.builder().method("GET").path("/items").build())
                                .expected(TestCommandExpectedDto.builder().statusCodes(List.of(200)).build())
                                .build()
                ))
                .nextAction(NextAction.EXECUTE_BATCH)
                .build();

        // Batch 2 definition (supplied inside analysis of Batch 1)
        TestBatchMessage batch2 = TestBatchMessage.builder()
                .testRunId(testRunId)
                .testId("FAST-01")
                .testName("Inventory Fast Conformance")
                .testType(TestType.FAST)
                .batch(TestBatchMetadata.builder().batchId("BATCH-02").sequence(2).isLastBatch(true).build())
                .commands(List.of(
                        TestCommandDto.builder()
                                .commandId("CMD-02")
                                .order(2)
                                .name("Add Item")
                                .request(TestCommandRequestDto.builder().method("POST").path("/items").build())
                                .expected(TestCommandExpectedDto.builder().statusCodes(List.of(201)).build())
                                .build()
                ))
                .nextAction(NextAction.EXECUTE_BATCH)
                .build();

        // Mock AI initial batch
        when(aiClient.generateTestBatch(any(TestContext.class))).thenReturn(batch1);

        // Mock executor responses
        when(executor.execute(any(TestCommandDto.class), any(), any(TestContext.class)))
                .thenAnswer(inv -> {
                    TestCommandDto cmd = inv.getArgument(0);
                    if ("CMD-01".equals(cmd.getCommandId())) {
                        return CommandExecutionResultDto.builder()
                                .commandId("CMD-01")
                                .order(1)
                                .name(cmd.getName())
                                .actualRequest(ActualRequestDto.builder().method("GET").resolvedUrl("https://api.inventory.com/v1/items").build())
                                .statusCode(200)
                                .durationMs(40L)
                                .backendValidation(CommandValidationEvaluationDto.builder().statusPassed(true).evidence("200 OK").build())
                                .build();
                    } else {
                        return CommandExecutionResultDto.builder()
                                .commandId("CMD-02")
                                .order(2)
                                .name(cmd.getName())
                                .actualRequest(ActualRequestDto.builder().method("POST").resolvedUrl("https://api.inventory.com/v1/items").build())
                                .statusCode(201)
                                .durationMs(55L)
                                .backendValidation(CommandValidationEvaluationDto.builder().statusPassed(true).evidence("201 Created").build())
                                .build();
                    }
                });

        // Mock AI analysis for Batch 1 -> instructs EXECUTE_BATCH with batch2
        BatchAnalysisMessage analysis1 = BatchAnalysisMessage.builder()
                .testRunId(testRunId)
                .testId("FAST-01")
                .batchId("BATCH-01")
                .status(AnalysisStatus.PASSED)
                .report(BatchAnalysisReportDto.builder()
                        .summary("GET /items returned 200 OK as expected")
                        .findings(List.of())
                        .commandResults(List.of(
                                CommandAnalysisResultDto.builder().commandId("CMD-01").status(CommandStatus.PASSED).build()
                        ))
                        .build())
                .nextAction(NextAction.EXECUTE_BATCH)
                .nextBatch(batch2)
                .build();

        // Mock AI analysis for Batch 2 -> instructs FINISH_TESTING with 1 finding
        BatchAnalysisMessage analysis2 = BatchAnalysisMessage.builder()
                .testRunId(testRunId)
                .testId("FAST-01")
                .batchId("BATCH-02")
                .status(AnalysisStatus.PASSED)
                .report(BatchAnalysisReportDto.builder()
                        .summary("POST /items completed with 1 recommendation")
                        .findings(List.of(
                                TestFindingDto.builder()
                                        .id("FIND-01")
                                        .severity(FindingSeverity.LOW)
                                        .title("Missing Location header")
                                        .description("201 Created should include Location header pointing to created resource")
                                        .relatedCommandId("CMD-02")
                                        .build()
                        ))
                        .commandResults(List.of(
                                CommandAnalysisResultDto.builder().commandId("CMD-02").status(CommandStatus.PASSED).build()
                        ))
                        .build())
                .nextAction(NextAction.FINISH_TESTING)
                .build();

        AtomicInteger analysisCount = new AtomicInteger(0);
        when(aiClient.analyzeBatchResults(any(BatchExecutionResultsMessage.class), any(TestContext.class)))
                .thenAnswer(inv -> {
                    if (analysisCount.getAndIncrement() == 0) {
                        return analysis1;
                    } else {
                        return analysis2;
                    }
                });

        // Real production validators and report generator
        OpenApiDocumentValidator openApiValidator = new OpenApiDocumentValidator();
        TestCommandValidator commandValidator = new TestCommandValidator();
        TestReportGenerator reportGenerator = new TestReportGenerator();

        TestOrchestrator orchestrator = new TestOrchestrator(
                openApiValidator, commandValidator, executor, aiClient, reportGenerator
        );

        TestRunConfiguration config = TestRunConfiguration.builder()
                .testRunId(testRunId)
                .testName("Inventory Fast Conformance Suite")
                .openApiDocument(SAMPLE_OPENAPI)
                .targetBaseUrl("https://api.inventory.com/v1")
                .testType(TestType.FAST)
                .maxIterations(5)
                .maxCommandsPerBatch(5)
                .build();

        List<String> eventTrail = new ArrayList<>();
        TestProgressListener listener = new TestProgressListener() {
            @Override
            public void onTestStarted(TestContext context) {
                eventTrail.add("STARTED");
            }

            @Override
            public void onBatchGenerated(TestBatchMessage batchMessage, TestContext context) {
                eventTrail.add("BATCH_GEN_" + batchMessage.getBatch().getBatchId());
            }

            @Override
            public void onBatchExecuted(BatchExecutionResultsMessage executionResults, TestContext context) {
                eventTrail.add("BATCH_EXEC_" + executionResults.getBatchId());
            }

            @Override
            public void onBatchAnalyzed(BatchAnalysisMessage analysisMessage, TestContext context) {
                eventTrail.add("BATCH_ANALYZE_" + analysisMessage.getBatchId());
            }

            @Override
            public void onTestCompleted(TestRunCompletedMessage completedMessage, TestContext context) {
                eventTrail.add("COMPLETED");
            }
        };

        TestRunCompletedMessage result = orchestrator.runTest(config, listener);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TestRunStatus.COMPLETED);
        assertThat(result.getFinalReport().getContent()).contains("# openSpec Test Run Report");
        assertThat(result.getFinalReport().getContent()).contains("Missing Location header");
        assertThat(result.getFinalReport().getContent()).contains("| Total Commands Executed | 2 |");

        // Verify lifecycle event sequence
        assertThat(eventTrail).containsExactly(
                "STARTED",
                "BATCH_GEN_BATCH-01",
                "BATCH_EXEC_BATCH-01",
                "BATCH_ANALYZE_BATCH-01",
                "BATCH_GEN_BATCH-02",
                "BATCH_EXEC_BATCH-02",
                "BATCH_ANALYZE_BATCH-02",
                "COMPLETED"
        );
    }

    @Test
    @DisplayName("Enforce bounded execution limit to prevent infinite loops")
    void testBoundedIterationLimit() {
        AiTestingClient aiClient = mock(AiTestingClient.class);
        TestCommandExecutor executor = mock(TestCommandExecutor.class);

        UUID testRunId = UUID.randomUUID();

        TestBatchMessage endlessBatch = TestBatchMessage.builder()
                .testRunId(testRunId)
                .testId("FAST-01")
                .batch(TestBatchMetadata.builder().batchId("BATCH-LOOP").sequence(1).build())
                .commands(List.of(
                        TestCommandDto.builder()
                                .commandId("CMD-01")
                                .name("List")
                                .request(TestCommandRequestDto.builder().method("GET").path("/items").build())
                                .build()
                ))
                .nextAction(NextAction.EXECUTE_BATCH)
                .build();

        when(aiClient.generateTestBatch(any())).thenReturn(endlessBatch);
        when(executor.execute(any(), any(), any())).thenReturn(
                CommandExecutionResultDto.builder().commandId("CMD-01").statusCode(200).build()
        );

        // AI perpetually requests EXECUTE_BATCH
        BatchAnalysisMessage endlessAnalysis = BatchAnalysisMessage.builder()
                .testRunId(testRunId)
                .nextAction(NextAction.EXECUTE_BATCH)
                .nextBatch(endlessBatch)
                .report(BatchAnalysisReportDto.builder().findings(List.of()).build())
                .build();

        when(aiClient.analyzeBatchResults(any(), any())).thenReturn(endlessAnalysis);

        TestOrchestrator orchestrator = new TestOrchestrator(
                new OpenApiDocumentValidator(),
                new TestCommandValidator(),
                executor,
                aiClient,
                new TestReportGenerator()
        );

        // Limit strictly to 2 iterations
        TestRunConfiguration config = TestRunConfiguration.builder()
                .testRunId(testRunId)
                .openApiDocument(SAMPLE_OPENAPI)
                .targetBaseUrl("https://api.inventory.com/v1")
                .maxIterations(2)
                .build();

        TestRunCompletedMessage result = orchestrator.runTest(config, null);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TestRunStatus.COMPLETED);
        // Authoritative record shows exactly 2 commands executed (1 per iteration)
        assertThat(result.getFinalReport().getContent()).contains("| Total Commands Executed | 2 |");
    }
}
