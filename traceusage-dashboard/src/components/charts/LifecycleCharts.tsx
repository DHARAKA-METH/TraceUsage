"use client";

import type { EndpointLifecycleItem, EndpointLifecycleStatus } from "@/types/lifecycle";
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { ChartCard } from "./ChartCard";

const LABELS: Record<EndpointLifecycleStatus, string> = {
  NEWLY_MONITORED: "Newly monitored",
  ACTIVE: "Active",
  INACTIVE: "Low activity",
  DEPRECATED_ACTIVE: "Deprecated, active",
  DEPRECATED_INACTIVE: "Deprecated, inactive",
  REMOVAL_CANDIDATE: "Manual review candidate",
};

const COLORS: Record<EndpointLifecycleStatus, string> = {
  NEWLY_MONITORED: "#60a5fa",
  ACTIVE: "#34d399",
  INACTIVE: "#94a3b8",
  DEPRECATED_ACTIVE: "#a78bfa",
  DEPRECATED_INACTIVE: "#fbbf24",
  REMOVAL_CANDIDATE: "#f59e0b",
};

function daysSince(value: string | null) {
  if (!value) return null;
  return Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 86_400_000));
}

export function LifecycleCharts({ endpoints }: { endpoints: EndpointLifecycleItem[] }) {
  const distribution = Object.entries(LABELS)
    .map(([status, name]) => ({ status: status as EndpointLifecycleStatus, name, value: endpoints.filter((endpoint) => endpoint.status === status).length }))
    .filter((item) => item.value > 0);
  const timeline = [...endpoints]
    .sort((a, b) => (daysSince(b.lastSeen) ?? -1) - (daysSince(a.lastSeen) ?? -1))
    .slice(0, 8);

  if (endpoints.length === 0) return null;

  return (
    <section className="chart-grid">
      <ChartCard title="Lifecycle distribution" description="Lifecycle recommendations are evidence for manual review, never an automatic removal action.">
        <div className="coverage-chart">
          <div className="chart-canvas">
            <ResponsiveContainer height="100%" width="100%">
              <PieChart>
                <Pie data={distribution} dataKey="value" innerRadius={56} outerRadius={82} paddingAngle={3} stroke="none">
                  {distribution.map((item) => <Cell fill={COLORS[item.status]} key={item.status} />)}
                </Pie>
                <Tooltip contentStyle={{ background: "var(--surface-solid)", border: "1px solid var(--border-strong)", borderRadius: 12, color: "var(--text)" }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="chart-legend-list">
            {distribution.map((item) => <div className="chart-legend-item" key={item.status}><span style={{ background: COLORS[item.status] }} />{item.name}<strong>{item.value}</strong></div>)}
          </div>
        </div>
      </ChartCard>

      <ChartCard title="Inactivity timeline" description="Days since last traffic compared with each endpoint’s configured inactivity threshold.">
        <div className="inactivity-list">
          {timeline.map((endpoint) => {
            const inactiveDays = daysSince(endpoint.lastSeen);
            const percentage = inactiveDays === null ? 100 : Math.min(100, (inactiveDays / endpoint.inactivityThresholdDays) * 100);
            return <div className="inactivity-item" key={endpoint.endpointId}>
              <div className="inactivity-label"><code>{endpoint.method} {endpoint.endpoint}</code><span>{inactiveDays === null ? "No traffic" : `${inactiveDays}d inactive`}</span></div>
              <div className="inactivity-track"><span style={{ background: COLORS[endpoint.status], width: `${Math.max(3, percentage)}%` }} /></div>
              <small>Threshold: {endpoint.inactivityThresholdDays} days</small>
            </div>;
          })}
        </div>
      </ChartCard>
    </section>
  );
}
