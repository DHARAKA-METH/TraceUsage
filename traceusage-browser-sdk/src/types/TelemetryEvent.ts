export type TraceUsageHttpMethod =
  | "GET"
  | "POST"
  | "PUT"
  | "PATCH"
  | "DELETE"
  | "OPTIONS"
  | "HEAD"
  | string;

export type TraceUsageResponseContext = {
  endpoint: string;
  method: TraceUsageHttpMethod;
  schema?: string;
};

export type TraceUsageClientIdentity = {
  projectId?: string;
  clientId?: string;
  clientVersion?: string;
};

export type TraceUsageFieldAccessEvent = TraceUsageClientIdentity & TraceUsageResponseContext & {
  fieldPath: string;
  accessedAt: Date;
};

export type TraceUsageObservedFieldsEvent = TraceUsageClientIdentity & TraceUsageResponseContext & {
  fieldPaths: string[];
  observedAt: Date;
  truncated: boolean;
  skipped: boolean;
  reason?: string;
};

export type FieldAccessRecorder = (event: TraceUsageFieldAccessEvent) => void;

export type ObservedFieldsRecorder = (event: TraceUsageObservedFieldsEvent) => void;

export type TraceUsageBufferedField = {
  fieldPath: string;
  accessCount: number;
};

export type TraceUsageBufferedResponse = TraceUsageClientIdentity & TraceUsageResponseContext & {
  observedFields: string[];
  fields: TraceUsageBufferedField[];
};

export type TraceUsageFieldBatchPayload = TraceUsageClientIdentity & {
  batchId: string;
  observedAt: string;
  responses: Array<TraceUsageResponseContext & {
    observedFields: string[];
    fields: TraceUsageBufferedField[];
  }>;
};
