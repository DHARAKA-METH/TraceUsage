export function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="card">
      <div className="muted">{label}</div>
      <div className="stat-value">{value}</div>
    </div>
  );
}
