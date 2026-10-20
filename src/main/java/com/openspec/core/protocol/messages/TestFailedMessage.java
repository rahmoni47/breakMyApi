package com.openspec.core.protocol.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openspec.core.protocol.enums.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestFailedMessage extends ProtocolMessage {

    private String reason;
    private String details;

    public TestFailedMessage() {
        super(MessageType.TEST_FAILED, null);
    }

    @Builder
    public TestFailedMessage(UUID testRunId, String reason, String details) {
        super(MessageType.TEST_FAILED, testRunId);
        this.reason = reason;
        this.details = details;
    }
}
