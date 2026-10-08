import { defaultTraceUsageClient, type TraceUsageClient } from "./TraceUsageClient.js";
import type { TraceUsageResponseContext } from "../types/TelemetryEvent.js";
import { buildFieldPath, isIgnoredProperty } from "../utils/fieldPath.js";

type JsonLikeObject = Record<string, unknown> | unknown[];

const proxyCache = new WeakMap<object, Map<string, unknown>>();

export type TrackResponseOptions = {
  client?: TraceUsageClient;
};

export function trackResponse<T>(
  data: T,
  context: TraceUsageResponseContext,
  options: TrackResponseOptions = {},
): T {
  const client = options.client ?? defaultTraceUsageClient;

  if (!isTrackableObject(data)) {
    return data;
  }

  return createTrackedProxy(data, context, "", client) as T;
}

function createTrackedProxy<T extends object>(
  target: T,
  context: TraceUsageResponseContext,
  basePath: string,
  client: TraceUsageClient,
): T {
  const cachedProxy = getCachedProxy(target, basePath);
  if (cachedProxy) {
    return cachedProxy as T;
  }

  const proxy = new Proxy(target, {
    get(currentTarget, property, receiver) {
      const value = Reflect.get(currentTarget, property, receiver);

      if (isIgnoredProperty(currentTarget, property)) {
        return value;
      }

      const fieldPath = buildFieldPath(basePath, property, Array.isArray(currentTarget));
      if (!fieldPath) {
        return value;
      }

      if (isTrackableObject(value)) {
        return createTrackedProxy(value, context, fieldPath, client);
      }

      client.recordFieldAccess({
        ...context,
        fieldPath,
        accessedAt: new Date(),
      });

      return value;
    },
  });

  cacheProxy(target, basePath, proxy);
  return proxy;
}

function getCachedProxy(target: object, basePath: string): unknown | null {
  return proxyCache.get(target)?.get(basePath) ?? null;
}

function cacheProxy(target: object, basePath: string, proxy: unknown): void {
  let proxiesByPath = proxyCache.get(target);
  if (!proxiesByPath) {
    proxiesByPath = new Map<string, unknown>();
    proxyCache.set(target, proxiesByPath);
  }

  proxiesByPath.set(basePath, proxy);
}

function isTrackableObject(value: unknown): value is JsonLikeObject {
  if (value === null || typeof value !== "object") {
    return false;
  }

  if (Array.isArray(value)) {
    return true;
  }

  const prototype = Object.getPrototypeOf(value);
  return prototype === Object.prototype || prototype === null;
}
