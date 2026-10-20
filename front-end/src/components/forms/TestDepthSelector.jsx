import { Zap, Orbit, Radar } from "lucide-react";
import { TEST_DEPTH_OPTIONS } from "../../data/demoModels";

const ICON_BY_ID = {
  FAST: Zap,
  MEDIUM: Orbit,
  DEEP: Radar,
};

export default function TestDepthSelector({ value, onChange, error }) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <label className="text-sm font-semibold text-text-main">
          Test Speed & Coverage Depth *
        </label>
        <span className="text-xs text-error">Confirmation Needed</span>
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        {TEST_DEPTH_OPTIONS.map((option) => {
          const Icon = ICON_BY_ID[option.id] || Zap;
          const selected = value === option.id;
          return (
            <button
              key={option.id}
              type="button"
              onClick={() => onChange(option.id)}
              className={`rounded-xl border p-4 text-left transition ${
                selected
                  ? "border-primary bg-indigo-50"
                  : "border-indigo-100 bg-soft-blue hover:border-indigo-300"
              }`}
            >
              <div className="flex items-center justify-between">
                <p className="flex items-center gap-2 text-base font-semibold text-text-main">
                  <Icon className="h-4 w-4 text-primary" /> {option.title}
                </p>
                <span className="text-xs font-semibold text-text-secondary">
                  {option.eta}
                </span>
              </div>
              <p className="mt-2 text-xs text-text-secondary">
                {option.description}
              </p>
              <p className="mt-3 text-xs font-semibold text-text-main">
                {option.checks}
              </p>
            </button>
          );
        })}
      </div>

      <p className="mt-2 text-[11px] text-text-secondary">
        Duration and check counts are estimates for planning only.
      </p>
      {error && <p className="mt-1 text-xs font-medium text-error">{error}</p>}
    </div>
  );
}
