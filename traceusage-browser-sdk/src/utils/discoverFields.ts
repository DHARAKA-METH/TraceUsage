import { buildFieldPath } from "./fieldPath.js";

export type FieldDiscoveryOptions = {
  maxDepth?: number;
  maxFields?: number;
  maxPayloadBytes?: number;
};

export type FieldDiscoveryResult = {
  fieldPaths: string[];
  truncated: boolean;
  skipped: boolean;
  reason?: string;
};

const DEFAULT_MAX_DEPTH = 8;
const DEFAULT_MAX_FIELDS = 500;
const DEFAULT_MAX_PAYLOAD_BYTES = 200_000;

export function discoverFields(data: unknown, options: FieldDiscoveryOptions = {}): FieldDiscoveryResult {
  const maxDepth = options.maxDepth ?? DEFAULT_MAX_DEPTH;
  const maxFields = options.maxFields ?? DEFAULT_MAX_FIELDS;
  const maxPayloadBytes = options.maxPayloadBytes ?? DEFAULT_MAX_PAYLOAD_BYTES;

  const payloadSize = estimatePayloadBytes(data);
  if (payloadSize > maxPayloadBytes) {
    return {
      fieldPaths: [],
      truncated: false,
      skipped: true,
      reason: `Payload size ${payloadSize} bytes exceeds maxPayloadBytes ${maxPayloadBytes}`,
    };
  }

  const fieldPaths = new Set<string>();
  const visited = new WeakSet<object>();
  let truncated = false;

  function visit(value: unknown, basePath: string, depth: number): void {
    if (fieldPaths.size >= maxFields) {
      truncated = true;
      return;
    }

    if (!isDiscoverableObject(value)) {
      if (basePath) {
        fieldPaths.add(basePath);
      }
      return;
    }

    if (visited.has(value)) {
      return;
    }
    visited.add(value);

    if (depth >= maxDepth) {
      if (basePath) {
        fieldPaths.add(basePath);
      }
      truncated = true;
      return;
    }

    if (Array.isArray(value)) {
      if (value.length === 0 && basePath) {
        fieldPaths.add(`${basePath}[]`);
        return;
      }

      for (const item of value) {
        if (fieldPaths.size >= maxFields) {
          truncated = true;
          return;
        }
        visit(item, basePath ? `${basePath}[]` : "[]", depth + 1);
      }
      return;
    }

    const entries = Object.entries(value);
    if (entries.length === 0 && basePath) {
      fieldPaths.add(basePath);
      return;
    }

    for (const [property, childValue] of entries) {
      if (fieldPaths.size >= maxFields) {
        truncated = true;
        return;
      }

      const fieldPath = buildFieldPath(basePath, property, false);
      if (!fieldPath) {
        continue;
      }

      visit(childValue, fieldPath, depth + 1);
    }
  }

  visit(data, "", 0);

  return {
    fieldPaths: [...fieldPaths].sort(),
    truncated,
    skipped: false,
  };
}

function estimatePayloadBytes(data: unknown): number {
  try {
    return new TextEncoder().encode(JSON.stringify(data)).byteLength;
  } catch {
    return Number.MAX_SAFE_INTEGER;
  }
}

function isDiscoverableObject(value: unknown): value is Record<string, unknown> | unknown[] {
  if (value === null || typeof value !== "object") {
    return false;
  }

  if (Array.isArray(value)) {
    return true;
  }

  const prototype = Object.getPrototypeOf(value);
  return prototype === Object.prototype || prototype === null;
}
