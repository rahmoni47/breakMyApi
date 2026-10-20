package com.openspec.dto.event;

import com.openspec.enums.TestEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Server-Sent Event (SSE) payload dispatched during test run execution")
public class TestRunEventDto {

    @Schema(description = "Unique event sequence or correlation identifier", example = "evt-10293")
    private String eventId;

    @Schema(description = "Identifier of the test run emitting this event", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID testRunId;

    @Schema(description = "Event classification type", example = "TEST_STEP_STARTED")
    private TestEventType type;

    @Schema(description = "Timestamp when the event occurred", example = "2026-10-20T12:01:10Z")
    private Instant timestamp;

    @Schema(description = "Human-readable event narrative or message", example = "Executing test step: POST /v1/charges with boundary amount 999999.99")
    private String message;

    @Schema(
        description = "Optional structured event metadata or context payload (e.g. stepIndex, endpoint, HTTP status, finding details)",
        example = "{\"stepIndex\": 3, \"endpoint\": \"/v1/charges\", \"method\": \"POST\", \"status\": 200}"
    )
    private Map<String, Object> details;
}
