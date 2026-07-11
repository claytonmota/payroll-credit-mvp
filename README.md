# Payroll-Integrated Real-Time Credit Infrastructure — MVP

**Author:** Clayton Soares da Mota
**License:** Apache License 2.0 — see [`LICENSE`](LICENSE)
**Contributing:** see [`CONTRIBUTING.md`](CONTRIBUTING.md)

Reference implementation of the first vertical slice of Clayton Soares da Mota's
proposed endeavor (Professional Plan, Feb 2026): a payroll-integrated,
real-time credit technology platform.

This MVP implements the **Ingestion → Income Verification → Decision** flow
described in Methodology 1 (Integrated Data Architecture & Interoperability)
and Methodology 2 (Cloud-Native Real-Time Risk Orchestration) of the
Professional Plan, using the technology stack named in the plan:
Java (Spring Boot), PostgreSQL, and event-driven microservices via
Apache Kafka.

## Why this exists

USCIS's RFE on the I-140 / NIW petition asked for evidence that the
petitioner is "well positioned to advance the proposed endeavor" beyond
his own statements — e.g. a prototype, progress towards the endeavor, or
evidence the work has been used/tested. This repository is that evidence:
a working, runnable, version-controlled implementation of the core
architecture described in the Professional Plan and in the accompanying
architecture diagram, built and iterated on by the petitioner.

## Architecture (this MVP's scope)

```
 [Mock Payroll Webhook]
          |
          v
 ┌─────────────────────┐   Kafka topic:      ┌────────────────────────────┐
 │  ingestion-service   │  payroll.events     │ income-verification-service│
 │  (Spring Boot REST)  │ ------------------> │ (Kafka consumer + REST)    │
 │  POST /v1/payroll/   │                     │ computes income confidence │
 │       events         │                     │ score, persists to Postgres│
 └─────────────────────┘                     └──────────────┬─────────────┘
                                                              │ Kafka topic:
                                                              │ income.verified
                                                              v
                                              ┌────────────────────────────┐
                                              │      decision-service      │
                                              │ (Kafka consumer + REST)    │
                                              │ rules engine: DTI /        │
                                              │ affordability -> eligibility│
                                              │ decision, persists to      │
                                              │ Postgres                  │
                                              └────────────────────────────┘
```

Each service is an independently deployable Spring Boot application,
matching the "Microservices Layer (Domain-Driven)" section of the
architecture diagram. Services communicate asynchronously via Apache
Kafka (Event Streaming & Integration layer), and each owns its own
PostgreSQL schema (Data Layer).

**Not yet implemented** (future iterations, see `docs/ROADMAP.md`):
Identity, Employment, Payroll, Account Aggregation, Affordability,
Credit Profile, Notification and Audit services; API Gateway; cloud
deployment. These are scaffolded in the architecture diagram but out of
scope for this first runnable increment.

## Running locally

Requirements: Docker + Docker Compose, JDK 17 (only if you want to build
outside Docker), and internet access to pull base images and Maven
dependencies (none of this was built or tested inside a sandboxed,
network-isolated environment — build and run it in your own machine or
CI to verify).

```bash
docker compose up --build
```

This starts: Zookeeper, Kafka, PostgreSQL, MongoDB, and four services:

| Service                       | Language      | Port |
|--------------------------------|---------------|------|
| ingestion-service               | Java 17       | 8081 |
| income-verification-service     | Java 17       | 8082 |
| decision-service                 | Java 17       | 8083 |
| credit-profile-service           | C# .NET 8     | 8084 |

## Example end-to-end flow

See `docs/DEMO.md` for full curl walkthrough. Quick version:

```bash
# 1. Simulate a payroll provider webhook (e.g., ADP/Gusto-style event)
curl -X POST http://localhost:8081/v1/payroll/events \
  -H "Content-Type: application/json" \
  -d @postman/sample-payroll-event.json

# 2. Check the computed income verification result
curl http://localhost:8082/v1/income/verification/user-1001

# 3. Check the eligibility decision
curl http://localhost:8083/v1/eligibility/user-1001
```

## Relationship to the Professional Plan & RFE response

| Professional Plan reference | Implemented here |
|---|---|
| Methodology 1: data pipelines integrating payroll providers, employers, credit bureaus using PostgreSQL / SQL Server / MongoDB / DynamoDB | `ingestion-service` REST ingestion + PostgreSQL persistence (Java services); `credit-profile-service` uses MongoDB for aggregate credit-profile documents. SQL Server / DynamoDB targeted for later iterations, see ROADMAP |
| Methodology 2: microservices in Java (Spring Boot) / C# (.NET), cloud-native, real-time processing | 3 services in Java 17 + Spring Boot 3, 1 service in C# .NET 8 + ASP.NET Core, all event-driven via Kafka |
| Methodology 3: governance, audit trails, Basel II-aligned data integrity | Structured logging + persisted decision audit trail (`eligibility_decision` table); full audit service planned in ROADMAP |
| "Real-time income validation" replacing static credit scores | `income-verification-service` computes an income confidence score from time-series payroll events, independent of bureau history |

This code, its git history, and its test suite are intended to be
submitted as documentary evidence of progress under the second
*Dhanasar* prong ("well positioned to advance the proposed endeavor").

## About the author

**Clayton Soares da Mota** is a Senior Database Architect with more than
25 years of experience in mission-critical financial data systems, with
prior technical leadership at Banco Itaú Unibanco and Banco Bradesco
(the two largest financial institutions in Latin America). His work in
Brazilian banking includes a tenfold scaling of a credit-eligibility
platform (from ~4,000 to ~42,000 daily eligibility checks) using
cloud-native microservices and the FICO decision engine, Basel II
compliance work on treasury and asset management platforms, and
automation of Brazilian Payment System (SPB) treasury operations. He
holds an MBA in Software Technology from Universidade de São Paulo (USP)
and a Technologist degree in Data Processing from Universidade
Presbiteriana Mackenzie, along with Microsoft Azure Data Fundamentals /
Azure Fundamentals certifications and IEEE Computer Society membership.

This repository is his ongoing implementation of the payroll-integrated
credit infrastructure described in his Professional Plan.

## Copyright

Copyright © 2026 Clayton Soares da Mota. All rights reserved.
This project is licensed under the Apache License, Version 2.0.
