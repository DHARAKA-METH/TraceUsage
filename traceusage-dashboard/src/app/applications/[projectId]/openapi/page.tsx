"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { Navbar } from "@/components/Navbar";
import { StatCard } from "@/components/StatCard";
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
        <Link className="muted" href="/applications">← Back to applications</Link>

        <div>
          <h1>OpenAPI import</h1>
          <p className="muted">
            Upload <code>openapi.json</code>, <code>openapi.yaml</code>, or <code>openapi.yml</code> so TraceUsage can discover schema-defined endpoints and response fields.
          </p>
        </div>

        <form className="card form" onSubmit={submit}>
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
              <StatCard label="Endpoints discovered" value={result.endpointsDiscovered} />
              <StatCard label="Fields discovered" value={result.fieldsDiscovered} />
              <StatCard label="Deprecated endpoints" value={result.deprecatedEndpoints} />
              <StatCard label="Deprecated fields" value={result.deprecatedFields} />
            </section>

            <section className="card">
              <h2 style={{ marginTop: 0 }}>Import result</h2>
              <p><strong>Status:</strong> {result.status === "ALREADY_IMPORTED" ? "Already imported" : "Imported"}</p>
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
