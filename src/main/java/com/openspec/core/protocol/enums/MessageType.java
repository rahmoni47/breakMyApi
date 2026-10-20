package com.openspec.core.protocol.enums;

/**
 * Discriminator type identifying protocol messages exchanged between openSpec backend and AI.
 */
public enum MessageType {
    TEST_BATCH,
    BATCH_EXECUTION_RESULTS,
    BATCH_ANALYSIS,
    TEST_RUN_COMPLETED,
    TEST_FAILED
}
