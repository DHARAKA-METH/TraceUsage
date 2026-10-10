"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
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
        <div style={{ alignItems: "center", display: "flex", justifyContent: "space-between" }}>
          <div>
            <h1>Applications</h1>
            <p className="muted">Create applications, copy API keys, and view endpoint analytics.</p>
          </div>
          <Link className="button" href="/applications/new">Create application</Link>
        </div>

        <div className="card">
          {loading && <p>Loading applications...</p>}
          {error && <p className="error">{error}</p>}
          {!loading && !error && applications.length === 0 && (
            <p className="muted">No applications yet. Create your first application to get an API key.</p>
          )}
          {applications.length > 0 && (
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Environment</th>
                  <th>Project ID</th>
                  <th>Created</th>
                  <th>Analytics</th>
                </tr>
              </thead>
              <tbody>
                {applications.map((application) => (
                  <tr key={application.id}>
                    <td><strong>{application.name}</strong></td>
                    <td>{application.environment}</td>
                    <td><code>{application.projectId}</code></td>
                    <td>{new Date(application.createdAt).toLocaleDateString()}</td>
                    <td style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
                      <Link className="button secondary" href={`/applications/${application.projectId}/analytics`}>
                        Endpoints
                      </Link>
                      <Link className="button secondary" href={`/applications/${application.projectId}/fields`}>
                        Fields
                      </Link>
                      <Link className="button secondary" href={`/applications/${application.projectId}/openapi`}>
                        OpenAPI
                      </Link>
                      <Link className="button secondary" href={`/applications/${application.projectId}/lifecycle`}>
                        Lifecycle
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>
    </AuthGuard>
  );
}
