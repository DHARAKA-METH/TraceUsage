export function StatCard({
  label,
  value,
  hint,
  tone = "neutral",
}: {
  label: string;
  value: string | number;
  hint?: string;
  tone?: "neutral" | "success" | "warning" | "danger" | "info" | "purple";
}) {
  return (
    <div className="card" style={{ position: "relative", overflow: "hidden" }}>
      <div className={`badge ${tone}`} style={{ marginBottom: 8 }}>{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <p className="muted" style={{ margin: "8px 0 0", fontSize: 13 }}>{hint}</p>}
    </div>
  );
}
