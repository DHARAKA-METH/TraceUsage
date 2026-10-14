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
    <header className="nav-shell">
      <div className="container" style={{ alignItems: "center", display: "flex", justifyContent: "space-between", padding: "18px 0" }}>
        <Link href="/applications" style={{ alignItems: "center", display: "inline-flex", gap: 10, fontSize: 20, fontWeight: 900, letterSpacing: "-0.03em" }}>
          <span className="brand-mark">T</span>
          <span>TraceUsage</span>
        </Link>
        <nav style={{ alignItems: "center", display: "flex", gap: 16 }}>
          <Link href="/applications" className="muted">Applications</Link>
          <Link href="/applications/new" className="button secondary">New application</Link>
          <button className="button ghost" onClick={logout} type="button">Logout</button>
        </nav>
      </div>
    </header>
  );
}
