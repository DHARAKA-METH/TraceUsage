export type EndpointLifecycleStatus =
  | "NEWLY_MONITORED"
  | "ACTIVE"
  | "INACTIVE"
  | "DEPRECATED_ACTIVE"
  | "DEPRECATED_INACTIVE"
  | "REMOVAL_CANDIDATE";

export type EndpointLifecycleItem = {
  endpointId: number;
  method: string;
  endpoint: string;
  requestCount: number;
  firstSeen: string | null;
  lastSeen: string | null;
  monitoringStartedAt: string | null;
  inactivityThresholdDays: number;
  deprecated: boolean;
  deprecatedAt: string | null;
  deprecationReason: string | null;
  replacementEndpoint: string | null;
  targetRemovalDate: string | null;
  activeClientCount: number;
  monitoringCoverageSufficient: boolean;
  clientCoverageSufficient: boolean;
  status: EndpointLifecycleStatus;
  reason: string;
};