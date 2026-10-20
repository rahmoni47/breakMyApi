package com.openspec.core.orchestration;

import com.openspec.enums.TestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Configuration payload supplied to {@link TestOrchestrator} to launch a test run.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunConfiguration {

    private UUID testRunId;
    private String testName;
    private String openApiDocument;
    private String targetBaseUrl;

    @Builder.Default
    private TestType testType = TestType.FAST;

    @Builder.Default
    private int maxIterations = 3;

    @Builder.Default
    private int maxCommandsPerBatch = 5;

    @Builder.Default
    private long defaultTimeoutMs = 5000L;

    @Builder.Default
    private boolean allowLocalNetwork = false;
}
