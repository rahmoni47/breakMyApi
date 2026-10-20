package com.openspec.core.orchestration;

import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;

/**
 * Non-intrusive progress and event listener for test orchestration.
 * Allows future SSE broadcasting or logging adapters without coupling to the core testing logic.
 */
public interface TestProgressListener {

    default void onTestStarted(TestContext context) {}

    default void onBatchGenerated(TestBatchMessage batchMessage, TestContext context) {}

    default void onBatchExecuted(BatchExecutionResultsMessage executionResults, TestContext context) {}

    default void onBatchAnalyzed(BatchAnalysisMessage analysisMessage, TestContext context) {}

    default void onTestCompleted(TestRunCompletedMessage completedMessage, TestContext context) {}

    default void onTestFailed(String reason, TestContext context) {}
}
