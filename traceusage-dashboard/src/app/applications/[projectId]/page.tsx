"use client";

import { ApplicationTabs } from "@/components/ApplicationTabs";
import { AuthGuard } from "@/components/AuthGuard";
import { EmptyState } from "@/components/EmptyState";
import { Navbar } from "@/components/Navbar";
import { PageHeader } from "@/components/PageHeader";
import { StatCard } from "@/components/StatCard";
import { apiFetch } from "@/lib/api";
import type { ApiResponse } from "@/types/api";
import type { EndpointAnalyticsResponse, FieldAnalyticsResponse } from "@/types/analytics";
import type { EndpointLifecycleItem } from "@/types/lifecycle";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

function thirtyDaysAgo() {
  const date = new Date();
  date.setDate(date.getDate() - 30);
  return date.toISOString();
}

export default function ApplicationOverviewPage() {
  const params = useParams<{ projectId: string }>();
  const [endpoints, setEndpoints] = useState<EndpointAnalyticsResponse | null>(null);
  const [fields, setFields] = useState<FieldAnalyticsResponse | null>(null);
  const [lifecycle, setLifecycle] = useState<EndpointLifecycleItem[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError("");
      try {
        const query = new URLSearchParams({ from: thirtyDaysAgo() });
        const [endpointResponse, fieldResponse, lifecycleResponse] = await Promise.all([
          apiFetch<ApiResponse<EndpointAnalyticsResponse>>(`/api/v1/applications/${params.projectId}/analytics/endpoints?${query}`),
          apiFetch<ApiResponse<FieldAnalyticsResponse>>(`/api/v1/applications/${params.projectId}/analytics/fields?${query}`),
          apiFetch<ApiResponse<EndpointLifecycleItem[]>>(`/api/v1/applications/${params.projectId}/endpoints/lifecycle`),
        ]);
        setEndpoints(endpointResponse.data);
        setFields(fieldResponse.data);
        setLifecycle(lifecycleResponse.data);
      } catch (exception) {
        setError(exception instanceof Error ? exception.message : "Failed to load application workspace");
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [params.projectId]);

  const totals = useMemo(() => {
    const endpointItems = endpoints?.endpoints ?? [];
    const fieldItems = fields?.fields ?? [];
    return {
      requests: endpointItems.reduce((sum, item) => sum + item.totalRequests, 0),
      endpointCount: endpointItems.length,
      clientCount: new Set(fieldItems.map((item) => item.clientId).filter(Boolean)).size,
      reviewCount: lifecycle.filter((item) => item.status === "REMOVAL_CANDIDATE").length,
    };
  }, [endpoints, fields, lifecycle]);

  return (
    <AuthGuard>
      <Navbar />
      <main className="container grid">
        <PageHeader
          backHref="/applications"
          backLabel="Applications"
          eyebrow="Application workspace"
          title={endpoints?.applicationName ?? "Application overview"}
          description={<>{endpoints?.environment ?? "Loading environment"} · Project <code>{params.projectId}</code> · Last 30 days · UTC</>}
          actions={<><Link className="button secondary" href={`/applications/${params.projectId}/openapi`}>Import OpenAPI</Link><Link className="button" href={`/applications/${params.projectId}/endpoints`}>View endpoints</Link></>}
        />

        <ApplicationTabs projectId={params.projectId} />

        {error && <p className="error">{error}</p>}
        {loading && <section className="stats-grid" aria-label="Loading application overview"><div className="skeleton-card" /><div className="skeleton-card" /><div className="skeleton-card" /><div className="skeleton-card" /></section>}

        {!loading && !error && (
          <>
            <section className="stats-grid">
              <Link className="stat-link" href={`/applications/${params.projectId}/endpoints`}><StatCard label="Requests" value={totals.requests} hint="Observed API calls" tone="info" /></Link>
              <Link className="stat-link" href={`/applications/${params.projectId}/endpoints`}><StatCard label="Active endpoints" value={totals.endpointCount} hint="Routes with telemetry" tone="success" /></Link>
              <Link className="stat-link" href={`/applications/${params.projectId}/fields`}><StatCard label="Browser clients" value={totals.clientCount} hint="Instrumented clients" tone="purple" /></Link>
              <Link className="stat-link" href={`/applications/${params.projectId}/lifecycle`}><StatCard label="Manual review" value={totals.reviewCount} hint="Evidence, not deletion" tone="warning" /></Link>
            </section>

            <section className="workspace-grid">
              <div className="card workspace-panel">
                <div className="panel-title"><div><h2>Explore monitoring evidence</h2><p className="muted">Choose a monitoring view without leaving this application workspace.</p></div></div>
                <div className="workspace-actions">
                  <Link href={`/applications/${params.projectId}/endpoints`}><strong>Endpoints</strong><span>Traffic, status codes, and errors</span><b>→</b></Link>
                  <Link href={`/applications/${params.projectId}/fields`}><strong>Fields</strong><span>Runtime field access by client</span><b>→</b></Link>
                  <Link href={`/applications/${params.projectId}/openapi`}><strong>OpenAPI</strong><span>Contract and runtime coverage</span><b>→</b></Link>
                  <Link href={`/applications/${params.projectId}/lifecycle`}><strong>Lifecycle</strong><span>Manual review evidence</span><b>→</b></Link>
                </div>
              </div>
              <div className="card workspace-panel">
                <div className="panel-title"><div><h2>Monitoring coverage</h2><p className="muted">Interpret zeroes with coverage context.</p></div></div>
                <div className="coverage-list">
                  <div><span>Endpoint telemetry</span><strong>{totals.endpointCount > 0 ? "Receiving events" : "No traffic observed"}</strong></div>
                  <div><span>Browser telemetry</span><strong>{totals.clientCount > 0 ? `${totals.clientCount} active client${totals.clientCount === 1 ? "" : "s"}` : "No client telemetry"}</strong></div>
                  <div><span>Lifecycle evidence</span><strong>{totals.reviewCount > 0 ? `${totals.reviewCount} review candidate${totals.reviewCount === 1 ? "" : "s"}` : "No review candidates"}</strong></div>
                </div>
              </div>
            </section>

            {totals.endpointCount === 0 && <EmptyState icon="⌁" title="No endpoint telemetry yet" description="Install the Spring Boot starter and send traffic through a route under /api/** to begin monitoring." actionHref={`/applications/${params.projectId}/endpoints`} actionLabel="View endpoint setup" />}
          </>
        )}
      </main>
    </AuthGuard>
  );
}
