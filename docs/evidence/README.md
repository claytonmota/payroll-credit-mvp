# Evidence Materials

Visual evidence supporting the I-140 / EB-2 NIW response is held privately by
the petitioner, Clayton Soares da Mota, and provided to counsel of record as
part of the formal submission package. This file records what exists and how
it was produced, so that the inventory is public even though the material is
not.

Everything listed here documents a system that is itself publicly verifiable.
The source is at
[github.com/claytonmota/payroll-credit-mvp](https://github.com/claytonmota/payroll-credit-mvp),
the deployment is reachable at [payroll-credit.com](https://payroll-credit.com),
and [`../RFE_EVIDENCE.md`](../RFE_EVIDENCE.md) maps each component to the
Professional Plan. A reviewer who prefers to check the system directly rather
than watch a recording of it can do so in about five minutes.

## Recorded demonstration

A continuous screen recording of the full pipeline exercised against the live
deployment.

**Structure.** The first part runs from a workstation over the public
internet: health checks against all four HTTPS subdomains, four payroll events
submitted to the ingestion service, the income confidence score returned, an
eligibility decision with its human-readable reasoning, the aggregate credit
profile written by the C# service, and then the thin-file case — a worker with
verified, stable income and a null bureau score, classified `THIN_FILE` rather
than declined for absence of data. The second part connects to the host by SSH
and shows the container stack, the append-only decision records in PostgreSQL,
and the aggregate document in MongoDB.

**Provenance.** Single take, no cuts, no post-production, no overlaid
graphics. Every response on screen is a live HTTP response from the deployed
system at the moment of recording, not a replay, a mock, or a re-enactment.
Narration is live. A `NOTES.md` accompanying the file records the recording
date, the tool used, the user identifiers exercised, and the repository commit
hash at the time of recording, so the recording can be tied to a specific
state of the source.

## Screenshots

Captured as a set covering the same ground in static form:

- The container stack running on AWS EC2
- The EC2 instance in the AWS console, with instance type and region visible
- AWS billing, showing the deployment is funded and operating
- Request and response pairs for each of the four services
- PostgreSQL query output showing persisted immutable eligibility decisions
- MongoDB query output showing an aggregate profile, including the thin-file
  case
- Kafka console consumer output showing events traversing the platform
- The GitHub repository: file tree, commit history, and contributor list


## Why this material is held privately

The screenshots and recording show the deployment host's internal structure,
its address, and operator context that has no reason to be published. The
system they document is already public and independently verifiable, so
nothing of evidentiary substance is withheld by keeping these particular
artifacts out of the repository — only the operational detail.

For access, or for verification of any claim made here, contact counsel of
record.
