import { format } from "../../utils/time";
import StatusBadge from "../common/StatusBadge";

export default function TestHistory({ history, activeTestId, onSelect }) {
  return (
    <div className="space-y-2">
      {history.map((item) => {
        const active = item.id === activeTestId;
        return (
          <button
            key={item.id}
            className={`w-full rounded-xl border px-3 py-2 text-left ${
              active
                ? "border-primary bg-indigo-50"
                : "border-indigo-100 bg-white hover:bg-soft-blue"
            }`}
            onClick={() => onSelect(item.id)}
          >
            <p className="text-sm font-semibold text-text-main">
              {item.projectName}
            </p>
            <p className="text-xs text-text-secondary">
              {format(item.createdAt)}
            </p>
            <div className="mt-1 flex items-center justify-between">
              <StatusBadge status={item.status} />
              <span className="text-[11px] text-text-secondary">
                {item.testDepth}
              </span>
            </div>
          </button>
        );
      })}
    </div>
  );
}
