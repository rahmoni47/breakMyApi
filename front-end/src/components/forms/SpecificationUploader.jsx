import { FileUp, X, AlertTriangle } from "lucide-react";
import { formatBytes, MAX_SPEC_SIZE_BYTES } from "../../utils/fileUtils";

export default function SpecificationUploader({
  file,
  specMeta,
  onSelectFile,
  onRemove,
  error,
  parseError,
}) {
  const onDrop = (event) => {
    event.preventDefault();
    const droppedFile = event.dataTransfer.files?.[0];
    if (droppedFile) onSelectFile(droppedFile);
  };

  const onBrowse = (event) => {
    const selected = event.target.files?.[0];
    if (selected) onSelectFile(selected);
  };

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <label className="text-sm font-semibold text-text-main">
          JSON API Specification *
        </label>
        <span className="text-xs text-text-secondary">
          OpenAPI 3.x / Swagger JSON
        </span>
      </div>

      <div
        onDrop={onDrop}
        onDragOver={(event) => event.preventDefault()}
        className={`rounded-xl border-2 border-dashed p-6 text-center transition ${
          error || parseError
            ? "border-red-300 bg-red-50/40"
            : "border-indigo-200 bg-white hover:border-primary"
        }`}
      >
        {!file ? (
          <>
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-rose-100 text-error">
              <FileUp className="h-6 w-6" />
            </div>
            <p className="mt-4 text-base font-semibold text-text-main">
              Drop your OpenAPI schema file here
            </p>
            <p className="mt-1 text-sm text-text-secondary">
              Accepts .json files up to {formatBytes(MAX_SPEC_SIZE_BYTES)}.
            </p>
            <label className="mt-4 inline-flex cursor-pointer rounded-lg border border-indigo-100 bg-soft-blue px-4 py-2 text-xs font-semibold text-text-main">
              Browse Local File
              <input
                type="file"
                accept="application/json,.json"
                className="hidden"
                onChange={onBrowse}
              />
            </label>
          </>
        ) : (
          <div className="text-left">
            <div className="flex items-center justify-between rounded-lg bg-soft-blue px-3 py-2">
              <div>
                <p className="text-sm font-semibold text-text-main">
                  {file.name}
                </p>
                <p className="text-xs text-text-secondary">
                  {formatBytes(file.size)}
                </p>
              </div>
              <button
                onClick={onRemove}
                className="rounded p-1 text-text-secondary hover:bg-white hover:text-text-main"
                type="button"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {specMeta && (
              <div className="mt-3 rounded-lg border border-indigo-100 bg-indigo-50/40 p-3 text-xs text-text-main">
                <p>
                  <strong>Detected:</strong> {specMeta.format}
                </p>
                <p>
                  <strong>API:</strong> {specMeta.title} ({specMeta.version})
                </p>
                <p>
                  <strong>Endpoints:</strong> {specMeta.endpointCount}
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {(error || parseError) && (
        <p className="mt-2 flex items-center gap-1.5 text-xs font-medium text-error">
          <AlertTriangle className="h-3.5 w-3.5" /> {parseError || error}
        </p>
      )}
    </div>
  );
}
