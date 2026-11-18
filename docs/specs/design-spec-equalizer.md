# Design Spec: Graphic Equalizer

**Status:** Approved (2026-11-18) · **Spec:** docs/specs/spec-retro-player.md · **Date:** 2026-11-18 · **Mockup:** `docs/design/stitch/02-graphic-equalizer.png`

## Purpose

Shape the sound with 10 bands and presets. Most important action: pick a preset or move a band and hear the change.

## Layout

1. **EQ main unit:**
   - Header plate: `CANH AMP EQUALIZER` / `DSP PRO AUDIO RACK // MOD-10B`, the power LED toggle (`ON` / `BYPASS`) and the PRESET button.
   - Curve well: `> CURVE RESPONSE: [ROCK]` and `GAIN: x.x dB`, then a 64 dp phosphor grid with the curve (B45).
   - Fader deck: a `+12dB / 0dB (FLAT) / -12dB` scale, then an 11-column grid: PRE (amber knob line) plus 60…16K. Each column is a `VFader` 144 dp tall with the value label below (`+5`, `-1`).
2. **Analog harmonic processor:** `BASS BOOST` and `TREBLE` `Knob`s (0..12 dB, ring arc). The 3D SPATIAL card from the mockup is out of scope and replaced by a `BALANCE` mini fader that mirrors Main.
3. **Action row:** RESET FLAT, SAVE PRESET, and a third button for the user presets list (`MY PRESETS`). The mockup's AUTO-EQ is out of scope.
4. **Footnote:** `CANH AMP DSP ENGINE v0.1` and `STATUS: LOCKED {kHz}kHz` (from `NowPlaying.sampleRateHz`).

**Preset drawer:** a bottom sheet in the module style with a 2-column grid of preset buttons (name + tag: CLASSIC, BASS+, 0dB…). The active preset has a green left border and `ACTIVE`; user presets are tagged `USER`, and a long press deletes one.

## States

| State | What the user sees |
|-------|--------------------|
| Default (first run) | Flat preset, ON |
| Bypass | LED off, `BYPASS`; curve flat and grey; faders still movable (edits apply when turned on) |
| DSP unsupported | Pill `DSP N/A FOR THIS STREAM`; controls enabled but note shown (B42) |
| Custom | Label `[CUSTOM]` |
| Save error | Inline under the name field: `NAME 1-24 CHARS`, `NAME IN USE BY BUILT-IN`; or an overwrite confirm |

## Interactions

- Fader drag snaps to 1 dB steps; double tap resets that band to 0. Haptic tick on every dB step and at 0.
- Knob: vertical drag or rotation gesture; double tap resets to 0.
- PRESET opens the drawer, and choosing a preset closes it (B46). SAVE PRESET opens a dialog with a text field, Save and Cancel (B48).

## Content (keys)

| Key | EN | VI |
|-----|----|----|
| `eq_title` | CANH AMP EQUALIZER | BỘ CÂN BẰNG CANH AMP |
| `eq_on` / `eq_bypass` | ON / BYPASS | BẬT / BỎ QUA |
| `eq_preset` / `eq_custom` | PRESET / [CUSTOM] | CÀI SẴN / [TÙY CHỈNH] |
| `eq_curve` / `eq_gain` | CURVE RESPONSE: / GAIN: | ĐƯỜNG CONG: / ĐỘ LỢI: |
| `eq_reset` / `eq_save` / `eq_my_presets` | RESET FLAT / SAVE PRESET / MY PRESETS | VỀ PHẲNG / LƯU / CỦA TÔI |
| `eq_bass` / `eq_treble` | BASS BOOST / TREBLE | TĂNG BASS / TREBLE |
| `eq_name_invalid` / `eq_name_builtin` / `eq_overwrite` | NAME 1-24 CHARS / NAME IN USE BY BUILT-IN / OVERWRITE "%1$s"? | TÊN 1-24 KÝ TỰ / TRÙNG TÊN CÓ SẴN / GHI ĐÈ "%1$s"? |
| `eq_dsp_na` | DSP N/A FOR THIS STREAM | DSP KHÔNG HỖ TRỢ LUỒNG NÀY |
| Preset names | ROCK, POP, TECHNO, FLAT, FULL BASS, CLUB, REGGAE, VOCAL | (same, not translated) |

## Accessibility

- Each fader is a slider with a label "60 hertz" and a value "plus 5 decibels". Volume keys do not move EQ faders.
- Faders are 24 dp wide visually; the 11-column grid at 360 dp gives about 29 dp per column, so each fader's touch area extends vertically to 144 dp and horizontally to the column width. TalkBack swipe up/down adjusts by 1 dB, which works around the narrow width.
- The curve is decorative; the current preset name is announced on change.
