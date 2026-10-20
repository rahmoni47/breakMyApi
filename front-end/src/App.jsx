import { useEffect, useMemo, useState } from "react";
import Sidebar from "./components/layout/Sidebar";
import Topbar from "./components/layout/Topbar";
import CreateTestPage from "./pages/CreateTestPage";
import TestExecutionPage from "./pages/TestExecutionPage";
import ProjectReportPage from "./pages/ProjectReportPage";
import SettingsPage from "./pages/SettingsPage";
import Toast from "./components/common/Toast";
import { useTests } from "./context/TestContext";
import { getRunById } from "./services/testService";

function resolveScreen(run) {
  if (!run) return "create";
  if (run.status === "RUNNING" || run.status === "PENDING") return "running";
  if (
    run.status === "COMPLETED" ||
    run.status === "COMPLETED_WITH_WARNINGS" ||
    run.status === "FAILED" ||
    run.status === "CANCELLED"
  ) {
    return "report";
  }
  return "create";
}

export default function App() {
  const {
    history,
    activeTestId,
    setActiveTestId,
    refreshHistory,
    createAndStartTest,
    runProgressTick,
    getRunReport,
    getRunLogs,
    cancelRun,
  } = useTests();

  const [currentRun, setCurrentRun] = useState(() =>
    activeTestId ? getRunById(activeTestId) : null,
  );
  const [markdown, setMarkdown] = useState("");
  const [logs, setLogs] = useState("");
  const [showSettings, setShowSettings] = useState(false);
  const [toast, setToast] = useState({ type: "success", message: "" });

  useEffect(() => {
    refreshHistory();
  }, [refreshHistory]);

  useEffect(() => {
    if (!activeTestId) {
      setCurrentRun(null);
      return;
    }
    setCurrentRun(getRunById(activeTestId));
  }, [activeTestId, history]);

  useEffect(() => {
    async function hydrateArtifacts() {
      if (!currentRun) return;
      if (resolveScreen(currentRun) !== "report") return;
      const [reportContent, logContent] = await Promise.all([
        getRunReport(currentRun.id),
        getRunLogs(currentRun.id),
      ]);
      setMarkdown(reportContent || "");
      setLogs(logContent || "");
    }

    hydrateArtifacts();
  }, [currentRun, getRunReport, getRunLogs]);

  const screen = useMemo(() => resolveScreen(currentRun), [currentRun]);

  const startRun = async (config) => {
    const id = await createAndStartTest(config);
    setActiveTestId(id);
    setCurrentRun(getRunById(id));
  };

  const onTick = async (testId) => {
    const updated = await runProgressTick(testId);
    setCurrentRun(updated);
    return updated;
  };

  const selectTest = (testId) => {
    setActiveTestId(testId);
    setCurrentRun(getRunById(testId));
  };

  return (
    <div className="flex min-h-screen bg-bg-main text-text-main">
      <Sidebar
        history={history}
        activeTestId={activeTestId}
        onSelectTest={selectTest}
        onNewTest={() => {
          setActiveTestId(null);
          setCurrentRun(null);
          setMarkdown("");
          setLogs("");
        }}
      />

      <div className="min-h-screen min-w-0 flex-1">
        <Topbar onOpenSettings={() => setShowSettings(true)} />

        <main className="p-4 md:p-6">
          {screen === "create" && <CreateTestPage onStart={startRun} />}

          {screen === "running" && (
            <TestExecutionPage
              run={currentRun}
              onTick={onTick}
              onDone={(id) => setCurrentRun(getRunById(id))}
              onCancel={async (id) => {
                const cancelled = await cancelRun(id);
                setCurrentRun(cancelled);
              }}
            />
          )}

          {screen === "report" && currentRun && (
            <ProjectReportPage
              run={currentRun}
              markdown={markdown}
              logs={logs}
              onToast={setToast}
            />
          )}
        </main>
      </div>

      {showSettings && (
        <SettingsPage
          onClose={() => setShowSettings(false)}
          onToast={setToast}
        />
      )}

      <Toast
        type={toast.type}
        message={toast.message}
        onClose={() => setToast({ type: "success", message: "" })}
      />
    </div>
  );
}
