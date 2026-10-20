package com.openspec.controller;

import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.ProtocolMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import com.openspec.core.protocol.messages.TestFailedMessage;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.service.ProtocolSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Controller exposing mock sandbox endpoints and real-time SSE streams demonstrating
 * the internal openSpec testing protocol.
 * <p>
 * Provides the frontend team with realistic sample payloads for all protocol message types
 * (TEST_BATCH, BATCH_EXECUTION_RESULTS, BATCH_ANALYSIS, TEST_RUN_COMPLETED, TEST_FAILED)
 * and an interactive event stream to validate UI integration without requiring live AI models.
 */
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
@Tag(name = "Protocol Simulation", description = "Mock sandbox demonstrating the internal testing communication protocol for frontend handoff")
public class ProtocolSimulationController {

    private final ProtocolSimulationService simulationService;

    @Operation(
            summary = "Get simulated TEST_BATCH message",
            description = "Returns a sample TestBatchMessage representing commands proposed by the AI planner. "
                    + "Includes target endpoint methods, paths, headers, JSON request payloads, and expected HTTP status codes. "
                    + "Sequence 1 returns baseline discovery checks; Sequence 2 returns boundary and security fuzzing checks."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulated test batch retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestBatchMessage.class)))
    })
    @GetMapping(value = "/test-batch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestBatchMessage> getSimulatedTestBatch(
            @Parameter(description = "Batch sequence number (1 for baseline tests, 2 for boundary/fuzzing tests)")
            @RequestParam(defaultValue = "1") int sequence) {
        return ResponseEntity.ok(simulationService.getSimulatedTestBatch(sequence));
    }

    @Operation(
            summary = "Get simulated BATCH_EXECUTION_RESULTS message",
            description = "Returns a sample BatchExecutionResultsMessage as emitted by the execution engine after executing commands against the target server. "
                    + "Contains resolved URLs, HTTP status codes, response headers, response bodies, execution durations, and backend assertion verdicts. "
                    + "Sequence 1 demonstrates successful responses (200/201); Sequence 2 demonstrates an unhandled 500 server crash on negative quantity."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulated execution results retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BatchExecutionResultsMessage.class)))
    })
    @GetMapping(value = "/execution-results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchExecutionResultsMessage> getSimulatedExecutionResults(
            @Parameter(description = "Batch sequence number (1 for baseline results, 2 for boundary results)")
            @RequestParam(defaultValue = "1") int sequence) {
        return ResponseEntity.ok(simulationService.getSimulatedExecutionResults(sequence));
    }

    @Operation(
            summary = "Get simulated BATCH_ANALYSIS message",
            description = "Returns a sample BatchAnalysisMessage as returned by the AI analyzer after evaluating execution results against the OpenAPI contract. "
                    + "Contains command PASS/FAIL verdicts, explanations, and severity-ranked findings (e.g. HIGH finding on 500 server crash). "
                    + "Also specifies nextAction (CONTINUE_NEXT_BATCH or FINISH_TEST_RUN)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulated batch analysis retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BatchAnalysisMessage.class)))
    })
    @GetMapping(value = "/batch-analysis", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchAnalysisMessage> getSimulatedBatchAnalysis(
            @Parameter(description = "Batch sequence number (1 for baseline analysis, 2 for boundary analysis)")
            @RequestParam(defaultValue = "1") int sequence) {
        return ResponseEntity.ok(simulationService.getSimulatedBatchAnalysis(sequence));
    }

    @Operation(
            summary = "Get simulated TEST_RUN_COMPLETED message",
            description = "Returns a sample TestRunCompletedMessage concluding the test run. "
                    + "Contains overall status, high-level summary, and the final GitHub Flavored Markdown report with executive summary, "
                    + "findings table, and execution audit trail."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulated test run completed message retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestRunCompletedMessage.class)))
    })
    @GetMapping(value = "/test-run-completed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestRunCompletedMessage> getSimulatedTestRunCompleted() {
        return ResponseEntity.ok(simulationService.getSimulatedTestRunCompleted());
    }

    @Operation(
            summary = "Get simulated TEST_FAILED message",
            description = "Returns a sample TestFailedMessage demonstrating an abnormal run abort (e.g. unreachable target host or unrecoverable error)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulated failure message retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestFailedMessage.class)))
    })
    @GetMapping(value = "/test-failed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestFailedMessage> getSimulatedTestFailed() {
        return ResponseEntity.ok(simulationService.getSimulatedTestFailed());
    }

    @Operation(
            summary = "Get full simulated protocol conversation flow",
            description = "Returns a complete chronological list of all protocol messages in order: "
                    + "Batch 1 TEST_BATCH -> Batch 1 BATCH_EXECUTION_RESULTS -> Batch 1 BATCH_ANALYSIS -> "
                    + "Batch 2 TEST_BATCH -> Batch 2 BATCH_EXECUTION_RESULTS -> Batch 2 BATCH_ANALYSIS -> "
                    + "TEST_RUN_COMPLETED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulated full flow retrieved successfully")
    })
    @GetMapping(value = "/full-flow", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProtocolMessage>> getSimulatedFullFlow() {
        return ResponseEntity.ok(simulationService.getSimulatedFullFlow());
    }

    @Operation(
            summary = "Subscribe to real-time SSE stream of simulated protocol messages",
            description = "Streams all 7 simulated protocol messages sequentially in real time via Server-Sent Events (SSE). "
                    + "Each event's name matches the MessageType (e.g. event: TEST_BATCH, event: BATCH_EXECUTION_RESULTS, event: BATCH_ANALYSIS, event: TEST_RUN_COMPLETED). "
                    + "The interval between events is configurable via intervalMs (default: 1000ms)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE event stream initiated successfully",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE))
    })
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSimulationEvents(
            @Parameter(description = "Delay between successive events in milliseconds (default: 1000)")
            @RequestParam(defaultValue = "1000") long intervalMs) {
        return simulationService.streamSimulation(intervalMs);
    }
}
