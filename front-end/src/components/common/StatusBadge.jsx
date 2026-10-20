const statusMap = {
  PENDING: "bg-indigo-100 text-indigo-700",
  RUNNING: "bg-violet-100 text-violet-700",
  COMPLETED: "bg-teal-100 text-success",
  COMPLETED_WITH_WARNINGS: "bg-amber-100 text-amber-700",
  FAILED: "bg-red-100 text-red-700",
  CANCELLED: "bg-slate-200 text-slate-700",
};

export default function StatusBadge({ status }) {
  return (
    <span
      className={`rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide ${statusMap[status] || "bg-slate-100 text-slate-700"}`}
    >
      {String(status || "unknown").replaceAll("_", " ")}
    </span>
  );
}
