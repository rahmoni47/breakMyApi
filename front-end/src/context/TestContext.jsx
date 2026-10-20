import { createContext, useContext, useMemo, useState } from "react";
import {
  createTest,
  getRunById,
  getTestHistory,
  getTestLogs,
  getTestReport,
  getTestStatus,
  pollDemoProgress,
  startTest,
  stopTest,
} from "../services/testService";

const TestContext = createContext(null);

export function TestProvider({ children }) {
  const [history, setHistory] = useState(getTestHistory());
  const [activeTestId, setActiveTestId] = useState(history[0]?.id || null);

  const refreshHistory = () => {
    setHistory(getTestHistory());
  };

  const createAndStartTest = async (config) => {
    const created = await createTest(config);
    await startTest(created.id);
    refreshHistory();
    setActiveTestId(created.id);
    return created.id;
  };

  const getActiveRun = () => (activeTestId ? getRunById(activeTestId) : null);

  const runProgressTick = async (testId) => {
    const updated = await pollDemoProgress(testId);
    refreshHistory();
    return updated;
  };

  const getRunStatus = async (testId) => getTestStatus(testId);
  const getRunReport = async (testId) => getTestReport(testId);
  const getRunLogs = async (testId) => getTestLogs(testId);
  const cancelRun = async (testId) => {
    const result = await stopTest(testId);
    refreshHistory();
    return result;
  };

  const value = useMemo(
    () => ({
      history,
      refreshHistory,
      activeTestId,
      setActiveTestId,
      createAndStartTest,
      getActiveRun,
      getRunStatus,
      runProgressTick,
      getRunReport,
      getRunLogs,
      cancelRun,
    }),
    [history, activeTestId],
  );

  return <TestContext.Provider value={value}>{children}</TestContext.Provider>;
}

export function useTests() {
  const context = useContext(TestContext);
  if (!context) throw new Error("useTests must be used inside TestProvider");
  return context;
}
