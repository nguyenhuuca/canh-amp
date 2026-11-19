# Design Spec: Settings

**Status:** Approved (2026-11-19, owner request) · **Spec:** docs/specs/spec-retro-player.md · **Date:** 2026-11-19

## Purpose

A single place for app-level options. It replaces the SKINS tab in the bottom nav with SETTINGS. The most important actions are changing the skin and seeing what is new in this version.

## Layout

1. **Shell:** the bottom-nav tab is `SETTINGS` (gear icon), so the tabs are MAIN · EQ · PLAYLIST · LIBRARY · SETTINGS. This changes spec B69 and R26. The header subtitle reads `SETTINGS`.
2. **Tab row:** deck-style LED chips that scroll horizontally. Initial tabs:
   - `SKIN`
   - `RELEASE NOTES`

   The tabs are defined by the `SettingsTab` enum, so more can be added later. The selected tab survives navigation.
3. **SKIN tab:** the content of `design-spec-skins.md`, unchanged.
4. **RELEASE NOTES tab:**
   - A VFD well showing `CANH AMP vX.Y.Z (code)` from `BuildConfig`.
   - Then one deck module per version section, newest first.
   - Notes are bundled in `res/raw/release_notes.md` (EN) and `res/raw-vi/release_notes.md` (VI). The app has no network permission (R30), so nothing is fetched online.

## States

| State | What the user sees |
|-------|--------------------|
| Release notes loading | Version well only |
| Release notes unreadable | Version well plus an `—` module |
| Populated | Version well plus one section per version |

## Content (keys)

| Key | EN | VI |
|-----|----|----|
| `nav_settings` / `subtitle_settings` | SETTINGS | CÀI ĐẶT |
| `settings_tab_skin` | SKIN | GIAO DIỆN |
| `settings_tab_release_notes` | RELEASE NOTES | GHI CHÚ PHÁT HÀNH |
| `settings_version` | VERSION %1$s (%2$d) | PHIÊN BẢN %1$s (%2$d) |

## Accessibility

- Tabs use `Role.Tab` with a selected state and a hit area of at least 48 dp.
- Release-note sections are read as heading plus bullets.

## Maintenance

Add a `## <version> — <date>` section to both release-notes files in the same commit that bumps the version, before running `release.yml`.

## Language tab (added 2026-11-19, owner request)

- The third tab is `LANGUAGE`, with three radio options:
  - `SYSTEM DEFAULT` / `THEO HỆ THỐNG`
  - `ENGLISH`
  - `TIẾNG VIỆT`

  The last two are always shown in their own language.
- It is applied through AppCompat per-app locales (`AppCompatDelegate.setApplicationLocales`). On Android 13+ the app also shows up in the system "App languages" settings (`locales_config.xml`).
- The choice persists across restarts, stored by AppCompat with `autoStoreLocales`.
- Keys: `settings_tab_language` (LANGUAGE / NGÔN NGỮ), `settings_lang_system`.
