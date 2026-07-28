# 0001 — Kafka producers omit type headers

**Status:** Accepted
**Date:** 2026-07-13
**Affects:** `ingestion-service`, `income-verification-service`, and every future producer

## Context

Spring Kafka's `JsonSerializer` emits a `__TypeId__` header on every published
message by default. The header carries the fully qualified Java class name of
the producing service's data transfer object — for example
`com.mota.incomeverification.model.IncomeVerifiedEvent`.

A consumer using `JsonDeserializer` reads that header and attempts to resolve
the named class against a configured list of trusted packages. When the class
is absent from the list, deserialization fails with:

```
java.lang.IllegalArgumentException: The class
'com.mota.incomeverification.model.IncomeVerifiedEvent' is not in the trusted
packages: [java.util, java.lang, com.mota.decision.model,
com.mota.decision.model.*]
```

This is not an edge case. It is the guaranteed outcome whenever two services
own their own DTOs in their own packages, which is the normal arrangement in a
microservice system. It surfaced here between `ingestion-service` and
`income-verification-service`, and again between `income-verification-service`
and `decision-service`.

For the C# `credit-profile-service` the header is worse than useless. There is
no JVM class loader on that side, so a header naming a Java class is noise that
a .NET consumer must be written to ignore.

Two remedies were available:

1. Widen each consumer's trusted-packages list to include the producer's
   package, or set it to `*`.
2. Stop emitting the header at the producer.

The first works, but it makes every consumer's configuration depend on the
internal package naming of every service that publishes to a topic it reads.
A package rename in one service silently breaks another. It also does nothing
for non-JVM consumers.

## Decision

All Kafka producers set `ADD_TYPE_INFO_HEADERS = false`.

The event schema — the JSON structure of the message body — is the contract.
The producing class name is an implementation detail and is not transmitted.

Each consumer declares the type it expects through `VALUE_DEFAULT_TYPE` and
sets `USE_TYPE_INFO_HEADERS = false`, so it deserializes into its own DTO in
its own package regardless of what produced the message.

Implementation:
- `ingestion-service/src/main/java/com/mota/ingestion/config/KafkaProducerConfig.java`
- `income-verification-service/src/main/java/com/mota/incomeverification/config/KafkaConfig.java` (see `incomeVerifiedProducerFactory`)

## Consequences

**Services are independently deployable across languages.** The C#
`credit-profile-service` consumes `income.verified` in its own consumer group,
alongside the Java `decision-service`, with no shared library and no generated
stubs between them.

**Future consumers in any language join without touching the producer.** A
service in Python, Go, or Rust can subscribe to an existing topic with no
coordination.

**Package renames are no longer breaking changes** for consumers. Internal
structure stops leaking across the service boundary.

**Consumers must know their target type at compile time.** There is no
automatic type resolution from the message. In a system where each service
owns its own DTOs this is arguably the desired property — it forces explicit
awareness of the contract instead of implicit coupling to another service's
internals — but it is a real constraint and would be inconvenient in a system
that genuinely needed polymorphic messages on one topic.

**Existing messages published before this change remain undeserialisable** by
consumers configured under the new scheme. Migration required resetting
consumer group offsets past them. Any future change to serialization
configuration carries the same requirement.

**The decision must be applied to every new producer.** It is configuration,
not a compile-time guarantee, so nothing prevents a future service from
omitting it and reintroducing the problem. A shared configuration module would
make it structural; that has not been built.
