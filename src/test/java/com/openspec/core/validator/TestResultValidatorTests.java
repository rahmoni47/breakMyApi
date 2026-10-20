package com.openspec.core.validator;

import com.openspec.core.protocol.dto.CommandValidationEvaluationDto;
import com.openspec.core.protocol.dto.TestCommandDto;
import com.openspec.core.protocol.dto.TestCommandExpectedDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestResultValidatorTests {

    private final TestResultValidator validator = new TestResultValidator();

    @Test
    @DisplayName("Evaluate matching status code and matching content-type as passed")
    void testMatchingResponsePassed() {
        TestCommandDto command = TestCommandDto.builder()
                .commandId("CMD-01")
                .expected(TestCommandExpectedDto.builder()
                        .statusCodes(List.of(200, 201))
                        .contentType("application/json")
                        .build())
                .build();

        CommandValidationEvaluationDto eval = validator.evaluate(
                command, 200, Map.of("Content-Type", "application/json;charset=UTF-8"), null
        );

        assertThat(eval.isStatusPassed()).isTrue();
        assertThat(eval.isContentTypePassed()).isTrue();
        assertThat(eval.getEvidence()).contains("Status code 200 matches expected [200, 201]");
    }

    @Test
    @DisplayName("Evaluate status code divergence as failed with evidence")
    void testStatusCodeDivergenceFailed() {
        TestCommandDto command = TestCommandDto.builder()
                .commandId("CMD-02")
                .expected(TestCommandExpectedDto.builder()
                        .statusCodes(List.of(200))
                        .build())
                .build();

        CommandValidationEvaluationDto eval = validator.evaluate(
                command, 500, Map.of(), null
        );

        assertThat(eval.isStatusPassed()).isFalse();
        assertThat(eval.getEvidence()).contains("Status code mismatch: received 500 but expected one of [200]");
    }

    @Test
    @DisplayName("Evaluate transport error as failed with transport error description")
    void testNetworkErrorFailed() {
        TestCommandDto command = TestCommandDto.builder()
                .commandId("CMD-03")
                .build();

        CommandValidationEvaluationDto eval = validator.evaluate(
                command, null, Map.of(), "Connection refused: /10.0.0.1:8080"
        );

        assertThat(eval.isStatusPassed()).isFalse();
        assertThat(eval.getEvidence()).contains("Transport execution failure: Connection refused");
    }
}
