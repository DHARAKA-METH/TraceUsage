"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

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

  return (
    <nav aria-label="Application monitoring views" className="application-tabs">
      {tabs.map((tab) => {
        const href = `${basePath}${tab.segment}`;
        const active = tab.segment === ""
          ? pathname === basePath
          : pathname === href || (tab.segment === "/endpoints" && pathname === `${basePath}/analytics`);

        return (
          <Link aria-current={active ? "page" : undefined} className={active ? "application-tab active" : "application-tab"} href={href} key={tab.label}>
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}
