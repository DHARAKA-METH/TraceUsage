"use client";

import type { EndpointAnalyticsItem } from "@/types/analytics";
import { Bar, BarChart, CartesianGrid, Cell, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ChartCard } from "./ChartCard";

const COLORS = { error: "#fb7185", success: "#34d399" };

function endpointLabel(endpoint: EndpointAnalyticsItem) {
  return `${endpoint.method} ${endpoint.endpoint}`;
}

function tooltipStyle() {
  return {
    background: "var(--surface-solid)",
    border: "1px solid var(--border-strong)",
    borderRadius: 12,
    color: "var(--text)",
  };
}

export function EndpointCharts({ endpoints }: { endpoints: EndpointAnalyticsItem[] }) {
  const statusTotals = new Map<string, number>();
  endpoints.forEach(endpoint => Object.entries(endpoint.statusCounts ?? {}).forEach(([status, count]) => {
    statusTotals.set(status, (statusTotals.get(status) ?? 0) + count);
  }));
  const statusData = [...statusTotals].sort(([a], [b]) => Number(a) - Number(b))
    .map(([status, requests]) => ({ status, requests }));
  const ranked = [...endpoints]
    .sort((a, b) => b.totalRequests - a.totalRequests)
    .slice(0, 10)
    .map((endpoint) => ({
      name: endpointLabel(endpoint),
      requests: endpoint.totalRequests,
      success: endpoint.successCount,
      errors: endpoint.failureCount,
    }));

  const errorRate = ranked.map((endpoint) => ({
    ...endpoint,
    errorRate: endpoint.requests === 0 ? 0 : Number(((endpoint.errors / endpoint.requests) * 100).toFixed(2)),
  }));

  if (ranked.length === 0) return null;

  return (
    <section className="chart-grid">
      <ChartCard title="HTTP status codes" description="Exact status counts across all routes in the selected environment and time window.">
        {statusData.length === 0 ? <p>No status-code breakdown available.</p> : <>
          <div className="chart-canvas">
            <ResponsiveContainer height="100%" width="100%">
              <BarChart data={statusData}>
                <CartesianGrid stroke="var(--chart-grid)" vertical={false} />
                <XAxis dataKey="status" tick={{ fill: "var(--chart-axis)" }} />
                <YAxis allowDecimals={false} tick={{ fill: "var(--chart-axis)" }} />
                <Tooltip contentStyle={tooltipStyle()} />
                <Bar dataKey="requests" name="Requests" radius={[6, 6, 0, 0]}>
                  {statusData.map(item => <Cell key={item.status} fill={Number(item.status) >= 500 ? COLORS.error : Number(item.status) >= 400 ? "#fbbf24" : COLORS.success} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <p className="muted">{statusData.map(item => `${item.status}: ${item.requests.toLocaleString()}`).join(" · ")}</p>
        </>}
      </ChartCard>
      <ChartCard title="Top endpoint traffic" description="Top 10 normalized routes by requests in the selected time window.">
        <div className="chart-canvas chart-canvas-tall">
          <ResponsiveContainer height="100%" width="100%">
            <BarChart data={ranked} layout="vertical" margin={{ left: 16, right: 18, top: 4, bottom: 4 }}>
              <CartesianGrid horizontal={false} stroke="var(--chart-grid)" />
              <XAxis axisLine={false} tick={{ fill: "var(--chart-axis)", fontSize: 12 }} tickLine={false} type="number" />
              <YAxis axisLine={false} dataKey="name" tick={{ fill: "var(--chart-axis)", fontSize: 11 }} tickFormatter={(value) => value.length > 30 ? `${value.slice(0, 29)}…` : value} tickLine={false} type="category" width={138} />
              <Tooltip contentStyle={tooltipStyle()} cursor={{ fill: "var(--chart-hover)" }} />
              <Legend wrapperStyle={{ color: "var(--text-muted)", fontSize: 12 }} />
              <Bar dataKey="success" fill={COLORS.success} name="Success" radius={[0, 5, 5, 0]} stackId="requests" />
              <Bar dataKey="errors" fill={COLORS.error} name="Errors" radius={[0, 5, 5, 0]} stackId="requests" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </ChartCard>

      <ChartCard title="Endpoint error rate" description="Error rate highlights routes needing operational attention.">
        <div className="chart-canvas chart-canvas-tall">
          <ResponsiveContainer height="100%" width="100%">
            <BarChart data={errorRate} margin={{ left: 4, right: 18, top: 4, bottom: 36 }}>
              <CartesianGrid stroke="var(--chart-grid)" strokeDasharray="3 3" vertical={false} />
              <XAxis axisLine={false} dataKey="name" interval={0} tick={{ fill: "var(--chart-axis)", fontSize: 10 }} tickFormatter={(value) => value.length > 15 ? `${value.slice(0, 14)}…` : value} tickLine={false} angle={-26} textAnchor="end" />
              <YAxis axisLine={false} domain={[0, "auto"]} tick={{ fill: "var(--chart-axis)", fontSize: 12 }} tickFormatter={(value) => `${value}%`} tickLine={false} />
              <Tooltip contentStyle={tooltipStyle()} formatter={(value) => [`${value}%`, "Error rate"]} />
              <Bar dataKey="errorRate" name="Error rate" radius={[6, 6, 0, 0]}>
                {errorRate.map((item) => <Cell fill={item.errorRate > 2 ? COLORS.error : "#fbbf24"} key={item.name} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </ChartCard>
    </section>
  );
}
