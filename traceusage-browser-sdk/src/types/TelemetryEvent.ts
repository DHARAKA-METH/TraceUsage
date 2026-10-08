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

export type TraceUsageFieldAccessEvent = TraceUsageResponseContext & {
  fieldPath: string;
  accessedAt: Date;
};

export type FieldAccessRecorder = (event: TraceUsageFieldAccessEvent) => void;
