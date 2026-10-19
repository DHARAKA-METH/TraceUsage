# TraceUsage

### REST API Endpoint & Response-Field Usage Monitoring

**TraceUsage** is a developer-focused monitoring and analytics platform that helps teams understand how their REST APIs are used. It tracks endpoint traffic and response-field access in instrumented applications, then presents the results in a dashboard to help identify **potentially unused endpoints and fields**.


## The Problem

As applications grow, APIs accumulate endpoints and response fields. Developers often struggle to answer:

- Which endpoints are being called, and how frequently?
- Which endpoints have not been observed recently?
- Which response fields are actually accessed by frontend code?
- Which parts of an API may be candidates for further review or cleanup?

Removing code without reliable usage evidence risks breaking consumers. Keeping everything indefinitely increases maintenance complexity.

## The Solution

TraceUsage collects usage metadata through lightweight backend and browser SDKs, processes telemetry events, and exposes endpoint- and field-level analytics through a Next.js dashboard.

It gives developers **observed usage data** to support API maintenance decisions without inspecting request or response values.

## Key Features

- **API Endpoint Monitoring** — Track request counts, normalized routes, HTTP status codes, and last-seen activity.
- **API Field Monitoring** — Discover response fields and track property access in instrumented JavaScript responses.
- **API Usage Analytics** — Explore endpoint and field usage to identify potentially unused APIs and response fields.

## What We Built

A full-stack API monitoring application built with **Spring Boot, Next.js, TypeScript, Apache Kafka, and PostgreSQL**, plus SDKs that integrate with existing applications.

| Component | Responsibility |
| --- | --- |
| Spring Boot backend | Telemetry collection, validation, analytics APIs, and processing |
| Spring Boot starter | Captures endpoint metadata from instrumented Spring MVC APIs |
| TypeScript browser SDK | Discovers JSON response fields and observes JavaScript property access |
| Apache Kafka | Buffers and distributes telemetry events for asynchronous processing |
| PostgreSQL / Neon | Stores application metadata and usage analytics |
| Next.js dashboard | Displays endpoint and field usage, filters, and supporting analysis |
| Docker Compose | Runs the platform and local supporting services |


## Architecture

<img width="2362" height="892" alt="TraceUsage_Architecture drawio" src="https://github.com/user-attachments/assets/85e9ba29-2fc6-480b-8f0c-090402a9ab9c" />


## Repository modules

| Module | Purpose |
| --- | --- |
| `TraceUsage` | Collector, Kafka consumers, analytics, lifecycle and OpenAPI backend |
| `traceusage-dashboard` | Next.js monitoring dashboard |
| `traceusage-spring-boot-starter` | Spring Boot endpoint telemetry SDK |
| `traceusage-browser-sdk` | Browser response-field telemetry SDK |
| `traceusage-demo-api` | Optional instrumented API for local testing |
| `traceusage-demo-web` | Optional instrumented web application for local testing |

## Prerequisites

- Git
- Docker Engine with Docker Compose
- A Neon PostgreSQL database
- Java 17 and Maven for building the local Spring SDK
- Node.js and npm for building the local browser SDK

## 1. Clone the repository

```bash
git clone https://github.com/DHARAKA-METH/TraceUsage.git
cd TraceUsage
```

## 2. Configure the platform

Create the root environment file:

```bash
cp .env.example .env
```

Edit `.env`:

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR-NEON-HOST/neondb?sslmode=require
SPRING_DATASOURCE_USERNAME=YOUR_NEON_USERNAME
SPRING_DATASOURCE_PASSWORD=YOUR_NEON_PASSWORD

# Use a long, random secret. Do not commit it.
JWT_SECRET=replace-with-a-long-random-secret

# This URL is used by the user's browser, not by another container.
NEXT_PUBLIC_TRACEUSAGE_API_URL=http://localhost:8080
```

Generate a JWT secret, for example:

```bash
openssl rand -base64 48
```

TraceUsage does not require an OpenRouter API key. Never place third-party API keys in this repository, frontend variables, screenshots, or documentation.

## 3. Start TraceUsage

Validate Compose configuration:

```bash
docker compose config --quiet
```

Build and start Kafka, the backend, and dashboard:

```bash
docker compose up -d --build
```

Check containers and logs:

```bash
docker compose ps
docker compose logs -f backend dashboard
```

Open:

| Service | URL |
| --- | --- |
| Dashboard | <http://localhost:3000> |
| Backend | <http://localhost:8080> |

Flyway runs automatically when the backend starts. It creates missing tables and applies unapplied migrations to the configured Neon database. Hibernate validates the resulting schema; it does not create the schema itself.

## 4. Create an application

1. Open <http://localhost:3000>.
2. Register or sign in.
3. Select **Create application**.
4. Enter an application name and environment.
5. Save the generated credentials immediately:
   - `tru_sk_...` — server secret for the Spring Boot SDK.
   - `tru_pk_...` — public browser ingestion key.
   - `projectId` — application identity used by the browser SDK.

The raw keys are shown once. Never expose `tru_sk_...` in browser code or any `NEXT_PUBLIC_*` variable.

---

# Spring Boot integration

The starter monitors normalized Spring MVC routes under `/api/**` and sends endpoint method, normalized route, status code and timestamp. It does not send response bodies.

## 1. Install the starter locally

From the cloned TraceUsage repository:

```bash
cd traceusage-spring-boot-starter
mvn clean install
```

The current local artifact version is `1.0.0-SNAPSHOT`.

## 2. Add the dependency

In the target Spring Boot application's `pom.xml`:

```xml
<dependency>
    <groupId>com.traceusage</groupId>
    <artifactId>traceusage-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 3. Configure the application

In `application.properties`:

```properties
server.port=8081

traceusage.enabled=true
traceusage.server-url=${TRACEUSAGE_SERVER_URL:http://localhost:8080}
traceusage.api-key=${TRACEUSAGE_API_KEY}
```

Set the generated server secret outside source control:

```bash
export TRACEUSAGE_API_KEY=tru_sk_replace_with_your_server_key
export TRACEUSAGE_SERVER_URL=http://localhost:8080
```

Start the target application:

```bash
mvn spring-boot:run
```

Send requests to routes under `/api/**`, then open the application's **Endpoints** tab in TraceUsage. A real repeated request receives a new event ID and increases the count. A retry or Kafka replay with the same event ID is deduplicated.

---

# React / Next.js browser integration

The browser SDK discovers response field paths and uses JavaScript Proxy access tracking. It sends field paths and access counts—never response values.

## 1. Build the browser SDK

From the cloned repository:

```bash
cd traceusage-browser-sdk
npm install
npm run build
```

## 2. Install it in the target frontend

From the React or Next.js project:

```bash
npm install /absolute/path/to/TraceUsage/traceusage-browser-sdk
```

The import name is:

```text
@traceusage/browser
```

## 3. Configure Next.js variables

Create `.env.local` in the target frontend:

```dotenv
NEXT_PUBLIC_TRACEUSAGE_SERVER_URL=http://localhost:8080
NEXT_PUBLIC_TRACEUSAGE_PUBLIC_KEY=tru_pk_replace_with_your_public_key
NEXT_PUBLIC_TRACEUSAGE_PROJECT_ID=proj_replace_with_your_project_id
NEXT_PUBLIC_TRACEUSAGE_CLIENT_ID=customer-web
NEXT_PUBLIC_TRACEUSAGE_CLIENT_VERSION=1.0.0
```

# OpenAPI and lifecycle monitoring

Import an OpenAPI JSON or YAML document from:

```text
Application workspace → OpenAPI → Import specification
```

TraceUsage joins contract fields with runtime browser evidence:

- **Access observed** — an instrumented browser read the field.
- **No access observed** — no matching browser read was received in the selected period.
- **Manual review candidate** — lifecycle evidence warrants human review.

These labels are not deletion recommendations. Monitoring coverage, external consumers, scheduled jobs and non-browser clients must be considered.

The environment selector is stored in the dashboard URL:

```text
?environmentId=12
```

Development, staging and production telemetry remain isolated.

---

# Optional demo applications

The demos are intentionally excluded from the production Compose stack.

## Demo API

```bash
cd traceusage-demo-api
TRACEUSAGE_API_KEY=tru_sk_replace_me \
TRACEUSAGE_SERVER_URL=http://localhost:8080 \
mvn spring-boot:run
```

The demo API runs at <http://localhost:8081>.

## Demo web

```bash
cd traceusage-demo-web
cp .env.example .env.local
```

Edit `.env.local` with the application's `tru_pk_...` key and project ID, then run:

```bash
npm install
npm run dev
```

Open <http://localhost:3001>. Exercise Catalog, Detail, Reviews, Orders and Tests, wait at least five seconds, then refresh TraceUsage.

An importable demo contract is available at:

```text
traceusage-demo-api/openapi/demo-api.yaml
```

---

# Operations

## Rebuild after source changes

```bash
docker compose up -d --build backend dashboard
```

## Reload changed root `.env` values

```bash
docker compose up -d --build --force-recreate backend dashboard
```

`NEXT_PUBLIC_*` dashboard values are compiled at image-build time. Force a clean dashboard rebuild when they change:

```bash
docker compose build --no-cache dashboard
docker compose up -d --force-recreate dashboard
```

## Stop the platform

```bash
docker compose down
```

Do not use `docker compose down -v` unless deleting Docker-managed Kafka data is intentional. Neon data is external and is not removed by Compose.

## Useful checks

```bash
docker compose config --quiet
docker compose ps
docker compose logs --tail=200 backend
docker compose logs --tail=200 dashboard
```

---

# Telemetry behavior

## Endpoint events

Hourly endpoint rows are grouped by:

```text
environment + endpoint + UTC hour + status code
```

A new request with a new event ID increases the count. Re-delivery of the same event ID does not.

## Browser field batches

Field usage is grouped by:

```text
environment + endpoint + field + client ID + client version + UTC hour
```

A new browser batch increases observation/access counts. Re-delivery of the same stable batch ID does not.

## Retention

Default settings retain recent raw events and longer-lived hourly analytics:

```properties
traceusage.telemetry.retention.enabled=true
traceusage.telemetry.retention.storage-mode=HYBRID
traceusage.telemetry.retention.raw-days=14
traceusage.telemetry.retention.aggregate-days=730
traceusage.telemetry.retention.deduplication-days=30
```

---

# Development verification

```bash
# Backend tests
cd TraceUsage
bash mvnw test

# Dashboard production build
cd ../traceusage-dashboard
npm install
npm run build

# Spring starter
cd ../traceusage-spring-boot-starter
mvn test

# Browser SDK
cd ../traceusage-browser-sdk
npm install
npm run build
```

PostgreSQL aggregation tests run production Flyway migrations against disposable PostgreSQL. Environment-ingestion tests cover direct mode and embedded Kafka mode, environment isolation, replay deduplication, field access aggregation, and exact HTTP status buckets.

## Troubleshooting

### Dashboard still shows old UI

```bash
docker compose build --no-cache dashboard
docker compose up -d --force-recreate dashboard
```

Then hard refresh with `Ctrl + Shift + R`.

### Field status remains “No access observed”

1. Confirm the frontend uses `tru_pk_...`, never `tru_sk_...`.
2. Confirm project ID and public key belong to the same application/environment.
3. Render/read fields from the object returned by `trackResponse()`.
4. Wait for the batch flush interval.
5. Select the correct dashboard environment and time range.
6. Inspect browser Network tools for `POST /api/v1/field-events/batch`.

### Docker socket permission denied

Add your user to the Docker group, sign out and sign back in:

```bash
sudo usermod -aG docker "$USER"
```

## Screenshots

<img width="1384" height="817" alt="1" src="https://github.com/user-attachments/assets/0c7c6c93-e625-46e0-8c7b-3debba73bdaf" />

<img width="1357" height="804" alt="2" src="https://github.com/user-attachments/assets/1147dd37-994e-4f85-b85c-826482e59d1c" />

<img width="914" height="799" alt="3" src="https://github.com/user-attachments/assets/4237b531-4a08-42bf-9464-7954d26d9fce" />

<img width="1069" height="798" alt="4" src="https://github.com/user-attachments/assets/c97447be-e9d4-4343-948c-45d4c0044d57" />

<img width="1094" height="803" alt="5" src="https://github.com/user-attachments/assets/75b3c967-b2f1-4a8e-803b-f77dd69bea4e" />









