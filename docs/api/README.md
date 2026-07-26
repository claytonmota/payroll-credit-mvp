# API Specifications

Formal OpenAPI 3.0.3 contracts for the four public services of the
Payroll-Integrated Real-Time Credit Infrastructure.

| Service | Specification | Public base URL |
|---|---|---|
| Ingestion | [`ingestion-service.openapi.yaml`](ingestion-service.openapi.yaml) | `https://ingestion.payroll-credit.com` |
| Income Verification | [`income-verification-service.openapi.yaml`](income-verification-service.openapi.yaml) | `https://income.payroll-credit.com` |
| Decision | [`decision-service.openapi.yaml`](decision-service.openapi.yaml) | `https://decision.payroll-credit.com` |
| Credit Profile | [`credit-profile-service.openapi.yaml`](credit-profile-service.openapi.yaml) | `https://creditprofile.payroll-credit.com` |

Each document is self-contained — all schemas are defined inline with no
external `$ref` — so any file can be opened on its own in any OpenAPI viewer
without resolving siblings.

## Viewing the specifications

**In a browser, with no installation.** Open [editor.swagger.io](https://editor.swagger.io),
then *File → Import file* and select any of the YAML documents. The rendered
documentation appears on the right, including every schema, enum, and example.

**In an IDE.** Both IntelliJ IDEA and VS Code render OpenAPI documents natively;
VS Code requires the *OpenAPI (Swagger) Editor* extension.

**In Postman or Insomnia.** Import the YAML directly. Both tools generate a
complete request collection from the specification, including example bodies.

**On the command line**, if you have Node.js available:

```bash
npx @redocly/cli preview-docs docs/api/decision-service.openapi.yaml
```

## Generating a client

The specifications are complete enough to generate working client libraries:

```bash
npx @openapitools/openapi-generator-cli generate \
  -i docs/api/decision-service.openapi.yaml \
  -g typescript-fetch \
  -o ./clients/decision-ts
```

Substitute `-g java`, `-g python`, `-g csharp`, or any other supported
generator target. This is the practical demonstration that these are
contracts, not documentation: a consuming institution could integrate against
them without reading the source.

## What the specifications document

Beyond the mechanical request and response shapes, each document records the
decision rules that a consuming institution would need to understand:

- **Ingestion** — the canonical `PayrollEvent` contract, the pay-frequency
  multipliers used for monthly normalization, and the accumulation semantics
  (how many events are needed before a stable assessment can be produced).
- **Income Verification** — the confidence score method in full, and the
  threshold table that maps a score to a stability label.
- **Decision** — the ordered rules table the engine evaluates, the credit
  limit and APR sizing policy, and the append-only audit semantics of the
  history endpoint.
- **Credit Profile** — the thin-file classification rules, the nullable bureau
  fields that define the thin-file case, and the cross-language consumption
  model.

## Verification before publication

Two values in these documents were written from the System Architecture
Document rather than observed directly, and should be confirmed against the
running system before the specifications are cited as evidence:

**1. Success status code on `POST /v1/payroll/events`.** The specification
declares `202 Accepted`. Confirm with:

```bash
curl -i -X POST https://ingestion.payroll-credit.com/v1/payroll/events \
  -H "Content-Type: application/json" \
  -d '{"userId":"spec-check-001","employerName":"Spec Check","payPeriodStart":"2026-06-01","payPeriodEnd":"2026-06-15","grossPay":3200.00,"netPay":2450.00,"payFrequency":"BIWEEKLY","sourceProvider":"Gusto"}'
```

If the first response line reads `HTTP/2 200`, change `'202':` to `'200':` in
`ingestion-service.openapi.yaml`.

**2. Health endpoint response body.** The specification declares
`{"service": "...", "status": "UP"}`. Confirm with:

```bash
curl https://ingestion.payroll-credit.com/v1/payroll/health
curl https://income.payroll-credit.com/v1/income/health
curl https://decision.payroll-credit.com/v1/eligibility/health
curl https://creditprofile.payroll-credit.com/v1/credit-profile/health
```

Adjust the `Health` schema in each document if the shapes differ.

A specification that does not match the running system is worse than no
specification, so this check is not optional.

## Keeping the specifications accurate

These are hand-written documents. They will drift from the code unless
maintained. Two options:

**Option A — treat them as the source of truth.** Edit the YAML first, then
implement to match. This is contract-first design and is appropriate when
external consumers depend on stability.

**Option B — generate them from the code.** Add `springdoc-openapi` to the
Java services and `Swashbuckle` to the .NET service, and the specifications
become a build artifact that cannot drift. This also exposes an interactive
Swagger UI on each public subdomain, which is considerably more compelling to
a reviewer than a YAML file.

See [`ENABLING_LIVE_API_DOCS.md`](ENABLING_LIVE_API_DOCS.md) for the exact
changes required for Option B.
