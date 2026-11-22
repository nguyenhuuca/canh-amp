# Canh Amp

Canh Amp is a retro rack-mount style local music player for Android. It plays audio files stored on the device, with an original name and branding.

- Product requirements: [docs/prd](docs/prd)
- Specs: [docs/specs](docs/specs)
- Decisions: [docs/adr](docs/adr)
- Docs site (MkDocs): https://nguyenhuuca.github.io/canh-amp/ — run `mkdocs serve` locally

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
bash scripts/ci/check-forbidden.sh   # run after a build
```

## CI builds

Every push and pull request to `main` runs the "Build & Test" workflow. The debug APK (`canh-amp-<shortsha>.apk`) is attached to the workflow run as an artifact (Actions tab, open the run, Artifacts section), kept for 14 days. Releases are cut by manually running the "Release" workflow with a `MAJOR.MINOR.PATCH` version.

## GitHub secrets (ADR-0004)

Add these under Settings > Secrets and variables > Actions.

| Secret | Used for |
|--------|----------|
| `DEBUG_KEYSTORE_BASE64` | Debug keystore, base64 (keeps CI APKs installable over each other) |
| `DEBUG_KEYSTORE_PASSWORD` | Debug keystore password |
| `DEBUG_KEY_ALIAS` | Optional, debug key alias |
| `DEBUG_KEY_PASSWORD` | Optional, debug key password |
| `KEYSTORE_BASE64` | Release keystore, base64 |
| `KEYSTORE_PASSWORD` | Release keystore password |
| `KEY_ALIAS` | Release key alias |
| `KEY_PASSWORD` | Release key password |

Create a keystore and encode it:

```bash
keytool -genkeypair -v -keystore release.jks -alias canhamp \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 release.jks > release.jks.b64   # paste the content into KEYSTORE_BASE64
```

Do the same with a separate `debug.jks` for `DEBUG_KEYSTORE_BASE64`. Never commit keystores or passwords.
