package com.openspec.core.protocol.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openspec.core.protocol.dto.TestBatchMetadata;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.enums.MessageType;
import com.openspec.core.protocol.enums.NextAction;
import com.openspec.enums.TestType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestBatchMessage extends ProtocolMessage {

    private String testId;
    private String testName;
    private TestType testType;
    private String description;
    private TestBatchMetadata batch;
    private List<TestCommandDto> commands;
    private NextAction nextAction;

    public TestBatchMessage() {
        super(MessageType.TEST_BATCH, null);
    }

    @Builder
    public TestBatchMessage(UUID testRunId, String testId, String testName, TestType testType,
                            String description, TestBatchMetadata batch,
                            List<TestCommandDto> commands, NextAction nextAction) {
        super(MessageType.TEST_BATCH, testRunId);
        this.testId = testId;
        this.testName = testName;
        this.testType = testType;
        this.description = description;
        this.batch = batch;
        this.commands = commands;
        this.nextAction = nextAction;
    }
}
