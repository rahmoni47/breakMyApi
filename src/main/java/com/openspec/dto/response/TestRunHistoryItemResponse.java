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
@Schema(description = "Summary item representing a test run in historical listings")
public class TestRunHistoryItemResponse {

    @Schema(description = "Unique identifier of the test run", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID testRunId;

    @Schema(description = "Name of the test run", example = "Payment Service Regression Suite")
    private String name;

    @Schema(description = "Test type executed", example = "FAST")
    private TestType testType;

    @Schema(description = "Current lifecycle status", example = "COMPLETED")
    private TestRunStatus status;

    @Schema(description = "Creation timestamp", example = "2026-10-20T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Completion timestamp, if finished", example = "2026-10-20T12:05:30Z")
    private Instant completedAt;
}
