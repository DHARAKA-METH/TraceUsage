import Link from "next/link";

export function EmptyState({
  icon = "◇",
  title,
  description,
  actionHref,
  actionLabel,
}: {
  icon?: string;
  title: string;
  description: string;
  actionHref?: string;
  actionLabel?: string;
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">{icon}</div>
      <h3 style={{ marginBottom: 0 }}>{title}</h3>
      <p className="muted" style={{ maxWidth: 520, margin: 0 }}>{description}</p>
      {actionHref && actionLabel && <Link className="button secondary" href={actionHref}>{actionLabel}</Link>}
    </div>
  );
}
