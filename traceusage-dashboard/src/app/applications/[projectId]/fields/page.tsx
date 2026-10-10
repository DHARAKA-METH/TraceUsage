"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { StatCard } from "@/components/StatCard";
import { apiFetch } from "@/lib/api";
import type { ApiResponse } from "@/types/api";
import type { FieldAnalyticsResponse, SchemaFieldAnalyticsResponse } from "@/types/analytics";
import Link from "next/link";
import { useParams } from "next/navigation";
import { FormEvent, useEffect, useMemo, useState } from "react";

function thirtyDaysAgo() {
  const date = new Date();
  date.setDate(date.getDate() - 30);
  return date.toISOString();
}

function toDatetimeLocal(value: string) {
  return value.slice(0, 16);
}

function formatDate(value: string | null) {
  if (!value) {
    return "Never";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function FieldAnalyticsPage() {
  const params = useParams<{ projectId: string }>();
  const [analytics, setAnalytics] = useState<FieldAnalyticsResponse | null>(null);
  const [schemaAnalytics, setSchemaAnalytics] = useState<SchemaFieldAnalyticsResponse | null>(null);
  const [endpoint, setEndpoint] = useState("");
  const [clientId, setClientId] = useState("");
  const [clientVersion, setClientVersion] = useState("");
  const [from, setFrom] = useState(thirtyDaysAgo());
  const [to, setTo] = useState(new Date().toISOString());
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  async function load() {
    setError("");
    setLoading(true);

    try {
      const query = new URLSearchParams();
      if (endpoint.trim()) query.set("endpoint", endpoint.trim());
      if (clientId.trim()) query.set("clientId", clientId.trim());
      if (clientVersion.trim()) query.set("clientVersion", clientVersion.trim());
      if (from) query.set("from", from);
      if (to) query.set("to", to);

      const [runtimeResponse, schemaResponse] = await Promise.all([
        apiFetch<ApiResponse<FieldAnalyticsResponse>>(
          `/api/v1/applications/${params.projectId}/analytics/fields?${query.toString()}`,
        ),
        apiFetch<ApiResponse<SchemaFieldAnalyticsResponse>>(
          `/api/v1/applications/${params.projectId}/analytics/schema-fields?${query.toString()}`,
        ),
      ]);

      setAnalytics(runtimeResponse.data);
      setSchemaAnalytics(schemaResponse.data);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Failed to load field analytics");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.projectId]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void load();
  }

  const totals = useMemo(() => {
    const fields = analytics?.fields ?? [];
    const schemaFields = schemaAnalytics?.fields ?? [];
    return {
      fields: fields.length,
      accesses: fields.reduce((sum, field) => sum + field.totalAccesses, 0),
      noAccess: fields.filter((field) => field.status === "NO_ACCESS_OBSERVED").length,
      schemaFields: schemaFields.length,
      deprecatedSchemaFields: schemaFields.filter((field) => field.deprecated).length,
      schemaNoAccess: schemaFields.filter((field) => field.status !== "ACCESS_OBSERVED").length,
    };
  }, [analytics, schemaAnalytics]);

  function schemaStatusLabel(status: string) {
    if (status === "ACCESS_OBSERVED") return "Access observed";
    if (status === "DEPRECATED_NO_ACCESS_OBSERVED") return "Deprecated, no access observed";
    return "No access observed";
  }

  return (
    <AuthGuard>
      <Navbar />
      <main className="container grid">
        <Link className="muted" href="/applications">← Back to applications</Link>
        <div>
          <h1>{analytics?.applicationName ?? "Field usage"}</h1>
          <p className="muted">
            {analytics?.environment ?? "Loading environment"} · Project <code>{params.projectId}</code>
          </p>
        </div>

        <form className="card" onSubmit={submit}>
          <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
            <div>
              <label className="label" htmlFor="endpoint">Endpoint</label>
              <input className="input" id="endpoint" onChange={(event) => setEndpoint(event.target.value)} placeholder="/api/products/{id}" value={endpoint} />
            </div>
            <div>
              <label className="label" htmlFor="clientId">Client ID</label>
              <input className="input" id="clientId" onChange={(event) => setClientId(event.target.value)} placeholder="product-web" value={clientId} />
            </div>
            <div>
              <label className="label" htmlFor="clientVersion">Client version</label>
              <input className="input" id="clientVersion" onChange={(event) => setClientVersion(event.target.value)} placeholder="1.0.0" value={clientVersion} />
            </div>
            <div>
              <label className="label" htmlFor="from">From</label>
              <input className="input" id="from" onChange={(event) => setFrom(new Date(event.target.value).toISOString())} type="datetime-local" value={toDatetimeLocal(from)} />
            </div>
            <div>
              <label className="label" htmlFor="to">To</label>
              <input className="input" id="to" onChange={(event) => setTo(new Date(event.target.value).toISOString())} type="datetime-local" value={toDatetimeLocal(to)} />
            </div>
          </div>
          <button className="button" style={{ marginTop: 16 }} type="submit">Apply filters</button>
        </form>

        {error && <p className="error">{error}</p>}
        {loading && <p>Loading field analytics...</p>}

        {analytics && !loading && (
          <>
            <section className="stats-grid">
              <StatCard label="Fields" value={totals.fields} />
              <StatCard label="Observed accesses" value={totals.accesses} />
              <StatCard label="No access observed" value={totals.noAccess} />
              <StatCard label="Schema fields" value={totals.schemaFields} />
              <StatCard label="Deprecated schema fields" value={totals.deprecatedSchemaFields} />
              <StatCard label="Schema no access observed" value={totals.schemaNoAccess} />
            </section>

            <section className="card">
              <div style={{ alignItems: "center", display: "flex", gap: 16, justifyContent: "space-between" }}>
                <div>
                  <h2 style={{ marginTop: 0 }}>Schema-aware fields</h2>
                  <p className="muted">
                    OpenAPI contract fields joined with runtime access telemetry. This uses "No access observed", not "safe to delete".
                  </p>
                </div>
                <Link className="button secondary" href={`/applications/${params.projectId}/openapi`}>
                  Import OpenAPI
                </Link>
              </div>

              {!schemaAnalytics || schemaAnalytics.fields.length === 0 ? (
                <p className="muted">No schema fields found. Import an OpenAPI document first.</p>
              ) : (
                <div style={{ overflowX: "auto" }}>
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Endpoint</th>
                        <th>Field</th>
                        <th>Type</th>
                        <th>Required</th>
                        <th>Deprecated</th>
                        <th>Runtime accesses</th>
                        <th>Last accessed</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {schemaAnalytics.fields.map((field) => (
                        <tr key={`${field.method}-${field.endpoint}-${field.responseStatus}-${field.contentType}-${field.fieldPath}`}>
                          <td><code>{field.method} {field.endpoint}</code></td>
                          <td><code>{field.fieldPath}</code></td>
                          <td>{field.fieldType ?? "-"}</td>
                          <td>{field.required ? "Yes" : "No"}</td>
                          <td>{field.deprecated ? "Yes" : "No"}</td>
                          <td>{field.totalAccesses}</td>
                          <td>{formatDate(field.lastAccessed)}</td>
                          <td>{schemaStatusLabel(field.status)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section className="card">
              <h2 style={{ marginTop: 0 }}>Runtime field usage</h2>
              {analytics.fields.length === 0 ? (
                <p className="muted">No field usage data found for these filters.</p>
              ) : (
                <div style={{ overflowX: "auto" }}>
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Endpoint</th>
                        <th>Field</th>
                        <th>Client</th>
                        <th>Version</th>
                        <th>Accesses</th>
                        <th>Last accessed</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {analytics.fields.map((field) => (
                        <tr key={`${field.method}-${field.endpoint}-${field.fieldPath}-${field.clientId ?? "none"}-${field.clientVersion ?? "none"}`}>
                          <td><code>{field.method} {field.endpoint}</code></td>
                          <td><code>{field.fieldPath}</code></td>
                          <td>{field.clientId ?? "-"}</td>
                          <td>{field.clientVersion ?? "-"}</td>
                          <td>{field.totalAccesses}</td>
                          <td>{formatDate(field.lastAccessed)}</td>
                          <td>{field.status === "ACCESS_OBSERVED" ? "Access observed" : "No access observed"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </AuthGuard>
  );
}
