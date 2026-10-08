import { trackResponse as trackResponseWithClient, type TrackResponseOptions } from "./ResponseTracker.js";
import { FieldUsageBuffer } from "./FieldUsageBuffer.js";
import { BatchSender, type BatchSenderOptions } from "../transport/BatchSender.js";
import type {
  FieldAccessRecorder,
  ObservedFieldsRecorder,
  TraceUsageClientIdentity,
  TraceUsageFieldAccessEvent,
  TraceUsageObservedFieldsEvent,
  TraceUsageResponseContext,
} from "../types/TelemetryEvent.js";
import type { FieldDiscoveryOptions } from "../utils/discoverFields.js";

export type TraceUsageTransportOptions = Partial<Pick<BatchSenderOptions,
  "endpointPath" | "flushInterval" | "maxBatchSize" | "timeoutMs" | "maxRetries" | "retryBaseDelayMs"
>> & {
  serverUrl?: string;
  publicKey?: string;
  enabled?: boolean;
  maxBufferEntries?: number;
};

export type TraceUsageClientOptions = TraceUsageClientIdentity & {
  debug?: boolean;
  recorder?: FieldAccessRecorder;
  observedFieldsRecorder?: ObservedFieldsRecorder;
  discovery?: FieldDiscoveryOptions;
  transport?: TraceUsageTransportOptions;
};

export type TraceUsageResponseSnapshot = TraceUsageClientIdentity & TraceUsageResponseContext & {
  observedFields: string[];
  accessedFields: Array<{ fieldPath: string; accessCount: number }>;
  noAccessObservedFields: string[];
};

export class TraceUsageClient {
  private readonly debug: boolean;
  private readonly recorder?: FieldAccessRecorder;
  private readonly observedFieldsRecorder?: ObservedFieldsRecorder;
  private readonly identity: TraceUsageClientIdentity;
  private readonly discoveryOptions: FieldDiscoveryOptions;
  private readonly observedFieldsByResponse = new Map<string, Set<string>>();
  private readonly accessedFieldsByResponse = new Map<string, Map<string, number>>();
  private readonly contextByResponse = new Map<string, TraceUsageResponseContext>();
  private readonly buffer: FieldUsageBuffer;
  private readonly sender?: BatchSender;
  private flushTimer: ReturnType<typeof setInterval> | null = null;

  constructor(options: TraceUsageClientOptions = {}) {
    this.debug = options.debug ?? true;
    this.recorder = options.recorder;
    this.observedFieldsRecorder = options.observedFieldsRecorder;
    this.identity = {
      projectId: options.projectId,
      clientId: options.clientId,
      clientVersion: options.clientVersion,
    };
    this.discoveryOptions = options.discovery ?? {};
    this.buffer = new FieldUsageBuffer({
      maxBufferEntries: options.transport?.maxBufferEntries,
      onDrop: (reason) => {
        if (this.debug) {
          console.warn(reason);
        }
      },
    });

    const transportEnabled = options.transport?.enabled ?? true;
    if (transportEnabled && options.transport?.serverUrl && options.transport.publicKey) {
      this.sender = new BatchSender({
        projectId: this.identity.projectId,
        clientId: this.identity.clientId,
        clientVersion: this.identity.clientVersion,
        serverUrl: options.transport.serverUrl,
        publicKey: options.transport.publicKey,
        endpointPath: options.transport.endpointPath,
        flushInterval: options.transport.flushInterval,
        maxBatchSize: options.transport.maxBatchSize,
        timeoutMs: options.transport.timeoutMs,
        maxRetries: options.transport.maxRetries,
        retryBaseDelayMs: options.transport.retryBaseDelayMs,
        debug: this.debug,
      });
      this.start();
      this.registerFinalFlushHandlers();
    }
  }

  trackResponse<T>(data: T, context: TraceUsageResponseContext, options: Omit<TrackResponseOptions, "client"> = {}): T {
    return trackResponseWithClient(data, context, {
      ...options,
      client: this,
    });
  }

  getDiscoveryOptions(): FieldDiscoveryOptions {
    return this.discoveryOptions;
  }

  recordObservedFields(event: Omit<TraceUsageObservedFieldsEvent, keyof TraceUsageClientIdentity>): void {
    const enrichedEvent: TraceUsageObservedFieldsEvent = {
      ...this.identity,
      ...event,
    };

    const responseKey = this.createResponseKey(enrichedEvent);
    this.contextByResponse.set(responseKey, {
      method: enrichedEvent.method,
      endpoint: enrichedEvent.endpoint,
      schema: enrichedEvent.schema,
    });

    const observedFields = this.getOrCreateObservedFields(responseKey);
    enrichedEvent.fieldPaths.forEach((fieldPath) => observedFields.add(fieldPath));
    this.buffer.recordObservedFields(this.identity, enrichedEvent, enrichedEvent.fieldPaths);

    if (this.observedFieldsRecorder) {
      this.observedFieldsRecorder(enrichedEvent);
    }

    if (this.debug) {
      if (enrichedEvent.skipped) {
        console.warn(
          `[TraceUsage] Field discovery skipped for ${enrichedEvent.method} ${enrichedEvent.endpoint}: ${enrichedEvent.reason}`,
        );
        return;
      }

      console.log(
        `[TraceUsage] Observed fields for ${enrichedEvent.method} ${enrichedEvent.endpoint}\n${enrichedEvent.fieldPaths.join("\n")}`,
      );
    }
  }

  recordFieldAccess(event: TraceUsageFieldAccessEvent): void {
    const enrichedEvent: TraceUsageFieldAccessEvent = {
      ...this.identity,
      ...event,
    };

    const responseKey = this.createResponseKey(enrichedEvent);
    this.contextByResponse.set(responseKey, {
      method: enrichedEvent.method,
      endpoint: enrichedEvent.endpoint,
      schema: enrichedEvent.schema,
    });

    const accessedFields = this.getOrCreateAccessedFields(responseKey);
    accessedFields.set(enrichedEvent.fieldPath, (accessedFields.get(enrichedEvent.fieldPath) ?? 0) + 1);
    this.buffer.recordFieldAccess(this.identity, enrichedEvent, enrichedEvent.fieldPath);

    if (this.recorder) {
      this.recorder(enrichedEvent);
      return;
    }

    if (this.debug) {
      console.log(`[TraceUsage] ${enrichedEvent.method} ${enrichedEvent.endpoint}\nField accessed: ${enrichedEvent.fieldPath}`);
    }
  }

  getFieldUsageSnapshot(): TraceUsageResponseSnapshot[] {
    const responseKeys = new Set([
      ...this.observedFieldsByResponse.keys(),
      ...this.accessedFieldsByResponse.keys(),
    ]);

    return [...responseKeys].map((responseKey) => {
      const context = this.contextByResponse.get(responseKey) ?? parseResponseKey(responseKey);
      const observedFields = [...(this.observedFieldsByResponse.get(responseKey) ?? new Set<string>())].sort();
      const accessedFieldEntries = this.accessedFieldsByResponse.get(responseKey) ?? new Map<string, number>();
      const accessedFields = [...accessedFieldEntries.entries()]
        .map(([fieldPath, accessCount]) => ({ fieldPath, accessCount }))
        .sort((left, right) => left.fieldPath.localeCompare(right.fieldPath));
      const accessedFieldNames = new Set(accessedFields.map((field) => field.fieldPath));

      return {
        ...this.identity,
        ...context,
        observedFields,
        accessedFields,
        noAccessObservedFields: observedFields.filter((fieldPath) => !accessedFieldNames.has(fieldPath)),
      };
    });
  }

  start(): void {
    if (!this.sender || this.flushTimer) {
      return;
    }

    this.flushTimer = setInterval(() => {
      void this.flush();
    }, this.sender.flushInterval);
  }

  stop(): void {
    if (!this.flushTimer) {
      return;
    }

    clearInterval(this.flushTimer);
    this.flushTimer = null;
  }

  async flush(): Promise<void> {
    if (!this.sender || this.buffer.size() === 0) {
      return;
    }

    const batch = this.buffer.drain(this.sender.maxBatchSize);
    const result = await this.sender.sendWithRetry(batch);
    if (!result.ok) {
      this.buffer.requeue(batch);
      if (this.debug) {
        console.warn(
          `[TraceUsage] Failed to send field telemetry batch${result.status ? ` status=${result.status}` : ""}; requeued locally`,
        );
      }
    }
  }

  flushOnExit(): void {
    if (!this.sender || this.buffer.size() === 0) {
      return;
    }

    const batch = this.buffer.drain(this.sender.maxBatchSize);
    const accepted = this.sender.sendOnExit(batch);
    if (!accepted) {
      this.buffer.requeue(batch);
    }
  }

  private getOrCreateObservedFields(responseKey: string): Set<string> {
    let observedFields = this.observedFieldsByResponse.get(responseKey);
    if (!observedFields) {
      observedFields = new Set<string>();
      this.observedFieldsByResponse.set(responseKey, observedFields);
    }
    return observedFields;
  }

  private getOrCreateAccessedFields(responseKey: string): Map<string, number> {
    let accessedFields = this.accessedFieldsByResponse.get(responseKey);
    if (!accessedFields) {
      accessedFields = new Map<string, number>();
      this.accessedFieldsByResponse.set(responseKey, accessedFields);
    }
    return accessedFields;
  }

  private createResponseKey(context: TraceUsageResponseContext): string {
    return [
      this.identity.projectId ?? "",
      this.identity.clientId ?? "",
      this.identity.clientVersion ?? "",
      context.method,
      context.endpoint,
      context.schema ?? "",
    ].join("|");
  }

  private registerFinalFlushHandlers(): void {
    if (typeof window === "undefined") {
      return;
    }

    window.addEventListener("pagehide", () => this.flushOnExit());
    document.addEventListener("visibilitychange", () => {
      if (document.visibilityState === "hidden") {
        this.flushOnExit();
      }
    });
  }
}

export const defaultTraceUsageClient = new TraceUsageClient();

function parseResponseKey(responseKey: string): TraceUsageResponseContext {
  const [, , , method, endpoint, schema] = responseKey.split("|");
  return {
    method,
    endpoint,
    schema: schema || undefined,
  };
}
