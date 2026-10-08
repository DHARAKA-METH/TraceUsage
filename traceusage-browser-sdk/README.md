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
import { trackResponse } from "@traceusage/browser";

const product = trackResponse(responseData, {
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
- Logs field access locally for now; batching/server ingestion comes later.
