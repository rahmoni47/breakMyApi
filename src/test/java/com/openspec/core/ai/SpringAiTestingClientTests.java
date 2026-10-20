package com.openspec.core.ai;

import com.openspec.core.context.TestContext;
import com.openspec.core.protocol.enums.NextAction;
import com.openspec.core.protocol.messages.BatchAnalysisMessage;
import com.openspec.core.protocol.messages.BatchExecutionResultsMessage;
import com.openspec.core.protocol.messages.TestBatchMessage;
import com.openspec.enums.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SpringAiTestingClientTests {

    @Test
    @DisplayName("Parse AI response wrapped in markdown json code block")
    void testExtractJsonFromMarkdownCodeBlock() {
        ChatClient mockChatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        String rawAiOutput = """
                Here is the proposed test batch:
                ```json
                {
                  "type": "TEST_BATCH",
                  "testRunId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "testId": "FAST-01",
                  "testName": "Auth API Conformance",
                  "testType": "FAST",
                  "description": "Verify token endpoint",
                  "batch": {
                    "batchId": "BATCH-01",
                    "sequence": 1,
                    "isLastBatch": true
                  },
                  "commands": [
                    {
                      "commandId": "CMD-001",
                      "order": 1,
                      "name": "Get Auth Token",
                      "purpose": "Verify 200 OK",
                      "request": {
                        "method": "POST",
                        "path": "/v1/auth/token"
                      },
                      "expected": {
                        "statusCodes": [200]
                      }
                    }
                  ],
                  "nextAction": "EXECUTE_BATCH"
                }
                ```
                Let me know if you need any adjustments.
                """;

        when(mockChatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn(rawAiOutput);

        SpringAiTestingClient client = new SpringAiTestingClient(mockChatClient);

        TestContext context = TestContext.builder()
                .testRunId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
                .testType(TestType.FAST)
                .openApiDocument("{}")
                .build();

        TestBatchMessage batch = client.generateTestBatch(context);

        assertThat(batch).isNotNull();
        assertThat(batch.getTestId()).isEqualTo("FAST-01");
        assertThat(batch.getCommands()).hasSize(1);
        assertThat(batch.getCommands().get(0).getCommandId()).isEqualTo("CMD-001");
        assertThat(batch.getNextAction()).isEqualTo(NextAction.EXECUTE_BATCH);
    }

    @Test
    @DisplayName("Parse BatchAnalysisMessage successfully")
    void testParseBatchAnalysisResponse() {
        ChatClient mockChatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        String rawAnalysis = """
                {
                  "type": "BATCH_ANALYSIS",
                  "testRunId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "testId": "FAST-01",
                  "batchId": "BATCH-01",
                  "status": "PASSED",
                  "report": {
                    "summary": "All tests passed",
                    "findings": [],
                    "commandResults": []
                  },
                  "nextAction": "FINISH_TESTING"
                }
                """;

        when(mockChatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn(rawAnalysis);

        SpringAiTestingClient client = new SpringAiTestingClient(mockChatClient);

        TestContext context = TestContext.builder()
                .testRunId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
                .build();

        BatchAnalysisMessage analysis = client.analyzeBatchResults(
                BatchExecutionResultsMessage.builder().build(), context
        );

        assertThat(analysis).isNotNull();
        assertThat(analysis.getNextAction()).isEqualTo(NextAction.FINISH_TESTING);
        assertThat(analysis.getReport().getSummary()).isEqualTo("All tests passed");
    }

    @Test
    @DisplayName("Throw clear IllegalStateException when AI response is unparseable")
    void testThrowOnInvalidJson() {
        ChatClient mockChatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        when(mockChatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn("Sorry, as an AI language model I cannot help with that.");

        SpringAiTestingClient client = new SpringAiTestingClient(mockChatClient);

        TestContext context = TestContext.builder()
                .testRunId(UUID.randomUUID())
                .testType(TestType.FAST)
                .openApiDocument("{}")
                .build();

        assertThatThrownBy(() -> client.generateTestBatch(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI returned invalid JSON protocol response");
    }
}
