# TraceUsage Demo Web

Next.js frontend demo for TraceUsage V2 field-usage tracking experiments.

This app fetches a product response from the Spring Boot demo API and intentionally renders only `name` and `price`.

## Setup

```bash
cd traceusage-demo-web
cp .env.example .env.local
npm install
```

## Run

Start the Spring Boot demo API first on `http://localhost:8081`, then run:

```bash
npm run dev
```

Open:

```text
http://localhost:3001
```

Expected display:

```text
Laptop
Rs. 250000
```

The API response also includes fields such as `description` and `legacyCode`, but this UI does not display them.
