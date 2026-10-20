import { sanitizeFilename } from "./fileUtils";

export function downloadTextFile(filename, content, mimeType = "text/plain") {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export function markdownFilename(projectName, testId) {
  return `report_${sanitizeFilename(projectName)}_${testId.slice(0, 8)}.md`;
}

export function logFilename(projectName, testId) {
  return `logs_${sanitizeFilename(projectName)}_${testId.slice(0, 8)}.jsonl`;
}
