# Design Spec: App shell and design system

**Status:** Approved (2026-11-18) · **Spec:** docs/specs/spec-retro-player.md · **Date:** 2026-11-18

## Purpose

Give every screen the same rack-mount chassis: a header plate, the scrolling module stack, the mini player and the
5-tab nav. The one action that matters most is controlling playback from anywhere (mini player).

## Layout

Phone portrait, 360–430 dp wide; mockups `docs/design/stitch/*.png` at 390 dp. Top to bottom:

1. **System status bar** (edge-to-edge, `surface-container` behind it). The fake status row from the mockup is dropped (PRD R26).
2. **DeckHeader**, 56 dp: app mark (vector, phosphor), title `CANH AMP` (`title-md`, phosphor glow) over the screen subtitle (`label-sm`), and on the right the STEREO/MONO LED pill. The avatar is dropped (no accounts).
3. **Content**: a vertical scroll of `DeckModule`s, 12 dp side margins, 8 dp gaps.
4. **MiniPlayerBar**, 44 dp, above the nav. Hidden while the queue is empty.
5. **DeckNavBar**, 64 dp + gesture inset: MAIN, EQ, PLAYLIST, LIBRARY, SKINS. The active tab is recessed (`surface-container-highest`, inner shadow, phosphor text).

Landscape and tablets use the same single column, centred, max width 480 dp (PRD non-goal: dual chassis).

## Design tokens (`ui.theme`)

- **Colours:** named tokens from `docs/design/stitch/README.md`, as `DeckColors`. Theme palettes swap the values (spec B67).
- **Type:** `DeckTypography` with Space Mono (display, title, labels; labels always uppercase) and Space Grotesk (body). Fonts are bundled in `res/font` with `OFL.txt` in `assets/licenses/`.
- **Shapes:** 4 dp radius on modules and buttons, matching the PNGs (PRD Q3); LEDs are circles.
- **Depth primitives:**
  - `RecessedWell`: `surface-container-lowest` with a top inner shadow `0 2 4 #000 @85%`.
  - `DeckModule`: raised; 1 dp top/left highlight `#4A525D`, 1 dp bottom/right `#0F1112`.
  - `TactileButton`: vertical gradient `#383E46→#202326`. Pressed inverts the gradient, offsets content by 1 dp and adds a phosphor glow on the icon.
- **Glow:** text glow for `VfdText` is done with `Shadow(color = primary @ 60%, blurRadius = 6dp)`.

## States

| State | What the user sees |
|-------|--------------------|
| Empty (no queue) | Mini player hidden; Main shows the idle marquee (spec B25) |
| Loading (service connecting) | Mini player and deck show `--:--` and dimmed LEDs; there is no spinner |
| Error (session not connected after 3 s) | Deck message `ERR: AUDIO ENGINE OFFLINE`; reconnect retries every 2 s |
| Populated | Mini player shows `[mm:ss] ARTIST - TITLE` with prev / play-pause / next |

## Interactions

- Tap a tab to switch, keeping per-tab scroll state. System back returns to MAIN; back on MAIN exits.
- Mini player: tapping the text opens MAIN; the buttons act directly, with haptic `KEYBOARD_TAP` on press.
- Decorative screws and serial plates are not focusable.

## Content (translation keys, EN / VI)

| Key | EN | VI |
|-----|----|----|
| `app_name` | Canh Amp | Canh Amp |
| `nav_main` | MAIN | CHÍNH |
| `nav_eq` | EQ | EQ |
| `nav_playlist` | PLAYLIST | DANH SÁCH |
| `nav_library` | LIBRARY | THƯ VIỆN |
| `nav_skins` | SKINS | GIAO DIỆN |
| `led_stereo` / `led_mono` | STEREO / MONO | STEREO / MONO |
| `err_engine_offline` | ERR: AUDIO ENGINE OFFLINE | LỖI: MÁY PHÁT CHƯA SẴN SÀNG |
| `cd_play` / `cd_pause` / `cd_next` / `cd_previous` | Play / Pause / Next track / Previous track | Phát / Tạm dừng / Bài tiếp / Bài trước |

## Accessibility

- Every interactive element has a hit area of at least 48 dp (`Modifier.minimumInteractiveComponentSize()`), even when its visual is 28 dp.
- Contrast: phosphor `#00FF41` on `#0B0F11` is about 13:1; dim labels `#B9CCB2` on `#1C2023` about 9:1. Theme palettes must keep body text at ≥ 4.5:1, which a unit test checks per palette.
- TalkBack: the nav tabs are `Role.Tab` with a selected state; LED toggles use `Role.Switch` with a state description.
- Reduced motion: when the animator scale is 0, the marquee, LED pulse and glow animation are static (spec B70).
