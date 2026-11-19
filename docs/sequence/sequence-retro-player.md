# Sequence Diagrams: Canh Amp, retro MP3 player

- **PRD:** [PRD-retro-player](../prd/PRD-retro-player.md)
- **ADR:** [0001](../adr/0001-audio-dsp-pipeline.md), [0002](../adr/0002-visualizer-pcm-tap.md), [0003](../adr/0003-persistence-room-datastore.md), [0004](../adr/0004-app-id-sdk-and-ci-signing.md)
- **Spec:** [spec-retro-player](../specs/spec-retro-player.md)
- **Status:** Approved design (2026-11-18). Mark "As-built" once implemented.

## 1. Play a track from the Library (main path)

The user taps Play on a library row. The queue is replaced and playback starts with the DSP chain, the notification
and the deck updates (B11, B20, B24).

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant LS as LibraryScreen
    participant LVM as LibraryViewModel
    participant PC as PlayerConnection (MediaController)
    participant SVC as CanhAmpPlaybackService
    participant EXO as ExoPlayer
    participant DSP as DSP chain (Equalizer→Balance→Limiter→SpectrumTap)
    participant QR as QueueRepository
    participant DB as Room (queue_items)
    participant MS as MainScreen / MiniPlayerBar

    U->>LS: tap ▶ on row i
    LS->>LVM: playNow(trackId)
    LVM->>LVM: build items = visible list window (≤500), start = i
    LVM->>PC: setQueue(items, start) + play()
    PC->>SVC: MediaSession setMediaItems / play
    SVC->>EXO: setMediaItems(items, start, 0), then prepare(), then play()
    EXO->>EXO: request audio focus (USAGE_MEDIA)
    EXO->>DSP: 16-bit PCM buffers
    DSP-->>EXO: processed PCM (limited ≤ -0.1 dBFS)
    SVC-->>U: media notification (foreground, mediaPlayback)
    EXO-->>SVC: onTimelineChanged / onMediaItemTransition
    SVC->>QR: mirror(items) (debounced 500 ms)
    QR->>DB: REPLACE queue_items in one txn
    EXO-->>PC: onTracksChanged(Format: bitrate, sampleRate, channels)
    PC-->>MS: NowPlaying(item, isPlaying=true, kbps, kHz, STEREO)
    MS-->>U: timer, marquee, LEDs, mini player update
```

## 2. App start: restore the queue, position and modes

The service is created when the UI connects. It restores the last session, paused (B31, B56, B57).

```mermaid
sequenceDiagram
    autonumber
    participant MA as MainActivity
    participant PC as PlayerConnection
    participant SVC as CanhAmpPlaybackService
    participant SET as SettingsRepository (DataStore)
    participant QR as QueueRepository
    participant DB as Room (queue_items)
    participant DE as DspEngine
    participant EXO as ExoPlayer

    MA->>PC: connect() (SessionToken)
    PC->>SVC: bind → onCreate()
    SVC->>SET: first(settings)
    SET-->>SVC: shuffle, repeat, volume, balance, eq*, queueIndex, queuePositionMs
    SVC->>DE: update(config from settings)
    SVC->>QR: load()
    QR->>DB: SELECT * FROM queue_items ORDER BY position
    DB-->>QR: rows
    QR-->>SVC: QueueSnapshot(items)
    SVC->>EXO: setMediaItems(items, queueIndex, queuePositionMs), then prepare()
    SVC->>EXO: shuffleModeEnabled, repeatMode, volume
    Note over EXO: stays paused (playWhenReady=false)
    SVC-->>PC: session connected
    PC-->>MA: NowPlaying(index, positionMs, isPlaying=false)
    Note over SVC,SET: while playing: savePosition every 10 s, on pause, on transition, onTaskRemoved
```

## 3. Library scan: permission denied, then granted

The first open of Library has no permission. The user grants it, and the scan fills the index (B1–B6).

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant LS as LibraryScreen
    participant LVM as LibraryViewModel
    participant OS as Android permission UI
    participant LR as LibraryRepository
    participant MSS as MediaStoreScanner
    participant MSP as MediaStore provider
    participant DB as Room (tracks)
    participant SET as SettingsRepository

    LS->>LVM: onStart
    LVM->>LVM: check READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE
    LVM-->>LS: state = PermissionNeeded (rationale panel)
    U->>LS: tap GRANT
    LS->>OS: requestPermission
    alt denied
        OS-->>LS: denied
        LS-->>U: rationale stays, if permanently denied → button opens app settings
    else granted
        OS-->>LS: granted
        LS->>LVM: onPermissionGranted
        LVM->>LR: scan() (Mutex, joins a running scan)
        LR->>SET: read scanGeneration g
        LR->>MSS: query(projection, music & not ringtone/alarm, ≥10 s)
        MSS->>MSP: ContentResolver.query(Audio.Media)
        MSP-->>MSS: cursor
        loop per 500 rows
            MSS-->>LR: List<TrackEntity> (generation g+1)
            LR->>DB: UPSERT by uri (txn)
        end
        LR->>DB: DELETE MEDIASTORE rows WHERE scanGeneration ≤ g
        LR->>SET: scanGeneration = g+1
        LR-->>LVM: ScanResult(added, updated, removed, total)
        LVM-->>LS: paged list + "INDEXED: n" + storage bar
    end
```

## 4. EQ change: audio and visualizer update

The user moves the 1K band. The config is published lock-free, the audio thread picks it up at the next buffer, and the
curve and spectrum reflect it (B43–B52, ADR-0001/0002).

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant EQ as EqScreen
    participant EVM as EqViewModel
    participant DE as DspEngine
    participant EP as EqualizerProcessor (audio thread)
    participant LP as LimiterProcessor
    participant TAP as SpectrumTapProcessor
    participant SA as SpectrumAnalyzer (Default dispatcher)
    participant SET as SettingsRepository
    participant MS as MainScreen (SpectrumView)

    U->>EQ: drag 1K fader to +3
    EQ->>EVM: setBand(4, +3)
    EVM->>DE: update(gains[4] = 3, presetName = empty)
    DE->>DE: AtomicReference.set(DspConfig v+1), autoPreamp from analytic peak
    EVM-->>EQ: curve points (64), label [CUSTOM], GAIN x.x dB
    EVM->>SET: persist eqGains, eqPresetName (debounced 300 ms)
    Note over EP: next buffer (≤ 20 ms)
    EP->>DE: read config v+1
    EP->>EP: recompute coefficients (keep filter state)
    EP->>LP: filtered PCM (float)
    LP->>TAP: limited PCM 16-bit
    TAP->>TAP: copy mono samples → RingBuffer
    TAP-->>EP: pass-through to AudioTrack
    loop ≤ 30 fps while MainScreen is collecting
        SA->>TAP: read latest 1024 samples
        SA->>SA: Hann + FFT → 16 log bands, decay 18 dB/s
        SA-->>MS: StateFlow<SpectrumFrame>
    end
```

## 5. Load an M3U playlist with missing entries

The user loads a `.m3u` file. Entries resolve only through the library index; unresolved ones are counted (B65, B66, B76).

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant PL as PlaylistScreen
    participant PVM as PlaylistViewModel
    participant SAF as System file picker
    participant PFR as PlaylistFileRepository
    participant CODEC as M3uCodec
    participant DB as Room (tracks)
    participant PC as PlayerConnection

    U->>PL: LIST >> → LOAD
    PL->>SAF: ACTION_OPEN_DOCUMENT (m3u mime types)
    SAF-->>PL: uri
    PL->>PVM: load(uri)
    PVM->>PFR: load(uri)
    PFR->>PFR: size check (≤ 2 MB) else TooLarge
    PFR->>CODEC: decode(text, baseDir from document id)
    alt > 10,000 lines or a line > 4,096 chars
        CODEC-->>PFR: NotAPlaylist
        PFR-->>PVM: error
        PVM-->>PL: "NOT A VALID PLAYLIST"
    else ok
        CODEC-->>PFR: entries (content / absolute / relative→abs / http)
        loop each entry
            PFR->>DB: SELECT by uri or filePath (parameterised)
            DB-->>PFR: track or null (http → missing)
        end
        PFR-->>PVM: LoadResult(items, missing=m)
        PVM-->>PL: confirm "REPLACE QUEUE?" (if queue not empty)
        U->>PL: confirm
        PVM->>PC: setQueue(items, 0)
        PVM-->>PL: "LOADED n, MISSING m"
    end
```

## 6. Interruptions: phone call and headphones unplugged

Audio focus and becoming-noisy handling come from Media3 defaults configured in `PlayerFactory` (B21).

```mermaid
sequenceDiagram
    autonumber
    participant SYS as Android audio system
    participant EXO as ExoPlayer
    participant SVC as CanhAmpPlaybackService
    participant QR as QueueRepository / Settings
    participant UI as MainScreen

    SYS-->>EXO: AUDIOFOCUS_LOSS_TRANSIENT (incoming call)
    EXO->>EXO: pause (playWhenReady kept, suppression=TRANSIENT_AUDIO_FOCUS_LOSS)
    EXO-->>SVC: onIsPlayingChanged(false)
    SVC->>QR: savePosition(index, ms)
    EXO-->>UI: isPlaying=false (bars fall to floor ≤ 500 ms)
    SYS-->>EXO: AUDIOFOCUS_GAIN (call ended)
    EXO->>EXO: resume
    EXO-->>UI: isPlaying=true
    SYS-->>EXO: ACTION_AUDIO_BECOMING_NOISY (headphones out)
    EXO->>EXO: pause (playWhenReady=false)
    EXO-->>SVC: onIsPlayingChanged(false)
    SVC->>QR: savePosition
    Note over SVC: notification stays, service may leave foreground after Media3 timeout
```

## 7. Import a folder through SAF (failure: empty folder)

The user imports a folder that MediaStore misses (B10).

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant LS as LibraryScreen
    participant LVM as LibraryViewModel
    participant SAF as System folder picker
    participant LR as LibraryRepository
    participant SFS as SafFolderScanner
    participant TMR as TrackMetadataReader
    participant DB as Room (saf_folders, tracks)

    U->>LS: + IMPORT FOLDER
    LS->>SAF: ACTION_OPEN_DOCUMENT_TREE
    SAF-->>LS: treeUri
    LS->>LVM: importFolder(treeUri)
    LVM->>LR: importFolder(treeUri)
    LR->>DB: exists(treeUri)?
    alt already imported
        LR-->>LVM: AlreadyImported
    else new
        LR->>LR: takePersistableUriPermission(READ)
        LR->>SFS: walk(treeUri, depth ≤ 8, files ≤ 10,000, audio ext only)
        alt no audio files
            SFS-->>LR: []
            LR->>LR: releasePersistableUriPermission
            LR-->>LVM: Empty
            LVM-->>LS: "NO AUDIO IN FOLDER"
        else n files
            SFS-->>LR: documents
            LR->>DB: INSERT saf_folders, then INSERT tracks (name-derived metadata)
            LR-->>LVM: ImportResult(n)
            LVM-->>LS: list shows n new tracks
            par background, 4 in parallel
                LR->>TMR: read(tags, duration, bitrate)
                TMR-->>LR: metadata
                LR->>DB: UPDATE tracks
            end
        end
    end
```
