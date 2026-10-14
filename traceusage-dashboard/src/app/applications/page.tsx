"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { EmptyState } from "@/components/EmptyState";
import { Navbar } from "@/components/Navbar";
import { PageHeader } from "@/components/PageHeader";
import { StatCard } from "@/components/StatCard";
import { apiFetch } from "@/lib/api";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { Application } from "@/types/application";
import Link from "next/link";
import { useEffect, useState } from "react";

export default function ApplicationsPage() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const response = await apiFetch<ApiResponse<PageResponse<Application>>>("/api/applications");
        setApplications(response.data.content);
      } catch (exception) {
        setError(exception instanceof Error ? exception.message : "Failed to load applications");
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  return (
    <AuthGuard>
      <Navbar />
      <main className="container grid">
        <PageHeader
          eyebrow="Monitoring workspace"
          title="Applications"
          description="Monitor endpoint traffic, field usage, OpenAPI schema coverage, and lifecycle signals across your instrumented applications."
          actions={<Link className="button" href="/applications/new">Create application</Link>}
        />

        <section className="stats-grid">
          <StatCard label="Applications" value={applications.length} hint="Configured telemetry sources" tone="info" />
          <StatCard label="Production" value={applications.filter((app) => app.environment === "production").length} hint="Live environments" tone="success" />
          <StatCard label="Monitoring areas" value="4" hint="Endpoints, fields, schema, lifecycle" tone="purple" />
        </section>

        <div className="card">
          <div className="panel-title">
            <div>
              <h2>Application inventory</h2>
              <p className="muted" style={{ marginBottom: 0 }}>Choose an application to inspect endpoint, field, schema, or lifecycle monitoring.</p>
            </div>
          </div>
          {loading && <p>Loading applications...</p>}
          {error && <p className="error">{error}</p>}
          {!loading && !error && applications.length === 0 && (
            <EmptyState
              icon="↗"
              title="No applications yet"
              description="Create your first application to generate API keys and begin collecting endpoint and field telemetry."
              actionHref="/applications/new"
              actionLabel="Create application"
            />
          )}
          {applications.length > 0 && (
            <div style={{ overflowX: "auto" }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Environment</th>
                    <th>Project ID</th>
                    <th>Created</th>
                    <th>Monitoring views</th>
                  </tr>
                </thead>
                <tbody>
                  {applications.map((application) => (
                    <tr key={application.id}>
                      <td><strong>{application.name}</strong></td>
                      <td><span className="badge neutral">{application.environment}</span></td>
                      <td><code>{application.projectId}</code></td>
                      <td>{new Date(application.createdAt).toLocaleDateString()}</td>
                      <td>
                        <div className="actions-row">
                          <Link className="button secondary" href={`/applications/${application.projectId}/analytics`}>Endpoints</Link>
                          <Link className="button secondary" href={`/applications/${application.projectId}/fields`}>Fields</Link>
                          <Link className="button secondary" href={`/applications/${application.projectId}/openapi`}>OpenAPI</Link>
                          <Link className="button secondary" href={`/applications/${application.projectId}/lifecycle`}>Lifecycle</Link>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </AuthGuard>
  );
}
