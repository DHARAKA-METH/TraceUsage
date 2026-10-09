"use client";

import { useState } from "react";

export function CopyApiKeyBox({
  apiKey,
  title = "Copy your API key now",
  description = "This raw key is returned only when the application is created.",
}: {
  apiKey: string;
  title?: string;
  description?: string;
}) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    await navigator.clipboard.writeText(apiKey);
    setCopied(true);
  }

  return (
    <div className="card" style={{ borderColor: "#facc15", background: "#fffbeb" }}>
      <h3 style={{ marginTop: 0 }}>{title}</h3>
      <p className="muted">{description}</p>
      <code style={{ display: "block", overflowX: "auto", padding: 12, background: "white", borderRadius: 8 }}>{apiKey}</code>
      <button className="button" onClick={copy} style={{ marginTop: 12 }} type="button">
        {copied ? "Copied" : "Copy API key"}
      </button>
    </div>
  );
}
