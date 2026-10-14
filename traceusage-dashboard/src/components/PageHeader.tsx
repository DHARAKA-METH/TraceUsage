import Link from "next/link";
import type { ReactNode } from "react";

export function PageHeader({
  eyebrow,
  title,
  description,
  backHref,
  backLabel = "Back",
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: ReactNode;
  backHref?: string;
  backLabel?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="page-hero">
      <div>
        {backHref && <Link className="muted" href={backHref}>← {backLabel}</Link>}
        {eyebrow && <div className="eyebrow" style={{ marginTop: backHref ? 18 : 0 }}>{eyebrow}</div>}
        <h1>{title}</h1>
        {description && <p className="muted" style={{ maxWidth: 760, marginBottom: 0 }}>{description}</p>}
      </div>
      {actions && <div className="actions-row">{actions}</div>}
    </div>
  );
}
