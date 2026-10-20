import { Copy, Download, FileDown } from "lucide-react";
import Button from "../common/Button";

export default function ReportToolbar({
  fileName,
  fileSize,
  onCopy,
  onDownloadMarkdown,
  onExport,
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-indigo-100 bg-white px-3 py-2">
      <div className="rounded-lg bg-soft-blue px-3 py-2 text-xs text-text-main">
        <span className="font-semibold">{fileName}</span>
        <span className="ml-2 text-text-secondary">{fileSize}</span>
      </div>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          className="px-3 py-2 text-xs"
          onClick={onCopy}
        >
          <Copy className="h-3.5 w-3.5" /> Copy Content
        </Button>
        <Button
          variant="secondary"
          className="px-3 py-2 text-xs"
          onClick={onExport}
        >
          <FileDown className="h-3.5 w-3.5" /> Export
        </Button>
        <Button className="px-3 py-2 text-xs" onClick={onDownloadMarkdown}>
          <Download className="h-3.5 w-3.5" /> Download .md
        </Button>
      </div>
    </div>
  );
}
