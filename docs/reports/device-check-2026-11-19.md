# Device check: Canh Amp v0.1.0 candidate

**Build:** CI artifact `canh-amp-<sha>` from the latest green run on `main` (Actions tab) · **Tester:** owner · **Device / Android version:** ______

Install the APK. The debug build installs as `com.canhamp.debug`. Tick each line or note what happened.

## Goals (PRD G1–G6)

- [ ] **G1** Play a queue with the screen off for 60 min. Playback does not stop.
- [ ] **G2** Library → RESCAN on the real library. Time to a filled list: ___ s for ___ tracks (target ≤ 5 s / 2,000).
- [ ] **G6** Cold start (swipe the app away, then open it) to the Main deck: feels ≤ 1.5 s.
- [ ] **G4** EQ: play a 1 kHz tone (or any track) and move the 1K fader to +6. The change is clearly audible. BYPASS returns to the original sound.

## Playback (R8–R12)

- [ ] The notification and lock-screen controls work (play/pause, next, previous).
- [ ] Unplugging wired headphones or disconnecting Bluetooth pauses playback.
- [ ] An incoming call pauses playback, and it resumes after the call.
- [ ] The Bluetooth headset buttons work.
- [ ] Main deck: the timer toggles between elapsed and remaining; KBPS/KHZ/STEREO are correct; the marquee scrolls.
- [ ] The spectrum bars move with the music and fall when paused. MODE switches BAR/WAVE.
- [ ] Volume, balance (fully left = right channel silent), shuffle and repeat (OFF/ALL/1) work.
- [ ] **FLAC 16-bit** plays and the EQ affects it.
- [ ] **FLAC 24-bit / hi-res** plays and the EQ affects it. The EQ screen does NOT show "DSP N/A".

## Library, playlist, persistence (R1–R7, R15–R18)

- [ ] On first open the app asks for access; denying it shows the rationale, and GRANT works.
- [ ] Tabs, search, folder drill-down and the storage bar all look right.
- [ ] + IMPORT FOLDER (SAF) adds a folder that the scan missed. Long-press → REMOVE FOLDER works.
- [ ] Playlist: drag to reorder, × with UNDO, SORT, RND, filter, +FILE / +DIR pick mode.
- [ ] Kill the app while playing track 3 at 1:20, then reopen it: track 3 is loaded at about 1:20, paused.
- [ ] LIST >> SAVE AS M3U8, then open that file in another player: same tracks, same order.
- [ ] LOAD M3U with a few deleted files shows "LOADED n, MISSING m".

## EQ, skins, accessibility (R19–R28)

- [ ] Presets apply; editing a band shows [CUSTOM]; SAVE PRESET works and the preset survives a restart.
- [ ] The Skins screen applies all 4 themes instantly, and the theme survives a restart.
- [ ] Vietnamese system language shows Vietnamese labels.
- [ ] TalkBack: the transport buttons, faders and tabs are announced with names.

## Result

- Pass / fail: ______
- Issues found (screen, steps, what happened): ______

When everything passes, tell Claude to dispatch `release.yml` with version `0.1.0`.
