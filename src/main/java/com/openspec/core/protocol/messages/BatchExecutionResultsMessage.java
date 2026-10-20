package com.openspec.core.protocol.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.enums.BatchExecutionStatus;
import com.openspec.core.protocol.enums.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchExecutionResultsMessage extends ProtocolMessage {

    private String testId;
    private String batchId;
    private List<CommandExecutionResultDto> executionResults;
    private BatchExecutionStatus batchStatus;
    private String instruction;

    public BatchExecutionResultsMessage() {
        super(MessageType.BATCH_EXECUTION_RESULTS, null);
    }

    @Builder
    public BatchExecutionResultsMessage(UUID testRunId, String testId, String batchId,
                                        List<CommandExecutionResultDto> executionResults,
                                        BatchExecutionStatus batchStatus, String instruction) {
        super(MessageType.BATCH_EXECUTION_RESULTS, testRunId);
        this.testId = testId;
        this.batchId = batchId;
        this.executionResults = executionResults;
        this.batchStatus = batchStatus;
        this.instruction = instruction != null ? instruction :
                "Please analyze these execution results against expectations and the OpenAPI contract. "
                        + "Identify findings, determine command pass/fail verdicts, and return a BATCH_ANALYSIS message with your next action.";
    }
}
