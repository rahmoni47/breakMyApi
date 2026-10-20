export default function ProjectNameField({ value, onChange, error }) {
  return (
    <div>
      <label
        htmlFor="projectName"
        className="mb-2 block text-sm font-semibold text-text-main"
      >
        Project Name *
      </label>
      <input
        id="projectName"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="e.g. Identity Auth Gateway v3"
        className={`w-full rounded-xl border px-4 py-3 text-sm text-text-main shadow-sm outline-none transition ${
          error
            ? "border-red-300 bg-red-50/50"
            : "border-indigo-100 bg-white focus:border-primary"
        }`}
      />
      {error && <p className="mt-2 text-xs font-medium text-error">{error}</p>}
    </div>
  );
}
