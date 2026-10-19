"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { ApiResponse } from "@/types/api";

const tabs = [
  { label: "Overview", segment: "" },
  { label: "Endpoints", segment: "/endpoints" },
  { label: "Fields", segment: "/fields" },
  { label: "OpenAPI", segment: "/openapi" },
  { label: "Lifecycle", segment: "/lifecycle" },
];

export function ApplicationTabs({ projectId }: { projectId: string }) {
  const pathname = usePathname();
  const basePath = `/applications/${projectId}`;
  const [environmentId, setEnvironmentId] = useState("");
  const [environments, setEnvironments] = useState<Array<{ id: number; name: string }>>([]);
  const [error, setError] = useState("");
  useEffect(() => {
    setEnvironmentId(new URLSearchParams(window.location.search).get("environmentId") ?? "");
    let active = true;
    apiFetch<ApiResponse<Array<{ id: number; name: string }>>>(`/api/v1/applications/${projectId}/environments`)
      .then(response => { if (active) setEnvironments(response.data); })
      .catch(error => { if (active) setError(error instanceof Error ? error.message : "Cannot load environments"); });
    return () => { active = false; };
  }, [projectId]);

  return (
    <div><label>Environment <select className="input" value={environmentId} onChange={event => {
      const url = new URL(window.location.href);
      if (event.target.value) url.searchParams.set("environmentId", event.target.value);
      else url.searchParams.delete("environmentId");
      window.location.assign(url.toString());
    }}><option value="">All environments</option>{environments.map(environment => <option key={environment.id} value={environment.id}>{environment.name}</option>)}</select></label>
    {error && <p role="alert" className="error">{error}</p>}
    <nav aria-label="Application monitoring views" className="application-tabs">
      {tabs.map((tab) => {
        const href = `${basePath}${tab.segment}`;
        const active = tab.segment === ""
          ? pathname === basePath
          : pathname === href || (tab.segment === "/endpoints" && pathname === `${basePath}/analytics`);

        return (
          <Link aria-current={active ? "page" : undefined} className={active ? "application-tab active" : "application-tab"} href={environmentId ? `${href}?environmentId=${environmentId}` : href} key={tab.label}>
            {tab.label}
          </Link>
        );
      })}
    </nav></div>
  );
}
