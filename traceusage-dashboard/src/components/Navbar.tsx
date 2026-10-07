"use client";

import { clearToken } from "@/lib/auth";
import Link from "next/link";
import { useRouter } from "next/navigation";

export function Navbar() {
  const router = useRouter();

  function logout() {
    clearToken();
    router.push("/login");
  }

  return (
    <header style={{ background: "white", borderBottom: "1px solid var(--border)", marginBottom: 32 }}>
      <div className="container" style={{ alignItems: "center", display: "flex", justifyContent: "space-between", padding: "18px 0" }}>
        <Link href="/applications" style={{ fontSize: 20, fontWeight: 800 }}>
          TraceUsage
        </Link>
        <nav style={{ alignItems: "center", display: "flex", gap: 16 }}>
          <Link href="/applications" className="muted">Applications</Link>
          <Link href="/applications/new" className="button secondary">New application</Link>
          <button className="button" onClick={logout} type="button">Logout</button>
        </nav>
      </div>
    </header>
  );
}
