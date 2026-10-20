package com.openspec.core.protocol.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openspec.core.protocol.dto.BatchAnalysisReportDto;
import com.openspec.core.protocol.enums.AnalysisStatus;
import com.openspec.core.protocol.enums.MessageType;
import com.openspec.core.protocol.enums.NextAction;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchAnalysisMessage extends ProtocolMessage {

    private String testId;
    private String batchId;
    private AnalysisStatus status;
    private BatchAnalysisReportDto report;
    private NextAction nextAction;
    private TestBatchMessage nextBatch;

    public BatchAnalysisMessage() {
        super(MessageType.BATCH_ANALYSIS, null);
    }

    @Builder
    public BatchAnalysisMessage(UUID testRunId, String testId, String batchId,
                                AnalysisStatus status, BatchAnalysisReportDto report,
                                NextAction nextAction, TestBatchMessage nextBatch) {
        super(MessageType.BATCH_ANALYSIS, testRunId);
        this.testId = testId;
        this.batchId = batchId;
        this.status = status;
        this.report = report;
        this.nextAction = nextAction;
        this.nextBatch = nextBatch;
    }
}
