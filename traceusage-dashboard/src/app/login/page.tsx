"use client";

import { apiFetch } from "@/lib/api";
import { setToken } from "@/lib/auth";
import type { ApiResponse } from "@/types/api";
import type { LoginResponse } from "@/types/auth";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await apiFetch<ApiResponse<LoginResponse>>("/api/auth/login", {
        auth: false,
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setToken(response.data.accessToken);
      router.push("/applications");
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="container" style={{ display: "grid", minHeight: "100vh", placeItems: "center" }}>
      <div className="card" style={{ maxWidth: 440, width: "100%" }}>
        <h1>Login</h1>
        <p className="muted">Access your TraceUsage applications and endpoint analytics.</p>
        <form className="form" onSubmit={submit}>
          <div>
            <label className="label" htmlFor="email">Email</label>
            <input className="input" id="email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
          </div>
          <div>
            <label className="label" htmlFor="password">Password</label>
            <input className="input" id="password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
          </div>
          {error && <p className="error">{error}</p>}
          <button className="button" disabled={loading} type="submit">{loading ? "Logging in..." : "Login"}</button>
        </form>
        <p className="muted">No account? <Link href="/register">Register</Link></p>
      </div>
    </main>
  );
}
