package com.openspec.core.orchestration;

import com.openspec.core.ai.AiTestingClient;
import com.openspec.core.context.TestContext;
import com.openspec.core.executor.TestCommandExecutor;
import com.openspec.core.openapi.OpenApiDocumentValidator;
import com.openspec.core.openapi.OpenApiSpecification;
import com.openspec.core.protocol.dto.ActualRequestDto;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.CommandValidationEvaluationDto;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.enums.BatchExecutionStatus;
import com.openspec.core.protocol.enums.NextAction;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.core.report.TestReportGenerator;
import com.openspec.core.validator.TestCommandValidator;
import com.openspec.enums.TestRunStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Core orchestration engine managing the iterative FAST API testing workflow.
 * Coordinates AI command generation, security validation, execution, feedback loops, and report synthesis.
 */
@Service
@RequiredArgsConstructor
public class TestOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TestOrchestrator.class);

    private final OpenApiDocumentValidator openApiValidator;
    private final TestCommandValidator commandValidator;
    private final TestCommandExecutor commandExecutor;
    private final AiTestingClient aiTestingClient;
    private final TestReportGenerator reportGenerator;

    /**
     * Executes the complete iterative FAST test run according to configuration and OpenAPI specification.
     *
     * @param config test run parameters
     * @param listener optional event listener for progress notifications
     * @return completed test run message with final Markdown report
     */
    public TestRunCompletedMessage runTest(TestRunConfiguration config, TestProgressListener listener) {
        if (config == null) {
            throw new IllegalArgumentException("TestRunConfiguration cannot be null");
        }

        UUID testRunId = config.getTestRunId() != null ? config.getTestRunId() : UUID.randomUUID();

        // Step A: Parse and validate OpenAPI document
        OpenApiSpecification spec = openApiValidator.parseAndValidate(config.getOpenApiDocument());

        // Resolve target base URL
        String targetUrl = config.getTargetBaseUrl();
        if ((targetUrl == null || targetUrl.isBlank()) && spec.getServerUrls() != null && !spec.getServerUrls().isEmpty()) {
            targetUrl = spec.getServerUrls().get(0);
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("No target base URL provided or detected in OpenAPI specification");
        }

        // Initialize isolated TestContext
        TestContext context = TestContext.builder()
                .testRunId(testRunId)
                .testId("FAST-" + testRunId.toString().substring(0, 8))
                .testName(config.getTestName() != null ? config.getTestName() : spec.getTitle())
                .testType(config.getTestType())
                .targetBaseUrl(targetUrl)
                .openApiDocument(config.getOpenApiDocument())
                .parsedSpec(spec)
                .maxIterations(config.getMaxIterations())
                .maxCommandsPerBatch(config.getMaxCommandsPerBatch())
                .defaultTimeoutMs(config.getDefaultTimeoutMs())
                .allowLocalNetwork(config.isAllowLocalNetwork())
                .startTime(Instant.now())
                .build();

        if (listener != null) {
            listener.onTestStarted(context);
        }

        TestRunStatus finalStatus = TestRunStatus.COMPLETED;
        TestBatchMessage pendingBatch = null;

        try {
            // Iterative execution loop
            while (context.getCurrentBatchSequence() < context.getMaxIterations()) {
                if (context.isCancelled()) {
                    finalStatus = TestRunStatus.STOPPED;
                    log.info("Test run {} was stopped by user request.", testRunId);
                    break;
                }

                // Step B: Ask AI for test commands (or use nextBatch from previous analysis)
                TestBatchMessage currentBatch;
                if (pendingBatch != null) {
                    currentBatch = pendingBatch;
                    pendingBatch = null;
                } else {
                    currentBatch = aiTestingClient.generateTestBatch(context);
                }

                // Validate the batch structure
                validateBatch(currentBatch, context);
                context.setCurrentBatchSequence(context.getCurrentBatchSequence() + 1);
                context.recordCommands(currentBatch.getCommands());

                if (listener != null) {
                    listener.onBatchGenerated(currentBatch, context);
                }

                // Step C: Validate and execute commands
                List<CommandExecutionResultDto> executionResults = new ArrayList<>();
                for (TestCommandDto cmd : currentBatch.getCommands()) {
                    if (context.isCancelled()) {
                        finalStatus = TestRunStatus.STOPPED;
                        break;
                    }

                    TestCommandValidator.CommandValidationResult validation = commandValidator.validate(cmd, context);
                    if (!validation.valid()) {
                        // Unsafe or invalid command: recorded as failed without hitting the network
                        CommandExecutionResultDto rejectedResult = CommandExecutionResultDto.builder()
                                .commandId(cmd.getCommandId())
                                .order(cmd.getOrder())
                                .name(cmd.getName())
                                .actualRequest(ActualRequestDto.builder()
                                        .method(cmd.getRequest() != null ? cmd.getRequest().getMethod() : "UNKNOWN")
                                        .resolvedUrl(cmd.getRequest() != null ? cmd.getRequest().getPath() : "UNKNOWN")
                                        .build())
                                .statusCode(null)
                                .networkError("Command rejected by security or schema validator: " + validation.reason())
                                .backendValidation(CommandValidationEvaluationDto.builder()
                                        .statusPassed(false)
                                        .contentTypePassed(false)
                                        .evidence("Pre-execution validation failed: " + validation.reason())
                                        .build())
                                .durationMs(0L)
                                .build();
                        executionResults.add(rejectedResult);
                    } else {
                        // Execute authorized command
                        CommandExecutionResultDto execResult = commandExecutor.execute(cmd, validation.resolvedUri(), context);
                        executionResults.add(execResult);
                    }
                }

                context.recordExecutionResults(executionResults);

                // Step D: Construct execution results message
                BatchExecutionStatus batchStatus = determineBatchStatus(executionResults);
                String batchId = currentBatch.getBatch() != null && currentBatch.getBatch().getBatchId() != null ?
                        currentBatch.getBatch().getBatchId() : "BATCH-" + context.getCurrentBatchSequence();

                BatchExecutionResultsMessage resultsMessage = BatchExecutionResultsMessage.builder()
                        .testRunId(testRunId)
                        .testId(context.getTestId())
                        .batchId(batchId)
                        .executionResults(executionResults)
                        .batchStatus(batchStatus)
                        .build();

                if (listener != null) {
                    listener.onBatchExecuted(resultsMessage, context);
                }

                // Step E: Send execution results to AI for analysis
                BatchAnalysisMessage analysis = aiTestingClient.analyzeBatchResults(resultsMessage, context);

                // Record findings
                if (analysis.getReport() != null && analysis.getReport().getFindings() != null) {
                    context.addFindings(analysis.getReport().getFindings());
                }

                if (listener != null) {
                    listener.onBatchAnalyzed(analysis, context);
                }

                // Step F: Handle nextAction
                NextAction nextAction = analysis.getNextAction();
                if (nextAction == null || nextAction == NextAction.FINISH_TESTING || nextAction == NextAction.NEXT_TEST) {
                    log.info("AI signaled completion of test run {} with action: {}", testRunId, nextAction);
                    break;
                } else if (nextAction == NextAction.ABORT_TESTING) {
                    log.warn("AI signaled abortion of test run {} due to fatal condition.", testRunId);
                    finalStatus = TestRunStatus.FAILED;
                    break;
                } else if (nextAction == NextAction.EXECUTE_BATCH) {
                    if (analysis.getNextBatch() != null && analysis.getNextBatch().getCommands() != null && !analysis.getNextBatch().getCommands().isEmpty()) {
                        pendingBatch = analysis.getNextBatch();
                    }
                    // Loop continues with next iteration
                }
            }

        } catch (Exception ex) {
            log.error("Fatal error during test run orchestration: {}", ex.getMessage(), ex);
            finalStatus = TestRunStatus.FAILED;
            if (listener != null) {
                listener.onTestFailed(ex.getMessage(), context);
            }
        } finally {
            context.setEndTime(Instant.now());
        }

        // Step G: Generate final authoritative report
        TestRunCompletedMessage completedMessage = reportGenerator.generateCompletedMessage(context, finalStatus);
        if (listener != null) {
            listener.onTestCompleted(completedMessage, context);
        }

        return completedMessage;
    }

    private void validateBatch(TestBatchMessage batch, TestContext context) {
        if (batch == null) {
            throw new IllegalStateException("AI returned null TestBatchMessage");
        }
        if (batch.getCommands() == null || batch.getCommands().isEmpty()) {
            throw new IllegalStateException("AI returned a batch with zero commands");
        }
        if (batch.getCommands().size() > context.getMaxCommandsPerBatch()) {
            throw new IllegalStateException(String.format(
                    "AI batch exceeded max command limit (received %d, max allowed %d)",
                    batch.getCommands().size(), context.getMaxCommandsPerBatch()
            ));
        }

        // Check for duplicate command IDs
        Set<String> seenIds = new HashSet<>();
        for (TestCommandDto cmd : batch.getCommands()) {
            if (cmd.getCommandId() != null && !seenIds.add(cmd.getCommandId())) {
                throw new IllegalStateException("Duplicate commandId in AI batch: " + cmd.getCommandId());
            }
        }
    }

    private BatchExecutionStatus determineBatchStatus(List<CommandExecutionResultDto> results) {
        long failures = results.stream()
                .filter(r -> r.getStatusCode() == null || r.getStatusCode() >= 500 || r.getNetworkError() != null)
                .count();

        if (failures == 0) {
            return BatchExecutionStatus.COMPLETED;
        } else if (failures == results.size()) {
            return BatchExecutionStatus.FAILED;
        } else {
            return BatchExecutionStatus.PARTIAL_FAILURE;
        }
    }
}
