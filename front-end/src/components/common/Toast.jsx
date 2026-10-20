import { CheckCircle2, AlertCircle } from "lucide-react";

export default function Toast({ type = "success", message, onClose }) {
  if (!message) return null;

  const isError = type === "error";

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-fade-in rounded-xl border border-indigo-100 bg-white px-4 py-3 shadow-card">
      <div className="flex items-start gap-3">
        {isError ? (
          <AlertCircle className="mt-0.5 h-4 w-4 text-error" />
        ) : (
          <CheckCircle2 className="mt-0.5 h-4 w-4 text-success" />
        )}
        <div>
          <p
            className={`text-sm font-semibold ${isError ? "text-error" : "text-success"}`}
          >
            {isError ? "Action failed" : "Success"}
          </p>
          <p className="text-xs text-text-secondary">{message}</p>
        </div>
        <button
          onClick={onClose}
          className="ml-2 text-xs text-text-secondary hover:text-text-main"
        >
          Close
        </button>
      </div>
    </div>
  );
}
