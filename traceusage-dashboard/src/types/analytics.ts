export type EndpointAnalyticsItem = {
  method: string;
  endpoint: string;
  totalRequests: number;
  successCount: number;
  failureCount: number;
  lastSeen: string | null;
};

export type EndpointAnalyticsResponse = {
  projectId: string;
  applicationName: string;
  environment: string;
  endpoints: EndpointAnalyticsItem[];
};

export type FieldAnalyticsItem = {
  method: string;
  endpoint: string;
  schema: string | null;
  fieldPath: string;
  clientId: string | null;
  clientVersion: string | null;
  totalAccesses: number;
  firstSeen: string | null;
  lastSeen: string | null;
  lastAccessed: string | null;
  status: "ACCESS_OBSERVED" | "NO_ACCESS_OBSERVED" | string;
};

export type FieldAnalyticsResponse = {
  projectId: string;
  applicationName: string;
  environment: string;
  fields: FieldAnalyticsItem[];
};
