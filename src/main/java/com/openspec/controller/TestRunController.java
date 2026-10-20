package com.openspec.controller;

import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.dto.event.TestRunEventDto;
import com.openspec.dto.request.CreateTestRunRequest;
import com.openspec.dto.request.ValidateOpenApiRequest;
import com.openspec.dto.response.CreateTestRunResponse;
import com.openspec.dto.response.ErrorResponse;
import com.openspec.dto.response.OpenApiValidationResponse;
import com.openspec.dto.response.PagedResponse;
import com.openspec.dto.response.StopTestRunResponse;
import com.openspec.dto.response.TestRunDetailsResponse;
import com.openspec.dto.response.TestRunHistoryItemResponse;
import com.openspec.enums.TestRunStatus;
import com.openspec.service.TestRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Controller exposing REST endpoints for OpenAPI validation, test run creation,
 * SSE event streaming, history retrieval, details, reports, logs, and
 * cancellation.
 */
@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Tag(name = "Test Run Management", description = "Endpoints for OpenAPI document validation, test run execution, SSE monitoring, history, and artifacts")
public class TestRunController {

        private final TestRunService testRunService;

        @Operation(summary = "Validate an OpenAPI/Swagger specification document", description = "Analyzes the provided OpenAPI document content (in JSON or YAML string format) for syntactic and schema correctness. "
                        + "Returns validation status, errors, warnings, detected OpenAPI version, and total endpoint count. "
                        + "Note: In this API contract phase, actual semantic validation is stubbed.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Validation analysis completed", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OpenApiValidationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Missing or empty OpenAPI document", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "422", description = "Unprocessable Entity - Malformed specification syntax", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<OpenApiValidationResponse> validateOpenApiDocument(
                        @Valid @RequestBody ValidateOpenApiRequest request) {
                OpenApiValidationResponse response = testRunService.validateOpenApiDocument(request);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Create and enqueue a new test run", description = "Submits a new test run for asynchronous execution with a specified OpenAPI document, descriptive name, "
                        + "and testing profile depth (FAST, MEDIUM, or DEEP). Returns HTTP 202 Accepted with the generated testRunId. "
                        + "Clients should then connect to the SSE endpoint GET /api/v1/tests/{testRunId}/events to stream execution progress.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "202", description = "Test run accepted and queued for asynchronous execution", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CreateTestRunResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Validation failure on request payload", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "422", description = "Unprocessable Entity - Invalid OpenAPI specification", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<CreateTestRunResponse> createTestRun(
                        @Valid @RequestBody CreateTestRunRequest request) {
                CreateTestRunResponse response = testRunService.createTestRun(request);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }

        @Operation(summary = "Execute a FAST test run synchronously", description = "Executes an end-to-end FAST test run synchronously using Google AI Studio Gemini model. "
                        + "Sends the initial prompt to the AI explaining openSpec's testing workflow, receives the generated test batch, executes HTTP requests against the target API, and returns the complete test run details.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Fast test run executed successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestRunCompletedMessage.class))),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Validation failure on request payload", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping(value = "/fast-run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<TestRunCompletedMessage> executeFastTest(
                        @Valid @RequestBody CreateTestRunRequest request) {
                TestRunCompletedMessage response = testRunService.executeFastTestSynchronously(request);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Subscribe to real-time Server-Sent Events (SSE) for a test run", description = "Establishes a persistent SSE stream (text/event-stream) transmitting real-time execution events. "
                        + "Each event adheres to TestRunEventDto with event types defined in TestEventType "
                        + "(e.g., TEST_RUN_STARTED, TEST_STEP_STARTED, TEST_STEP_COMPLETED, FINDING_DISCOVERED, PROGRESS_UPDATED, WARNING, "
                        + "TEST_RUN_FAILED, TEST_RUN_COMPLETED, TEST_RUN_STOPPED, FINAL_REPORT_AVAILABLE).")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "SSE event stream initiated successfully", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(implementation = TestRunEventDto.class))),
                        @ApiResponse(responseCode = "404", description = "Test run not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/{testRunId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter subscribeToEvents(
                        @Parameter(description = "UUID of the test run to monitor", required = true) @PathVariable UUID testRunId) {
                return testRunService.subscribeToEvents(testRunId);
        }

        @Operation(summary = "Retrieve test run details by ID", description = "Returns complete execution details, statistics, and findings for a given test run.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Test run details retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TestRunDetailsResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Test run not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/{testRunId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<TestRunDetailsResponse> getTestRunDetails(
                        @Parameter(description = "UUID of the test run", required = true) @PathVariable UUID testRunId) {
                TestRunDetailsResponse response = testRunService.getTestRunDetails(testRunId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "List historical test runs with pagination and filters", description = "Retrieves a paginated collection of past test runs. Results can optionally be filtered by status and name.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Paginated list of test runs retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid query parameters", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<PagedResponse<TestRunHistoryItemResponse>> getTestRunHistory(
                        @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,
                        @Parameter(description = "Optional filter by test run status", example = "COMPLETED") @RequestParam(required = false) TestRunStatus status,
                        @Parameter(description = "Optional filter by test run name substring", example = "Payment") @RequestParam(required = false) String name) {
                PagedResponse<TestRunHistoryItemResponse> response = testRunService.getTestRunHistory(page, size,
                                status, name);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Retrieve final Markdown test report", description = "Returns the test report rendered in GitHub Flavored Markdown (text/markdown). "
                        + "Can be rendered directly by UI markdown viewers or exported.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Markdown test report content", content = @Content(mediaType = "text/markdown", schema = @Schema(type = "string", example = "# openSpec Test Run Report\n\n## Summary\n- **Status**: COMPLETED\n- **Endpoints Tested**: 15\n- **Findings**: 2"))),
                        @ApiResponse(responseCode = "404", description = "Test run or report not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/{testRunId}/report", produces = "text/markdown;charset=UTF-8")
        public ResponseEntity<String> getMarkdownReport(
                        @Parameter(description = "UUID of the test run", required = true) @PathVariable UUID testRunId) {
                String report = testRunService.getMarkdownReport(testRunId);
                return ResponseEntity.ok(report);
        }

        @Operation(summary = "Retrieve raw execution logs", description = "Returns the raw text execution log file (text/plain) containing runtime trace entries.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Raw execution log contents", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "[2026-10-20T12:00:00Z] [INFO] Initializing test run 3fa85f64...\n[2026-10-20T12:00:05Z] [INFO] Executing step 1/24..."))),
                        @ApiResponse(responseCode = "404", description = "Test run or logs not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/{testRunId}/logs", produces = "text/plain;charset=UTF-8")
        public ResponseEntity<String> getLogs(
                        @Parameter(description = "UUID of the test run", required = true) @PathVariable UUID testRunId) {
                String logs = testRunService.getLogs(testRunId);
                return ResponseEntity.ok(logs);
        }

        @Operation(summary = "Download raw execution logs as a file attachment", description = "Triggers a browser download of the test execution log file with Content-Disposition header.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Log file attachment", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", format = "binary"))),
                        @ApiResponse(responseCode = "404", description = "Test run or logs not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/{testRunId}/logs/download", produces = MediaType.TEXT_PLAIN_VALUE)
        public ResponseEntity<String> downloadLogs(
                        @Parameter(description = "UUID of the test run", required = true) @PathVariable UUID testRunId) {
                String logs = testRunService.getLogs(testRunId);
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"test-run-" + testRunId + ".log\"")
                                .body(logs);
        }

        @Operation(summary = "Stop / cancel an active or queued test run", description = "Requests cancellation of an in-progress or pending test run. "
                        + "Returns HTTP 202 Accepted confirming receipt of the stop command.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "202", description = "Test run stop request accepted", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StopTestRunResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Test run not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "409", description = "Conflict - Test run cannot be stopped (e.g. already COMPLETED or FAILED)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping(value = "/{testRunId}/stop", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<StopTestRunResponse> stopTestRun(
                        @Parameter(description = "UUID of the test run to stop", required = true) @PathVariable UUID testRunId) {
                StopTestRunResponse response = testRunService.stopTestRun(testRunId);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }
}
