import type { EndpointAnalyticsItem } from "@/types/analytics";

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
    return <p className="muted">No endpoint usage found for this period.</p>;
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
              <td><strong>{endpoint.method}</strong></td>
              <td><code>{endpoint.endpoint}</code></td>
              <td>{endpoint.totalRequests}</td>
              <td>{endpoint.successCount}</td>
              <td>{endpoint.failureCount}</td>
              <td>{formatDate(endpoint.lastSeen)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
