package com.openspec.core.executor;

import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.TestCommandDto;

import java.net.URI;

/**
 * Interface for executing test commands against the target API.
 */
public interface TestCommandExecutor {

    /**
     * Safely executes an individual test command and returns the execution result.
     *
     * @param command validated test command
     * @param resolvedUri resolved target URI
     * @param context test run context
     * @return recorded execution result with response and validation details
     */
    CommandExecutionResultDto execute(TestCommandDto command, URI resolvedUri, TestContext context);
}
