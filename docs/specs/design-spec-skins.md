# Design Spec: Skins (themes)

**Status:** Approved (2026-11-18) · **Spec:** docs/specs/spec-retro-player.md · **Date:** 2026-11-18 · **Mockup:** `docs/design/stitch/03-skins-browser.png`

## Purpose

Change the deck's colour theme. Most important action: APPLY SKIN.

## Layout

1. **Skin engine banner:** a palette icon with `SKIN SELECTOR // RETRO ENGINE`, subtitle `BUILT-IN THEME DECK` (replacing the mockup's `.WSZ / .WAL` archive text, since import is out of scope) and an `ENG: v0.1` badge. Below it, a "Mounted active deck" well with a mini preview icon, the active theme name and description, and `ACTIVE`. The mockup's IMPORT .WSZ and ARCHIVE buttons are omitted.
2. **Filter chips:** `ALL SKINS (4)`, `CLASSIC`, `MODERN`, `CYBER / NEON`, `DARK / OLED`. Each theme has one category.
3. **Theme cards** (4), one per palette (spec B67): `ID: 0n // <TYPE>` with the name and a tag badge; a **live** mini preview drawn with that palette (top bar, LCD well with the current track's time and title or the idle text, the mini 6-bar spectrum from the live `DspEngine.spectrum` when animated, and a mini transport row); palette swatches; a meta line (style description); and `APPLY SKIN` / `APPLIED` (with check). The mockup's star ratings and fake authors are dropped.
4. **Engine preferences:** `Preserve custom EQ colours` (`ENABLED` / `DISABLED`) and `Allow animated visualizer` (`ENABLED` / `DISABLED`) (B68, B26). Window scaling is omitted. Footer: `4 SKINS • BUILT-IN`.

| Card | Name | Tag | Category | Description |
|------|------|-----|----------|-------------|
| 01 | Classic Deck | CLASSIC | CLASSIC | Phosphor green on charcoal, amber accents |
| 02 | Modern Bento | MODERN | MODERN | Slate and brushed aluminium, gold LEDs |
| 03 | Titanium Black | HIGH CONTRAST | DARK / OLED | Pure black for OLED, signal-red accents |
| 04 | Cyber Neon Matrix | ANIMATED | CYBER / NEON | Terminal green on black, scanline glow |

## States

| State | What the user sees |
|-------|--------------------|
| Default | Classic applied; its card shows `[CURRENT ACTIVE]` and `APPLIED` |
| Filtered | Only matching cards; chip LED lit |
| Applying | Instant recomposition of the whole app (no loading state) |

## Interactions

- APPLY SKIN writes the theme (B67). The whole UI, including nav and mini player, switches immediately, and the card becomes APPLIED.
- Preference rows toggle on tap anywhere in the row.

## Content (keys)

| Key | EN | VI |
|-----|----|----|
| `skins_title` / `skins_subtitle` | SKIN SELECTOR // RETRO ENGINE / BUILT-IN THEME DECK | CHỌN GIAO DIỆN / BỘ GIAO DIỆN CÓ SẴN |
| `skins_active` / `skins_current` | ACTIVE / [CURRENT ACTIVE] | ĐANG DÙNG / [ĐANG DÙNG] |
| `skins_apply` / `skins_applied` | APPLY SKIN / APPLIED | ÁP DỤNG / ĐÃ ÁP DỤNG |
| `skins_all` / chip names | ALL SKINS (%1$d) / CLASSIC / MODERN / CYBER / NEON / DARK / OLED | TẤT CẢ (%1$d) / CỔ ĐIỂN / HIỆN ĐẠI / CYBER / NEON / TỐI / OLED |
| `skins_pref_eq_colors` / `skins_pref_anim_vis` | Preserve custom EQ colours / Allow animated visualizer | Giữ màu EQ gốc / Cho phép visualizer chuyển động |
| `enabled` / `disabled` | ENABLED / DISABLED | BẬT / TẮT |
| Theme names and descriptions | as the table above | names unchanged; descriptions translated |

## Accessibility

- Each card is a group: "Titanium Black, high contrast theme, not applied" with an Apply button.
- The previews are decorative.
- The palette contrast test from the shell spec applies to all four themes; Titanium Black's red `#FFB4AB` on `#0B0F12` is about 11:1.
