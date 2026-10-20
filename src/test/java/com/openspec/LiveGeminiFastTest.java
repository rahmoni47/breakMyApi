package com.openspec;

import com.openspec.core.protocol.messages.TestRunCompletedMessage;
import com.openspec.dto.request.CreateTestRunRequest;
import com.openspec.enums.TestType;
import com.openspec.service.TestRunService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LiveGeminiFastTest {

    @Autowired
    private TestRunService testRunService;

    @Test
    @DisplayName("Execute live FAST test run with Google AI Studio Gemini model")
    void testLiveFastRunExecution() {
        CreateTestRunRequest request = CreateTestRunRequest.builder()
                .name("Live Gemini 2.5 Flash Lite Fast Conformance Test")
                .testType(TestType.FAST)
                .build();

        TestRunCompletedMessage completed = testRunService.executeFastTestSynchronously(request);

        assertThat(completed).isNotNull();
        assertThat(completed.getTestRunId()).isNotNull();
        assertThat(completed.getStatus()).isNotNull();
        assertThat(completed.getSummary()).isNotBlank();
        assertThat(completed.getFinalReport()).isNotNull();
        assertThat(completed.getFinalReport().getContent()).isNotBlank();

        System.out.println("\n=======================================================");
        System.out.println("=== LIVE GEMINI FAST TEST COMPLETED SUCCESSFULLY ===");
        System.out.println("=======================================================");
        System.out.println("Test Run ID: " + completed.getTestRunId());
        System.out.println("Status:      " + completed.getStatus());
        System.out.println("Summary:     " + completed.getSummary());
        System.out.println("\n--- Generated Markdown Report ---");
        System.out.println(completed.getFinalReport().getContent());
        System.out.println("=======================================================\n");
    }
}
