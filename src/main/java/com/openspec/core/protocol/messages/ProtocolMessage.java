package com.openspec.core.protocol.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.openspec.core.protocol.enums.MessageType;
import lombok.Getter;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TestBatchMessage.class, name = "TEST_BATCH"),
        @JsonSubTypes.Type(value = BatchExecutionResultsMessage.class, name = "BATCH_EXECUTION_RESULTS"),
        @JsonSubTypes.Type(value = BatchAnalysisMessage.class, name = "BATCH_ANALYSIS"),
        @JsonSubTypes.Type(value = TestRunCompletedMessage.class, name = "TEST_RUN_COMPLETED"),
        @JsonSubTypes.Type(value = TestFailedMessage.class, name = "TEST_FAILED")
})
@Getter
public abstract class ProtocolMessage {

    private final MessageType type;
    private final UUID testRunId;

    protected ProtocolMessage(MessageType type, UUID testRunId) {
        this.type = type;
        this.testRunId = testRunId;
    }
}
