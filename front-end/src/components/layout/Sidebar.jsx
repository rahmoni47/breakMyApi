import {
  FlaskConical,
  History,
  Plus,
  CircleCheck,
  CircleDashed,
  CircleX,
  Loader2,
} from "lucide-react";
import Button from "../common/Button";
import StatusBadge from "../common/StatusBadge";

function iconForStatus(status) {
  if (status === "COMPLETED")
    return <CircleCheck className="h-4 w-4 text-success" />;
  if (status === "COMPLETED_WITH_WARNINGS")
    return <CircleCheck className="h-4 w-4 text-amber-600" />;
  if (status === "FAILED" || status === "CANCELLED")
    return <CircleX className="h-4 w-4 text-error" />;
  if (status === "RUNNING")
    return <Loader2 className="h-4 w-4 animate-spin text-warning" />;
  return <CircleDashed className="h-4 w-4 text-slate-400" />;
}

export default function Sidebar({
  history,
  activeTestId,
  onSelectTest,
  onNewTest,
}) {
  return (
    <aside className="sticky top-0 flex h-screen w-full max-w-[250px] shrink-0 flex-col border-r border-indigo-100 bg-white">
      <div className="px-5 pb-5 pt-4">
        <h1 className="text-xl font-bold tracking-tight text-text-main">
          OpenSpec
        </h1>
        <Button className="mt-6 w-full" onClick={onNewTest}>
          <Plus className="h-4 w-4" /> New Test
        </Button>
      </div>

      <div className="px-5">
        <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-text-secondary">
          Workspace
        </p>
        <button className="mt-3 flex w-full items-center gap-2 rounded-lg px-2 py-2 text-left text-sm font-medium text-text-main hover:bg-soft-blue">
          <FlaskConical className="h-4 w-4 text-primary" /> OpenSpec
        </button>
      </div>

      <div className="mt-5 flex min-h-0 flex-1 flex-col px-5 pb-4">
        <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-text-secondary">
          Test History
        </p>
        <div className="mt-3 flex min-h-0 flex-1 flex-col gap-1 overflow-auto">
          {history.length === 0 && (
            <p className="rounded-lg bg-soft-blue px-3 py-2 text-xs text-text-secondary">
              No test runs yet.
            </p>
          )}
          {history.map((item, index) => {
            const isActive = activeTestId === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onSelectTest(item.id)}
                className={`rounded-lg px-3 py-2 text-left transition ${
                  isActive ? "bg-indigo-100" : "hover:bg-soft-blue"
                }`}
              >
                <div className="flex items-start gap-2">
                  <span className="mt-0.5">{iconForStatus(item.status)}</span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-text-main">
                      Test {String(index + 1).padStart(2, "0")} -{" "}
                      {item.projectName}
                    </p>
                    <div className="mt-1">
                      <StatusBadge status={item.status} />
                    </div>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex items-center justify-between border-t border-indigo-100 px-5 py-3 text-xs text-text-secondary">
        <span>Vortex</span>
        <span>v1.0</span>
      </div>
    </aside>
  );
}
