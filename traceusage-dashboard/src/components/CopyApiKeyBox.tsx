"use client";

import { useState } from "react";

export function CopyApiKeyBox({
  apiKey,
  title = "Copy your API key now",
  description = "This raw key is returned only when the application is created.",
  kind = "secret",
}: {
  apiKey: string;
  title?: string;
  description?: string;
  kind?: "secret" | "public";
}) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    await navigator.clipboard.writeText(apiKey);
    setCopied(true);
  }

  return (
    <section className={`credential-card ${kind}`}>
      <div className="credential-heading"><span className="credential-kind">{kind === "secret" ? "Server secret" : "Browser public"}</span><span className="credential-once">Shown once</span></div>
      <h3>{title}</h3>
      <p className="muted">{description}</p>
      <code className="credential-value">{apiKey}</code>
      <button className="button" onClick={copy} type="button">
        {copied ? "✓ Copied" : "Copy key"}
      </button>
    </section>
  );
}
