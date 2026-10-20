package com.openspec.core.protocol.enums;

/**
 * Action instructed by the AI after analyzing batch execution results.
 */
public enum NextAction {
    EXECUTE_BATCH,
    NEXT_TEST,
    FINISH_TESTING,
    ABORT_TESTING
}
