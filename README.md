# Pogues API

[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=InseeFr_Pogues-Back-Office&metric=alert_status)](https://sonarcloud.io/dashboard?id=InseeFr_Pogues-Back-Office)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=InseeFr_Pogues-Back-Office&metric=security_rating)](https://sonarcloud.io/dashboard?id=InseeFr_Pogues-Back-Office)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=InseeFr_Pogues-Back-Office&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=InseeFr_Pogues-Back-Office)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=InseeFr_Pogues-Back-Office&metric=coverage)](https://sonarcloud.io/dashboard?id=InseeFr_Pogues-Back-Office)

Navigation: [Client][1] | **Back-office** | [Pogues model][2]

[1]: https://github.com/InseeFr/Pogues
[2]: https://github.com/InseeFr/Pogues-Model

## Introduction

Pogues is a tool that allow to design questionnaires with components that are structural (sequences, questions...) and dynamic (filters, controls, loops...).

This is the repository of the back-end part of Pogues.

## Local run with the Pogues front (no Keycloak)

The `local` profile uses anonymous auth (Guybrush / `FAKEPERMISSION`), mocks Magma and DDI-AS, and embeds Postgres (no Docker).

Prerequisites: Java 25, Maven. Same commands on macOS, Linux and Windows (PowerShell, Git Bash or cmd).

From IntelliJ: run the shared `Pogues (local)` configuration, or start `fr.insee.pogues.Pogues` (the `local` profile is selected on its own in a workspace classpath).

From the command line:

```bash
mvn spring-boot:run
```

That starts Postgres (port 5433, data in `.local-postgres/`) then the API on http://localhost:8081. Swagger: http://localhost:8081/

The first start can take a while: Postgres binaries are extracted once.

In the Pogues frontend repo, in parallel:

```bash
pnpm --dir next install
pnpm dev:api
```

→ http://localhost:5173

### Add a questionnaire

Drop a Pogues `.json` file in `local-questionnaires/` (this repo root). The API picks it up on its own; refresh the UI.

One questionnaire per file. The JSON `id` must be alphanumeric (`qdemo1`, no hyphen). The folder wins: on start and on drop, a file overwrites the same id in the database (logged as `Replaced questionnaire …`). UI edits on that id are lost when you restart or replace the file.

`qdemo1.json` is the sample loaded on the first start.

`.local-postgres/` is kept across restarts so Postgres binaries are not extracted every time. Delete it only if you want a full schema wipe (API stopped).

Visualization (Eno, Queen, Stromae) is not started in this mode.
