package com.openspec.core.validator;

import com.openspec.core.protocol.dto.CommandValidationEvaluationDto;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestCommandExpectedDto;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validates actual HTTP responses against documented expectations and produces evidence.
 */
@Component
public class TestResultValidator {

    public CommandValidationEvaluationDto evaluate(TestCommandDto command, Integer actualStatus,
                                                   Map<String, String> responseHeaders, String networkError) {

        if (networkError != null) {
            return CommandValidationEvaluationDto.builder()
                    .statusPassed(false)
                    .contentTypePassed(false)
                    .evidence("Transport execution failure: " + networkError)
                    .build();
        }

        if (actualStatus == null) {
            return CommandValidationEvaluationDto.builder()
                    .statusPassed(false)
                    .contentTypePassed(false)
                    .evidence("No HTTP status code was received from the target API")
                    .build();
        }

        TestCommandExpectedDto expected = command.getExpected();
        boolean statusPassed = true;
        boolean contentTypePassed = true;
        StringBuilder evidence = new StringBuilder();

        // Check status codes
        if (expected != null && expected.getStatusCodes() != null && !expected.getStatusCodes().isEmpty()) {
            statusPassed = expected.getStatusCodes().contains(actualStatus);
            if (statusPassed) {
                evidence.append(String.format("Status code %d matches expected %s. ",
                        actualStatus, expected.getStatusCodes()));
            } else {
                evidence.append(String.format("Status code mismatch: received %d but expected one of %s. ",
                        actualStatus, expected.getStatusCodes()));
            }
        } else {
            // Default check: 2xx or 3xx considered healthy if no expected status code declared
            statusPassed = actualStatus >= 200 && actualStatus < 400;
            evidence.append(String.format("Status code received: %d. ", actualStatus));
        }

        // Check content-type
        if (expected != null && expected.getContentType() != null && !expected.getContentType().isBlank()) {
            String actualContentType = findHeader(responseHeaders, "Content-Type");
            if (actualContentType != null && actualContentType.toLowerCase().contains(expected.getContentType().toLowerCase())) {
                contentTypePassed = true;
                evidence.append(String.format("Content-Type matches '%s'.", expected.getContentType()));
            } else {
                contentTypePassed = false;
                evidence.append(String.format("Content-Type mismatch: expected '%s' but received '%s'.",
                        expected.getContentType(), actualContentType != null ? actualContentType : "none"));
            }
        }

        return CommandValidationEvaluationDto.builder()
                .statusPassed(statusPassed)
                .contentTypePassed(contentTypePassed)
                .evidence(evidence.toString().trim())
                .build();
    }

    private String findHeader(Map<String, String> headers, String target) {
        if (headers == null) {
            return null;
        }
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(target))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
