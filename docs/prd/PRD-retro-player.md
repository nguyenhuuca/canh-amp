# PRD: Retro rack-mount MP3 player for Android

**Status:** Approved (2026-11-18) · **Owner:** project owner · **Date:** 2026-11-18 · **PR-FAQ:** n/a (single-owner app; owner chose to go straight to PRD)

App name: **Canh Amp**, package `com.canhamp` (Q1). The design is in `docs/design/stitch/` (5 screens + the "Titanium Deck" design system).

## Problem

The owner has designed a skinnable late-90s audio deck UI in Stitch (phosphor-green VFD display, 10-band EQ, terminal-style
playlist, local file library) and wants it as a real Android app that plays music stored on the phone. Stock and
mainstream players (Google Files, Samsung Music, Spotify-style UIs) are flat Material designs, have no real 10-band
graphic EQ and are built around streaming, not local MP3 folders. Today the owner uses one of those players and gets
neither the look nor the EQ control.

Evidence: the five Stitch screens (`docs/design/stitch/01..05-*.png`) and the owner's request. No user research beyond
that, so this is an app the owner builds for themself, with the option of sharing APKs later.

## Goals and Non-Goals

| Goal | Measure | Target |
|------|---------|--------|
| G1 Plays the local library reliably | Playback keeps going with the screen off and app in background | 60 min without stopping, on a device with Android 14+ |
| G2 Library is usable at real size | Time from "Rescan" to a populated list | ≤ 5 s for 2,000 tracks on a mid-range phone |
| G3 Looks like the design | Owner review, each screen side by side with its Stitch PNG | All 5 screens accepted, no layout breaking at 360–430 dp widths |
| G4 EQ audibly works | Changing a band or preset changes the output; bypass restores it | Verified per band with a test tone (see R9) |
| G5 Ships through CI like the reference repo | `build.yml` green on every push; `release.yml` produces a signed APK | First tagged release v0.1.0 |
| G6 Feels responsive | Cold start to interactive main screen | ≤ 1.5 s on a mid-range phone (release build) |

**Not in scope (v1):**
- Importing real `.wsz` / `.wal` skin files. Skins are 4 built-in themes.
- A-B loop, editing ID3 tags, setting a ringtone, Auto-EQ, 3D spatial / virtualizer.
- Streaming, internet radio, "WINAMP NET", user accounts (the avatar in the header), cloud sync.
- Google Play Store publishing (GitHub Release APKs only).
- Tablet/foldable dual-chassis layout (phone portrait only; landscape must not crash).
- Any "Winamp", "Nullsoft" or "Llama" names, slogans or logos (see R13).

## Users and Stories

- As a listener with MP3s on my phone, I want the app to find my music by itself so that I do not have to add files one by one.
- As a listener, I want playback to continue with the screen off and controls on the lock screen and in the notification.
- As a tinkerer, I want a real 10-band EQ with presets and my own saved curves so that I can shape the sound per song type.
- As a playlist builder, I want to queue, reorder, remove, sort and save tracks as M3U so that I can reuse them in other players.
- As a fan of retro hardware, I want the deck look (VFD display, spectrum bars, marquee, tactile buttons) and a few colour themes.

## Requirements

Priorities: **must** = v1 cannot ship without it; **should** = in v1 unless it slips; **could** = only if cheap.

### Library (screen 5)

| ID | Requirement | Priority | Acceptance |
|----|-------------|----------|------------|
| R1 | Scan device audio through MediaStore after the user grants `READ_MEDIA_AUDIO` (API 33+) or `READ_EXTERNAL_STORAGE` (API 26–32). Show each track with title, artist, album, format, bitrate, sample rate, size and duration. | must | Given 50 audio files on the device and permission granted, when the Library opens, then all 50 appear with correct format and duration, and the header shows the indexed count. |
| R2 | Permission denied state: explain why and offer a retry / settings link. The app must not crash. | must | Given permission is denied, when Library opens, then a rationale panel with a "Grant" button is shown and no list query runs. |
| R3 | Tabs: All tracks, Folders, Artists, Albums, Recent (by date added). | must (All, Folders, Artists, Albums); should (Recent) | Given tracks in 3 folders, when the user opens Folders and taps one, then only that folder's tracks appear with a breadcrumb path. |
| R4 | Search across title, artist, album and file name, as the user types. | must | Given "prodigy" is typed, when the list filters, then only tracks matching in any of those fields remain, case-insensitively. |
| R5 | Rescan button and "Import folder" via the Storage Access Framework (folder picker, permission persisted) for folders MediaStore misses. | must (Rescan); should (Import folder) | Given a SAF folder was imported, when the app restarts, then its tracks are still listed without asking again. |
| R6 | Per-track actions: Play now, Add to queue, Play next, Track details (read-only tags and stream info). | must | Given a track's ⋮ menu, when "Play next" is chosen, then that track plays right after the current one. |
| R7 | Storage usage bar (used / free) as in the design. | could | Given the screen is open, then the bar matches the values reported by `StatFs` within 1%. |

### Playback and main deck (screen 1)

| ID | Requirement | Priority | Acceptance |
|----|-------------|----------|------------|
| R8 | Playback via Media3 ExoPlayer inside a `MediaSessionService`: play, pause, stop, previous, next, seek. Includes a media notification, lock-screen controls, headset and Bluetooth buttons, audio focus (pause on call, duck on notification) and pausing when headphones are unplugged. Supported formats: whatever ExoPlayer decodes natively (MP3, AAC, FLAC, OGG/Opus, WAV). | must | Given a track is playing, when the screen is turned off for 60 min, then playback continues; when wired headphones are unplugged, then playback pauses within 1 s. |
| R9 | Main deck display: big elapsed/remaining timer (tap to toggle), track number, kbps and kHz, stereo/mono indicator, scrolling marquee with "artist - title (duration)". | must | Given a 320 kbps / 44.1 kHz stereo MP3, when it plays, then the deck shows "320", "44.1" and STEREO, and tapping the timer switches to a negative remaining time. |
| R10 | Spectrum visualizer with 16 bands from the real audio output, plus a BAR / WAVE mode switch. It must not need the microphone (`RECORD_AUDIO`) permission (see ADR need A2). | must (BAR); should (WAVE); could (FIRE) | Given music plays, then the bars move with the audio and stop when paused; the app manifest has no `RECORD_AUDIO`. |
| R11 | Seek bar, volume slider (app-level gain, 0–100%) and balance slider (L–C–R with a centre detent). | must (seek, volume); should (balance) | Given balance is moved fully left, when playing, then the right channel is silent. |
| R12 | Shuffle and repeat toggles (off / all / one) with LED states; they persist across restarts. | must | Given shuffle is on and the app is killed, when it restarts, then shuffle is still on. |
| R13 | No third-party trademarks: app name, header, footer "serial" plates, marquee filler and logo use the app's own name and assets. The visual style stays. | must | Given a text search of the APK resources for "winamp", "nullsoft" or "llama", then there are no matches. |
| R14 | Album art from embedded tags in the "now playing" card, with a styled placeholder when missing. | should | Given a file without embedded art, then the placeholder renders and no network request is made. |

### Playlist / queue (screen 4)

| ID | Requirement | Priority | Acceptance |
|----|-------------|----------|------------|
| R15 | Queue list in terminal style: index, title, bitrate badge, duration; playing row in inverted green, selected row in amber. Header stats: track count, total time, selected track time. | must | Given 18 tracks queued, then the header shows 18 and the sum of their durations. |
| R16 | Queue editing: tap to play, remove (× and DEL), reorder by drag, +FILE and +DIR (from the library or the folder picker), sort A–Z / by time, shuffle the queue, filter. | must (play, remove, add, filter); should (drag reorder, sort) | Given a track is dragged from position 5 to 2, when playback reaches position 2, then that track plays. |
| R17 | The queue and the current position persist across app restarts. | must | Given track 3 at 1:20 when the app is killed, when it relaunches, then track 3 is loaded at 1:20, paused. |
| R18 | Save the queue as `.m3u8` and load `.m3u` / `.m3u8` through the system file picker. Relative paths resolve against the playlist's folder; missing entries are skipped and counted. | must | Given a saved M3U8 is loaded in another player, then the same tracks appear in order. Given an M3U with 2 missing files, when loaded, then a message says "2 entries not found". |

### Equalizer and effects (screen 2)

| ID | Requirement | Priority | Acceptance |
|----|-------------|----------|------------|
| R19 | 10-band graphic EQ at 60, 170, 310, 600 Hz, 1, 3, 6, 12, 14, 16 kHz, each −12…+12 dB in 1 dB steps, plus a preamp. Applied in the app's own audio pipeline, so it works the same on every device (see ADR need A1). | must | Given a 1 kHz test tone, when the 1K band goes to +6 dB, then the output level rises by 6 dB ±1 dB; other bands change it by less than 1 dB. |
| R20 | EQ on / bypass toggle; curve graph that redraws as bands move. | must | Given bypass, then the output equals the input (null test) and the curve is drawn flat and grey. |
| R21 | Presets: Rock, Pop, Techno, Flat, Full Bass, Club, Reggae, Vocal (values from the design HTML), user presets with save and delete, Reset flat. The active preset name shows "[CUSTOM]" after manual edits. | must | Given "Rock" is chosen and then the 60 Hz band is moved, then the label shows [CUSTOM]; given "Save preset" with a name, then it appears in the preset list after a restart. |
| R22 | Bass boost (0–12 dB low shelf) and treble (0–12 dB high shelf) knobs. | should | Given bass boost at +6, then a 60 Hz tone is 6 dB ±1 louder. |
| R23 | Output protection: when the EQ gain could clip, apply a limiter or automatic preamp drop so nothing clips audibly. | must | Given all bands at +12 dB on a 0 dBFS track, then the output has no samples clipping above 0 dBFS. |

### Themes (screen 3) and shell

| ID | Requirement | Priority | Acceptance |
|----|-------------|----------|------------|
| R24 | Four built-in themes from the design: Classic (green/amber), Modern "Bento" (slate/amber), Titanium Black (OLED black/red), Cyber Neon Matrix. Apply instantly and persist. Cards show a live mini preview, palette swatches and Apply / Applied. | must | Given "Titanium Black" is applied, when the app restarts, then all screens use its palette. |
| R25 | Theme options: "Preserve custom EQ colours" and "Allow animated visualizer" toggles. Pixel-scaling option omitted. | could | Given the animated visualizer is off, then the bars are drawn static at peak-hold. |
| R26 | App shell: header with app name and screen name, a persistent mini-player bar (title, elapsed time, prev / play-pause / next) and bottom navigation MAIN · EQ · PLAYLIST · LIBRARY · SKINS. The fake status row (clock, "network", "POWER ON") is dropped because Android already shows a status bar. | must | Given any screen, when the mini-player play/pause is tapped, then playback toggles and the main deck reflects it. |
| R27 | Design fidelity: the Titanium Deck tokens (colours, Space Mono / Space Grotesk bundled as OFL fonts, 4 dp grid, recessed/raised depth, pressed states) live in one Compose theme; touch targets are at least 48 dp; TalkBack labels on every control. | must | Given the Accessibility Scanner runs on each screen, then there are no touch-target or missing-label findings. |
| R28 | Strings in English and Vietnamese, checked in CI like the reference repo. | must | Given a string is added only to `values/`, when CI runs, then the translation check fails. |

### Delivery

| ID | Requirement | Priority | Acceptance |
|----|-------------|----------|------------|
| R29 | Stack and CI copied from `D:\DO\android`: Kotlin, Jetpack Compose, Gradle version catalog, minSdk 26, target/compile SDK 35, JDK 24 runner, default branch `main`. `build.yml` (on push/PR to `main`: unit tests, lint translation check, then build the APK and upload it as a workflow artifact so the owner can install and test it) and `release.yml` (manual dispatch: tests, signed APK from secrets, tag, changelog, GitHub Release). | must | Given a push to `main`, then `build.yml` passes and the run has a downloadable `canh-amp-<sha>.apk` artifact that installs on the owner's phone; given a release is dispatched with 0.1.0, then a release `v0.1.0` with a signed APK appears. |
| R30 | No network permission and no analytics or telemetry. | must | Given the merged manifest, then `android.permission.INTERNET` is absent. |

## Constraints and Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Trademark.** The mockups use Winamp/Nullsoft names, slogan and logo. | Takedown or legal claim if shared. | R13: own name and assets; CI check greps resources for the banned words. |
| **The Android `Equalizer` effect is device-dependent** (often 5 bands at fixed frequencies). It cannot honour 10 fixed bands. | EQ would not match the design. | A1: 10 biquad peaking filters in a custom Media3 `AudioProcessor` (pure Kotlin, unit-testable on the JVM). Measure CPU cost; cap at 48 kHz stereo float. |
| **The `Visualizer` API needs `RECORD_AUDIO`**, which scares users, and is unreliable on some OEMs. | Permission prompt or a dead visualizer. | A2: tap PCM after the EQ with an `AudioProcessor`, run an FFT on a background thread at about 30 fps, and expose the bands as a `StateFlow`. |
| Android 14+ foreground service rules (`mediaPlayback` type) and OEM battery killers. | Playback stops in background (G1). | Use Media3 `MediaSessionService` with the correct service type; test on one Samsung/Xiaomi-class device. |
| Scoped storage: MediaStore misses some folders; SAF URIs are slow to query. | Missing tracks. | Rescan plus SAF import (R5); index SAF folders into Room once and refresh on demand. |
| Heavy skeuomorphic UI (gradients, glows, marquee, 30 fps bars) costs battery and frame drops. | Jank, battery drain. | Draw the visualizer in one `Canvas`; stop animations when the app is not visible; check with a Macrobenchmark or the JankStats baseline. |
| Fonts licensing. | Legal. | Space Mono and Space Grotesk are SIL OFL: bundle them with the licence file. |
| Signing keys. | Leaked key. | Same approach as the reference repo: keystore base64 and passwords only in GitHub secrets, never in the repo (`security.md`). |

**ADR needs implied by this PRD (write during `/spec`):**
- **A1** Audio DSP pipeline: custom Media3 `AudioProcessor` chain (EQ → bass/treble → balance/gain → limiter → tap) instead of `android.media.audiofx`.
- **A2** Visualizer source: PCM tap + FFT instead of `Visualizer` + `RECORD_AUDIO`.
- **A3** Persistence: Room for the library index, queue and user presets; DataStore for settings and theme.
- **A4** App id `com.canhamp`, min SDK 26 / target SDK 35: hard to change after the first release.

## Open Questions

| # | Question | Owner | Default if no answer |
|---|----------|-------|----------------------|
| Q1 | Final app name and package id? | Owner | **Answered:** "Canh Amp", `com.canhamp` |
| Q2 | Bottom nav: the Skins screen shows 5 tabs (with SKINS), the other screens show 4. Keep 5 everywhere? | Owner | 5 tabs everywhere (R26), default taken |
| Q3 | The design HTML uses 4 dp rounded corners while the design doc says 0 dp. Which wins? | Owner | Follow the PNGs (slightly rounded), default taken |
| Q4 | Which physical test device(s) count for G1/G2/G6? | Owner | **Answered:** the owner installs the APK built by CI on each push (R29) and tests on their own phone |
| Q5 | GitHub repository and default branch? | Owner | **Answered:** default branch `main`; the repo URL is needed before the first push |
