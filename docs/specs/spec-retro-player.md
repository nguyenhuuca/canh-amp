# Spec: Canh Amp, retro MP3 player

**Status:** Approved (2026-11-18) · **PRD:** docs/prd/PRD-retro-player.md · **ADRs:** 0001 DSP chain, 0002 visualizer tap, 0003 Room + DataStore, 0004 app id and CI signing · **Date:** 2026-11-18

## Overview

Canh Amp is a single-module Android app (`:app`, package `com.canhamp`), built with Kotlin and Jetpack Compose. It
plays local audio through Media3 ExoPlayer inside a `MediaSessionService`, with an in-app DSP chain (ADR-0001) and a
PCM-tap visualizer (ADR-0002). It indexes music from MediaStore and SAF folders into Room (ADR-0003). The UI follows the
Stitch "Titanium Deck" design across five screens: Main, EQ, Playlist, Library and Skins. It covers PRD R1–R30. The
trace table at the end maps each requirement to behaviours (B-numbers) and sequence diagrams
(`docs/sequence/sequence-retro-player.md`). Design specs: `docs/specs/design-spec-*.md`.

### Module layout (single Gradle module, packages)

| Package | Responsibility |
|---------|----------------|
| `com.canhamp` | `CanhAmpApp` (Application, builds `AppContainer`), `MainActivity` |
| `di` | `AppContainer`: manual DI, as in the reference repo (no Hilt) |
| `data.db` | `CanhAmpDatabase`, entities, DAOs |
| `data.library` | `MediaStoreScanner`, `SafFolderScanner`, `TrackMetadataReader`, `LibraryRepository` |
| `data.queue` | `QueueRepository` (queue and position persistence) |
| `data.eq` | `EqRepository` (built-in and user presets, active EQ state) |
| `data.settings` | `SettingsRepository` (DataStore) |
| `data.playlist` | `M3uCodec` (pure), `PlaylistFileRepository` (SAF read/write) |
| `playback` | `CanhAmpPlaybackService`, `PlayerFactory`, `PlayerConnection` (UI-side `MediaController` wrapper), `NowPlaying` |
| `playback.dsp` | `DspEngine`, `DspConfig`, `BiquadDesign`, `Biquad`, `EqualizerProcessor`, `BalanceProcessor`, `LimiterProcessor`, `SpectrumTapProcessor`, `RingBuffer`, `Fft`, `SpectrumAnalyzer` |
| `ui.theme` | `CanhAmpTheme`, `DeckColors` (4 palettes), `DeckTypography`, `DeckShapes`, `Dimens` |
| `ui.components` | `DeckModule`, `RecessedWell`, `TactileButton`, `LedToggle`, `VfdText`, `SegmentDigits`, `Marquee`, `SpectrumView`, `HFader`, `VFader`, `Knob`, `MiniPlayerBar`, `DeckHeader`, `DeckNavBar` |
| `ui.main`, `ui.eq`, `ui.playlist`, `ui.library`, `ui.skins` | Screen + `ViewModel` each |
| `util` | `Logger` (wraps `android.util.Log`, no PII), `Durations`, `LikeEscaper` |

Dependencies, added to the version catalog at the latest stable versions compatible with AGP and Kotlin:

- carried over from the reference repo: Compose BOM, Navigation Compose, Lifecycle, Room + KSP, Coroutines, JUnit4, Mockito;
- new: `androidx.media3:media3-exoplayer`, `media3-session` and `androidx.datastore:datastore-preferences`;
- reorderable list: `sh.calvin.reorderable`, already used by the reference repo.

No other libraries.

## Data Model

Room database `canhamp.db`, version 1, `exportSchema = true` (ADR-0003).

### `tracks`: library index (cache, rebuildable)

| Column | Type | Constraints / notes |
|--------|------|---------------------|
| `id` | INTEGER | PK autoincrement |
| `uri` | TEXT | NOT NULL, UNIQUE. `content://media/...` or SAF document URI |
| `source` | TEXT | NOT NULL, `MEDIASTORE` or `SAF` |
| `mediaStoreId` | INTEGER | nullable |
| `safFolderId` | INTEGER | nullable, FK → `saf_folders.id` ON DELETE CASCADE |
| `filePath` | TEXT | nullable. Absolute path when known (MediaStore `DATA`), used by M3U |
| `folderPath` | TEXT | NOT NULL. Display folder, for example `/Music/Retro_Vault_98`. Indexed |
| `fileName` | TEXT | NOT NULL |
| `title` | TEXT | NOT NULL. Falls back to the file name without extension |
| `artist` | TEXT | NOT NULL. Falls back to `<unknown>`. Indexed |
| `album` | TEXT | NOT NULL. Falls back to `<unknown>`. Indexed |
| `format` | TEXT | NOT NULL, upper-case extension: `MP3`, `FLAC`, `OGG`, `OPUS`, `M4A`, `AAC`, `WAV` |
| `mimeType` | TEXT | nullable |
| `durationMs` | INTEGER | NOT NULL, ≥ 0 |
| `bitrateKbps` | INTEGER | nullable |
| `sampleRateHz` | INTEGER | nullable. Filled lazily (B8) |
| `sizeBytes` | INTEGER | NOT NULL |
| `dateAddedSec` | INTEGER | NOT NULL. Indexed |
| `dateModifiedSec` | INTEGER | NOT NULL |
| `scanGeneration` | INTEGER | NOT NULL. The scan run that last saw the row (B5) |

### `saf_folders`

| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER | PK |
| `treeUri` | TEXT | NOT NULL, UNIQUE |
| `displayName` | TEXT | NOT NULL |
| `addedAtSec` | INTEGER | NOT NULL |

### `queue_items`: the play queue, in order

| Column | Type | Notes |
|--------|------|-------|
| `position` | INTEGER | PK (0-based, contiguous) |
| `uri` | TEXT | NOT NULL |
| `title`, `artist` | TEXT | NOT NULL, snapshot for display when the track is gone from the library |
| `durationMs` | INTEGER | NOT NULL |
| `bitrateKbps` | INTEGER | nullable |
| `trackId` | INTEGER | nullable. Soft link to `tracks.id`, no FK, because tracks can be rebuilt |

### `eq_presets`: user presets only (built-ins live in code)

| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER | PK |
| `name` | TEXT | NOT NULL, UNIQUE (case-insensitive, `COLLATE NOCASE`), 1–24 chars |
| `gainsCsv` | TEXT | NOT NULL, 10 integers in −12..12 |
| `preampDb` | INTEGER | NOT NULL, −12..12 |
| `createdAtSec` | INTEGER | NOT NULL |

### DataStore `settings` (Preferences)

| Key | Type | Default |
|-----|------|---------|
| `theme` | String (`CLASSIC`, `BENTO`, `TITANIUM_BLACK`, `CYBER_NEON`) | `CLASSIC` |
| `shuffle` | Boolean | false |
| `repeat` | String (`OFF`, `ALL`, `ONE`) | `OFF` |
| `volume` | Float 0..1 | 0.8 |
| `balance` | Float −1..1 | 0 |
| `eqEnabled` | Boolean | true |
| `eqGains` | String CSV of 10 integers | `0,0,0,0,0,0,0,0,0,0` |
| `eqPreamp` | Int | 0 |
| `eqPresetName` | String, empty = custom | `Flat` |
| `bassDb`, `trebleDb` | Int 0..12 | 0 |
| `timeMode` | String (`ELAPSED`, `REMAINING`) | `ELAPSED` |
| `visMode` | String (`BAR`, `WAVE`) | `BAR` |
| `visAnimated` | Boolean | true |
| `queueIndex` | Int | 0 |
| `queuePositionMs` | Long | 0 |
| `scanGeneration` | Long | 0 |

**Migrations:** v1 only. Every future change adds a `Migration(n, n+1)` plus a `MigrationTestHelper` test. A failed
migration may drop `tracks` and `saf_folders` only, never `eq_presets` or `queue_items` (ADR-0003).

## Interfaces

The app has no network API. These are the internal contracts that screens and the service depend on. All are
`suspend` or `Flow`. Errors are sealed `Result`-style types, never thrown across layers. Authorization means Android
runtime permissions and SAF grants.

| Kind | Name | Input | Output | Errors | Auth / permission |
|------|------|-------|--------|--------|-------------------|
| Repo | `LibraryRepository.scan()` | none | `ScanResult(added, updated, removed, total)` | `PermissionMissing`, `StorageUnavailable` | `READ_MEDIA_AUDIO` (33+) / `READ_EXTERNAL_STORAGE` (≤32) |
| Repo | `LibraryRepository.importFolder(treeUri)` | SAF tree `Uri` | `ImportResult(trackCount)` | `NotPersistable`, `Empty`, `AlreadyImported` | SAF persisted read grant |
| Repo | `LibraryRepository.tracks(tab, query, folder?)` | `LibraryTab`, query ≤ 100 chars, optional folder | `Flow<PagingData<TrackRow>>` | none | none |
| Repo | `LibraryRepository.groups(tab)` | `FOLDERS`, `ARTISTS` or `ALBUMS` | `Flow<List<GroupRow(name, count)>>` | none | none |
| Repo | `LibraryRepository.details(trackId)` | id | `TrackDetails` (tags + stream info) | `NotFound`, `Unreadable` | as for scan |
| Repo | `QueueRepository.observe()` / `save(items)` / `savePosition(index, ms)` / `load()` | | `Flow<List<QueueItem>>`, `QueueSnapshot` | none | none |
| Repo | `EqRepository.presets()` | | `Flow<List<EqPreset>>` (8 built-in + user) | | |
| Repo | `EqRepository.savePreset(name, gains, preamp)` | name 1–24 chars (trimmed, no control chars) | `EqPreset` | `InvalidName`, `DuplicateName`, `BuiltInName` | |
| Repo | `EqRepository.deletePreset(id)` | user preset id | Unit | `NotFound` | |
| Repo | `SettingsRepository` | typed setters | `Flow<Settings>` | | |
| Repo | `PlaylistFileRepository.save(uri, items)` | `ACTION_CREATE_DOCUMENT` uri | `SaveResult(written)` | `WriteFailed` | SAF grant (one-off) |
| Repo | `PlaylistFileRepository.load(uri)` | `ACTION_OPEN_DOCUMENT` uri | `LoadResult(items, missingCount)` | `TooLarge`, `NotAPlaylist`, `ReadFailed` | SAF grant (one-off) |
| Pure | `M3uCodec.encode(entries)` / `decode(text, baseDir?)` | | `String` / `List<M3uEntry>` | `TooManyLines`, `LineTooLong` | |
| Engine | `DspEngine.update(transform)` | `DspConfig -> DspConfig` | Unit | none (values clamped) | |
| Engine | `DspEngine.spectrum` | | `StateFlow<SpectrumFrame>` | | |
| Engine | `DspEngine.status` | | `StateFlow<DspStatus>` (`ACTIVE`, `BYPASSED`, `UNSUPPORTED_FORMAT`) | | |
| Session | `PlayerConnection` (`MediaController`) | `play`, `pause`, `stop`, `seekTo`, `next`, `previous`, `setQueue(items, start)`, `addToQueue`, `playNext`, `move(from,to)`, `remove(i)`, `setShuffle`, `setRepeat`, `setVolume` | `StateFlow<NowPlaying>` | `NotConnected` (retries connect) | none, same process |
| Service | `CanhAmpPlaybackService` | `MediaSession` commands from the UI, notification, Bluetooth, headset | media notification | | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` |

`NowPlaying` = `(item, isPlaying, positionMs, durationMs, bitrateKbps, sampleRateHz, channelCount, index, queueSize, shuffle, repeat)`.
Position updates come from a ticker at 4 Hz while playing (UI side).

**Manifest permissions** (complete list): `READ_MEDIA_AUDIO`; `READ_EXTERNAL_STORAGE` with `maxSdkVersion=32`;
`FOREGROUND_SERVICE`; `FOREGROUND_SERVICE_MEDIA_PLAYBACK`; `WAKE_LOCK`. No `INTERNET`, `RECORD_AUDIO` or
`POST_NOTIFICATIONS` (media-session notifications are exempt).

## Behavior

### Library: R1–R7

- **B1** On first open of Library, if the permission is missing, show the rationale panel. Request the permission only when "Grant" is tapped. After a permanent denial, the button opens the app's system settings. (R2)
- **B2** A scan queries `MediaStore.Audio.Media` with `IS_MUSIC != 0 OR IS_AUDIOBOOK = 0`. It excludes ringtones, notifications and alarms (`IS_RINGTONE`, `IS_NOTIFICATION`, `IS_ALARM` = 0) and files shorter than 10 s. It runs in one cursor pass on `Dispatchers.IO`. (R1)
- **B3** Map each row to a `tracks` row:
  - `format` from the extension of `DISPLAY_NAME` (else from `MIME_TYPE`);
  - `bitrateKbps` from `BITRATE` on API 30+, else `sizeBytes * 8 / durationMs` rounded;
  - `folderPath` from `RELATIVE_PATH` (29+) or the parent of `DATA`;
  - `filePath` from `DATA` when it is readable.

  Empty title, artist or album fall back as in the data model. (R1)
- **B4** Upsert by `uri` in batches of 500 inside one transaction per batch, setting `scanGeneration = current + 1`. (G2)
- **B5** After a full MediaStore scan, delete `source = MEDIASTORE` rows with an older `scanGeneration`, then store the new generation in DataStore. SAF rows are refreshed per folder the same way (B10). This also repairs a stale index after a backup restore. (R1)
- **B6** A scan runs on app start if the last scan is older than 24 h or the permission was just granted, on Rescan, and after an import. Only one scan runs at a time (a `Mutex`): a second request while one runs joins the running one. (R5)
- **B7** Search: trim the query, cap it at 100 chars, debounce 150 ms, escape `%`, `_` and `\`. Match with `LIKE ? ESCAPE '\'` on `title`, `artist`, `album` and `fileName`. SQLite `LIKE` is ASCII case-insensitive; Vietnamese diacritics must match as stored. Paging uses pages of 50. (R4)
- **B8** `sampleRateHz` is unknown from MediaStore. It is filled when (a) the track is played, from the ExoPlayer `Format.sampleRate`, or (b) Track details opens, from `MediaMetadataRetriever` (`METADATA_KEY_SAMPLERATE` on API 31+). The value is written back to the row. Lists show `—` while unknown. (R1, R9)
- **B9** Tabs:
  - All: title A–Z, case-insensitive.
  - Folders: groups by `folderPath` with counts; tapping a group shows its tracks and a breadcrumb.
  - Artists and Albums: grouped the same way.
  - Recent: `dateAddedSec` DESC, at most 100 rows.

  The counts in tab labels come from the same queries. (R3)
- **B10** Import folder: open `ACTION_OPEN_DOCUMENT_TREE` and call `takePersistableUriPermission(READ)`.
  - Walk the tree with `DocumentsContract` children queries, breadth-first, at most 8 levels deep and 10,000 files, keeping supported extensions only.
  - Rows are inserted with file-name-derived metadata first. Then `TrackMetadataReader` fills tags, duration and bitrate on a background pass, with up to 4 files in parallel.
  - The same tree URI twice gives `AlreadyImported`. A folder with no audio gives `Empty`, and the grant is released.
  - On start, persisted grants that were lost (`contentResolver.persistedUriPermissions`) remove that folder and its tracks. (R5)
- **B11** Track actions:
  - Play now replaces the queue with the current list view (≤ 500 items around the tapped one) and starts at the tapped track.
  - Add to queue appends.
  - Play next inserts at `currentIndex + 1`.
  - Details shows the read-only `TrackDetails` sheet.

  Each action shows a short toast-style deck message. (R6)
- **B12** Storage bar: `StatFs(Environment.getExternalStorageDirectory())` gives total and free. The audio share is the sum of `tracks.sizeBytes`. (R7)
- **B13** A track whose URI no longer opens (`FileNotFoundException`, `SecurityException`) is skipped by the player (B27) and removed from `tracks` on the next scan.
- **B14** The scan and import progress shows in the storage panel as "SCANNING n" and does not block browsing.

### Playback and main deck: R8–R14

- **B20** `CanhAmpPlaybackService` extends `MediaSessionService` with `foregroundServiceType="mediaPlayback"`. It owns the single ExoPlayer built by `PlayerFactory` with:
  - `setAudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MUSIC, handleAudioFocus = true)`;
  - `setHandleAudioBecomingNoisy(true)`;
  - `setWakeMode(C.WAKE_MODE_LOCAL)`;
  - the DSP chain from ADR-0001.

  Media3 provides the notification and lock-screen controls through `DefaultMediaNotificationProvider`. (R8)
- **B21** Audio focus: on a call (permanent or transient loss), pause, and resume on a transient gain. Duck on `CAN_DUCK` (Media3 default). Unplugging wired headphones or Bluetooth disconnecting pauses within 1 s. (R8)
- **B22** Transport:
  - Previous within 3 s of the start goes to the previous item; otherwise it restarts the current one.
  - Stop = pause + seek to 0 + keep the queue.
  - Next on the last item with repeat OFF stops at the end.
  - Open (eject) navigates to Library. (R8)
- **B23** Media buttons (headset single/double/triple press, Bluetooth AVRCP) are handled by `MediaSession` defaults. (R8)
- **B24** Deck display:
  - The timer shows `mm:ss`, or `h:mm:ss` for tracks of 1 h or more. Tapping toggles `ELAPSED` / `REMAINING` (shown as `-mm:ss`), and the choice persists.
  - kbps comes from `Format.bitrate` (or the `tracks` value). kHz shows one decimal.
  - STEREO lights when `channelCount ≥ 2`, otherwise MONO.
  - The track number is the 1-based queue index. (R9)
- **B25** The marquee text is `"*** {index}. {artist} - {title} ({duration}) [{kbps}kbps {stereo|mono} {kHz}kHz] ***"`. It scrolls at 30 dp/s and pauses while the app is backgrounded. With an empty queue it shows `*** CANH AMP *** LOAD A TRACK FROM THE LIBRARY ***`. (R9, R13)
- **B26** Visualizer:
  - Collect `DspEngine.spectrum` only while the Main screen is resumed.
  - BAR mode draws 16 columns of 2 dp segments: the bottom 10 green, the next 4 amber, the top 2 orange, plus a peak-hold dot that decays at 12 dB/s.
  - WAVE mode draws the last 256 mono samples as a polyline.
  - When paused or stopped, the bars fall to the floor within 500 ms.
  - If `visAnimated` is off, draw the static peak-hold only. (R10, R25)
- **B27** If the current item fails to open or decode (`PlaybackException`), show `ERR: CANNOT READ FILE` in the marquee for 3 s and skip to the next item. After 5 failures in a row, stop. (R8)
- **B28** Seek bar drag: preview the time while dragging, and seek once on release. (R11)
- **B29** Volume: `player.volume` (0..1) maps linearly to 0..100%, in steps of 1%. The value persists. (R11)
- **B30** Balance −1..1 in steps of 0.05, with a snap to 0 when |b| < 0.05 (centre detent). `BalanceProcessor` multiplies left by `min(1, 1 − b)` and right by `min(1, 1 + b)`, so −1 silences right and +1 silences left. Mono input is upmixed by the decoder as stereo. (R11)
- **B31** Shuffle and repeat map to `player.shuffleModeEnabled` and `repeatMode` (OFF, ALL, ONE cycle on tap). Both persist in DataStore and are restored before playback resumes. (R12)
- **B32** Now-playing card: embedded artwork from `MediaMetadata.artworkData`, decoded at 64 dp with `inSampleSize`. Otherwise show the placeholder cassette drawable. There is no network loading. (R14)
- **B33** Branding: strings, drawables and the launcher icon use "Canh Amp" only. Header plate: `CANH AMP // MAIN PLAYER`. Chassis plates: `CHASSIS 2.91 // AMP-DEC` (decorative; contains no trademark), footer `SERIAL NO: CA-2026-001` and `CANH AMP`. (R13)

### Equalizer and DSP: R19–R23

- **B40** Bands, in Hz: `[60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000]`. Each is an RBJ peaking filter with Q = 1.41 and gain −12..12 dB, integer steps. A band whose centre is ≥ 0.45 × sample rate is skipped (for example 16 kHz at a 32 kHz rate). (R19)
- **B41** Preamp −12..12 dB is applied before the filters. Bass is a low-shelf at 100 Hz (S = 1), 0..12 dB; treble is a high-shelf at 10 kHz, 0..12 dB. (R19, R22)
- **B42** Processors handle 16-bit PCM, mono or stereo, at 8–96 kHz. For any other input format the chain reports `UNSUPPORTED_FORMAT` and passes audio through. The EQ screen then shows `DSP N/A FOR THIS STREAM`. (R19)
- **B43** Config changes: `DspEngine.update` publishes a new immutable `DspConfig`. Processors pick it up at the next buffer (≤ 20 ms). Coefficients are recomputed then. Filter state carries over, so slider moves do not click; changing the sample rate resets the state. (R19)
- **B44** Bypass (`eqEnabled = false`) skips preamp, bands, bass and treble, and copies the samples bit-exact. Balance, volume and the limiter still apply. The curve draws flat in `outline` colour, and the LED shows BYPASS. (R20)
- **B45** The curve graph is the analytic magnitude response of the current config (bands + preamp + shelves), sampled at 64 log points from 20 Hz to 20 kHz on a ±15 dB scale. It is drawn with a glow stroke and a gradient fill, and recomputes on every change. (R20)
- **B46** Presets: built-ins with the gains from the Stitch HTML (preamp 0):

  | Preset | Gains |
  |--------|-------|
  | Rock | 5,7,3,-1,-2,1,4,7,8,9 |
  | Pop | -1,2,4,5,4,2,-1,-2,-2,-2 |
  | Techno | 7,6,2,-3,-4,0,5,8,8,7 |
  | Flat | 0×10 |
  | Full Bass | 11,10,8,4,0,-2,-4,-6,-7,-8 |
  | Club | 0,2,4,3,3,3,2,0,1,2 |
  | Reggae | 0,1,-1,-4,-1,3,4,2,0,0 |
  | Vocal | -3,-4,-2,4,7,6,3,1,-1,-3 |

  Choosing a preset sets the gains and preamp and writes `eqPresetName`. (R21)
- **B47** Any manual band or preamp change clears `eqPresetName`, and the label shows `[CUSTOM]`. Reset flat applies Flat. (R21)
- **B48** Save preset opens a name dialog.
  - Validation: trim; 1–24 chars; letters, digits, space, `-` and `_` only.
  - It must not equal a built-in name (case-insensitive). If a user preset has the same name, ask "Overwrite?".
  - User presets show after the built-ins, marked `USER`, and can be deleted with a long press and confirm. (R21)
- **B49** Headroom: effective pre-gain = `preamp − max(0, maxBoost)`, where `maxBoost` is the peak of the analytic response from B45 (bands + shelves) in dB. This "auto-preamp" keeps loudness steady and is shown as `GAIN: x.x dB` on the EQ screen. (R23)
- **B50** `LimiterProcessor`:
  - Peak limiter with a −0.1 dBFS ceiling, 0.5 ms attack and 80 ms release, linked across channels.
  - It runs in float after balance and converts back to 16-bit with clamping, so output never exceeds full scale. It runs even in bypass, and is transparent while the signal stays under the ceiling. (R23)
- **B51** Analyser: 1024-point FFT with a Hann window at ≤ 30 fps.
  - 16 log-spaced bands from 50 Hz to 16 kHz; each band's value is the max bin magnitude in dB, normalised over −60..0 dB to 0..1.
  - Smoothing: attack instant, decay 18 dB/s. (R10)
- **B52** All DSP and EQ settings persist in DataStore on change, debounced 300 ms, and are applied on service start before the first buffer. (R19–R22)

### Queue and playlists: R15–R18

- **B55** The queue is the player's media item list. `QueueRepository` mirrors it to `queue_items` on every timeline change, replacing all rows in one transaction, debounced 500 ms, with up to 5,000 items. Adding beyond 5,000 rejects the excess and shows `QUEUE FULL (5000)`. (R15, R17)
- **B56** Position is saved to DataStore every 10 s while playing, on pause, on item transition and in `onTaskRemoved` / `onDestroy`. (R17)
- **B57** Restore on service create: load `queue_items`, set the items, seek to `queueIndex` / `queuePositionMs`, and stay paused. Items whose URI is no longer readable stay listed, greyed with `MISSING`, and are skipped by B27. (R17)
- **B58** The header stats are the track count, the total duration (sum of `durationMs`, `h:mm:ss`) and the selected row's duration. They update with every queue change. (R15)
- **B59** Row states: playing = inverted green with a speaker icon; selected (last tapped, not playing) = amber; others default. A single tap selects; a double tap or the play icon plays. A long press starts a drag (`reorderable`); the drop calls `player.moveMediaItem(from, to)`. (R15, R16)
- **B60** Remove: the row's × or DEL removes the selected row. Removing the playing item goes to the next one, or stops if it was the last. Remove shows an UNDO action for 4 s. (R16)
- **B61** +FILE opens Library in pick mode (multi-select, then "ADD n"). +DIR opens the Folders tab in pick mode (adds the whole folder, sorted by file name). (R16)
- **B62** SORT cycles title A–Z → artist A–Z → duration ascending, and is applied as one reorder that keeps the playing item playing. RND shuffles the queue order once; this differs from shuffle mode. The loop button cycles repeat (B31). (R16)
- **B63** The filter is local to the queue view: case-insensitive `contains` on title and artist. It does not change the queue. Clearing it restores the view. (R16)
- **B64** M3U save:
  - Use `ACTION_CREATE_DOCUMENT` with the default name `canh-amp-YYYYMMDD-HHmm.m3u8` and MIME `audio/x-mpegurl`.
  - Write UTF-8 without BOM: `#EXTM3U`, then for each item `#EXTINF:<seconds>,<artist> - <title>` and a path line.
  - The path line is `filePath` when known, else the content URI. (R18)
- **B65** M3U load:
  - Use `ACTION_OPEN_DOCUMENT` (`audio/x-mpegurl`, `audio/mpegurl`, `application/vnd.apple.mpegurl`, `text/plain`).
  - Reject files over 2 MB, or with more than 10,000 lines or a line over 4,096 chars, as `TooLarge` / `NotAPlaylist`. Read UTF-8; for `.m3u` without valid UTF-8, fall back to ISO-8859-1.
  - Skip comments and blank lines.
  - Resolve each entry in order: (1) `content://` → match `tracks.uri`; (2) an absolute path → match `tracks.filePath`; (3) a relative path → resolve against the playlist's directory, derived from the document id `primary:<dir>/x.m3u` → `/storage/emulated/0/<dir>`, then (2); (4) `file://` → strip and use (2).
  - Unresolved entries count as missing. `..` segments are normalised; resolution never opens arbitrary files, only `tracks` lookups.
  - Result: replace the queue (with confirm if the queue is not empty) and show `LOADED n, MISSING m`. (R18)
- **B66** M3U never follows `http(s)` entries: they count as missing, because there is no network (R30).

### Themes and shell: R24–R28

- **B67** `ThemeId` → `DeckColors` palette, one per design card:
  - CLASSIC: charcoal `#292D32`, phosphor `#00FF41`, amber `#F1C100`, steel `#363A3D`.
  - BENTO: slate `#1C2430`, amber/gold `#F1C100` as primary, aluminium `#313538`, ice white `#EBFFE2`.
  - TITANIUM_BLACK: obsidian `#0B0F12`, gunmetal `#313538`, signal red `#FFB4AB` as primary, white `#E0E3E7`.
  - CYBER_NEON: terminal black `#050B07`, matrix `#00FF66`, phosphor `#00FF41`, emerald `#003B14`.

  Each palette maps onto the same token names, so components never hard-code colours. Applying a theme writes DataStore, and `CanhAmpTheme` recomposes immediately. (R24)
- **B68** If "Preserve custom EQ colours" is on, the spectrum and EQ curve keep the CLASSIC green/amber/orange whatever theme is active. (R25)
- **B69** Shell:
  - `DeckHeader`: app mark, `CANH AMP` and the screen subtitle, plus the STEREO/MONO LED.
  - Content area.
  - `MiniPlayerBar`: hidden when the queue is empty; otherwise `[mm:ss] ARTIST - TITLE` with prev / play-pause / next; tapping the text opens Main.
  - `DeckNavBar` with 5 tabs: MAIN, EQ, PLAYLIST, LIBRARY, SKINS.

  Navigation Compose keeps state per tab with `saveState` / `restoreState`. Back from a non-Main tab returns to Main, and back on Main exits. (R26)
- **B70** Accessibility and fidelity:
  - Every control has a ≥ 48 dp hit area and a `contentDescription` / `stateDescription` (for example the shuffle LED: "Shuffle, on").
  - Sliders expose `progressSemantics` with a value text ("1 kilohertz, plus 3 decibels").
  - Font scaling is supported up to 1.3× without clipping the main deck. Text in `VfdText` wraps or ellipsizes, and never overlaps.
  - Animations (marquee, LED pulse, bars) respect `Settings.Global.ANIMATOR_DURATION_SCALE == 0` by staying static. (R27)

### Delivery and security: R28–R30

- **B71** CI `build.yml` fails when:
  - the merged debug manifest contains `INTERNET`, `RECORD_AUDIO` or `MODIFY_AUDIO_SETTINGS`;
  - a case-insensitive grep finds `winamp`, `nullsoft` or `llama` in `app/src/main/res`, `app/src/main/assets` or `app/src/main/java`;
  - a `MissingTranslation` / `ExtraTranslation` lint issue exists (copied from the reference repo). (R13, R28, R30)
- **B72** `build.yml` runs on push to `main` and on PRs to `main`, with `permissions: contents: read`. Steps:
  1. JDK 24.
  2. `testDebugUnitTest`.
  3. Lint translation check.
  4. B71 checks.
  5. Decode `DEBUG_KEYSTORE_BASE64` if present.
  6. `assembleDebug` with `CI_VERSION_CODE=${{ github.run_number }}`.
  7. Upload `canh-amp-${shortsha}.apk` (retention 14 days) and the test reports. (R29, ADR-0004)
- **B73** `release.yml` is copied from the reference repo with the APK named `canh-amp-<version>.apk`, the branch `main` and the version code from the run number. (R29)
- **B74** Strings live in `values/strings.xml` and `values-vi/strings.xml`. Deck labels are uppercase per design. They are translated where meaningful (for example `LIBRARY` / `THƯ VIỆN`); units (KBPS, KHZ, dB) are not. (R28)
- **B75** `Logger` logs ids, counts and error classes only, never file names, titles or URIs (personal data, `security.md`). Release builds log warnings and errors only.
- **B76** Content from files (tags, M3U text, folder names) is untrusted. It is length-capped (tags at 256 chars for display) and rendered as plain text. It is never used to build SQL (Room parameters only) or file paths (B65).

## Non-Functional

| Target | Value | How it is checked |
|--------|-------|-------------------|
| Scan 2,000 MediaStore tracks | ≤ 5 s (G2) | Timed log (count + ms) and a manual check on the owner's phone |
| Cold start to first frame of Main | ≤ 1.5 s, release build (G6) | `adb shell am start -W` on the owner's phone |
| Background playback | 60 min with the screen off (G1) | Owner manual test, checklist in `docs/reports/` |
| DSP CPU | < 3% of one core at 48 kHz stereo | Microbenchmark-style JVM test: process 60 s of audio in < 1.8 s on a CI runner |
| UI frame time | No janky frames while the visualizer runs on Main | `JankStats` logging in debug builds, plus owner observation |
| APK size | ≤ 15 MB (debug) | CI job summary prints the size |
| Memory | Library list paged (50); artwork decoded ≤ 64 dp | Code review |

Security controls: no network permission (B71); SAF grants are persisted only for imported folders and released on
remove (B10); there are no exported components except `MainActivity` (launcher) and the service's
`MediaSessionService` intent filter; all SQL goes through Room parameters (B7, B76); signing keys exist only in GitHub
secrets (ADR-0004).

Observability: `Logger` with tags `Scan`, `Player`, `Dsp`, `M3u` and `Queue`. There is no remote telemetry (R30).

## Testing

| Layer | What | Behaviours |
|-------|------|------------|
| JVM unit (CI) | `BiquadDesign` + `EqualizerProcessor`: magnitude at each band centre within ±1 dB of the set gain; neighbour bands < 1 dB at 1 kHz; band skip at low sample rates | B40–B43 |
| JVM unit | Bypass null test (output == input byte-for-byte) | B44 |
| JVM unit | Limiter: all bands +12 on a 0 dBFS sine, no sample at or above 32767; transparent at −6 dBFS | B49, B50 |
| JVM unit | `BalanceProcessor` at −1 / 0 / +1 | B30 |
| JVM unit | `Fft` + `SpectrumAnalyzer`: sine peaks in the right band; silence at the floor | B51 |
| JVM unit | `M3uCodec` round trip; EXTINF parsing; relative / absolute / `file://` / http; limits; ISO-8859-1 fallback; `..` normalisation | B64–B66 |
| JVM unit | Preset logic: CUSTOM on edit, name validation, built-in collision | B46–B48 |
| JVM unit | MediaStore row → `TrackEntity` mapping (fake cursor rows): fallbacks, format, bitrate fallback | B3 |
| JVM unit | `LikeEscaper`, duration formatting, marquee text builder | B7, B24, B25 |
| JVM unit | ViewModels with fake repositories (`kotlinx-coroutines-test`): timer toggle, queue stats, sort orders, filter | B24, B58, B62, B63 |
| Instrumented (manual run, not CI) | Room DAOs + migration helper; scan generation delete; queue save/restore | B4, B5, B55–B57 |
| Instrumented (manual) | Compose UI: nav and back behaviour, mini player toggle, preset selection updates the label | B47, B69 |
| Manual on device (owner) | Checklist: background 60 min, unplug pause, call interruption, lock-screen controls, Bluetooth buttons, SAF import, M3U in another player | B20–B23, B10, B64 |

The gate for each commit is `./gradlew testDebugUnitTest lintDebug`. `sdlc.config.json` gets an `android` component with
these gates during the first build task.

## Rollout

- Branch `main`. Each push produces a debug APK artifact (`com.canhamp.debug`) that installs over the previous one (ADR-0004). The owner tests from these.
- Milestones, as `swarm-plan` will refine them:
  1. M1 skeleton: CI, theme, shell, nav.
  2. M2 library and playback.
  3. M3 queue and M3U.
  4. M4 DSP and EQ.
  5. M5 visualizer and deck polish.
  6. M6 themes.
  7. M7 release v0.1.0 via `release.yml`.
- Database v1 ships in M2. Schema changes after M7 need migrations (ADR-0003).
- Rollback: install the previous artifact or release APK. If the DB version went up, that needs an uninstall, so schema bumps are avoided between test builds once the owner has data they care about. There are no remote flags.

## Trace: requirement → behaviour → sequence

| R | Behaviours | Sequence diagram |
|---|------------|------------------|
| R1 | B2, B3, B4, B5, B8 | 3 |
| R2 | B1 | 3 |
| R3 | B9 | — (UI query) |
| R4 | B7 | — |
| R5 | B6, B10, B14 | 3, 7 |
| R6 | B11 | 1 |
| R7 | B12 | — |
| R8 | B20, B21, B22, B23, B27 | 1, 6 |
| R9 | B8, B24, B25 | 1 |
| R10 | B26, B51 | 4 |
| R11 | B28, B29, B30 | 4 |
| R12 | B31 | 2 |
| R13 | B25, B33, B71 | — (CI check) |
| R14 | B32 | 1 |
| R15 | B55, B58, B59 | 2 |
| R16 | B59, B60, B61, B62, B63 | — |
| R17 | B55, B56, B57 | 2 |
| R18 | B64, B65, B66 | 5 |
| R19 | B40, B41, B42, B43, B52 | 4 |
| R20 | B44, B45 | 4 |
| R21 | B46, B47, B48 | — |
| R22 | B41 | 4 |
| R23 | B49, B50 | 4 |
| R24 | B67 | — |
| R25 | B26, B68 | — |
| R26 | B69 | 1 |
| R27 | B70 | — |
| R28 | B71, B74 | — |
| R29 | B72, B73 | — |
| R30 | B66, B71, B75 | — |

Every B-number above is cited by at least one R, except B13, B75 and B76, which are robustness and security rules from
`security.md` and support R1, R18 and R30.
