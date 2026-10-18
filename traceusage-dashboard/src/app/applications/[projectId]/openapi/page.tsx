"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { ApplicationTabs } from "@/components/ApplicationTabs";
import { Navbar } from "@/components/Navbar";
import { PageHeader } from "@/components/PageHeader";
import { StatCard } from "@/components/StatCard";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch } from "@/lib/api";
import type { ApiResponse } from "@/types/api";
import type { OpenApiImportResponse } from "@/types/openapi";
import Link from "next/link";
import { useParams } from "next/navigation";
import { FormEvent, useState } from "react";

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function OpenApiImportPage() {
  const params = useParams<{ projectId: string }>();
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<OpenApiImportResponse | null>(null);
  const [error, setError] = useState("");
  const [uploading, setUploading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setResult(null);

    if (!file) {
      setError("Choose an OpenAPI file first");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    setUploading(true);
    try {
      const response = await apiFetch<ApiResponse<OpenApiImportResponse>>(
        `/api/v1/applications/${params.projectId}/openapi/import`,
        {
          method: "POST",
          body: formData,
        },
      );
      setResult(response.data);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Failed to import OpenAPI document");
    } finally {
      setUploading(false);
    }
  }

  return (
    <AuthGuard>
      <Navbar />
      <main className="container grid">
        <PageHeader
          backHref={`/applications/${params.projectId}`}
          backLabel="Application overview"
          eyebrow="Schema intelligence"
          title="OpenAPI import"
          description={<>Upload <code>openapi.json</code>, <code>openapi.yaml</code>, or <code>openapi.yml</code> so TraceUsage can discover contract-defined endpoints and response fields.</>}
          actions={<Link className="button secondary" href={`/applications/${params.projectId}/fields`}>View schema analytics</Link>}
        />
        <ApplicationTabs projectId={params.projectId} />

        <form className="card elevated form" onSubmit={submit}>
          <div>
            <h2>Import API contract</h2>
            <p className="muted" style={{ marginBottom: 0 }}>TraceUsage will extract endpoints, response fields, and OpenAPI deprecated flags. Runtime values are never collected.</p>
          </div>
          <div>
            <label className="label" htmlFor="openapi-file">OpenAPI file</label>
            <input
              accept=".json,.yaml,.yml,application/json,application/yaml,text/yaml,text/x-yaml"
              className="input"
              id="openapi-file"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              type="file"
            />
            <p className="muted">Maximum size: 5 MB.</p>
          </div>

          {error && <p className="error">{error}</p>}

          <button className="button" disabled={uploading} type="submit">
            {uploading ? "Importing..." : "Import OpenAPI"}
          </button>
        </form>

        {result && (
          <>
            <section className="stats-grid">
              <StatCard label="Endpoints discovered" value={result.endpointsDiscovered} tone="info" />
              <StatCard label="Fields discovered" value={result.fieldsDiscovered} tone="purple" />
              <StatCard label="Deprecated endpoints" value={result.deprecatedEndpoints} tone={result.deprecatedEndpoints > 0 ? "warning" : "success"} />
              <StatCard label="Deprecated fields" value={result.deprecatedFields} tone={result.deprecatedFields > 0 ? "danger" : "success"} />
            </section>

            <section className="card">
              <div className="panel-title">
                <div>
                  <h2>Import result</h2>
                  <p className="muted" style={{ marginBottom: 0 }}>Contract intelligence is now available in field analytics.</p>
                </div>
                <StatusBadge status={result.status} />
              </div>
              <p><strong>File:</strong> {result.fileName}</p>
              <p><strong>Size:</strong> {formatBytes(result.sizeBytes)}</p>
              <p><strong>Imported at:</strong> {formatDate(result.importedAt)}</p>
              <Link className="button secondary" href={`/applications/${params.projectId}/fields`}>
                View schema-aware field analytics
              </Link>
            </section>
          </>
        )}
      </main>
    </AuthGuard>
  );
}
