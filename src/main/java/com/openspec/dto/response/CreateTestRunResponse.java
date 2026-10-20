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
@Schema(description = "Response returned upon accepted test run creation")
public class CreateTestRunResponse {

    @Schema(description = "Unique identifier for the test run", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID testRunId;

    @Schema(description = "Name of the test run", example = "Payment Service Regression Suite")
    private String name;

    @Schema(description = "Configured test type", example = "FAST")
    private TestType testType;

    @Schema(description = "Initial status of the test run", example = "PENDING")
    private TestRunStatus status;

    @Schema(description = "Timestamp when the test run was queued", example = "2026-10-20T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Asynchronous workflow initiation message", example = "Test run queued successfully. Subscribe to SSE events to monitor execution.")
    private String message;
}
