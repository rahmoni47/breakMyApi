package com.openspec.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Event types emitted over the Server-Sent Events (SSE) stream during test execution")
public enum TestEventType {
    @Schema(description = "Test run execution has commenced")
    TEST_RUN_STARTED,

    @Schema(description = "A new test step or endpoint scenario has started")
    TEST_STEP_STARTED,

    @Schema(description = "A test step has finished execution")
    TEST_STEP_COMPLETED,

    @Schema(description = "A vulnerability, anomaly, or defect finding was discovered")
    FINDING_DISCOVERED,

    @Schema(description = "Execution progress percentage and step counter updated")
    PROGRESS_UPDATED,

    @Schema(description = "A non-fatal warning occurred during execution")
    WARNING,

    @Schema(description = "The test run execution failed")
    TEST_RUN_FAILED,

    @Schema(description = "The test run execution finished all steps")
    TEST_RUN_COMPLETED,

    @Schema(description = "The test run was cancelled or stopped")
    TEST_RUN_STOPPED,

    @Schema(description = "The final comprehensive Markdown report is ready for retrieval")
    FINAL_REPORT_AVAILABLE
}
