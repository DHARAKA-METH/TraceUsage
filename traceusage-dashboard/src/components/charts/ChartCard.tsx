import type { ReactNode } from "react";

export function ChartCard({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <section className="card chart-card">
      <div className="panel-title">
        <div>
          <h2>{title}</h2>
          <p className="muted chart-description">{description}</p>
        </div>
      </div>
      {children}
    </section>
  );
}
