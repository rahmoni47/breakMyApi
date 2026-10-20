package com.openspec.core.report;

import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.dto.CommandExecutionResultDto;
import com.openspec.core.protocol.dto.FinalReportDto;
import com.openspec.core.protocol.dto.TestFindingDto;
import com.openspec.core.protocol.enums.FindingSeverity;
import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.enums.TestRunStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Authoritative backend generator for Markdown test reports and {@link TestRunCompletedMessage}.
 * Calculates metrics directly from recorded execution evidence.
 */
@Component
public class TestReportGenerator {

    /**
     * Builds the authoritative {@link TestRunCompletedMessage} and Markdown report.
     *
     * @param context test run context containing authoritative execution records
     * @param finalStatus completion status (COMPLETED, FAILED, STOPPED)
     * @return complete message with final Markdown report
     */
    public TestRunCompletedMessage generateCompletedMessage(TestContext context, TestRunStatus finalStatus) {
        List<CommandExecutionResultDto> executionHistory = context.getExecutionHistory();
        List<TestFindingDto> findings = context.getAccumulatedFindings();

        long totalCommands = executionHistory.size();
        long passedCommands = executionHistory.stream()
                .filter(res -> res.getBackendValidation() != null && res.getBackendValidation().isStatusPassed())
                .count();
        long failedCommands = totalCommands - passedCommands;

        long criticalFindings = countSeverity(findings, FindingSeverity.CRITICAL);
        long highFindings = countSeverity(findings, FindingSeverity.HIGH);
        long mediumFindings = countSeverity(findings, FindingSeverity.MEDIUM);
        long lowFindings = countSeverity(findings, FindingSeverity.LOW);
        long infoFindings = countSeverity(findings, FindingSeverity.INFO);

        Duration executionTime = Duration.between(
                context.getStartTime() != null ? context.getStartTime() : Instant.now(),
                context.getEndTime() != null ? context.getEndTime() : Instant.now()
        );

        String summary = String.format(
                "openSpec %s test run completed in %d ms across %d executed commands (%d passed, %d failed). Discovered %d findings.",
                context.getTestType(), executionTime.toMillis(), totalCommands, passedCommands, failedCommands, findings.size()
        );

        String markdownContent = buildMarkdownReport(
                context, totalCommands, passedCommands, failedCommands,
                criticalFindings, highFindings, mediumFindings, lowFindings, infoFindings,
                executionTime
        );

        return TestRunCompletedMessage.builder()
                .testRunId(context.getTestRunId())
                .status(finalStatus)
                .summary(summary)
                .finalReport(FinalReportDto.builder()
                        .format("markdown")
                        .content(markdownContent)
                        .build())
                .build();
    }

    private String buildMarkdownReport(TestContext context, long totalCommands, long passedCommands, long failedCommands,
                                       long critical, long high, long medium, long low, long info,
                                       Duration executionTime) {

        StringBuilder sb = new StringBuilder();

        sb.append("# openSpec Test Run Report\n\n");
        sb.append("## Executive Summary\n\n");
        sb.append("- **Test Run ID**: `").append(context.getTestRunId()).append("`\n");
        sb.append("- **Test Suite**: ").append(context.getTestName() != null ? context.getTestName() : "API Test Suite").append("\n");
        sb.append("- **Test Depth**: `").append(context.getTestType()).append("`\n");
        sb.append("- **Target Base URL**: `").append(context.getTargetBaseUrl()).append("`\n");
        sb.append("- **Execution Duration**: ").append(executionTime.toMillis()).append(" ms\n");
        sb.append("- **Total Batches Executed**: ").append(context.getCurrentBatchSequence()).append("\n\n");

        sb.append("### Execution Metrics\n\n");
        sb.append("| Metric | Count |\n");
        sb.append("|---|---|\n");
        sb.append("| Total Commands Executed | ").append(totalCommands).append(" |\n");
        sb.append("| Passed Checks | ").append(passedCommands).append(" |\n");
        sb.append("| Failed / Divergent Checks | ").append(failedCommands).append(" |\n");
        sb.append("| Total Findings Discovered | ").append(context.getAccumulatedFindings().size()).append(" |\n\n");

        sb.append("### Findings Breakdown by Severity\n\n");
        sb.append("| Severity | Count |\n");
        sb.append("|---|---|\n");
        sb.append("| CRITICAL | ").append(critical).append(" |\n");
        sb.append("| HIGH | ").append(high).append(" |\n");
        sb.append("| MEDIUM | ").append(medium).append(" |\n");
        sb.append("| LOW | ").append(low).append(" |\n");
        sb.append("| INFO | ").append(info).append(" |\n\n");

        // Discovered Findings
        sb.append("## Detailed Findings\n\n");
        if (context.getAccumulatedFindings().isEmpty()) {
            sb.append("*No vulnerabilities or specification violations discovered during this run.*\n\n");
        } else {
            for (TestFindingDto finding : context.getAccumulatedFindings()) {
                sb.append("### [").append(finding.getSeverity()).append("] ").append(finding.getTitle()).append("\n\n");
                sb.append("- **Description**: ").append(finding.getDescription()).append("\n");
                if (finding.getRelatedCommandId() != null) {
                    sb.append("- **Related Command**: `").append(finding.getRelatedCommandId()).append("`\n");
                }
                if (finding.getEvidence() != null && !finding.getEvidence().isBlank()) {
                    sb.append("- **Evidence**: `").append(finding.getEvidence()).append("`\n");
                }
                sb.append("\n");
            }
        }

        // Execution Evidence Log Table
        sb.append("## Executed Commands & Evidence\n\n");
        sb.append("| Command ID | Method | Path | Status | Duration | Verdict | Evidence |\n");
        sb.append("|---|---|---|---|---|---|---|\n");

        for (CommandExecutionResultDto res : context.getExecutionHistory()) {
            String method = res.getActualRequest() != null ? res.getActualRequest().getMethod() : "-";
            String url = res.getActualRequest() != null ? res.getActualRequest().getResolvedUrl() : "-";
            String status = res.getStatusCode() != null ? String.valueOf(res.getStatusCode()) : "ERROR";
            String verdict = res.getBackendValidation() != null && res.getBackendValidation().isStatusPassed() ? "PASS" : "FAIL";
            String evidence = res.getBackendValidation() != null ? res.getBackendValidation().getEvidence() : (res.getNetworkError() != null ? res.getNetworkError() : "-");

            sb.append(String.format("| `%s` | %s | `%s` | %s | %dms | %s | %s |\n",
                    res.getCommandId(), method, url, status, res.getDurationMs(), verdict, evidence));
        }
        sb.append("\n");

        // Remediation Recommendations
        sb.append("## Recommended Remediation Steps\n\n");
        if (context.getAccumulatedFindings().isEmpty() && failedCommands == 0) {
            sb.append("1. All executed conformance tests passed successfully against the declared OpenAPI specification.\n");
            sb.append("2. Consider scheduling MEDIUM and DEEP tests in staging for stateful flow verification.\n");
        } else {
            sb.append("1. Verify documented response schemas and error status codes against API controller implementations.\n");
            sb.append("2. Ensure endpoints reject invalid inputs with standard 400 Bad Request error envelopes.\n");
            sb.append("3. Address high and critical severity findings prior to production deployment.\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    private long countSeverity(List<TestFindingDto> findings, FindingSeverity severity) {
        if (findings == null) return 0;
        return findings.stream().filter(f -> f.getSeverity() == severity).count();
    }
}
