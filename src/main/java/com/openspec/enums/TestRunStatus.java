package com.openspec.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current lifecycle status of a test run")
public enum TestRunStatus {
    @Schema(description = "Test run queued and awaiting execution")
    PENDING,

    @Schema(description = "Test run actively executing")
    RUNNING,

    @Schema(description = "Test run finished successfully")
    COMPLETED,

    @Schema(description = "Test run encountered a fatal error")
    FAILED,

    @Schema(description = "Test run was cancelled or stopped by user request")
    STOPPED
}
