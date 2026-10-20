package com.openspec.core.protocol.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openspec.core.protocol.dto.FinalReportDto;
import com.openspec.core.protocol.enums.MessageType;
import com.openspec.enums.TestRunStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestRunCompletedMessage extends ProtocolMessage {

    private TestRunStatus status;
    private String summary;
    private FinalReportDto finalReport;

    public TestRunCompletedMessage() {
        super(MessageType.TEST_RUN_COMPLETED, null);
    }

    @Builder
    public TestRunCompletedMessage(UUID testRunId, TestRunStatus status, String summary, FinalReportDto finalReport) {
        super(MessageType.TEST_RUN_COMPLETED, testRunId);
        this.status = status;
        this.summary = summary;
        this.finalReport = finalReport;
    }
}
