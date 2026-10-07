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
    <main className="container" style={{ display: "grid", minHeight: "100vh", placeItems: "center" }}>
      <div className="card" style={{ maxWidth: 480, width: "100%" }}>
        <h1>Create account</h1>
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
