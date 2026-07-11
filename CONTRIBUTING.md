# Contributing

Thank you for your interest in this project. It is developed and
maintained by **Clayton Soares da Mota** as the reference implementation
of a payroll-integrated real-time credit infrastructure. Contributions
that advance the roadmap in [`docs/ROADMAP.md`](docs/ROADMAP.md) are
welcome.

## Ways to contribute

- **Bug reports** — open an issue with a minimal reproduction, the
  service affected, and the observed vs expected behavior.
- **Feature proposals** — open an issue describing the use case and how
  it maps to a component in [`docs/ROADMAP.md`](docs/ROADMAP.md) or the
  architecture diagram before starting work.
- **Pull requests** — fork the repo, create a feature branch, submit a
  PR against `main` with a clear description of what changed and why.
- **Documentation** — improvements to `README.md`, `docs/DEMO.md`,
  `docs/ROADMAP.md`, or inline Javadoc are always welcome.

## Development setup

Prerequisites:

- JDK 17 or newer
- Maven 3.9+
- Docker + Docker Compose
- Git

Clone and build:

```bash
git clone <this-repo-url>
cd payroll-credit-mvp
docker compose up --build
```

See [`docs/DEMO.md`](docs/DEMO.md) for the end-to-end walkthrough.

## Code style

- **Language:** Java 17. New services may introduce C# (.NET 8) as
  outlined in the roadmap; keep the languages/frameworks named in the
  Professional Plan as the baseline.
- **Layout:** each service is an independently deployable Spring Boot
  application under its own folder. Do not add cross-service compile-time
  dependencies; use Kafka events for cross-service communication.
- **Naming:** package prefix `com.mota.<service>`; DTO classes end with
  `Event` for Kafka payloads and `Result` / `Decision` for persisted
  outputs.
- **Formatting:** default IntelliJ / Spring Boot formatting; 4-space
  indent; UTF-8; LF line endings.
- **Tests:** every business-logic class (services, engines) must have a
  unit test. Kafka listeners and controllers may be covered by
  integration tests where value warrants it.

## Commit messages

Use short, imperative titles ("Add X", "Fix Y") followed by an optional
body explaining *why*. Group related changes into a single commit; avoid
bundling unrelated changes.

Example:

```
Add credit-profile-service scaffold in C# (.NET 8)

Implements the Credit Profile Service block from the architecture
diagram and delivers on the Professional Plan's promised Java + C# +
PostgreSQL + MongoDB stack. See docs/ROADMAP.md item #1.
```

## Pull request checklist

Before opening a PR, please verify:

- [ ] Code builds cleanly with `mvn clean verify`
- [ ] All existing unit tests pass
- [ ] New behavior is covered by at least one unit test
- [ ] Public API changes are documented in the affected service's README
      or in `docs/`
- [ ] No secrets, credentials, personal data, or production URLs are
      committed

## Reporting security issues

Do **not** open a public issue for security-sensitive reports. Email the
maintainer privately using the address listed on his public profile.

## License

By contributing you agree that your contributions will be licensed under
the Apache License 2.0, the same license that covers the rest of this
project. See [`LICENSE`](LICENSE) for details.
