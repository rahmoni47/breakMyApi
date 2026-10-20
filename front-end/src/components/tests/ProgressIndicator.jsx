export default function ProgressIndicator({ progress }) {
  if (!progress) return null;

  return (
    <div className="mt-4 rounded-xl border border-indigo-100 bg-white p-4">
      <div className="mb-2 flex items-center justify-between text-sm">
        <p className="font-semibold text-text-main">{progress.phase}</p>
        <p className="text-text-secondary">{progress.percent}%</p>
      </div>
      <div className="h-2 w-full rounded-full bg-soft-blue">
        <div
          className="h-2 rounded-full bg-primary transition-all"
          style={{ width: `${Math.max(2, progress.percent)}%` }}
        />
      </div>
      <p className="mt-2 text-xs text-text-secondary">
        {progress.checksCompleted} of {progress.checksTotal} estimated checks
        completed
      </p>
    </div>
  );
}
