import type {
  TraceUsageBufferedResponse,
  TraceUsageClientIdentity,
  TraceUsageResponseContext,
} from "../types/TelemetryEvent.js";

type BufferEntry = TraceUsageClientIdentity & TraceUsageResponseContext & {
  observedFields: Set<string>;
  accessedFields: Map<string, number>;
};

export type FieldUsageBufferOptions = {
  maxBufferEntries?: number;
  onDrop?: (reason: string) => void;
};

const DEFAULT_MAX_BUFFER_ENTRIES = 1_000;

export class FieldUsageBuffer {
  private readonly maxBufferEntries: number;
  private readonly onDrop?: (reason: string) => void;
  private readonly entries = new Map<string, BufferEntry>();

  constructor(options: FieldUsageBufferOptions = {}) {
    this.maxBufferEntries = options.maxBufferEntries ?? DEFAULT_MAX_BUFFER_ENTRIES;
    this.onDrop = options.onDrop;
  }

  recordObservedFields(identity: TraceUsageClientIdentity, context: TraceUsageResponseContext, fieldPaths: string[]): void {
    const entry = this.getOrCreateEntry(identity, context);
    if (!entry) {
      return;
    }

    fieldPaths.forEach((fieldPath) => entry.observedFields.add(fieldPath));
  }

  recordFieldAccess(identity: TraceUsageClientIdentity, context: TraceUsageResponseContext, fieldPath: string, count = 1): void {
    const entry = this.getOrCreateEntry(identity, context);
    if (!entry) {
      return;
    }

    entry.accessedFields.set(fieldPath, (entry.accessedFields.get(fieldPath) ?? 0) + count);
  }

  drain(maxResponses: number): TraceUsageBufferedResponse[] {
    const drained: TraceUsageBufferedResponse[] = [];

    for (const [key, entry] of this.entries) {
      if (drained.length >= maxResponses) {
        break;
      }

      drained.push(toBufferedResponse(entry));
      this.entries.delete(key);
    }

    return drained;
  }

  requeue(responses: TraceUsageBufferedResponse[]): void {
    for (const response of responses) {
      this.recordObservedFields(response, response, response.observedFields);
      response.fields.forEach((field) => {
        this.recordFieldAccess(response, response, field.fieldPath, field.accessCount);
      });
    }
  }

  snapshot(): TraceUsageBufferedResponse[] {
    return [...this.entries.values()].map(toBufferedResponse);
  }

  size(): number {
    return this.entries.size;
  }

  private getOrCreateEntry(identity: TraceUsageClientIdentity, context: TraceUsageResponseContext): BufferEntry | null {
    const key = createBufferKey(identity, context);
    const existingEntry = this.entries.get(key);
    if (existingEntry) {
      return existingEntry;
    }

    if (this.entries.size >= this.maxBufferEntries) {
      this.onDrop?.(`TraceUsage field usage buffer is full; dropping telemetry for ${context.method} ${context.endpoint}`);
      return null;
    }

    const entry: BufferEntry = {
      ...identity,
      ...context,
      observedFields: new Set<string>(),
      accessedFields: new Map<string, number>(),
    };
    this.entries.set(key, entry);
    return entry;
  }
}

function createBufferKey(identity: TraceUsageClientIdentity, context: TraceUsageResponseContext): string {
  return [
    identity.projectId ?? "",
    identity.clientId ?? "",
    identity.clientVersion ?? "",
    context.method,
    context.endpoint,
    context.schema ?? "",
  ].join("|");
}

function toBufferedResponse(entry: BufferEntry): TraceUsageBufferedResponse {
  return {
    projectId: entry.projectId,
    clientId: entry.clientId,
    clientVersion: entry.clientVersion,
    method: entry.method,
    endpoint: entry.endpoint,
    schema: entry.schema,
    observedFields: [...entry.observedFields].sort(),
    fields: [...entry.accessedFields.entries()]
      .map(([fieldPath, accessCount]) => ({ fieldPath, accessCount }))
      .sort((left, right) => left.fieldPath.localeCompare(right.fieldPath)),
  };
}
