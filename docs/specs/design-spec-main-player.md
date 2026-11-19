# Design Spec: Main Player (deck)

**Status:** Approved (2026-11-18) · **Spec:** docs/specs/spec-retro-player.md · **Date:** 2026-11-18 · **Mockup:** `docs/design/stitch/01-main-player.png`

## Purpose

See what is playing and control it like a hardware deck. Most important action: play/pause.

## Layout (modules top to bottom, as in the mockup)

1. **Chassis plate:** screws, `CHASSIS 2.91 // AMP-DEC`, and a `DSP READY` / `DSP BYPASS` / `DSP N/A` pill from `DspEngine.status`.
2. **VFD display well:**
   - Status row: `CANH AMP` and `PRO V0.1`, plus `AUDIO ON` when playing.
   - Grid of 12 columns:
     - left (5): track number, time mode label, 7-segment-style timer (`display-lg-mobile`, glow), KBPS and KHZ;
     - right (7): `SPECTRUM RTA` with the `MODE: BAR|WAVE` button, the 16-band `SpectrumView` and frequency labels `60 170 310 600 1K 3K 6K 12K 16K`.
   - Marquee strip.
   - Four badges: BITRATE, SAMPLING, CHANNELS STEREO (lit), MONO (dim unless mono).
3. **Song position seek:** a label with `elapsed / duration`, then an `HFader` with a ribbed thumb.
4. ~~**Volume | Balance**~~ removed on owner request (2026-11-19) to shorten the screen. Volume uses the phone's volume keys; balance stays on the EQ screen.
5. **Transport deck:** 6 `TactileButton`s in a row (PREV, PLAY, PAUSE, STOP, NEXT, OPEN). The active PLAY is recessed with a glow. A `MOTOR RUNNING` LED shows while playing.
6. **Toggles:** SHUFFLE and REPEAT (`OFF` / `ALL` / `ONE`, label `REPEAT`, `REPEAT ALL`, `REPEAT 1`) `LedToggle`s, plus a quick button that opens EQ. The mockup's A-B LOOP and ID3 TAG buttons are out of scope: ID3 TAG becomes `INFO` (track details sheet, read-only); A-B LOOP is omitted.
7. **Now-playing card:** 64 dp artwork or the cassette placeholder; a format badge (for example `MPEG-1 LAYER 3`, `FLAC`), title (`title-md`) and artist • album (`body-md`).
8. **Footer plate:** `SERIAL NO: CA-2026-001` and `CANH AMP`.

## States

| State | What the user sees |
|-------|--------------------|
| Empty queue | Timer `--:--`; bars at the floor; idle marquee; transport disabled except OPEN (goes to Library); card shows "NO TRACK LOADED" |
| Loading (preparing) | Timer frozen; `BUFFERING` replaces `AUDIO ON` |
| Error (file unreadable) | Marquee `ERR: CANNOT READ FILE` for 3 s, then auto-skip (B27) |
| Paused | PAUSE recessed; bars decay to the floor; motor LED off |
| Playing | PLAY recessed and glowing; bars live; marquee scrolling |

## Interactions

- Tap the timer to toggle elapsed/remaining (B24). Tap MODE to cycle BAR/WAVE (B26).
- Faders: drag or tap-to-jump. For TalkBack, volume steps 5% per swipe, balance 0.1, seek 5 s.
- Transport press: 1 dp offset + haptic; behaviour per B22.
- REPEAT tap cycles OFF → ALL → ONE (B31).

## Content (keys)

| Key | EN | VI |
|-----|----|----|
| `deck_time_elapsed` / `deck_time_remaining` | TIME [ELAPSED] / TIME [REMAIN] | THỜI GIAN [ĐÃ PHÁT] / [CÒN LẠI] |
| `deck_audio_on` / `deck_buffering` | AUDIO ON / BUFFERING | ĐANG PHÁT / ĐANG TẢI |
| `deck_seek` | SONG POS SEEK | VỊ TRÍ BÀI |
| `deck_volume` / `deck_balance` / `deck_center` | VOLUME / BALANCE / CENTER | ÂM LƯỢNG / CÂN BẰNG / GIỮA |
| `btn_prev` … `btn_open` | PREV, PLAY, PAUSE, STOP, NEXT, OPEN | TRƯỚC, PHÁT, DỪNG, NGỪNG, TIẾP, MỞ |
| `toggle_shuffle` / `toggle_repeat` / `toggle_repeat_all` / `toggle_repeat_one` | SHUFFLE / REPEAT / REPEAT ALL / REPEAT 1 | TRỘN / LẶP / LẶP HẾT / LẶP 1 |
| `deck_no_track` | NO TRACK LOADED | CHƯA CÓ BÀI |
| `err_cannot_read` | ERR: CANNOT READ FILE | LỖI: KHÔNG ĐỌC ĐƯỢC FILE |
| `marquee_idle` | *** CANH AMP *** LOAD A TRACK FROM THE LIBRARY *** | *** CANH AMP *** CHỌN BÀI TỪ THƯ VIỆN *** |

## Accessibility

- The timer has a `liveRegion` set to polite, announced only on toggle, not every second.
- The `SpectrumView` is decorative (`invisibleToUser`); the marquee exposes the full text once as its content description.
- Transport buttons use labels from the shell keys; the active state is announced ("Play, selected").
