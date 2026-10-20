import { useState } from "react";
import { AlertCircle } from "lucide-react";
import Card from "../components/common/Card";
import Button from "../components/common/Button";
import ProjectNameField from "../components/forms/ProjectNameField";
import SpecificationUploader from "../components/forms/SpecificationUploader";
import TestDepthSelector from "../components/forms/TestDepthSelector";
import { parseSpecification } from "../services/specificationService";
import { readTextFile } from "../utils/fileUtils";
import { validateNewTestForm } from "../utils/validation";
import { MODEL_OPTIONS } from "../data/demoModels";
import { useSettings } from "../context/SettingsContext";

export default function CreateTestPage({ onStart }) {
  const { aiSettings, isProviderConfigured, uiSettings } = useSettings();
  const [form, setForm] = useState({
    projectName: "",
    specFile: null,
    specification: null,
    specificationError: "",
    testDepth: uiSettings.defaultTestDepth,
    modelId: aiSettings.selectedModelId,
  });
  const [errors, setErrors] = useState({});
  const [topError, setTopError] = useState("");
  const [starting, setStarting] = useState(false);

  const selectedModel =
    MODEL_OPTIONS.find((model) => model.id === aiSettings.selectedModelId) ||
    MODEL_OPTIONS[0];

  const update = (patch) => {
    setForm((prev) => ({ ...prev, ...patch }));
  };

  const onFileChange = async (file) => {
    update({ specFile: file, specificationError: "" });
    try {
      const text = await readTextFile(file);
      const parsed = parseSpecification(text, file.name);
      update({ specification: parsed, specificationError: "" });
      setErrors((prev) => ({ ...prev, specFile: "", specification: "" }));
    } catch (error) {
      update({ specification: null, specificationError: error.message });
    }
  };

  const onSubmit = async () => {
    const payload = {
      ...form,
      projectName: form.projectName.trim(),
      modelId: selectedModel.id,
    };
    const providerConfigured = isProviderConfigured(selectedModel.provider);
    const validation = validateNewTestForm(payload, providerConfigured);

    if (!validation.isValid) {
      setErrors(validation.errors);
      setTopError(
        "Unable to start test run. Please resolve the missing required fields and verify model credentials before launching the test engine.",
      );
      return;
    }

    setTopError("");
    setErrors({});

    setStarting(true);
    await onStart({
      projectName: payload.projectName,
      specFile: payload.specFile,
      specification: payload.specification,
      testDepth: payload.testDepth,
      modelId: selectedModel.id,
      modelLabel: selectedModel.label,
      provider: selectedModel.provider,
    });
    setStarting(false);
  };

  return (
    <div className="mx-auto w-full max-w-5xl animate-fade-in space-y-4 pb-12">
      {topError && (
        <div className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-error">
          <AlertCircle className="mt-0.5 h-4 w-4" />
          <div>
            <p className="font-semibold">Unable to start test run</p>
            <p className="text-sm">
              Please resolve the missing required fields and verify model
              credentials before launching the test engine.
            </p>
          </div>
        </div>
      )}

      <Card className="p-8">
        <h2 className="text-4xl font-bold leading-tight text-text-main">
          Create New Test
        </h2>
        <p className="mt-1 text-text-secondary">
          Configure your target endpoint schema and set verification parameters.
        </p>

        <div className="mt-7 space-y-6">
          <ProjectNameField
            value={form.projectName}
            onChange={(projectName) => update({ projectName })}
            error={errors.projectName}
          />

          <SpecificationUploader
            file={form.specFile}
            specMeta={
              form.specification?.metadata && {
                ...form.specification.metadata,
                format: form.specification.format,
              }
            }
            onSelectFile={onFileChange}
            onRemove={() =>
              update({
                specFile: null,
                specification: null,
                specificationError: "",
              })
            }
            error={errors.specFile}
            parseError={form.specificationError || errors.specification}
          />

          <TestDepthSelector
            value={form.testDepth}
            onChange={(testDepth) => {
              update({ testDepth });
              setErrors((prev) => ({ ...prev, testDepth: "" }));
            }}
            error={errors.testDepth}
          />
        </div>

        <div className="mt-8 flex justify-end gap-3">
          <Button variant="secondary" onClick={() => window.location.reload()}>
            Cancel
          </Button>
          <Button disabled={starting} onClick={onSubmit}>
            {starting ? "Starting..." : "Start Test"}
          </Button>
        </div>
      </Card>
    </div>
  );
}
