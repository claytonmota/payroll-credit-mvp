# Evidence Materials

Visual evidence (screenshots and a demo video recording) for the
I-140 / EB-2 NIW RFE response is maintained privately by the
petitioner (Clayton Soares da Mota) and provided to counsel of record
as part of the formal submission package. Materials include:

- Docker Compose running the eight-container stack on AWS EC2
- End-to-end request/response demonstrations of the four public services
  (ingestion, income verification, decision, credit profile)
- PostgreSQL query output showing persisted immutable eligibility
  decisions
- MongoDB query output showing aggregate credit profiles including the
  thin-file case
- Kafka console consumer output showing events traversing the platform
- GitHub repository state (commit history and contributor list) as of
  the RFE submission date

The public deployment and source code in this repository are directly
verifiable by anyone. See [`../RFE_EVIDENCE.md`](../RFE_EVIDENCE.md) for
the reader's guide linking each element to the Professional Plan.

For access to the private evidence materials or additional
verification, contact counsel of record.
