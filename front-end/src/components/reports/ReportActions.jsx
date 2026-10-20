import { Download } from "lucide-react";
import Button from "../common/Button";

export default function ReportActions({ onDownloadReport, onDownloadLogs }) {
  return (
    <div className="sticky bottom-0 mt-5 flex items-center justify-between rounded-xl border border-indigo-100 bg-white px-4 py-3 shadow-card">
      <div>
        <p className="text-sm font-semibold text-text-main">
          Test Evaluation Ready
        </p>
        <p className="text-xs text-text-secondary">
          Artifacts available for export
        </p>
      </div>
      <div className="flex gap-2">
        <Button className="px-3 py-2 text-xs" onClick={onDownloadReport}>
          <Download className="h-3.5 w-3.5" /> Download .md Report
        </Button>
        <Button
          variant="secondary"
          className="px-3 py-2 text-xs"
          onClick={onDownloadLogs}
        >
          <Download className="h-3.5 w-3.5" /> Download Logs
        </Button>
      </div>
    </div>
  );
}
