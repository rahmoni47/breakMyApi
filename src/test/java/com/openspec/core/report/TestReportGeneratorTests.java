package com.openspec.core.report;

import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.dto.ActualRequestDto;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.CommandValidationEvaluationDto;
import com.openspec.core.protocol.dto.TestFindingDto;
import com.openspec.core.protocol.enums.FindingSeverity;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.enums.TestRunStatus;
import com.openspec.enums.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestReportGeneratorTests {

    private final TestReportGenerator reportGenerator = new TestReportGenerator();

    @Test
    @DisplayName("Generate authoritative TestRunCompletedMessage and Markdown report")
    void testReportGenerationMetrics() {
        UUID testRunId = UUID.randomUUID();
        TestContext context = TestContext.builder()
                .testRunId(testRunId)
                .testId("FAST-01")
                .testName("Order Management API")
                .testType(TestType.FAST)
                .targetBaseUrl("https://api.orders.com")
                .openApiDocument("{}")
                .startTime(Instant.now().minusSeconds(10))
                .endTime(Instant.now())
                .currentBatchSequence(2)
                .build();

        context.recordExecutionResults(List.of(
                CommandExecutionResultDto.builder()
                        .commandId("CMD-01")
                        .actualRequest(ActualRequestDto.builder().method("GET").resolvedUrl("https://api.orders.com/orders").build())
                        .statusCode(200)
                        .durationMs(50L)
                        .backendValidation(CommandValidationEvaluationDto.builder().statusPassed(true).evidence("200 OK").build())
                        .build(),
                CommandExecutionResultDto.builder()
                        .commandId("CMD-02")
                        .actualRequest(ActualRequestDto.builder().method("POST").resolvedUrl("https://api.orders.com/orders").build())
                        .statusCode(500)
                        .durationMs(120L)
                        .backendValidation(CommandValidationEvaluationDto.builder().statusPassed(false).evidence("Expected 201 got 500").build())
                        .build()
        ));

        context.addFindings(List.of(
                TestFindingDto.builder()
                        .id("FIND-01")
                        .severity(FindingSeverity.HIGH)
                        .title("Internal Server Error on Order Creation")
                        .description("Endpoint threw 500 when creating valid order")
                        .relatedCommandId("CMD-02")
                        .evidence("HTTP 500 Internal Server Error returned")
                        .build()
        ));

        TestRunCompletedMessage message = reportGenerator.generateCompletedMessage(context, TestRunStatus.COMPLETED);

        assertThat(message.getTestRunId()).isEqualTo(testRunId);
        assertThat(message.getStatus()).isEqualTo(TestRunStatus.COMPLETED);
        assertThat(message.getSummary()).contains("2 executed commands");
        assertThat(message.getSummary()).contains("1 passed, 1 failed");
        assertThat(message.getSummary()).contains("Discovered 1 findings");

        String reportMarkdown = message.getFinalReport().getContent();
        assertThat(reportMarkdown).contains("# openSpec Test Run Report");
        assertThat(reportMarkdown).contains("### Execution Metrics");
        assertThat(reportMarkdown).contains("| Total Commands Executed | 2 |");
        assertThat(reportMarkdown).contains("| Passed Checks | 1 |");
        assertThat(reportMarkdown).contains("| Failed / Divergent Checks | 1 |");
        assertThat(reportMarkdown).contains("| HIGH | 1 |");
        assertThat(reportMarkdown).contains("Internal Server Error on Order Creation");
        assertThat(reportMarkdown).contains("## Recommended Remediation Steps");
    }
}
