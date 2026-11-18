# ADR-0004: App id, SDK levels and CI signing for test builds

**Status:** Accepted (2026-11-18) · **Deciders:** project owner · **Tags:** build, release, security

## Context

The owner named the app **Canh Amp** (PRD Q1) and will test by installing the APK that CI builds on every push to
`main` (PRD Q4, R29). The CI is copied from `D:\DO\android/.github/workflows/build.yml` and `release.yml`:

- JDK 24 Temurin;
- `testDebugUnitTest`;
- a lint translation check;
- release signing from `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` secrets (`app/build.gradle.kts` of the reference repo, `signingConfigs` block).

The application id and the signing key cannot change after users install the app without forcing an uninstall. A debug
APK built on a fresh CI runner is signed with that runner's random debug key. Each new build therefore has a
different signature, so installing it over the previous one fails ("App not installed"), and uninstalling deletes the
owner's queue, presets and settings.

## Options

### A. Fixed id; per-push APK = debug build signed with a stable debug key from a secret
- **How:** `applicationId = "com.canhamp"`, minSdk 26, target and compile SDK 35.
  - `build.yml` decodes `DEBUG_KEYSTORE_BASE64` and signs `assembleDebug` with it, then uploads `canh-amp-<shortsha>.apk` as an artifact kept 14 days.
  - `release.yml` stays as in the reference repo, with the release key.
  - Debug builds use `applicationIdSuffix = ".debug"`, so a test build and a release can sit side by side.
- **Cost:** two keystores to create once.
- **Risk:** a leaked debug key only lets someone overwrite the debug variant.

### B. Per-push APK = release build signed with the release key
- **Cost:** one keystore.
- **Risk:** every push exposes the release key to more workflow runs. R8 minified builds are harder to debug, and builds are slower.

### C. Unsigned or per-runner debug key
- **Cost:** none.
- **Risk:** every install needs an uninstall, so data is lost and testing becomes painful.

## Decision

**A.** Stable, upgradeable test installs without widening the use of the release key. If `DEBUG_KEYSTORE_BASE64` is
missing (forks, first run), the workflow still builds with the runner key and prints a warning in the job summary.

`versionCode` comes from `GITHUB_RUN_NUMBER` in CI (falling back to 1 locally), so each push installs as an upgrade.
`versionName` is `0.1.0-dev+<shortsha>` on push and the dispatched version on release.

## Consequences

- Owner one-time setup: create `debug.jks` and `release.jks` locally (never committed, because `.gitignore` blocks `*.jks`), base64 them into GitHub secrets `DEBUG_KEYSTORE_BASE64`, `DEBUG_KEYSTORE_PASSWORD`, `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.
- Workflows use `permissions: contents: read` (build) and `contents: write` (release only), as in the reference repo.
- Branch is `main` (PRD Q5). The reference repo's `master` triggers are changed.
- Changing `com.canhamp` later requires a new app listing and a user reinstall. Treat it as fixed.

## Changelog

- 2026-11-18: created.
