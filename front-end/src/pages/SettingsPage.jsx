import { useState } from "react";
import Card from "../components/common/Card";
import Button from "../components/common/Button";
import { TEST_DEPTH_OPTIONS } from "../data/demoModels";
import { useSettings } from "../context/SettingsContext";

export default function SettingsPage({ onClose, onToast }) {
  const { uiSettings, updateUiSettings } = useSettings();
  const [depth, setDepth] = useState(uiSettings.defaultTestDepth);
  const [reportFormat, setReportFormat] = useState(uiSettings.reportFormat);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/35 px-4">
      <Card className="w-full max-w-xl p-6">
        <h2 className="text-2xl font-bold text-text-main">Settings</h2>
        <p className="mt-1 text-sm text-text-secondary">
          AI provider setup, default run preferences, and application info.
        </p>

        <div className="mt-5 space-y-5">
          <section>
            <p className="text-sm font-semibold text-text-main">
              Default testing depth
            </p>
            <select
              value={depth}
              onChange={(event) => setDepth(event.target.value)}
              className="mt-2 w-full rounded-lg border border-indigo-100 px-3 py-2 text-sm"
            >
              {TEST_DEPTH_OPTIONS.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.title}
                </option>
              ))}
            </select>
          </section>

          <section>
            <p className="text-sm font-semibold text-text-main">
              Report preference
            </p>
            <select
              value={reportFormat}
              onChange={(event) => setReportFormat(event.target.value)}
              className="mt-2 w-full rounded-lg border border-indigo-100 px-3 py-2 text-sm"
            >
              <option value="markdown">Markdown (.md)</option>
              <option value="json">JSON (future)</option>
            </select>
          </section>

          <section className="rounded-lg border border-indigo-100 bg-soft-blue p-3 text-xs text-text-secondary">
            <p className="font-semibold text-text-main">Application</p>
            <p>OpenSpec by Vortex</p>
            <p>Version: v1.0</p>
            <p>Credential handling: backend-only in production</p>
          </section>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              updateUiSettings({ defaultTestDepth: depth, reportFormat });
              onToast({ type: "success", message: "Settings updated." });
              onClose();
            }}
          >
            Save
          </Button>
        </div>
      </Card>
    </div>
  );
}
