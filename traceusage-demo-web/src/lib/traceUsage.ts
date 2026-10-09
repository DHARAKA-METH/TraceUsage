import { TraceUsageClient } from "@traceusage/browser";

let client: TraceUsageClient | null = null;

const PLACEHOLDER = "replace_with";

export function getTraceUsageClient() {
  if (client) {
    return client;
  }

  const publicKey = process.env.NEXT_PUBLIC_TRACEUSAGE_PUBLIC_KEY ?? "";
  const hasPublicKey = publicKey.length > 0 && !publicKey.includes(PLACEHOLDER);

  if (!hasPublicKey) {
    console.warn(
      "[TraceUsage] NEXT_PUBLIC_TRACEUSAGE_PUBLIC_KEY is not configured; field usage telemetry is disabled.",
    );
  }

  client = new TraceUsageClient({
    projectId: process.env.NEXT_PUBLIC_TRACEUSAGE_PROJECT_ID,
    clientId: process.env.NEXT_PUBLIC_TRACEUSAGE_CLIENT_ID ?? "product-web",
    clientVersion: process.env.NEXT_PUBLIC_TRACEUSAGE_CLIENT_VERSION ?? "1.0.0",
    debug: true,
    transport: hasPublicKey
      ? {
          serverUrl: process.env.NEXT_PUBLIC_TRACEUSAGE_SERVER_URL ?? "http://localhost:8080",
          publicKey,
          flushInterval: 5_000,
          maxBatchSize: 100,
        }
      : { enabled: false },
  });

  return client;
}