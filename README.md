# pubfleet

A supplier product-publication platform. Partners push product data. An AI pipeline
extracts structured records from documents. A worker fleet runs the jobs. The catalog
is indexed, searched, ranked, and priced. Operators drive it from a React console.

## What this is

A learning platform, built in the open. Each subsystem is designed first as a
system-design interview problem with fixed constraints, defended under push-back, then
built as a standalone library unit and wired into the running system.

The domain is a vehicle. The concepts are the point: correctness under concurrency,
event-driven consistency, resilience against flaky partners, and a front end that
makes the whole thing operable.

## Honest scope

- Built with heavy AI assistance. The design decisions and the trade-offs are the author's.
- Deployed to LocalStack. It has not run in AWS, and no document here claims that it has.
- The load numbers come from k6 runs on a laptop. Each number states the conditions.
- Some subsystems are design-only. `INDEX.md` marks them T3 and they carry no code.

## Stack

Java 25, Spring Boot 3, Maven. Postgres with Flyway, Redis, Kafka.
React 19, TypeScript, Vite, MUI, TanStack Query.
AWS CDK on LocalStack. OpenTelemetry into Grafana. Docker Compose, and k3d for the
Kubernetes unit.

## Layout

| Path | Holds |
|---|---|
| `INDEX.md` | The unit manifest and the current position. |
| `SKILLS.md` | The coverage ledger. |
| `SEEDS.md` | The real problems that produced the concepts. |
| `DECISIONS.md` | The ADR index. |
| `docs/adr/` | One record per accepted design. |
| `units/` | One folder per unit, with its context and its README. |

## Run it

The dev shell supplies JDK 25, Maven, Node, and the Playwright browsers.

```
nix develop
docker compose up -d
./mvnw install -DskipTests
```

The install step is necessary once. The two applications depend on the `contracts`
module, and Maven cannot resolve it until it is in the local repository.

Then start the three processes, each in its own shell:

```
./mvnw spring-boot:run -pl apps/control-plane
./mvnw spring-boot:run -pl apps/worker
npm --prefix apps/console ci && npm --prefix apps/console run dev
```

The console is on port 5173. The control plane is on port 8081. To change the port,
set `PUBFLEET_CONTROL_PLANE_PORT`.

## Test it

```
./mvnw clean verify                        # unit tests and Testcontainers tests
npm --prefix apps/console run test          # console unit tests
npm --prefix apps/console run test:e2e      # Playwright, needs the stack running
```

## License

MIT.
