# TraceUsage Browser SDK

Local TypeScript package for TraceUsage V2 response field access tracking.

Package name:

```text
@traceusage/browser
```

## Build

```bash
npm install
npm run build
```

## Basic usage

```ts
import { TraceUsageClient } from "@traceusage/browser";

const traceUsage = new TraceUsageClient({
  projectId: "proj_xxxxx",
  clientId: "product-web",
  clientVersion: "1.0.0",
  transport: {
    serverUrl: "http://localhost:8080",
    publicKey: "tru_pk_xxxxx",
    flushInterval: 5000,
    maxBatchSize: 100,
    maxBufferEntries: 1000,
  },
});

const product = traceUsage.trackResponse(responseData, {
  method: "GET",
  endpoint: "/api/products/{id}",
  schema: "Product",
});

product.name;
product.price;
```

Expected console output:

```text
[TraceUsage] GET /api/products/{id}
Field accessed: name

[TraceUsage] GET /api/products/{id}
Field accessed: price
```

`trackResponse()` also discovers fields present in the response immediately:

```text
[TraceUsage] Observed fields for GET /api/products/{id}
id
legacyCode
name
price
```

## Nested paths

```ts
product.supplier.name;
product.reviews[0].rating;
```

Records:

```text
supplier.name
reviews[].rating
```

## V2 Phase 2 scope

- Supports plain JSON objects and arrays.
- Uses JavaScript `Proxy` to observe property reads.
- Uses a `WeakMap` cache to avoid repeatedly wrapping the same object/path.
- Does not proxy special objects such as `Date`, `Map`, `Set`, or class instances.
- Discovers observed response fields separately from accessed fields.
- Tracks `projectId`, `clientId`, and `clientVersion` for frontend consumer identification.
- Aggregates field access counts locally instead of sending one request per property read.
- Sends field usage and response-field discovery data asynchronously in batches.
- Failed telemetry does not throw into the application UI path.

## Field usage snapshot

```ts
const snapshot = traceUsage.getFieldUsageSnapshot();
```

Example result:

```json
[
  {
    "projectId": "proj_xxxxx",
    "clientId": "product-web",
    "clientVersion": "1.0.0",
    "method": "GET",
    "endpoint": "/api/products/{id}",
    "schema": "Product",
    "observedFields": ["id", "name", "price", "legacyCode"],
    "accessedFields": [
      { "fieldPath": "name", "accessCount": 1 },
      { "fieldPath": "price", "accessCount": 1 }
    ],
    "noAccessObservedFields": ["id", "legacyCode"]
  }
]
```

## Discovery limits

Configure discovery safety limits:

```ts
const traceUsage = new TraceUsageClient({
  projectId: "proj_xxxxx",
  clientId: "product-web",
  clientVersion: "1.0.0",
  discovery: {
    maxDepth: 8,
    maxFields: 500,
    maxPayloadBytes: 200_000,
  },
});
```

## Asynchronous batching

Repeated reads are aggregated in memory:

```ts
product.name;
product.name;
product.price;

await traceUsage.flush();
```

Payload sent to TraceUsage:

```json
{
  "batchId": "...",
  "projectId": "proj_xxxxx",
  "clientId": "product-web",
  "clientVersion": "1.0.0",
  "responses": [
    {
      "method": "GET",
      "endpoint": "/api/products/{id}",
      "schema": "Product",
      "observedFields": ["id", "legacyCode", "name", "price"],
      "fields": [
        { "fieldPath": "name", "accessCount": 2 },
        { "fieldPath": "price", "accessCount": 1 }
      ]
    }
  ]
}
```

Transport behavior:

- Uses `fetch()`.
- Applies a timeout.
- Retries retryable failures such as `429`, `503`, and `5xx` with backoff.
- Reuses the same `batchId` during retries.
- Requeues failed batches locally when delivery fails.
- Attempts a final `keepalive` flush during `pagehide` / hidden visibility state.
- Drops new telemetry when the bounded buffer is full.
