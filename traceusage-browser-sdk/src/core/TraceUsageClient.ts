import type { FieldAccessRecorder, TraceUsageFieldAccessEvent } from "../types/TelemetryEvent.js";

export type TraceUsageClientOptions = {
  debug?: boolean;
  recorder?: FieldAccessRecorder;
};

export class TraceUsageClient {
  private readonly debug: boolean;
  private readonly recorder?: FieldAccessRecorder;

  constructor(options: TraceUsageClientOptions = {}) {
    this.debug = options.debug ?? true;
    this.recorder = options.recorder;
  }

  recordFieldAccess(event: TraceUsageFieldAccessEvent): void {
    if (this.recorder) {
      this.recorder(event);
      return;
    }

    if (this.debug) {
      console.log(`[TraceUsage] ${event.method} ${event.endpoint}\nField accessed: ${event.fieldPath}`);
    }
  }
}

export const defaultTraceUsageClient = new TraceUsageClient();
