"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { EndpointTable } from "@/components/EndpointTable";
import { Navbar } from "@/components/Navbar";
import { StatCard } from "@/components/StatCard";
import { apiFetch } from "@/lib/api";
import type { ApiResponse } from "@/types/api";
import type { EndpointAnalyticsResponse } from "@/types/analytics";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

function thirtyDaysAgo() {
  const date = new Date();
  date.setDate(date.getDate() - 30);
  return date.toISOString();
}

export default function AnalyticsPage() {
  const params = useParams<{ projectId: string }>();
  const [analytics, setAnalytics] = useState<EndpointAnalyticsResponse | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [from, setFrom] = useState(thirtyDaysAgo());

  useEffect(() => {
    async function load() {
      setError("");
      setLoading(true);
      try {
        const query = new URLSearchParams({ from }).toString();
        const response = await apiFetch<ApiResponse<EndpointAnalyticsResponse>>(
          `/api/v1/applications/${params.projectId}/analytics/endpoints?${query}`,
        );
        setAnalytics(response.data);
      } catch (exception) {
        setError(exception instanceof Error ? exception.message : "Failed to load analytics");
      } finally {
        setLoading(false);
      }
    }

    load();
  }, [from, params.projectId]);

  const totals = useMemo(() => {
    const endpoints = analytics?.endpoints ?? [];
    return {
      requests: endpoints.reduce((sum, endpoint) => sum + endpoint.totalRequests, 0),
      endpointCount: endpoints.length,
      errors: endpoints.reduce((sum, endpoint) => sum + endpoint.failureCount, 0),
    };
  }, [analytics]);

  return (
    <AuthGuard>
      <Navbar />
      <main className="container grid">
        <Link className="muted" href="/applications">← Back to applications</Link>
        <div style={{ alignItems: "end", display: "flex", gap: 16, justifyContent: "space-between" }}>
          <div>
            <h1>{analytics?.applicationName ?? "Endpoint analytics"}</h1>
            <p className="muted">
              {analytics?.environment ?? "Loading environment"} · Project <code>{params.projectId}</code>
            </p>
          </div>
          <div style={{ minWidth: 280 }}>
            <label className="label" htmlFor="from">From</label>
            <input className="input" id="from" onChange={(event) => setFrom(new Date(event.target.value).toISOString())} type="datetime-local" />
          </div>
        </div>

        {error && <p className="error">{error}</p>}
        {loading && <p>Loading analytics...</p>}

        {analytics && !loading && (
          <>
            <section className="stats-grid">
              <StatCard label="Requests" value={totals.requests} />
              <StatCard label="Endpoints" value={totals.endpointCount} />
              <StatCard label="Errors" value={totals.errors} />
            </section>

            <section className="card">
              <h2 style={{ marginTop: 0 }}>Endpoint usage</h2>
              <EndpointTable endpoints={analytics.endpoints} />
            </section>
          </>
        )}
      </main>
    </AuthGuard>
  );
}
