export default function Card({ children, className = "" }) {
  return (
    <div className={`rounded-xl bg-surface shadow-card ${className}`}>
      {children}
    </div>
  );
}
