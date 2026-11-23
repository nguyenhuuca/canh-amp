# Tech Strategy

The project's chosen technologies. Suggest an alternative only when the owner asks for one; otherwise build with what is listed here, at the latest stable version unless a version is pinned.

The installer fills the sections below from the stack packs it detected (`npx github:nguyenhuuca/agentic-sdlc init`). The owner confirms them with `/welcome`, which the first session offers; edit them freely afterwards: `update` never overwrites a file you changed.

<!-- sdlc:status confirmed 2026-11-22 -->

## Stack

<!-- sdlc:stack -->
| Area | Choice |
|------|--------|
| Language | Kotlin 2.0.21 |
| Platform / build | Android (compileSdk 35, minSdk 26), AGP 8.5.2, Gradle Kotlin DSL + version catalog |
| UI | Jetpack Compose (BOM 2024.09) + Material3, Navigation Compose |
| Playback | Media3 ExoPlayer + Session 1.4.1 |
| Storage | Room 2.6.1 (exported schemas in `app/schemas/`, KSP), DataStore Preferences |
| Lists | Paging 3.3.2 (Room paging source), Reorderable 2.4.0 (drag to reorder) |
| Tests | JUnit 4, Mockito 5, kotlinx-coroutines-test |
| CI / release | GitHub Actions (`build.yml`, `release.yml` → GitHub Release APK) |
| Docs | MkDocs Material 9.7.6 (`docs/requirements.txt`) → GitHub Pages via `docs.yml` (ADR-0005) |
<!-- /sdlc:stack -->

## Not Allowed

<!-- sdlc:prohibited -->
- No destructive Room migration: every schema change ships a `Migration` and an exported schema JSON; never `fallbackToDestructiveMigration`.
- No second playback library: audio goes through Media3 only.
<!-- /sdlc:prohibited -->
