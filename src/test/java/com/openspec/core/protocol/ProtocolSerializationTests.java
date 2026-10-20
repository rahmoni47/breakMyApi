package com.openspec.core.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspec.core.protocol.dto.ActualRequestDto;
import com.openspec.core.protocol.dto.BatchAnalysisReportDto;
import com.openspec.core.protocol.dto.CommandAnalysisResultDto;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.CommandValidationEvaluationDto;
import com.openspec.core.protocol.dto.FinalReportDto;
import com.openspec.core.protocol.dto.TestBatchMetadata;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestCommandExpectedDto;
import com.openspec.core.protocol.dto.TestCommandRequestDto;
import com.openspec.core.protocol.dto.TestFindingDto;
import com.openspec.core.protocol.enums.AnalysisStatus;
import com.openspec.core.protocol.enums.BatchExecutionStatus;
import com.openspec.core.protocol.enums.CommandStatus;
import com.openspec.core.protocol.enums.FindingSeverity;
import com.openspec.core.protocol.enums.NextAction;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.ProtocolMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import com.openspec.core.protocol.messages.TestFailedMessage;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.enums.TestRunStatus;
import com.openspec.enums.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolSerializationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final UUID TEST_RUN_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Test
    @DisplayName("Serialize and deserialize TEST_BATCH message polymorphically")
    void testTestBatchSerialization() throws Exception {
        TestBatchMessage message = TestBatchMessage.builder()
                .testRunId(TEST_RUN_ID)
                .testId("FAST-01")
                .testName("API Sanity Test")
                .testType(TestType.FAST)
                .description("Verifies health check endpoint")
                .batch(TestBatchMetadata.builder()
                        .batchId("BATCH-01")
                        .sequence(1)
                        .isLastBatch(false)
                        .build())
                .commands(List.of(
                        TestCommandDto.builder()
                                .commandId("CMD-001")
                                .order(1)
                                .name("Health check")
                                .purpose("Check 200 OK")
                                .request(TestCommandRequestDto.builder()
                                        .method("GET")
                                        .path("/health")
                                        .headers(Map.of("Accept", "application/json"))
                                        .build())
                                .expected(TestCommandExpectedDto.builder()
                                        .statusCodes(List.of(200))
                                        .contentType("application/json")
                                        .build())
                                .timeoutMs(3000L)
                                .build()
                ))
                .nextAction(NextAction.EXECUTE_BATCH)
                .build();

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).contains("\"type\":\"TEST_BATCH\"");
        assertThat(json).contains("\"commandId\":\"CMD-001\"");

        ProtocolMessage deserialized = objectMapper.readValue(json, ProtocolMessage.class);
        assertThat(deserialized).isInstanceOf(TestBatchMessage.class);
        TestBatchMessage batchMsg = (TestBatchMessage) deserialized;
        assertThat(batchMsg.getCommands()).hasSize(1);
        assertThat(batchMsg.getCommands().get(0).getName()).isEqualTo("Health check");
    }

    @Test
    @DisplayName("Serialize and deserialize BATCH_EXECUTION_RESULTS message polymorphically")
    void testBatchExecutionResultsSerialization() throws Exception {
        BatchExecutionResultsMessage message = BatchExecutionResultsMessage.builder()
                .testRunId(TEST_RUN_ID)
                .testId("FAST-01")
                .batchId("BATCH-01")
                .batchStatus(BatchExecutionStatus.COMPLETED)
                .executionResults(List.of(
                        CommandExecutionResultDto.builder()
                                .commandId("CMD-001")
                                .order(1)
                                .name("Health check")
                                .actualRequest(ActualRequestDto.builder()
                                        .method("GET")
                                        .resolvedUrl("https://api.example.com/health")
                                        .build())
                                .statusCode(200)
                                .responseHeaders(Map.of("content-type", "application/json"))
                                .responseBody("{\"status\":\"UP\"}")
                                .durationMs(45L)
                                .backendValidation(CommandValidationEvaluationDto.builder()
                                        .statusPassed(true)
                                        .contentTypePassed(true)
                                        .evidence("Status code 200 matches expected [200]")
                                        .build())
                                .build()
                ))
                .build();

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).contains("\"type\":\"BATCH_EXECUTION_RESULTS\"");

        ProtocolMessage deserialized = objectMapper.readValue(json, ProtocolMessage.class);
        assertThat(deserialized).isInstanceOf(BatchExecutionResultsMessage.class);
        BatchExecutionResultsMessage results = (BatchExecutionResultsMessage) deserialized;
        assertThat(results.getExecutionResults()).hasSize(1);
        assertThat(results.getExecutionResults().get(0).getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Serialize and deserialize BATCH_ANALYSIS message polymorphically")
    void testBatchAnalysisSerialization() throws Exception {
        BatchAnalysisMessage message = BatchAnalysisMessage.builder()
                .testRunId(TEST_RUN_ID)
                .testId("FAST-01")
                .batchId("BATCH-01")
                .status(AnalysisStatus.PASSED)
                .report(BatchAnalysisReportDto.builder()
                        .summary("Health endpoint functioning normally")
                        .findings(List.of(
                                TestFindingDto.builder()
                                        .id("FIND-01")
                                        .severity(FindingSeverity.LOW)
                                        .title("Missing cache control")
                                        .description("Health endpoint lacks Cache-Control header")
                                        .relatedCommandId("CMD-001")
                                        .build()
                        ))
                        .commandResults(List.of(
                                CommandAnalysisResultDto.builder()
                                        .commandId("CMD-001")
                                        .status(CommandStatus.PASSED)
                                        .explanation("Returned expected 200 OK")
                                        .build()
                        ))
                        .build())
                .nextAction(NextAction.FINISH_TESTING)
                .build();

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).contains("\"type\":\"BATCH_ANALYSIS\"");
        assertThat(json).contains("\"nextAction\":\"FINISH_TESTING\"");

        ProtocolMessage deserialized = objectMapper.readValue(json, ProtocolMessage.class);
        assertThat(deserialized).isInstanceOf(BatchAnalysisMessage.class);
        BatchAnalysisMessage analysis = (BatchAnalysisMessage) deserialized;
        assertThat(analysis.getReport().getFindings()).hasSize(1);
        assertThat(analysis.getNextAction()).isEqualTo(NextAction.FINISH_TESTING);
    }

    @Test
    @DisplayName("Serialize and deserialize TEST_RUN_COMPLETED message polymorphically")
    void testTestRunCompletedSerialization() throws Exception {
        TestRunCompletedMessage message = TestRunCompletedMessage.builder()
                .testRunId(TEST_RUN_ID)
                .status(TestRunStatus.COMPLETED)
                .summary("All 5 FAST tests passed.")
                .finalReport(FinalReportDto.builder()
                        .format("markdown")
                        .content("# Report\nAll passed.")
                        .build())
                .build();

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).contains("\"type\":\"TEST_RUN_COMPLETED\"");

        ProtocolMessage deserialized = objectMapper.readValue(json, ProtocolMessage.class);
        assertThat(deserialized).isInstanceOf(TestRunCompletedMessage.class);
        TestRunCompletedMessage completed = (TestRunCompletedMessage) deserialized;
        assertThat(completed.getFinalReport().getContent()).contains("# Report");
    }

    @Test
    @DisplayName("Serialize and deserialize TEST_FAILED message polymorphically")
    void testTestFailedSerialization() throws Exception {
        TestFailedMessage message = TestFailedMessage.builder()
                .testRunId(TEST_RUN_ID)
                .reason("Critical safety violation")
                .details("AI attempted to escape target host")
                .build();

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).contains("\"type\":\"TEST_FAILED\"");

        ProtocolMessage deserialized = objectMapper.readValue(json, ProtocolMessage.class);
        assertThat(deserialized).isInstanceOf(TestFailedMessage.class);
        TestFailedMessage failed = (TestFailedMessage) deserialized;
        assertThat(failed.getReason()).isEqualTo("Critical safety violation");
    }
}
