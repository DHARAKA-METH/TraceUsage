# TraceUsage Dashboard

Next.js dashboard for managing TraceUsage applications and viewing endpoint analytics.

## Setup

```bash
cd traceusage-dashboard
cp .env.example .env.local
npm install
```

`NEXT_PUBLIC_TRACEUSAGE_API_URL` should point to the TraceUsage backend server:

```env
NEXT_PUBLIC_TRACEUSAGE_API_URL=http://localhost:8080
```

## Run

Start the Spring Boot TraceUsage server first, then run:

```bash
npm run dev
```

Open:

```text
http://localhost:3000
```

## Features

- Register developer account
- Login and store JWT locally for V1
- List applications
- Create application and display the generated API key once
- View endpoint analytics by project ID

## Backend APIs used

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/applications`
- `POST /api/applications`
- `GET /api/v1/applications/{projectId}/analytics/endpoints`
