package com.openspec.dto.response;

import com.openspec.enums.TestRunStatus;
import com.openspec.enums.TestType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprehensive details and execution metadata for a test run")
public class TestRunDetailsResponse {

    @Schema(description = "Unique identifier of the test run", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID testRunId;

    @Schema(description = "Descriptive test run name", example = "Payment Service Regression Suite")
    private String name;

    @Schema(description = "Execution profile depth", example = "FAST")
    private TestType testType;

    @Schema(description = "Current lifecycle status", example = "RUNNING")
    private TestRunStatus status;

    @Schema(description = "Timestamp when queued", example = "2026-10-20T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when execution began", example = "2026-10-20T12:00:05Z")
    private Instant startedAt;

    @Schema(description = "Timestamp when execution completed", example = "2026-10-20T12:05:30Z")
    private Instant completedAt;

    @Schema(description = "Total number of scheduled test steps", example = "24")
    private Integer totalSteps;

    @Schema(description = "Number of completed test steps so far", example = "18")
    private Integer completedSteps;

    @Schema(description = "Number of findings or anomalies discovered", example = "2")
    private Integer findingsCount;

    @Schema(description = "Brief narrative summary of progress or outcome", example = "18 of 24 steps completed with 2 findings detected")
    private String summary;

    @Schema(description = "Error description if the run failed", example = "null")
    private String errorMessage;
}
