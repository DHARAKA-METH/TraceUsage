import type {
  TraceUsageBufferedResponse,
  TraceUsageClientIdentity,
  TraceUsageFieldBatchPayload,
} from "../types/TelemetryEvent.js";

export type BatchSenderOptions = TraceUsageClientIdentity & {
  serverUrl: string;
  publicKey: string;
  endpointPath?: string;
  flushInterval?: number;
  maxBatchSize?: number;
  timeoutMs?: number;
  maxRetries?: number;
  retryBaseDelayMs?: number;
  debug?: boolean;
};

export type BatchSendResult = {
  ok: boolean;
  status?: number;
  retryable: boolean;
};

const DEFAULT_ENDPOINT_PATH = "/api/v1/field-events/batch";
const DEFAULT_TIMEOUT_MS = 5_000;
const DEFAULT_MAX_RETRIES = 2;
const DEFAULT_RETRY_BASE_DELAY_MS = 500;

export class BatchSender {
  readonly flushInterval: number;
  readonly maxBatchSize: number;

  private readonly options: Required<Pick<BatchSenderOptions,
    "endpointPath" | "flushInterval" | "maxBatchSize" | "timeoutMs" | "maxRetries" | "retryBaseDelayMs"
  >> & BatchSenderOptions;

  constructor(options: BatchSenderOptions) {
    this.options = {
      ...options,
      endpointPath: options.endpointPath ?? DEFAULT_ENDPOINT_PATH,
      flushInterval: options.flushInterval ?? 5_000,
      maxBatchSize: options.maxBatchSize ?? 100,
      timeoutMs: options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
      maxRetries: options.maxRetries ?? DEFAULT_MAX_RETRIES,
      retryBaseDelayMs: options.retryBaseDelayMs ?? DEFAULT_RETRY_BASE_DELAY_MS,
    };
    this.flushInterval = this.options.flushInterval;
    this.maxBatchSize = this.options.maxBatchSize;
  }

  async sendWithRetry(responses: TraceUsageBufferedResponse[], batchId = createBatchId()): Promise<BatchSendResult> {
    if (responses.length === 0) {
      return { ok: true, retryable: false };
    }

    let lastResult: BatchSendResult = { ok: false, retryable: true };
    for (let attempt = 0; attempt <= this.options.maxRetries; attempt++) {
      lastResult = await this.sendOnce(responses, batchId, false);
      if (lastResult.ok || !lastResult.retryable) {
        return lastResult;
      }

      if (attempt < this.options.maxRetries) {
        await delay(this.options.retryBaseDelayMs * 2 ** attempt);
      }
    }

    return lastResult;
  }

  sendOnExit(responses: TraceUsageBufferedResponse[], batchId = createBatchId()): boolean {
    if (responses.length === 0 || typeof fetch === "undefined") {
      return false;
    }

    try {
      void fetch(this.url(), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-TraceUsage-Public-Key": this.options.publicKey,
        },
        body: JSON.stringify(this.createPayload(responses, batchId)),
        keepalive: true,
      });
      return true;
    } catch {
      return false;
    }
  }

  private async sendOnce(responses: TraceUsageBufferedResponse[], batchId: string, keepalive: boolean): Promise<BatchSendResult> {
    if (typeof navigator !== "undefined" && navigator.onLine === false) {
      return { ok: false, retryable: true };
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.options.timeoutMs);

    try {
      const response = await fetch(this.url(), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-TraceUsage-Public-Key": this.options.publicKey,
        },
        body: JSON.stringify(this.createPayload(responses, batchId)),
        keepalive,
        signal: controller.signal,
      });

      if (response.ok) {
        return { ok: true, status: response.status, retryable: false };
      }

      return {
        ok: false,
        status: response.status,
        retryable: response.status === 429 || response.status === 503 || response.status >= 500,
      };
    } catch {
      return { ok: false, retryable: true };
    } finally {
      clearTimeout(timeout);
    }
  }

  private createPayload(responses: TraceUsageBufferedResponse[], batchId: string): TraceUsageFieldBatchPayload {
    return {
      batchId,
      projectId: this.options.projectId,
      clientId: this.options.clientId,
      clientVersion: this.options.clientVersion,
      observedAt: new Date().toISOString(),
      responses: responses.map((response) => ({
        method: response.method,
        endpoint: response.endpoint,
        schema: response.schema,
        observedFields: response.observedFields,
        fields: response.fields,
      })),
    };
  }

  private url(): string {
    return `${this.options.serverUrl.replace(/\/+$/, "")}${this.options.endpointPath}`;
  }
}

function createBatchId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `batch_${Date.now()}_${Math.random().toString(36).slice(2)}`;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
