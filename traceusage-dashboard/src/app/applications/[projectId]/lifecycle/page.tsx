"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { apiFetch } from "@/lib/api";
import type { ApiResponse } from "@/types/api";
import type { EndpointLifecycleItem, EndpointLifecycleStatus } from "@/types/lifecycle";
import Link from "next/link";
import { useParams } from "next/navigation";
import { FormEvent, useCallback, useEffect, useState } from "react";

type StatusFilter = "ALL" | EndpointLifecycleStatus;

const FILTERS: Array<{ value: StatusFilter; label: string }> = [
  { value: "ALL", label: "All endpoints" },
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "DEPRECATED_ACTIVE", label: "Deprecated, still used" },
  { value: "DEPRECATED_INACTIVE", label: "Deprecated, unused" },
  { value: "REMOVAL_CANDIDATE", label: "Removal candidates" },
];

const STATUS_LABELS: Record<EndpointLifecycleStatus, string> = {
  NEWLY_MONITORED: "Newly monitored",
  ACTIVE: "Active",
  INACTIVE: "Inactive",
  DEPRECATED_ACTIVE: "Deprecated, still used",
  DEPRECATED_INACTIVE: "Deprecated, unused",
  REMOVAL_CANDIDATE: "Removal candidate",
};

const STATUS_COLORS: Record<EndpointLifecycleStatus, string> = {
  NEWLY_MONITORED: "#6b7280",
  ACTIVE: "#16a34a",
  INACTIVE: "#d97706",
  DEPRECATED_ACTIVE: "#ea580c",
  DEPRECATED_INACTIVE: "#7c3aed",
  REMOVAL_CANDIDATE: "#dc2626",
};

function formatDate(value: string | null) {
  if (!value) {
    return "Never";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function LifecyclePage() {
  const params = useParams<{ projectId: string }>();
  const [endpoints, setEndpoints] = useState<EndpointLifecycleItem[]>([]);
  const [filter, setFilter] = useState<StatusFilter>("ALL");
  const [selected, setSelected] = useState<EndpointLifecycleItem | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [deprecating, setDeprecating] = useState(false);
  const [reason, setReason] = useState("");
  const [replacement, setReplacement] = useState("");
  const [removalDate, setRemovalDate] = useState("");
  const [formError, setFormError] = useState("");

  const load = useCallback(async () => {
    setError("");
    setLoading(true);

    try {
      const response = await apiFetch<ApiResponse<EndpointLifecycleItem[]>>(
        `/api/v1/applications/${params.projectId}/endpoints/lifecycle`,
      );
      setEndpoints(response.data);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Failed to load lifecycle data");
    } finally {
      setLoading(false);
    }
  }, [params.projectId]);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = filter === "ALL" ? endpoints : endpoints.filter((endpoint) => endpoint.status === filter);

  async function submitDeprecation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) {
      return;
    }

    setFormError("");
    setDeprecating(true);

    try {
      await apiFetch(
        `/api/v1/applications/${params.projectId}/endpoints/${selected.endpointId}/deprecations`,
        {
          method: "POST",
          body: JSON.stringify({
            reason,
            replacementEndpoint: replacement || null,
            targetRemovalDate: removalDate || null,
          }),
        },
      );

      setSelected(null);
      setReason("");
      setReplacement("");
      setRemovalDate("");
      await load();
    } catch (exception) {
      setFormError(exception instanceof Error ? exception.message : "Failed to record deprecation");
    } finally {
      setDeprecating(false);
    }
  }

  return (
    <AuthGuard>
      <Navbar />
      <main className="container grid">
        <Link className="muted" href="/applications">← Back to applications</Link>
        <div>
          <h1>Endpoint lifecycle</h1>
          <p className="muted">
            Project <code>{params.projectId}</code>. Status is evidence for manual review, not an instruction to delete.
          </p>
        </div>

        <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
          {FILTERS.map((option) => (
            <button
              className={filter === option.value ? "button" : "button secondary"}
              key={option.value}
              onClick={() => setFilter(option.value)}
              type="button"
            >
              {option.label}
            </button>
          ))}
        </div>

        {error && <p className="error">{error}</p>}
        {loading && <p>Loading lifecycle data...</p>}

        {!loading && (
          <section className="card">
            {visible.length === 0 ? (
              <p className="muted">No endpoints registered yet. Send telemetry from your instrumented app first.</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table className="table">
                  <thead>
                    <tr>
                      <th>Endpoint</th>
                      <th>Requests</th>
                      <th>Last seen</th>
                      <th>Deprecated</th>
                      <th>Lifecycle</th>
                      <th>Detail</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visible.map((endpoint) => (
                      <tr key={endpoint.endpointId}>
                        <td><code>{endpoint.method} {endpoint.endpoint}</code></td>
                        <td>{endpoint.requestCount}</td>
                        <td>{formatDate(endpoint.lastSeen)}</td>
                        <td>{endpoint.deprecated ? "Yes" : "No"}</td>
                        <td style={{ color: STATUS_COLORS[endpoint.status], fontWeight: 700 }}>
                          {STATUS_LABELS[endpoint.status]}
                        </td>
                        <td>
                          <button className="button secondary" onClick={() => setSelected(endpoint)} type="button">
                            Open
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}

        {selected && (
          <section className="card">
            <h2 style={{ marginTop: 0 }}>
              {selected.method} {selected.endpoint}
            </h2>

            <div style={{ display: "grid", gap: 8, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
              <div><span className="muted">Lifecycle status</span><div style={{ fontWeight: 700, color: STATUS_COLORS[selected.status] }}>{STATUS_LABELS[selected.status]}</div></div>
              <div><span className="muted">Total requests</span><div style={{ fontWeight: 700 }}>{selected.requestCount}</div></div>
              <div><span className="muted">Last seen</span><div style={{ fontWeight: 700 }}>{formatDate(selected.lastSeen)}</div></div>
              <div><span className="muted">Monitoring started</span><div style={{ fontWeight: 700 }}>{formatDate(selected.monitoringStartedAt)}</div></div>
              <div><span className="muted">Inactivity threshold</span><div style={{ fontWeight: 700 }}>{selected.inactivityThresholdDays} days</div></div>
              <div><span className="muted">Known active clients</span><div style={{ fontWeight: 700 }}>{selected.activeClientCount}</div></div>
              <div><span className="muted">Monitoring coverage</span><div style={{ fontWeight: 700 }}>{selected.monitoringCoverageSufficient ? "Sufficient" : "Insufficient"}</div></div>
              <div><span className="muted">Client coverage</span><div style={{ fontWeight: 700 }}>{selected.clientCoverageSufficient ? "Sufficient" : "Unknown"}</div></div>
              <div><span className="muted">Deprecated</span><div style={{ fontWeight: 700 }}>{selected.deprecated ? "Yes" : "No"}</div></div>
              <div><span className="muted">Deprecated since</span><div style={{ fontWeight: 700 }}>{formatDate(selected.deprecatedAt)}</div></div>
              <div><span className="muted">Replacement endpoint</span><div style={{ fontWeight: 700 }}>{selected.replacementEndpoint ?? "-"}</div></div>
              <div><span className="muted">Target removal date</span><div style={{ fontWeight: 700 }}>{selected.targetRemovalDate ?? "-"}</div></div>
            </div>

            <p className="muted" style={{ marginTop: 16 }}>{selected.reason}</p>

            {selected.deprecationReason && (
              <p className="muted">Reason: {selected.deprecationReason}</p>
            )}

            {selected.deprecated ? (
              <p className="muted">Deprecation is recorded. Monitoring continues until you remove the endpoint yourself.</p>
            ) : (
              <form className="form" onSubmit={submitDeprecation} style={{ marginTop: 16 }}>
                <div>
                  <label className="label" htmlFor="reason">Reason</label>
                  <input className="input" id="reason" onChange={(event) => setReason(event.target.value)} required value={reason} />
                </div>
                <div>
                  <label className="label" htmlFor="replacement">Replacement endpoint</label>
                  <input className="input" id="replacement" onChange={(event) => setReplacement(event.target.value)} placeholder="/api/v2/products" value={replacement} />
                </div>
                <div>
                  <label className="label" htmlFor="removalDate">Target removal date</label>
                  <input className="input" id="removalDate" onChange={(event) => setRemovalDate(event.target.value)} type="date" value={removalDate} />
                </div>
                {formError && <p className="error">{formError}</p>}
                <div style={{ display: "flex", gap: 8 }}>
                  <button className="button" disabled={deprecating} type="submit">
                    {deprecating ? "Saving..." : "Mark deprecated"}
                  </button>
                  <button className="button secondary" onClick={() => setSelected(null)} type="button">
                    Cancel
                  </button>
                </div>
              </form>
            )}
          </section>
        )}
      </main>
    </AuthGuard>
  );
}