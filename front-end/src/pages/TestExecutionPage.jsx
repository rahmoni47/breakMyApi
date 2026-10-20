import { useEffect, useState } from "react";
import Card from "../components/common/Card";
import Button from "../components/common/Button";
import TestStatusCard from "../components/tests/TestStatusCard";
import ProgressIndicator from "../components/tests/ProgressIndicator";
import ConfirmDialog from "../components/common/ConfirmDialog";

export default function TestExecutionPage({ run, onTick, onDone, onCancel }) {
  const [confirmCancel, setConfirmCancel] = useState(false);

  useEffect(() => {
    if (!run || run.status !== "RUNNING") return;

    const interval = setInterval(async () => {
      const updated = await onTick(run.id);
      if (
        updated?.status === "COMPLETED" ||
        updated?.status === "COMPLETED_WITH_WARNINGS"
      ) {
        onDone(updated.id);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [run, onTick, onDone]);

  if (!run) return null;

  return (
    <div className="mx-auto w-full max-w-5xl animate-fade-in space-y-4">
      <h2 className="text-4xl font-bold text-text-main">{run.projectName}</h2>
      <TestStatusCard run={run} />
      <Card className="p-5">
        <ProgressIndicator progress={run.progress} />
        <div className="mt-4 flex justify-end">
          <Button variant="danger" onClick={() => setConfirmCancel(true)}>
            Cancel Test
          </Button>
        </div>
      </Card>

      <ConfirmDialog
        open={confirmCancel}
        title="Cancel running test?"
        description="This will stop the current run. You can start another run later from the New Test page."
        confirmText="Yes, cancel run"
        onConfirm={async () => {
          await onCancel(run.id);
          setConfirmCancel(false);
        }}
        onCancel={() => setConfirmCancel(false)}
      />
    </div>
  );
}
