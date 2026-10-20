package com.openspec.core.protocol.enums;

/**
 * Overall execution outcome of an executed batch of commands.
 */
public enum BatchExecutionStatus {
    COMPLETED,
    PARTIAL_FAILURE,
    FAILED
}
