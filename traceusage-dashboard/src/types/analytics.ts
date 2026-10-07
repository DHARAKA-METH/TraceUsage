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
