import { DEMO_PHASES } from "../data/demoModels";
import { readStorage, writeStorage } from "../utils/storage";
import { sanitizeFilename } from "../utils/fileUtils";
import { redactSensitive } from "../utils/logRedaction";
import { isBackendEnabled, request } from "./apiClient";

const HISTORY_KEY = "test-history";
const RUNS_KEY = "test-runs";

function getHistory() {
  return readStorage(HISTORY_KEY, []);
}

function setHistory(history) {
  writeStorage(HISTORY_KEY, history);
}

function getRuns() {
  return readStorage(RUNS_KEY, {});
}

function setRuns(runs) {
  writeStorage(RUNS_KEY, runs);
}

function buildRunId() {
  return crypto.randomUUID();
}

function nowIso() {
  return new Date().toISOString();
}

function updateRun(runId, patch) {
  const runs = getRuns();
  const run = runs[runId];
  if (!run) return null;
  runs[runId] = { ...run, ...patch };
  setRuns(runs);

  const history = getHistory().map((item) =>
    item.id === runId ? { ...item, ...patch } : item,
  );
  setHistory(history);
  return runs[runId];
}

function appendLog(runId, level, event, details = {}) {
  const runs = getRuns();
  const run = runs[runId];
  if (!run) return;

  const redactedDetails = redactSensitive(details);
  const log = {
    timestamp: nowIso(),
    level,
    event,
    details: redactedDetails,
  };

  run.logs = [...(run.logs || []), log];
  runs[runId] = run;
  setRuns(runs);
}

function createDemoReport(run) {
  const completedChecks = run.summary?.totalChecks || 0;
  const passed = run.summary?.passed || 0;
  const failed = run.summary?.failed || 0;
  const skipped = run.summary?.skipped || 0;
  const warnings = run.summary?.warnings || 0;

  const result = failed > 0 ? "Completed with warnings" : "Completed";

  const endpointRows = (run.endpointResults || [])
    .map(
      (endpoint) =>
        `| ${endpoint.method} ${endpoint.path} | ${endpoint.checks} | ${endpoint.avgMs || "n/a"} | ${endpoint.status} |`,
    )
    .join("\n");

  return `# OpenSpec Test Summary Report

Generated: ${new Date(run.completedAt || nowIso()).toUTCString()}

## Summary

- **Project:** ${run.projectName}
- **Execution Mode:** Demo simulation (no live backend test execution)
- **Specification:** ${run.specification.metadata.fileName}
- **Target:** ${run.specification.metadata.servers?.[0] || "Unavailable"}
- **Model:** ${run.modelLabel}
- **Depth:** ${run.testDepth}
- **Overall Result:** ${result}

## 1.0 Target Environment & Parameters

| Key Parameter | Specification | Telemetry Verified | Status |
| --- | --- | --- | --- |
| target_host | ${run.specification.metadata.servers?.[0] || "Unavailable"} | unavailable | unavailable |
| spec_reference | ${run.specification.metadata.fileName} | available | validated |
| evaluator_model | ${run.modelLabel} | demo mode | demo |
| sweep_depth | ${run.testDepth} | ${completedChecks} checks executed | complete |
| execution_id | ${run.id} | local storage | demo |

## 2.0 Endpoint Reliability & Payload Assertions

| Endpoint | Checks | Avg Response | Status |
| --- | --- | --- | --- |
${endpointRows || "| n/a | 0 | n/a | skipped |"}

## 3.0 Test Results Summary

- **Total checks:** ${completedChecks}
- **Passed checks:** ${passed}
- **Failed checks:** ${failed}
- **Skipped checks:** ${skipped}
- **Warnings:** ${warnings}
- **Success rate:** ${completedChecks ? Math.round((passed / completedChecks) * 100) : 0}%
- **Execution duration:** ${run.durationMs ? `${Math.round(run.durationMs / 1000)}s` : "Unavailable"}

## 4.0 Failures & Recommendations

${failed === 0 ? "- No failing checks in this demo run." : "- One or more endpoint checks failed. Review logs and retry with backend mode."}
${warnings === 0 ? "- No warnings in this demo run." : "- Warnings found. Validate schema constraints and edge input handling."}

## 5.0 Execution Metadata

- **Test ID:** ${run.id}
- **Started at:** ${run.startedAt || "Unavailable"}
- **Finished at:** ${run.completedAt || "Unavailable"}
- **Selected model:** ${run.modelLabel}
- **Selected depth:** ${run.testDepth}
- **Specification filename:** ${run.specification.metadata.fileName}
- **Execution environment:** Demo mode
- **Report generated at:** ${nowIso()}
`;
}

function createDemoEndpointResults(endpoints, depth) {
  const factor = depth === "DEEP" ? 4 : depth === "MEDIUM" ? 2 : 1;
  return endpoints.slice(0, 8).map((endpoint, index) => {
    const checks = factor * (index % 2 === 0 ? 6 : 4);
    const avgMs = 40 + index * 12;
    const failed = depth === "DEEP" && index === 1 ? 1 : 0;
    const warnings = depth !== "FAST" && index === 2 ? 1 : 0;
    return {
      ...endpoint,
      checks,
      avgMs: `${avgMs}ms`,
      status: failed ? "failed" : warnings ? "warning" : "passed",
      failed,
      warnings,
      passed: checks - failed,
    };
  });
}

function summarizeResults(endpointResults) {
  return endpointResults.reduce(
    (acc, endpoint) => {
      acc.totalChecks += endpoint.checks;
      acc.passed += endpoint.passed;
      acc.failed += endpoint.failed;
      acc.warnings += endpoint.warnings;
      return acc;
    },
    { totalChecks: 0, passed: 0, failed: 0, skipped: 0, warnings: 0 },
  );
}

export async function createTest(configuration) {
  if (isBackendEnabled()) {
    const payload = {
      name: configuration.projectName,
      openApiDocument: configuration.specification.rawText,
      testType: configuration.testDepth,
    };

    const response = await request("/api/v1/tests", {
      method: "POST",
      body: JSON.stringify(payload),
    });

    return {
      id: response.testRunId,
      backend: true,
      status: response.status,
      createdAt: response.createdAt,
      name: response.name,
      testType: response.testType,
      message: response.message,
    };
  }

  const id = buildRunId();
  const createdAt = nowIso();
  const reportFileName = `report_${sanitizeFilename(configuration.projectName)}_${id.slice(0, 8)}.md`;

  const testRecord = {
    id,
    projectName: configuration.projectName,
    testDepth: configuration.testDepth,
    modelId: configuration.modelId,
    modelLabel: configuration.modelLabel,
    provider: configuration.provider,
    status: "PENDING",
    mode: "demo",
    createdAt,
    startedAt: null,
    completedAt: null,
    durationMs: 0,
    reportFileName,
    specification: configuration.specification,
    progress: {
      phase: DEMO_PHASES[0],
      percent: 0,
      checksCompleted: 0,
      checksTotal: 0,
    },
    endpointResults: [],
    summary: {
      totalChecks: 0,
      passed: 0,
      failed: 0,
      skipped: 0,
      warnings: 0,
    },
    reportMarkdown: "",
    logs: [
      {
        timestamp: createdAt,
        level: "INFO",
        event: "TEST_CREATED",
        details: {
          mode: "demo",
          message: "Demo run created. No live API requests were sent.",
        },
      },
    ],
  };

  const runs = getRuns();
  runs[id] = testRecord;
  setRuns(runs);

  const history = [
    {
      id,
      projectName: configuration.projectName,
      status: "PENDING",
      createdAt,
      modelLabel: configuration.modelLabel,
      testDepth: configuration.testDepth,
      mode: "demo",
    },
    ...getHistory(),
  ];
  setHistory(history);

  return { ...testRecord };
}

export async function startTest(testId) {
  if (isBackendEnabled()) {
    return { started: true, backend: true, testId };
  }

  const run = updateRun(testId, {
    status: "RUNNING",
    startedAt: nowIso(),
  });

  appendLog(testId, "INFO", "TEST_STARTED", {
    message: "Run started in demo mode.",
  });

  return run;
}

export async function getTestStatus(testId) {
  if (isBackendEnabled()) {
    const history = await request(`/api/v1/tests?page=0&size=100`);
    const item = history?.items?.find(
      (row) => row.testRunId === testId || row.id === testId,
    );
    if (!item) throw new Error("Test run not found in backend history");
    return item;
  }

  const run = getRuns()[testId];
  if (!run) throw new Error("Test run not found");
  return run;
}

export async function pollDemoProgress(testId) {
  const run = getRuns()[testId];
  if (!run || run.status !== "RUNNING") return run;

  const elapsed = Date.now() - new Date(run.startedAt).getTime();
  const totalDurationMs =
    run.testDepth === "DEEP"
      ? 16000
      : run.testDepth === "MEDIUM"
        ? 10000
        : 6500;
  const percent = Math.min(100, Math.round((elapsed / totalDurationMs) * 100));

  const phaseIndex = Math.min(
    DEMO_PHASES.length - 1,
    Math.floor((percent / 100) * DEMO_PHASES.length),
  );
  const phase = DEMO_PHASES[phaseIndex];

  const checksTarget =
    run.testDepth === "DEEP" ? 180 : run.testDepth === "MEDIUM" ? 48 : 12;
  const checksCompleted = Math.min(
    checksTarget,
    Math.round((percent / 100) * checksTarget),
  );

  updateRun(testId, {
    progress: {
      phase,
      percent,
      checksCompleted,
      checksTotal: checksTarget,
    },
  });

  appendLog(testId, "INFO", "PROGRESS_UPDATED", {
    phase,
    percent,
    checksCompleted,
    checksTotal: checksTarget,
  });

  if (percent >= 100) {
    const refreshedRun = getRuns()[testId];
    const endpointResults = createDemoEndpointResults(
      refreshedRun.specification.endpoints,
      refreshedRun.testDepth,
    );
    const summary = summarizeResults(endpointResults);

    const completedRun = updateRun(testId, {
      status:
        summary.failed > 0 || summary.warnings > 0
          ? "COMPLETED_WITH_WARNINGS"
          : "COMPLETED",
      completedAt: nowIso(),
      durationMs: Date.now() - new Date(refreshedRun.startedAt).getTime(),
      endpointResults,
      summary,
    });

    const reportMarkdown = createDemoReport(completedRun);
    updateRun(testId, { reportMarkdown });

    appendLog(testId, "INFO", "TEST_COMPLETED", {
      status: completedRun.status,
      summary,
    });
  }

  return getRuns()[testId];
}

export async function stopTest(testId) {
  if (isBackendEnabled()) {
    return request(`/api/v1/tests/${testId}/stop`, { method: "POST" });
  }

  const run = updateRun(testId, {
    status: "CANCELLED",
    completedAt: nowIso(),
  });

  appendLog(testId, "WARNING", "TEST_STOPPED", {
    message: "Test was cancelled by user.",
  });

  return run;
}

export async function getTestReport(testId) {
  if (isBackendEnabled()) {
    return request(`/api/v1/tests/${testId}/report`, {
      method: "GET",
      headers: { Accept: "text/markdown" },
    });
  }

  const run = getRuns()[testId];
  if (!run) throw new Error("Test run not found");
  return run.reportMarkdown;
}

export async function getTestLogs(testId) {
  if (isBackendEnabled()) {
    return request(`/api/v1/tests/${testId}/logs`, {
      method: "GET",
      headers: { Accept: "text/plain" },
    });
  }

  const run = getRuns()[testId];
  if (!run) throw new Error("Test run not found");

  return run.logs.map((entry) => JSON.stringify(entry)).join("\n");
}

export function getTestHistory() {
  return getHistory();
}

export function getRunById(testId) {
  return getRuns()[testId] || null;
}
