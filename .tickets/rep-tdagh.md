---
id: rep-tdagh
status: closed
deps: []
links: []
created: 2026-09-14T13:47:34Z
type: bug
priority: 1
assignee: Stavros Korokithakis
---
# Commit the Gradle wrapper jar so ./gradlew works on a fresh clone

Objective: `gradle/wrapper/gradle-wrapper.jar` is missing from the repo, so `./gradlew` cannot run at all on a fresh clone. README.md tells people to run `./gradlew assembleDebug`, which therefore fails. The cause is the blanket `*.jar` rule at .gitignore line 18, which was written for build output and dependency jars and caught the wrapper by accident. It was never committed on any branch.

Scope:
- .gitignore: add a negation for gradle/wrapper/gradle-wrapper.jar after the `*.jar` rule.
- Generate gradle/wrapper/gradle-wrapper.jar and add it to the index.

The jar must match the version already pinned in gradle/wrapper/gradle-wrapper.properties, which is 8.11.1. Generate it with that exact Gradle distribution, do not fetch a jar of unknown provenance. Gradle 8.11.1 is already unpacked at /tmp/opencode/gradle-8.11.1 from earlier work in this session.

Non-goals: do not change the Gradle, AGP, or Java version. Do not modify gradlew or gradlew.bat, which are already committed and correct. Do not add CI or wrapper-validation tooling. Do not commit anything.

## Design

The wrapper jar is a ~43 KB bootstrapper, not Gradle itself. The Gradle distribution stays out of the repo; gradle-wrapper.properties points at it and the jar downloads it on demand. Committing the jar is the standard arrangement and is what the already-committed gradlew script requires.

The counter-argument is supply chain risk, since the jar executes on every build. The usual mitigation is a CI checksum step, and this repo has no CI. The considered alternative was deleting gradlew and gradlew.bat and requiring a system Gradle, which was rejected because it removes the version pinning the wrapper provides.

## Acceptance Criteria

On a fresh clone, ./gradlew --version reports Gradle 8.11.1 and exits 0.

