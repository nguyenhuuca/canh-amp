# Plan: Canh Amp, retro MP3 player

**Status:** Draft · **Date:** 2026-11-18 · **Inputs:** [PRD](../prd/PRD-retro-player.md), [spec](../specs/spec-retro-player.md), [sequences](../sequence/sequence-retro-player.md), ADR [0001](../adr/0001-audio-dsp-pipeline.md) [0002](../adr/0002-visualizer-pcm-tap.md) [0003](../adr/0003-persistence-room-datastore.md) [0004](../adr/0004-app-id-sdk-and-ci-signing.md), design specs `docs/specs/design-spec-*.md`

## Goal

Ship Canh Amp v0.1.0 (PRD R1–R30). It is done when every task below is `[x]`, `build.yml` is green on `main` with an
installable `canh-amp-<sha>.apk` artifact, the owner's device checklist (T23) passes, and `release.yml` has published
`v0.1.0`.

## Facts

This is a greenfield app: `D:\DO\winamp` has only docs and the agentic-sdlc scaffold, with no Gradle project yet. The
patterns to copy come from the owner's reference repo `D:\DO\android` (DocScanner).

**Build setup**
- Gradle wrapper 8.9: `D:\DO\android\gradle\wrapper\gradle-wrapper.properties:3`. Wrapper jar at `D:\DO\android\gradle\wrapper\gradle-wrapper.jar`.
- Version catalog: AGP 8.5.2, Kotlin 2.0.21, KSP 2.0.21-1.0.25, Compose BOM 2024.09.00, Navigation 2.8.3, Room 2.6.1, reorderable 2.4.0 (`D:\DO\android\gradle\libs.versions.toml:2-15`).
- `settings.gradle.kts` repository setup with `FAIL_ON_PROJECT_REPOS` and a single `:app` module: `D:\DO\android\settings.gradle.kts:1-23`.
- Root plugins applied `false`: `D:\DO\android\build.gradle.kts:1-6`.
- `gradle.properties`: `-Xmx2048m`, `nonTransitiveRClass`: `D:\DO\android\gradle.properties:1-4`.

**App module**
- compileSdk 35, minSdk 26: `D:\DO\android\app\build.gradle.kts:10,14`.
- Release signing from env `KEYSTORE_PATH` and friends, active only when set: `D:\DO\android\app\build.gradle.kts:22-43`.
- Minified release: `D:\DO\android\app\build.gradle.kts:41`. JVM target 21: `D:\DO\android\app\build.gradle.kts:58`.
- Unit tests with `isReturnDefaultValues = true`: `D:\DO\android\app\build.gradle.kts:68`. Debug coverage: `D:\DO\android\app\build.gradle.kts:38`.
- Room schema export via the KSP arg `room.schemaLocation`, committed under `app/schemas`: `D:\DO\android\app\build.gradle.kts:80-83`.
- Proguard keeps for Room: `D:\DO\android\app\proguard-rules.pro:1-3`.

**Wiring**
- Manual DI: `class AppContainer(context)` with `by lazy` singletons, and Room built via `Room.databaseBuilder` (`D:\DO\android\app\src\main\java\com\docscanner\di\AppContainer.kt:18-23`). It is created in `MyApplication.onCreate` and exposed as `container` (`D:\DO\android\app\src\main\java\com\docscanner\MyApplication.kt:10-21`).
- Package layout: `common`, `data`, `di`, `domain`, `ui` (+ `ui/theme`, `ui/AppNavGraph.kt`) under `D:\DO\android\app\src\main\java\com\docscanner\`.

**CI**
- `build.yml` triggers on `master` (`D:\DO\android\.github\workflows\build.yml:5,7`) with `permissions` read (`:9`) and JDK 24 (`:23`). Steps: `testDebugUnitTest` (`:31`), then lint with the translation grep (`:35-39`).
- `release.yml` decodes `KEYSTORE_BASE64` (`D:\DO\android\.github\workflows\release.yml:40`) and runs `assembleRelease` (`:43`).

**Local toolchain:** OpenJDK 24.0.1 on PATH. Android SDK at `C:\Users\CANH\AppData\Local\Android\Sdk`, per the reference repo's `local.properties`. `ANDROID_HOME` is not set, so `local.properties` must point `sdk.dir` there.

**This repo**
- `sdlc.config.json:2-4` has `deployBranch: main` and `components: []`, so there are no gates yet. Tracking files: `TASKS.md`, `docs/tracking.md` (`sdlc.config.json:7-10`).
- `.gitignore` already blocks `*.jks`, `keystore/` and `local.properties`. `.gitattributes` forces LF except `*.bat`.

**Versions:** builders keep the reference repo's versions unless a new library needs newer ones. Media3, DataStore and Paging do not exist in the reference catalog. T1 picks the latest stable releases compatible with AGP 8.5 / Kotlin 2.0 / compileSdk 35 and records them in the catalog (`core-directives.md` principle 1: check current docs).

## Tasks

Each task leaves `./gradlew testDebugUnitTest lintDebug assembleDebug` green, is ≤ ~400 changed lines where possible
(`sdlc.config.json` maxLoc), and adds its tests in the same task. "B" refers to spec behaviours, and "DS" to the design
spec of the screen.

### M1: skeleton, CI, design system

| ID | Task | Files | Acceptance criteria | Depends on |
|----|------|-------|---------------------|------------|
| T1 | Gradle project skeleton | `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew(.bat)`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `CanhAmpApp.kt`, `MainActivity.kt`, `di/AppContainer.kt`, `util/Durations.kt`, `values/strings.xml`, `values-vi/strings.xml`, `sdlc.config.json` | Builds the app per the T1 notes below. `DurationsTest` covers `mm:ss`, `h:mm:ss` and negative remaining time (B24). The three gate commands pass locally. | — |
| T2 | CI workflows and guard checks | `.github/workflows/build.yml`, `.github/workflows/release.yml`, `scripts/ci/check-forbidden.sh` | Implements B71–B73 per the T2 notes below. A push to `main` gives a green run with the artifact. Planting `winamp` in a string makes the script fail when run locally. | T1 |
| T3 | Theme tokens and 4 palettes | `ui/theme/{DeckColors,DeckTypography,DeckShapes,Dimens,CanhAmpTheme,ThemeId}.kt`, `res/font/space_mono_*.ttf`, `space_grotesk_*.ttf`, `assets/licenses/OFL.txt` | Every token from `docs/design/stitch/README.md` exists. The four palettes follow B67. `PaletteContrastTest` asserts on-surface/surface ≥ 4.5:1 and primary text/background ≥ 4.5:1 for each palette. | T1 |
| T4 | Deck components I | `ui/components/{DeckModule,RecessedWell,TactileButton,LedToggle,VfdText,Marquee,HFader}.kt` | Each has a `@Preview`. Pressed state: inverted gradient + 1 dp offset (DS shell). Minimum 48 dp hit area via `minimumInteractiveComponentSize`. Marquee and LED pulse are static when `ANIMATOR_DURATION_SCALE == 0` (B70); a `ReducedMotion` helper is unit-tested. | T3 |
| T5 | App shell and navigation | `ui/AppNavGraph.kt`, `ui/components/{DeckHeader,DeckNavBar,MiniPlayerBar}.kt`, placeholder `ui/{main,eq,playlist,library,skins}/*Screen.kt` | 5 tabs with save/restore state. Back goes to MAIN, and back on MAIN exits (B69). The mini player is hidden with an empty queue (fed by a fake `StateFlow` for now). Strings use the EN/VI keys from DS shell. | T4 |

**T1 notes.**
- App: `com.canhamp`, minSdk 26, target/compile SDK 35, JVM 21, Compose on.
- Version code from `CI_VERSION_CODE` (falls back to 1); version name from `CI_VERSION_NAME` (falls back to `0.1.0-dev`).
- Debug build: `applicationIdSuffix ".debug"`, signed with `DEBUG_KEYSTORE_PATH` when that is set.
- Release build: env signing as in the reference repo, minified.
- The `MainActivity` hello screen shows "CANH AMP".
- `sdlc.config.json` gets component `android` with gates `./gradlew testDebugUnitTest lintDebug --no-daemon`.

**T2 notes.**
- `build.yml` (B71, B72): on push/PR to `main`, `permissions: contents: read`.
  1. JDK 24.
  2. Unit tests.
  3. Lint translation grep (copied).
  4. `check-forbidden.sh`: banned words, plus the merged-manifest permission check.
  5. Optional debug keystore decode (warning in `$GITHUB_STEP_SUMMARY` if absent).
  6. `assembleDebug` with `CI_VERSION_CODE=${{ github.run_number }}`.
  7. Upload `canh-amp-<shortsha>.apk` (retention 14 days) and the test reports.
  8. Print the APK size in the summary.
- `release.yml` (B73): copied, with `main`, the APK name `canh-amp-<ver>.apk` and the version env.

### M2: library and playback

| ID | Task | Files | Acceptance criteria | Depends on |
|----|------|-------|---------------------|------------|
| T6 | Persistence layer | `data/db/{CanhAmpDatabase,TrackEntity,SafFolderEntity,QueueItemEntity,EqPresetEntity,TrackDao,SafFolderDao,QueueDao,EqPresetDao}.kt`, `app/schemas/**/1.json`, `data/settings/{SettingsRepository,Settings}.kt`, `di/AppContainer.kt` | Schema exactly as the spec data model (indexes on `folderPath`, `artist`, `album`, `dateAddedSec`; `eq_presets.name` NOCASE unique). `SettingsMappingTest` covers defaults and CSV parsing for `eqGains`, with clamping. The DAO instrumented test (`androidTest`, not in CI) covers upsert by uri, delete by generation and the queue replace transaction. No destructive fallback (ADR-0003). | T1 |
| T7 | MediaStore scan | `data/library/{MediaStoreScanner,TrackRowMapper,LibraryRepository,ScanResult}.kt`, `util/Logger.kt` | B2–B6. `TrackRowMapperTest` (plain row data class, no Android cursor) covers the title, artist and album fallbacks, format from extension then MIME, the bitrate fallback calculation and `folderPath` derivation. The `LibraryRepositoryTest` with a fake scanner and DAO covers generation increment and stale delete, and that concurrent `scan()` calls run the scanner once. `Logger` never logs titles or paths (B75). | T6 |
| T8 | Library screen | `ui/library/{LibraryScreen,LibraryViewModel,PermissionPanel,TrackCard,GroupCard,Breadcrumb}.kt`, `util/LikeEscaper.kt`, catalog adds `androidx.paging` (runtime + compose) | DS library states: no permission, scanning, empty, no match and populated. Implements B1, B7 and B9; the storage bar is in T22. `LikeEscaperTest` covers `%`, `_` and `\`. The `LibraryViewModelTest` (fake repo) covers the 150 ms search debounce and the tab and folder drill-down state. | T5, T7 |
| T9 | Playback service and connection | `playback/{CanhAmpPlaybackService,PlayerFactory,PlayerConnection,NowPlaying}.kt`, `AndroidManifest.xml` (service, foregroundServiceType, the permission list from spec Interfaces) | B20–B23. From Library, ▶, ADD TO QUEUE and PLAY NEXT work (B11, via an action sheet). A media notification appears with lock-screen controls. `NowPlayingMapperTest` covers the Format → kbps/kHz/stereo mapping. The manifest passes `check-forbidden.sh`. Manual: unplugging headphones pauses playback. | T8 |
| T10 | Main deck screen | `ui/main/{MainScreen,MainViewModel,DeckDisplay,TransportDeck,NowPlayingCard}.kt`, `ui/components/SegmentDigits.kt`, `res/drawable/cassette_placeholder.xml` | DS main states: empty, buffering, paused and playing. Implements B22, B24, B25, B28, B29, B31, B32 and B33. The spectrum area is a static placeholder until T20. Balance UI is disabled until T18. `MainViewModelTest` covers the timer toggle persisting, the repeat cycle, the "previous" 3 s rule and the marquee text (idle and normal). The mini player is wired to `PlayerConnection` (replacing T5's fake). | T9 |
| T11 | Track details and error handling | `data/library/TrackMetadataReader.kt`, `ui/library/TrackDetailsSheet.kt`, `playback/PlaybackErrorPolicy.kt` | B8: the sample rate is filled on play and on details, and written back. B13/B27: an unreadable file shows `ERR: CANNOT READ FILE`, skips, and stops after 5 in a row. `PlaybackErrorPolicyTest` covers the counter reset on success and the stop at 5. Tags are capped at 256 chars (B76). The INFO button on Main opens the details sheet. | T10 |

### M3: queue, playlists, SAF

| ID | Task | Files | Acceptance criteria | Depends on |
|----|------|-------|---------------------|------------|
| T12 | Queue persistence and restore | `data/queue/QueueRepository.kt`, `playback/CanhAmpPlaybackService.kt` | B55–B57, sequence 2. `QueueRepositoryTest` (fake DAO) covers the 500 ms debounce, the 5,000 cap and `QUEUE FULL`. The service restores the queue paused at the saved index and position; position is saved every 10 s, on pause, on transition and in `onTaskRemoved`. Manual: kill the app at 1:20 on track 3, relaunch, and track 3 is at 1:20, paused. | T10 |
| T13 | Playlist screen | `ui/playlist/{PlaylistScreen,PlaylistViewModel,QueueRow,ActionGrid}.kt`, `ui/library/*` (pick mode) | DS playlist states. Implements B58–B63: stats, select/double-tap play, remove with 4 s undo, drag reorder via `reorderable`, filter, the SORT cycle, one-off RND and the loop cycle. +FILE and +DIR use Library pick mode. `PlaylistViewModelTest` covers the stats sum formatting, every sort order with the playing item kept, the filter and undo. Accessibility actions Move up/down and Remove. | T12 |
| T14 | M3U codec | `data/playlist/{M3uCodec,M3uEntry}.kt` | Pure Kotlin. `M3uCodecTest` covers: an encode/decode round trip; EXTINF parsing; absolute, relative (with `..` normalised), `file://`, `content://` and `http` (marked remote); the line count and line length limits; the ISO-8859-1 fallback; skipping blank and comment lines (B64–B66, B76). | T1 |
| T15 | Playlist save and load | `data/playlist/PlaylistFileRepository.kt`, `ui/playlist/*` (LIST >> menu) | Sequence 5. Save writes `#EXTM3U` UTF-8 with the default name from B64. Load enforces the 2 MB limit, resolves only via `TrackDao` lookups, confirms before replacing, and toasts `LOADED n, MISSING m`. `PlaylistFileRepositoryTest` (fake resolver and DAO) covers mixed resolvable and missing entries and the base dir derived from the document id. | T13, T14 |
| T16 | SAF folder import | `data/library/SafFolderScanner.kt`, `data/library/LibraryRepository.kt`, `ui/library/*` | B10 and sequence 7: persisted grant; walk depth ≤ 8 and ≤ 10,000 files; the Empty and AlreadyImported paths; background metadata fill (4 parallel); removing a folder releases the grant and deletes its tracks; lost grants are pruned on start. `SafFolderScannerTest` (fake document tree) covers the depth and file caps and the extension filter. | T8 |

### M4: DSP and EQ

| ID | Task | Files | Acceptance criteria | Depends on |
|----|------|-------|---------------------|------------|
| T17 | DSP core and 10-band EQ | `playback/dsp/{Biquad,BiquadDesign,DspConfig,DspEngine,EqualizerProcessor,PcmConvert}.kt` | B40–B44 and ADR-0001. `EqualizerProcessorTest`: for each band, a sine at the centre frequency with the band at +6 dB rises 6 ±1 dB; the 1 kHz response with only one neighbour band moved changes < 1 dB; the 16 kHz band is skipped at 32 kHz; bypass output equals input byte-for-byte; a config swap mid-stream does not reset state (no discontinuity above −40 dBFS on a steady sine); a non-16-bit input format reports unsupported (B42). Perf: 60 s of 48 kHz stereo processes in < 1.8 s on JVM. | T1 |
| T18 | Balance, limiter, auto-preamp, player wiring | `playback/dsp/{BalanceProcessor,LimiterProcessor,ResponseCurve}.kt`, `playback/PlayerFactory.kt` (custom `RenderersFactory.buildAudioSink`), `ui/main/*` (enable balance) | B30, B45, B49, B50. `LimiterProcessorTest`: all bands +12 on a 0 dBFS sine gives no sample at 32767 or −32768; transparent at −6 dBFS (max diff ≤ 1 LSB). `BalanceProcessorTest` at −1/0/+1. `ResponseCurveTest`: the analytic peak matches the measured peak within 0.5 dB. Manual: EQ changes are audible on the device. | T17, T10 |
| T19 | EQ screen and presets | `data/eq/{EqRepository,BuiltInPresets,PresetNameValidator}.kt`, `ui/eq/{EqScreen,EqViewModel,VFader,Knob,CurveGraph,PresetSheet,SavePresetDialog}.kt`, `ui/components/{VFader,Knob}.kt` | DS equalizer states. Implements B41, B45–B48 and B52, with the preset table exactly as B46. `PresetNameValidatorTest`: the length limits, characters, built-in collision (case-insensitive) and the overwrite path. `EqViewModelTest`: an edit gives `[CUSTOM]`, Reset gives Flat, persistence is debounced 300 ms, and bypass draws a flat curve. Fader semantics announce hertz and decibels. | T18 |

### M5–M7: visualizer, themes, polish, release

| ID | Task | Files | Acceptance criteria | Depends on |
|----|------|-------|---------------------|------------|
| T20 | Spectrum analyser and SpectrumView | `playback/dsp/{RingBuffer,Fft,SpectrumTapProcessor,SpectrumAnalyzer,SpectrumFrame}.kt`, `ui/components/SpectrumView.kt`, `ui/main/*` | ADR-0002, B26, B51, sequence 4. `FftTest`: the bin of a known sine. `SpectrumAnalyzerTest`: a 1 kHz sine peaks in the band containing 1 kHz; silence stays at the floor; decay is 18 dB/s; nothing is emitted without subscribers. BAR and WAVE modes; bars fall within 500 ms on pause. The DSP status pill on Main and EQ comes from `DspEngine.status`. Manual: bars follow the music. | T18 |
| T21 | Skins screen and theme apply | `ui/skins/{SkinsScreen,SkinsViewModel,ThemeCard,ThemePreview}.kt`, `ui/theme/CanhAmpTheme.kt` | DS skins. Implements B67 and B68, plus the `visAnimated` part of B26. Apply recomposes the whole app instantly, and the choice persists across restart. Filter chips work. `SkinsViewModelTest` covers apply and filter. | T20 |
| T22 | Accessibility, storage bar and polish | `ui/**`, `ui/library/StorageBar.kt` | B12 storage bar. B70: semantics on every control; font scale 1.3 does not clip the Main deck (screenshot check with the Compose preview `fontScale = 1.3f`); reduced motion; `JankStats` logging in debug. The Accessibility Scanner on the 5 screens shows no touch-target or label findings (owner, manual). APK ≤ 15 MB. | T21, T16, T15 |
| T23 | v0.1.0 release | `docs/reports/device-check-2026-xx-xx.md`, `docs/tracking.md` | The owner runs the device checklist on a CI artifact: 60 min background (G1), scan of 2,000 tracks ≤ 5 s (G2), cold start ≤ 1.5 s (G6), call interruption, Bluetooth buttons, an M3U opened in another player, and the EQ test tone (G4). All pass. Then the owner dispatches `release.yml` with `0.1.0`; release `v0.1.0` exists with the signed APK. | T22 |

**Parallelism:** T14 and T17 depend only on T1 and can run alongside M2. T16 can run alongside M3. Everything else is sequential, as above.

## Risks

| Risk | Check that catches it |
|------|-----------------------|
| The Media3 version in the reference catalog's Kotlin/AGP range has a different `AudioProcessor` / `RenderersFactory` API from the one the spec assumes | T1 pins the version; T18's wiring compiles against it; T17 tests the processors on the JVM through the Media3 `BaseAudioProcessor` |
| Processors silently bypassed (offload or float output) so the EQ has no effect | ADR-0001 keeps both off. T18 manual audible check; T20 bars react to EQ changes (the tap sits after the EQ) |
| Android 14 foreground service crash (missing `foregroundServiceType` or permission) | T9 manifest review and a manual start of playback on an Android 14+ device; `check-forbidden.sh` checks the permission list |
| Debug keystore secrets not set, so each CI APK cannot install over the last one | T2 writes a warning in the job summary. The owner sets the secrets before T9 (ADR-0004) |
| MediaStore scan slow or incomplete on OEM phones | T7 logs counts and ms. G2 is measured in T23. SAF import (T16) covers missed folders |
| DSP CPU or jank on low-end phones | T17 perf test; T20 analyser only while subscribed; T22 JankStats |
| Trademark text sneaks in from the mockup HTML | `check-forbidden.sh` in CI from T2 on |
| Room schema change after the owner has data | ADR-0003: migration plus test for every bump; no destructive fallback except `tracks` |
| Tasks over 400 lines (T10, T13, T19 are large) | Builders split them into a/b commits where needed; each commit stays green |

## Verification

- **Gates per commit:** `./gradlew testDebugUnitTest lintDebug --no-daemon` (`sdlc.config.json` component `android`, added in T1). Plus `assembleDebug` and `scripts/ci/check-forbidden.sh` before push.
- **CI:** `build.yml` green on every push to `main`, with the APK artifact attached.
- **Unit tests:** the classes named in each task's acceptance criteria. The DSP and M3U tests carry the PRD acceptance numbers (R19, R20, R23, R18).
- **Instrumented (manual run):** DAO and migration tests (T6), and Compose navigation tests (T5/T19).
- **Owner manual checks:** after T9 (notification, unplug), T12 (restore), T18 (EQ audible), T20 (bars), T23 (full device checklist and release).
