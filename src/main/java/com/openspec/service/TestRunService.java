package com.openspec.service;

import com.openspec.dto.request.CreateTestRunRequest;
import com.openspec.dto.request.ValidateOpenApiRequest;
import com.openspec.dto.response.CreateTestRunResponse;
import com.openspec.dto.response.OpenApiValidationResponse;
import com.openspec.dto.response.PagedResponse;
import com.openspec.dto.response.StopTestRunResponse;
import com.openspec.dto.response.TestRunDetailsResponse;
import com.openspec.dto.response.TestRunHistoryItemResponse;
import com.openspec.enums.TestRunStatus;

import java.util.UUID;

/**
 * Service contract for test run operations and lifecycle management.
 * In this API contract phase, methods return contract stubs without persistent storage or background execution.
 */
public interface TestRunService {

    /**
     * Validates an OpenAPI specification document structure and schema conformance.
     *
     * @param request validation request payload
     * @return validation report with errors and warnings
     */
    OpenApiValidationResponse validateOpenApiDocument(ValidateOpenApiRequest request);

    /**
     * Enqueues a new asynchronous test run.
     *
     * @param request test run creation parameters
     * @return creation response with generated testRunId and initial status
     */
    CreateTestRunResponse createTestRun(CreateTestRunRequest request);

    /**
     * Retrieves a paginated history list of test runs.
     *
     * @param page zero-based page index
     * @param size page size
     * @param status optional status filter
     * @param name optional name filter
     * @return paginated test run items
     */
    PagedResponse<TestRunHistoryItemResponse> getTestRunHistory(int page, int size, TestRunStatus status, String name);

    /**
     * Retrieves full execution details and metadata for a specific test run.
     *
     * @param testRunId test run identifier
     * @return test run details
     */
    TestRunDetailsResponse getTestRunDetails(UUID testRunId);

    /**
     * Retrieves the generated Markdown test report.
     *
     * @param testRunId test run identifier
     * @return GitHub Flavored Markdown report string
     */
    String getMarkdownReport(UUID testRunId);

    /**
     * Retrieves the raw execution log file contents.
     *
     * @param testRunId test run identifier
     * @return plain text execution logs
     */
    String getLogs(UUID testRunId);

    /**
     * Requests cancellation of an in-progress or queued test run.
     *
     * @param testRunId test run identifier
     * @return cancellation acknowledgment response
     */
    StopTestRunResponse stopTestRun(UUID testRunId);

    /**
     * Subscribes to live Server-Sent Events (SSE) for an in-progress test run.
     *
     * @param testRunId test run identifier
     * @return active SseEmitter
     */
    org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribeToEvents(UUID testRunId);

    /**
     * Executes an immediate, synchronous FAST test run using Gemini AI and returns the full completed report.
     *
     * @param request optional request details (defaults to standard store test suite if null/blank)
     * @return completed test run message with Markdown report
     */
    com.openspec.core.protocol.messages.TestRunCompletedMessage executeFastTestSynchronously(CreateTestRunRequest request);
}
