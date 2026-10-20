package com.openspec.core.context;

import com.openspec.core.openapi.OpenApiSpecification;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestFindingDto;
import com.openspec.enums.TestType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Isolated runtime state for an individual test run execution.
 */
@Data
@Builder
public class TestContext {

    private final UUID testRunId;
    private final String testId;
    private final String testName;
    private final TestType testType;
    private String targetBaseUrl;
    private final String openApiDocument;
    private OpenApiSpecification parsedSpec;

    @Builder.Default
    private int currentBatchSequence = 0;

    @Builder.Default
    private final List<TestFindingDto> accumulatedFindings = Collections.synchronizedList(new ArrayList<>());

    @Builder.Default
    private final List<TestCommandDto> commandHistory = Collections.synchronizedList(new ArrayList<>());

    @Builder.Default
    private final List<CommandExecutionResultDto> executionHistory = Collections.synchronizedList(new ArrayList<>());

    @Builder.Default
    private int maxIterations = 5;

    @Builder.Default
    private int maxCommandsPerBatch = 10;

    @Builder.Default
    private long defaultTimeoutMs = 5000L;

    @Builder.Default
    private boolean allowLocalNetwork = false;

    @Builder.Default
    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    private Instant startTime;
    private Instant endTime;

    public void addFindings(List<TestFindingDto> findings) {
        if (findings != null) {
            accumulatedFindings.addAll(findings);
        }
    }

    public void recordCommands(List<TestCommandDto> commands) {
        if (commands != null) {
            commandHistory.addAll(commands);
        }
    }

    public void recordExecutionResults(List<CommandExecutionResultDto> results) {
        if (results != null) {
            executionHistory.addAll(results);
        }
    }

    public boolean isCancelled() {
        return cancellationRequested.get();
    }

    public void requestCancellation() {
        cancellationRequested.set(true);
    }
}
