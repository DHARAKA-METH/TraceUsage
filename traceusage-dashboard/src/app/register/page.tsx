"use client";

import { apiFetch } from "@/lib/api";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";

export default function RegisterPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      await apiFetch("/api/auth/register", {
        auth: false,
        method: "POST",
        body: JSON.stringify({ name, email, password }),
      });
      router.push("/login");
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="container auth-shell">
      <section className="auth-hero">
        <div className="eyebrow">Start monitoring</div>
        <h1>Create a TraceUsage workspace.</h1>
        <p className="muted">Generate keys, ingest telemetry, and compare runtime usage against your API contract.</p>
      </section>
      <div className="card elevated" style={{ width: "100%" }}>
        <h1 style={{ fontSize: 34 }}>Create account</h1>
        <p className="muted">Set up your developer account to begin monitoring applications.</p>
        <form className="form" onSubmit={submit}>
          <div>
            <label className="label" htmlFor="name">Name</label>
            <input className="input" id="name" onChange={(event) => setName(event.target.value)} required value={name} />
          </div>
          <div>
            <label className="label" htmlFor="email">Email</label>
            <input className="input" id="email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
          </div>
          <div>
            <label className="label" htmlFor="password">Password</label>
            <input className="input" id="password" minLength={8} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
          </div>
          {error && <p className="error">{error}</p>}
          <button className="button" disabled={loading} type="submit">{loading ? "Creating..." : "Register"}</button>
        </form>
        <p className="muted">Already have an account? <Link href="/login">Login</Link></p>
      </div>
    </main>
  );
}
