export default function Button({
  children,
  variant = "primary",
  className = "",
  disabled = false,
  ...props
}) {
  const variants = {
    primary:
      "bg-primary text-white hover:bg-primary-hover disabled:bg-indigo-200 disabled:cursor-not-allowed",
    secondary:
      "bg-white border border-indigo-100 text-text-main hover:bg-soft-blue disabled:opacity-60 disabled:cursor-not-allowed",
    ghost:
      "bg-transparent text-text-secondary hover:text-text-main hover:bg-white/80 disabled:opacity-60 disabled:cursor-not-allowed",
    danger:
      "bg-red-600 text-white hover:bg-red-700 disabled:opacity-60 disabled:cursor-not-allowed",
  };

  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition ${variants[variant]} ${className}`}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );
}
