import { useMemo } from "react";
import { formatBytes } from "../utils/fileUtils";
import {
  downloadTextFile,
  markdownFilename,
  logFilename,
} from "../utils/download";
import ReportToolbar from "../components/reports/ReportToolbar";
import MarkdownReport from "../components/reports/MarkdownReport";
import ReportActions from "../components/reports/ReportActions";

export default function ProjectReportPage({ run, markdown, logs, onToast }) {
  const fileName = useMemo(
    () => (run ? markdownFilename(run.projectName, run.id) : "report.md"),
    [run],
  );

  const fileSize = formatBytes(new Blob([markdown || ""]).size);

  const copyMarkdown = async () => {
    try {
      await navigator.clipboard.writeText(markdown || "");
      onToast({
        type: "success",
        message: "Markdown report copied to clipboard.",
      });
    } catch {
      onToast({
        type: "error",
        message:
          "Clipboard copy failed. Please check browser clipboard permissions.",
      });
    }
  };

  const downloadReport = () => {
    downloadTextFile(fileName, markdown || "", "text/markdown");
    onToast({ type: "success", message: "Markdown report downloaded." });
  };

  const downloadLogs = () => {
    const name = logFilename(run.projectName, run.id);
    downloadTextFile(name, logs || "", "application/x-ndjson");
    onToast({ type: "success", message: "Execution logs downloaded." });
  };

  return (
    <div className="mx-auto w-full max-w-5xl animate-fade-in space-y-4 pb-10">
      <div>
        <h2 className="text-4xl font-bold text-text-main">Project Report</h2>
        <p className="mt-1 text-text-secondary">
          Configure your target endpoint schema and set verification parameters.
        </p>
      </div>

      <ReportToolbar
        fileName={fileName}
        fileSize={fileSize}
        onCopy={copyMarkdown}
        onDownloadMarkdown={downloadReport}
        onExport={downloadReport}
      />

      <MarkdownReport markdown={markdown || "No report content available."} />

      <ReportActions
        onDownloadReport={downloadReport}
        onDownloadLogs={downloadLogs}
      />
    </div>
  );
}
