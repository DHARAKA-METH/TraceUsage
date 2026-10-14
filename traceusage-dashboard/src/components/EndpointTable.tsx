import type { EndpointAnalyticsItem } from "@/types/analytics";
import { EmptyState } from "./EmptyState";
import { StatusBadge } from "./StatusBadge";

function formatDate(value: string | null) {
  if (!value) {
    return "Never";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function EndpointTable({ endpoints }: { endpoints: EndpointAnalyticsItem[] }) {
  if (endpoints.length === 0) {
    return (
      <EmptyState
        icon="≋"
        title="No endpoint requests observed"
        description="No endpoint telemetry matched this time window. Try widening the time range or send traffic through your instrumented API."
      />
    );
  }

  return (
    <div style={{ overflowX: "auto" }}>
      <table className="table">
        <thead>
          <tr>
            <th>Method</th>
            <th>Endpoint</th>
            <th>Requests</th>
            <th>Success</th>
            <th>Failed</th>
            <th>Last seen</th>
          </tr>
        </thead>
        <tbody>
          {endpoints.map((endpoint) => (
            <tr key={`${endpoint.method}-${endpoint.endpoint}`}>
              <td><span className="badge info">{endpoint.method}</span></td>
              <td><code>{endpoint.endpoint}</code></td>
              <td>{endpoint.totalRequests}</td>
              <td><StatusBadge status="ACCESS_OBSERVED" label={`${endpoint.successCount} success`} /></td>
              <td>{endpoint.failureCount > 0 ? <StatusBadge status="DEPRECATED_ACTIVE" label={`${endpoint.failureCount} failed`} /> : <span className="muted">0</span>}</td>
              <td>{formatDate(endpoint.lastSeen)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
