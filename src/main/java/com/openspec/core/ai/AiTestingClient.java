package com.openspec.core.ai;

import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;

/**
 * Unified application-level interface for AI model communication.
 * Decouples the testing workflow from specific AI providers and SDKs.
 */
public interface AiTestingClient {

    /**
     * Solicits a structured batch of test commands from the AI based on the OpenAPI specification and test context.
     *
     * @param context active test run context
     * @return structured TestBatchMessage containing commands and nextAction
     */
    TestBatchMessage generateTestBatch(TestContext context);

    /**
     * Submits actual execution results to the AI for analysis, finding discovery, and next action determination.
     *
     * @param executionResults recorded results of the executed batch
     * @param context active test run context
     * @return structured BatchAnalysisMessage containing report and nextAction
     */
    BatchAnalysisMessage analyzeBatchResults(BatchExecutionResultsMessage executionResults, TestContext context);
}
