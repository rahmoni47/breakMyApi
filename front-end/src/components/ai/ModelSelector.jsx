import { useState } from "react";
import {
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  ChevronDown,
} from "lucide-react";
import { MODEL_OPTIONS } from "../../data/demoModels";
import { useSettings } from "../../context/SettingsContext";
import Button from "../common/Button";

export default function ModelSelector() {
  const { aiSettings, selectModel, isProviderConfigured, saveProviderConfig } =
    useSettings();
  const [open, setOpen] = useState(false);
  const [apiKeyHint, setApiKeyHint] = useState("");
  const selectedModel =
    MODEL_OPTIONS.find((model) => model.id === aiSettings.selectedModelId) ||
    MODEL_OPTIONS[0];

  const configured = isProviderConfigured(selectedModel.provider);

  const onSave = () => {
    saveProviderConfig({
      provider: selectedModel.provider,
      modelId: selectedModel.id,
      apiKeyHint,
    });
    setApiKeyHint("");
  };

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((prev) => !prev)}
        className="flex items-center gap-2 rounded-lg border border-indigo-100 bg-white px-3 py-2 text-sm font-semibold text-text-main"
      >
        <Sparkles className="h-4 w-4 text-primary" />
        {selectedModel.label}
        <ChevronDown className="h-4 w-4 text-text-secondary" />
      </button>

      {open && (
        <div className="absolute right-0 z-40 mt-2 w-[320px] rounded-xl border border-indigo-100 bg-white p-3 shadow-card">
          <div className="mb-2 flex items-center justify-between">
            <p className="text-sm font-bold text-text-main">
              Select AI Model & Engine
            </p>
            <span className="rounded bg-soft-blue px-2 py-0.5 text-[11px] font-semibold text-text-secondary">
              {MODEL_OPTIONS.length} models
            </span>
          </div>

          <div className="space-y-2">
            {MODEL_OPTIONS.map((model) => {
              const active = model.id === selectedModel.id;
              return (
                <button
                  key={model.id}
                  onClick={() =>
                    selectModel({ modelId: model.id, provider: model.provider })
                  }
                  className={`w-full rounded-lg border px-3 py-2 text-left ${
                    active
                      ? "border-primary bg-indigo-50"
                      : "border-indigo-100 hover:bg-soft-blue"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-semibold text-text-main">
                      {model.label}
                    </p>
                    {active && (
                      <CheckCircle2 className="h-4 w-4 text-primary" />
                    )}
                  </div>
                  <p className="text-xs text-primary">{model.provider}</p>
                  <p className="mt-0.5 text-xs text-text-secondary">
                    {model.description}
                  </p>
                </button>
              );
            })}
          </div>

          <div className="mt-3 rounded-lg border border-indigo-100 bg-soft-blue p-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-text-main">
                {selectedModel.provider} API Key
              </p>
              {!configured && (
                <span className="rounded bg-red-100 px-2 py-0.5 text-[10px] font-semibold text-error">
                  Key Missing
                </span>
              )}
            </div>
            <input
              type="password"
              value={apiKeyHint}
              onChange={(event) => setApiKeyHint(event.target.value)}
              className="mt-2 w-full rounded-md border border-indigo-200 bg-white px-2 py-1.5 text-xs focus:border-primary focus:outline-none"
              placeholder="sk-..."
            />
            <p className="mt-1 text-[10px] text-text-secondary">
              Demo config only. Credentials must be securely managed by backend.
            </p>
            <div className="mt-2 flex justify-end gap-2">
              <Button
                variant="secondary"
                className="px-3 py-1.5 text-xs"
                onClick={() => setOpen(false)}
              >
                Cancel
              </Button>
              <Button className="px-3 py-1.5 text-xs" onClick={onSave}>
                Save
              </Button>
            </div>
          </div>

          {!configured && (
            <div className="mt-2 flex items-start gap-2 rounded-lg bg-red-50 p-2 text-xs text-error">
              <AlertTriangle className="mt-0.5 h-3.5 w-3.5" />
              Provider not configured. Test start will be blocked until
              configured.
            </div>
          )}
        </div>
      )}
    </div>
  );
}
