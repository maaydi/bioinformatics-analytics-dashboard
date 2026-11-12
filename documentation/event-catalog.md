# Event Catalog

This document defines the event contracts and ownership boundaries for catalog and audit events. Catalog event
requirements are the ARCH-002 target contract. Audit fields reflect the shared `AuditLogEvent` contract; delivery
limitations of the current asynchronous publisher are called out explicitly.

## Conventions

- Kafka values are JSON. Consumers bind to an explicit local/default value type; producers do not add Java class
  type headers such as `__TypeId__`.
- Catalog envelopes use `eventId` (UUID), `eventType`, `schemaVersion`, `occurredAt` (UTC instant),
  `correlationId`, `actor`, and `payload`. `aggregateVersion` is the catalog's technical `revision` when applicable,
  never UniProt's source `entry_version`.
- Correlation IDs are propagated in the `X-Correlation-Id` Kafka header and event body. Actor identity, actor type,
  source service, and correlation ID are distinct values; none substitutes for another.
- Events contain only the fields needed by consumers. Do not publish raw protein sequences, credentials, tokens,
  raw request bodies, or unbounded metadata.

## Catalog Events

Both catalog topics are produced by `gene-service` from `gene_data.catalog_outbox`. The catalog mutation and its
outbox row commit in the same database transaction. A relay claims batches with `FOR UPDATE SKIP LOCKED`, publishes
them, and sets `published_at` only after broker acknowledgement. Delivery is at least once, so consumers deduplicate
by `eventId`. The planned relay polls every 200 ms, claims batches of up to 500 rows, and makes at most 10 publish
attempts per row. Exhausted/poison rows remain observable for operations and must not be silently treated as
published. Published outbox rows are eligible for cleanup after 7 days; broker retention is defined per topic below.

| Topic | Key | Kafka policy | Partitions | Meaning |
|-------|-----|--------------|-----------:|---------|
| `catalog.protein-entry.changed.v1` | `accession` | `compact` | 6 | Post-merge state snapshot, one event per materially changed entry. |
| `catalog.protein-ingestion.completed.v1` | `ingestionId` | `delete`, 7 days | 3 | One marker after all chunks in an ingestion have succeeded and completion is committed. |

### `catalog.protein-entry.changed.v1`

| Field | Contract |
|-------|----------|
| `eventType` | `CREATED` for a new accession; `UPDATED` for a material merge. No delete event is produced. |
| `aggregateVersion` | The entry's monotonically increasing technical `revision`. |
| `payload` | Entry state needed by future consumers, including accession and analytics-relevant fields. Include sequence length and molecular weight, never raw sequence. |
| Key | Accession, preserving per-accession partition ordering. |

Reapplying an unchanged input emits no entry event. Consumers that maintain a projection apply a version-guarded
upsert: accept the event if the row is absent or `incoming.revision` is greater than the stored revision. Analytics
does not consume this topic.

### `catalog.protein-ingestion.completed.v1`

| Field | Contract |
|-------|----------|
| `eventType` | `INGESTION_COMPLETED`. |
| `aggregateVersion` | Omitted; completion is not an entry revision. |
| `payload` | `ingestionId`, source type, `created`, `updated`, `unchanged`, `stale`, and `rejected` counters, and completion time. |
| Key | Ingestion ID. |

The ingestion status/counters and its completion outbox row commit together after all chunks succeed. Repeated
completion calls are idempotent and do not create another marker. Ordering between the two catalog topics is not a
correctness dependency: the statistics API reads committed catalog state.

### Catalog consumer: analytics-service

Analytics consumes only `catalog.protein-ingestion.completed.v1`. It durably records/deduplicates the completion and
pending refresh before acknowledging Kafka, coalesces work according to the configured quiet period and scheduling
ceiling, and calls the gene-service statistics API. It publishes the complete returned aggregate set atomically in
its own `analytics` schema. Duplicate/out-of-order markers may coalesce because each fetch reads the current catalog;
a marker arriving during a fetch remains pending for a subsequent fetch. Failure retains the last complete aggregate
set and leaves refresh failure visible and recoverable. Analytics does not consume per-entry snapshots or read
`gene_data` directly.

## Audit Events

| Property | Contract |
|----------|----------|
| Topic | Existing `audit.events.log`; no replacement topic is introduced. |
| Key | `eventId` UUID as a string. |
| Producer | `common-starter` audit aspect/publisher, explicitly enabled by an audit-producing service. |
| Consumer and owner | `audit-service` consumes and persists events; it alone owns the `audit_log` schema, migrations, authorized search API, and retention. |
| Transport | Asynchronous JSON without Java type headers. The consumer binds to `AuditLogEvent`. |

The audit value is the `AuditLogEvent` record:

| Field | Meaning |
|-------|---------|
| `eventId` | Unique event UUID and consumer idempotency key. |
| `actorIdentity`, `actorType` | Initiating user/system identity and actor classification. |
| `sourceService` | Producing service identity. |
| `correlationId` | Request/operation correlation; kept distinct from actor and source service. |
| `action`, `targetType`, `targetId` | Audited operation and affected resource. A default target is resolved from the action. |
| `status` | Audit outcome. |
| `ipAddress` | Client address when captured by the request context. |
| `createdAt` | Event creation instant. |
| `metadata` | Allow-listed key/value details; current producer includes HTTP method and endpoint where available. Never include secrets or raw bodies. |

### Audit delivery and failure behavior

Audit publishing is asynchronous and must not replace or fail the business response. The current producer uses a
bounded executor and logs queue rejection and synchronous/asynchronous Kafka send failures. It does not use a durable
outbox, so process failure, queue rejection, or an exhausted send can lose an event; this is best-effort delivery, not
at-least-once delivery. This limitation must remain visible until the producer's bounded delivery/retry and failure
policy required by ARCH-002 is explicitly approved and tested. Producer services and `common-starter` have no audit
database credentials or persistence classes.

The audit consumer deduplicates by `eventId`, persists before acknowledging the Kafka record, and allows processing
failures to be retried by Kafka. Duplicate delivery must not create duplicate audit records. Audit history currently
associated with dashboard migrations V13-V17 is handled through the approved P−1 transfer/baseline or archive/purge
decision; those migrations are not applied to `gene_data` and must not be blindly replayed by audit-service.

## Ownership and ordering summary

- `gene-service` owns catalog state and catalog outbox rows; only it writes catalog events.
- `analytics-service` is the only planned consumer of the ingestion-completed topic; entry-change events are a
  versioned contract for future consumers.
- `common-starter` creates and asynchronously transports audit events only. `audit-service` exclusively owns audit
  persistence, deduplication, search, and retention.
- Ordering is guaranteed only within a topic partition/key. Consumers must tolerate duplicates; cross-topic ordering
  must not be assumed.
- Catalog deletion is unsupported: no delete endpoint, tombstone, or `DELETED` event is part of this contract.