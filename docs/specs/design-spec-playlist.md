# Design Spec: Playlist Queue

**Status:** Approved (2026-11-18) · **Spec:** docs/specs/spec-retro-player.md · **Date:** 2026-11-18 · **Mockup:** `docs/design/stitch/04-playlist-queue.png`

## Purpose

See and edit what plays next, and save or load playlists. Most important action: tap a track to play it.

## Layout

1. **LCD header well:** `CANH AMP PLAYLIST EDITOR v0.1` with a pulse LED, then a metrics bar: `TRACKS: n / TIME: h:mm:ss / SELECTED: mm:ss` (B58).
2. **Filter deck:** a recessed search field (`FILTER QUEUE (ARTIST / TITLE)...`) with clear, plus RND (one-off shuffle) and a loop button (repeat cycle).
3. **Terminal list well:** header row `# | TRACK TITLE / FILE | RATE | TIME`; rows in Space Mono 12 sp (`001`-style index, title, bitrate badge, time, ×). The list fills the remaining height in a `LazyColumn` (the mockup's fixed 380 px is replaced) with drag-to-reorder.
4. **Action grid:** 3 × 2 beveled buttons: `+ FILE` (LOCAL MP3), `+ DIR` (FOLDER), `- DEL` (REMOVE), `MISC` (TAG / INFO: details of the selected row), `LIST >>` (SAVE / LOAD menu), `SORT` (A-Z / ARTIST / TIME).
5. **Path pill:** the folder of the playing track (`/MUSIC/...`) and `READ ONLY`. The mockup's `READ/WRITE` is wrong, because the app never writes audio files.
6. **Export banner:** `PLAYLIST EXPORT ENGINE: Supports .M3U, .M3U8`.

## States

| State | What the user sees |
|-------|--------------------|
| Empty | List well shows `QUEUE EMPTY` and a `+ ADD FROM LIBRARY` button; metrics show 0 / 0:00 / --:-- |
| Filter with no matches | `NO MATCH FOR "x"` |
| Missing file row | Greyed text, `MISSING` badge, time shown; skipped on play (B57) |
| Playing row | Inverted green background, speaker icon in place of the index |
| Selected row | Amber text on `surface-container-highest` |
| Dragging | Row lifted (elevation, phosphor outline); others shift |
| Load result | Deck toast `LOADED n, MISSING m` / `NOT A VALID PLAYLIST` / `FILE TOO LARGE` |
| Queue full | Toast `QUEUE FULL (5000)` |

## Interactions

- Single tap selects; double tap or tapping the index plays (B59). A long press starts the drag.
- × or DEL removes, with a 4 s UNDO in the toast (B60).
- `LIST >>` opens a popup: SAVE AS M3U8, LOAD M3U, CLEAR QUEUE (confirm).
- SORT cycles its mode with each tap, and the button sublabel shows the current mode (B62).

## Content (keys)

| Key | EN | VI |
|-----|----|----|
| `pl_title` | CANH AMP PLAYLIST EDITOR | TRÌNH SOẠN DANH SÁCH |
| `pl_tracks` / `pl_time` / `pl_selected` | TRACKS: / TIME: / SELECTED: | SỐ BÀI: / THỜI GIAN: / ĐANG CHỌN: |
| `pl_filter_hint` | FILTER QUEUE (ARTIST / TITLE)... | LỌC (CA SĨ / TÊN BÀI)... |
| `pl_empty` / `pl_add_from_library` | QUEUE EMPTY / + ADD FROM LIBRARY | DANH SÁCH TRỐNG / + THÊM TỪ THƯ VIỆN |
| `pl_no_match` | NO MATCH FOR "%1$s" | KHÔNG CÓ KẾT QUẢ "%1$s" |
| `pl_missing` | MISSING | THIẾU FILE |
| `pl_add_file` / `pl_add_dir` / `pl_del` / `pl_misc` / `pl_list` / `pl_sort` | + FILE / + DIR / - DEL / MISC / LIST >> / SORT | + FILE / + THƯ MỤC / - XÓA / KHÁC / DS >> / SẮP XẾP |
| `pl_save_m3u` / `pl_load_m3u` / `pl_clear` | SAVE AS M3U8 / LOAD M3U / CLEAR QUEUE | LƯU M3U8 / MỞ M3U / XÓA HẾT |
| `pl_loaded` | LOADED %1$d, MISSING %2$d | ĐÃ MỞ %1$d, THIẾU %2$d |
| `pl_invalid` / `pl_too_large` / `pl_full` | NOT A VALID PLAYLIST / FILE TOO LARGE / QUEUE FULL (%1$d) | FILE KHÔNG HỢP LỆ / FILE QUÁ LỚN / ĐẦY (%1$d) |
| `pl_replace_confirm` | REPLACE CURRENT QUEUE? | THAY DANH SÁCH HIỆN TẠI? |
| `undo` | UNDO | HOÀN TÁC |

## Accessibility

- Each row is announced as "3 of 18, Darude, Sandstorm, 3 minutes 45, 320 kilobits, selected".
- Reorder without drag: custom accessibility actions "Move up", "Move down" and "Remove".
- The × hit area is 48 dp, even though the icon is 16 dp.
