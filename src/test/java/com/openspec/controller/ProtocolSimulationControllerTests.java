package com.openspec.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProtocolSimulationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/simulation/test-batch returns valid TEST_BATCH message with commands")
    void testGetSimulatedTestBatch() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/test-batch")
                        .param("sequence", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("TEST_BATCH"))
                .andExpect(jsonPath("$.testRunId").value("e3b0c442-98fc-1c14-9afb-f4c8996fb924"))
                .andExpect(jsonPath("$.batch.batchId").value("batch-001"))
                .andExpect(jsonPath("$.batch.sequence").value(1))
                .andExpect(jsonPath("$.batch.isLastBatch").value(false))
                .andExpect(jsonPath("$.nextAction").value("EXECUTE_BATCH"))
                .andExpect(jsonPath("$.commands", hasSize(2)))
                .andExpect(jsonPath("$.commands[0].commandId").value("cmd-001"))
                .andExpect(jsonPath("$.commands[0].request.method").value("GET"))
                .andExpect(jsonPath("$.commands[0].request.path").value("/v1/products"));
    }

    @Test
    @DisplayName("GET /api/v1/simulation/execution-results returns valid BATCH_EXECUTION_RESULTS with responses")
    void testGetSimulatedExecutionResults() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/execution-results")
                        .param("sequence", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("BATCH_EXECUTION_RESULTS"))
                .andExpect(jsonPath("$.batchId").value("batch-002"))
                .andExpect(jsonPath("$.batchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.executionResults", hasSize(2)))
                .andExpect(jsonPath("$.executionResults[0].commandId").value("cmd-003"))
                .andExpect(jsonPath("$.executionResults[0].statusCode").value(500))
                .andExpect(jsonPath("$.executionResults[0].backendValidation.statusPassed").value(false))
                .andExpect(jsonPath("$.executionResults[1].commandId").value("cmd-004"))
                .andExpect(jsonPath("$.executionResults[1].statusCode").value(400))
                .andExpect(jsonPath("$.executionResults[1].backendValidation.statusPassed").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/simulation/batch-analysis returns BATCH_ANALYSIS message with findings")
    void testGetSimulatedBatchAnalysis() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/batch-analysis")
                        .param("sequence", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("BATCH_ANALYSIS"))
                .andExpect(jsonPath("$.batchId").value("batch-002"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.nextAction").value("FINISH_TESTING"))
                .andExpect(jsonPath("$.report.findings", hasSize(2)))
                .andExpect(jsonPath("$.report.findings[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.report.findings[0].relatedCommandId").value("cmd-003"))
                .andExpect(jsonPath("$.report.commandResults[0].status").value("FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/simulation/test-run-completed returns TEST_RUN_COMPLETED with Markdown report")
    void testGetSimulatedTestRunCompleted() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/test-run-completed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("TEST_RUN_COMPLETED"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.finalReport.format").value("markdown"))
                .andExpect(jsonPath("$.finalReport.content", containsString("# openSpec Automated API Test Report")))
                .andExpect(jsonPath("$.finalReport.content", containsString("find-001")));
    }

    @Test
    @DisplayName("GET /api/v1/simulation/test-failed returns TEST_FAILED message")
    void testGetSimulatedTestFailed() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/test-failed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("TEST_FAILED"))
                .andExpect(jsonPath("$.reason").value("TARGET_HOST_UNREACHABLE"))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("GET /api/v1/simulation/full-flow returns all 7 protocol messages in conversational sequence")
    void testGetSimulatedFullFlow() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/full-flow")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].type").value("TEST_BATCH"))
                .andExpect(jsonPath("$[1].type").value("BATCH_EXECUTION_RESULTS"))
                .andExpect(jsonPath("$[2].type").value("BATCH_ANALYSIS"))
                .andExpect(jsonPath("$[3].type").value("TEST_BATCH"))
                .andExpect(jsonPath("$[4].type").value("BATCH_EXECUTION_RESULTS"))
                .andExpect(jsonPath("$[5].type").value("BATCH_ANALYSIS"))
                .andExpect(jsonPath("$[6].type").value("TEST_RUN_COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/v1/simulation/events initiates SSE stream with text/event-stream")
    void testStreamSimulationEvents() throws Exception {
        mockMvc.perform(get("/api/v1/simulation/events")
                        .param("intervalMs", "10")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
}
