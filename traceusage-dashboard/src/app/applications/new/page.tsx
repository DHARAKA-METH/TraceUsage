"use client";

import { AuthGuard } from "@/components/AuthGuard";
import { CopyApiKeyBox } from "@/components/CopyApiKeyBox";
import { Navbar } from "@/components/Navbar";
import { apiFetch } from "@/lib/api";
import type { ApiResponse } from "@/types/api";
import type { CreateApplicationResponse } from "@/types/application";
import Link from "next/link";
import { FormEvent, useState } from "react";

export default function NewApplicationPage() {
  const [name, setName] = useState("");
  const [environment, setEnvironment] = useState("development");
  const [created, setCreated] = useState<CreateApplicationResponse | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await apiFetch<ApiResponse<CreateApplicationResponse>>("/api/applications", {
        method: "POST",
        body: JSON.stringify({ name, environment }),
      });
      setCreated(response.data);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Failed to create application");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthGuard>
      <Navbar />
      <main className="container grid" style={{ maxWidth: 760 }}>
        <div>
          <h1>Create application</h1>
          <p className="muted">Create an application to generate a telemetry API key for your instrumented services.</p>
        </div>

        {created ? (
          <div className="grid">
            <CopyApiKeyBox apiKey={created.apiKey} />
            <div className="card">
              <h3 style={{ marginTop: 0 }}>{created.name}</h3>
              <p className="muted">Project ID: <code>{created.projectId}</code></p>
              <p className="muted">Environment: {created.environment}</p>
              <pre style={{ background: "#f8fafc", borderRadius: 8, overflowX: "auto", padding: 16 }}>{`traceusage.enabled=true
traceusage.server-url=http://localhost:8080
traceusage.api-key=${created.apiKey}
traceusage.batch-size=100
traceusage.queue-capacity=10000`}</pre>
              <Link className="button" href={`/applications/${created.projectId}/analytics`}>View analytics</Link>
            </div>
          </div>
        ) : (
          <div className="card">
            <form className="form" onSubmit={submit}>
              <div>
                <label className="label" htmlFor="name">Application name</label>
                <input className="input" id="name" onChange={(event) => setName(event.target.value)} placeholder="Product Service" required value={name} />
              </div>
              <div>
                <label className="label" htmlFor="environment">Environment</label>
                <select className="select" id="environment" onChange={(event) => setEnvironment(event.target.value)} value={environment}>
                  <option value="development">development</option>
                  <option value="staging">staging</option>
                  <option value="production">production</option>
                </select>
              </div>
              {error && <p className="error">{error}</p>}
              <button className="button" disabled={loading} type="submit">{loading ? "Creating..." : "Create application"}</button>
            </form>
          </div>
        )}
      </main>
    </AuthGuard>
  );
}
