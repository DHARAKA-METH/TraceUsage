"use client";

import type { FieldAnalyticsItem, SchemaFieldAnalyticsItem } from "@/types/analytics";
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ChartCard } from "./ChartCard";

const COLORS = ["#60a5fa", "#818cf8", "#a78bfa", "#34d399", "#fbbf24", "#fb7185"];

function tooltipStyle() {
  return { background: "var(--surface-solid)", border: "1px solid var(--border-strong)", borderRadius: 12, color: "var(--text)" };
}

export function FieldCharts({ fields, schemaFields }: { fields: FieldAnalyticsItem[]; schemaFields: SchemaFieldAnalyticsItem[] }) {
  const byField = new Map<string, number>();
  fields.forEach((field) => byField.set(field.fieldPath, (byField.get(field.fieldPath) ?? 0) + field.totalAccesses));
  const fieldDistribution = [...byField.entries()]
    .map(([name, accesses]) => ({ name, accesses }))
    .sort((a, b) => b.accesses - a.accesses)
    .slice(0, 10);

  const accessed = schemaFields.filter((field) => field.status === "ACCESS_OBSERVED").length;
  const noAccess = schemaFields.filter((field) => field.status !== "ACCESS_OBSERVED").length;
  const coverage = [
    { name: "Access observed", value: accessed, color: "#34d399" },
    { name: "No access observed", value: noAccess, color: "#fbbf24" },
  ].filter((item) => item.value > 0);

  if (fieldDistribution.length === 0 && coverage.length === 0) return null;

  return (
    <section className="chart-grid">
      <ChartCard title="Field access distribution" description="Most-read runtime field paths across the selected endpoint and clients.">
        {fieldDistribution.length === 0 ? <p className="muted">No field access counts are available for this selection.</p> : (
          <div className="chart-canvas chart-canvas-tall">
            <ResponsiveContainer height="100%" width="100%">
              <BarChart data={fieldDistribution} layout="vertical" margin={{ left: 16, right: 18, top: 4, bottom: 4 }}>
                <CartesianGrid horizontal={false} stroke="var(--chart-grid)" />
                <XAxis axisLine={false} tick={{ fill: "var(--chart-axis)", fontSize: 12 }} tickLine={false} type="number" />
                <YAxis axisLine={false} dataKey="name" tick={{ fill: "var(--chart-axis)", fontSize: 11 }} tickLine={false} type="category" width={138} />
                <Tooltip contentStyle={tooltipStyle()} cursor={{ fill: "var(--chart-hover)" }} />
                <Bar dataKey="accesses" fill="#60a5fa" name="Accesses" radius={[0, 5, 5, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </ChartCard>

      <ChartCard title="Contract access coverage" description="OpenAPI fields compared with access observed among instrumented clients.">
        {coverage.length === 0 ? <p className="muted">Import an OpenAPI specification to view contract coverage.</p> : (
          <div className="coverage-chart">
            <div className="chart-canvas">
              <ResponsiveContainer height="100%" width="100%">
                <PieChart>
                  <Pie data={coverage} dataKey="value" innerRadius={56} outerRadius={82} paddingAngle={3} stroke="none">
                    {coverage.map((item) => <Cell fill={item.color} key={item.name} />)}
                  </Pie>
                  <Tooltip contentStyle={tooltipStyle()} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="chart-legend-list">
              {coverage.map((item) => <div className="chart-legend-item" key={item.name}><span style={{ background: item.color }} />{item.name}<strong>{item.value}</strong></div>)}
            </div>
          </div>
        )}
      </ChartCard>
    </section>
  );
}
