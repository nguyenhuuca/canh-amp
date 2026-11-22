# Canh Amp

Canh Amp is a retro rack-mount style local music player for Android. It plays audio files stored on the device, with an original name and branding.

## Where to start

| Section | What it holds |
|---------|---------------|
| [Feature Tracking](tracking.md) | Status of every feature and release |
| [PRD](prd/PRD-retro-player.md) | What the app does and for whom |
| [Spec](specs/spec-retro-player.md) | Behaviour, data model and acceptance criteria |
| [Design Specs](specs/design-spec-app-shell.md) | Screen-by-screen UI from the Stitch design |
| [Sequence Diagrams](sequence/sequence-retro-player.md) | Playback, library scan and DSP flows |
| [ADR](adr/0001-audio-dsp-pipeline.md) | Decisions that are hard to undo |
| [Plan](plans/plan-retro-player.md) | The build plan, task by task |

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
bash scripts/ci/check-forbidden.sh   # run after a build
```

Every push to `main` runs the "Build & Test" workflow and attaches the debug APK to the run. Releases are cut by running the "Release" workflow with a `MAJOR.MINOR.PATCH` version.

## Docs locally

```bash
pip install mkdocs-material
mkdocs serve
```
