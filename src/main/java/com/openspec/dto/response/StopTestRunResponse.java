package com.openspec.dto.response;

import com.openspec.enums.TestRunStatus;
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
@Schema(description = "Response returned upon test run cancellation request")
public class StopTestRunResponse {

    @Schema(description = "Identifier of the test run requested to stop", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID testRunId;

    @Schema(description = "Updated status following stop request", example = "STOPPED")
    private TestRunStatus status;

    @Schema(description = "Cancellation confirmation message", example = "Test run cancellation request accepted")
    private String message;

    @Schema(description = "Timestamp when the stop request was acknowledged", example = "2026-10-20T12:03:15Z")
    private Instant stoppedAt;
}
