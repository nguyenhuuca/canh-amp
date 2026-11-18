# Design Spec: Local Library

**Status:** Approved (2026-11-18) · **Spec:** docs/specs/spec-retro-player.md · **Date:** 2026-11-18 · **Mockup:** `docs/design/stitch/05-local-library.png`

## Purpose

Find music on the phone and send it to the deck. Most important action: tap ▶ on a track.

## Layout

1. **Storage telemetry bay:** the storage root label (`/INTERNAL STORAGE/MUSIC`) and RESCAN (sync icon, spins while scanning); a segmented capacity bar (audio / other used / free) with `CAPACITY USED: x GB / y GB` and `z GB FREE`; a footer `INDEXED: n AUDIO STREAMS` / `SCANNING n` and `DATABASE: V1 OK`.
2. **Search terminal:** a recessed field with a terminal icon (`QUERY: ARTIST / TITLE / FILE...`) and backspace-clear.
3. **Tab rail** (horizontal scroll): `ALL TRACKS (n)`, `FOLDERS`, `ARTISTS (n)`, `ALBUMS (n)`, `RECENT`.
4. **Breadcrumb well** (Folders drill-down only): `/0/Music / Retro_Vault_98/` and `n FILES`; tapping a segment goes up.
5. **List:** track cards (icon by format, `title-md` title, a badge row `.MP3 · 320 KBPS · 44.1 KHZ · 9.4 MB`, a second line `ALBUM: …` and `MOD: date`, then ▶ and ⋮ buttons at 32 dp visual / 48 dp hit). Group tabs show group cards (name, count, chevron).
6. **Sticky import bar:** `+ IMPORT FOLDER / SCAN` (primary green button) and `SD MNT: …`. Hidden in pick mode, where it becomes `ADD n TO QUEUE`.

**Action sheet (⋮):** the track title, then ADD TO QUEUE, PLAY NEXT and TRACK DETAILS & TAGS. The mockup's SET AS RINGTONE is out of scope.

**Track details sheet:** read-only rows for title, artist, album, year, genre, track number, format, bitrate, sample rate, channels, duration, size, path and date modified.

## States

| State | What the user sees |
|-------|--------------------|
| No permission | Bay replaced by a rationale module: `STORAGE ACCESS REQUIRED`, an explanation, and `GRANT ACCESS` (or `OPEN SETTINGS` after a permanent denial) (B1) |
| Scanning (first time) | Bay shows `SCANNING n`; the list shows skeleton rows with a dim phosphor shimmer (static when animations are off) |
| Empty (scanned, 0 tracks) | `NO AUDIO FOUND` with a hint to import a folder |
| Search no results | `NO MATCH FOR "x"` |
| Populated | Paged list |
| Pick mode (+FILE/+DIR from Playlist) | Checkboxes on cards; the sticky bar reads `ADD n TO QUEUE` |
| Import results | Toasts: `IMPORTED n TRACKS`, `NO AUDIO IN FOLDER`, `FOLDER ALREADY IMPORTED` |

## Interactions

- ▶ plays now (B11). Tapping a card body also plays now; ⋮ opens the sheet.
- Tab switch keeps the search text, and the search applies within the tab.
- RESCAN runs a scan (B6); the bar keeps working during it.
- Imported SAF folders appear as Folders groups with a `SAF` badge. A long press offers `REMOVE FOLDER` (releases the grant, deletes its tracks after confirm).

## Content (keys)

| Key | EN | VI |
|-----|----|----|
| `lib_rescan` / `lib_scanning` / `lib_indexed` | RESCAN / SCANNING %1$d / INDEXED: %1$d AUDIO STREAMS | QUÉT LẠI / ĐANG QUÉT %1$d / ĐÃ LẬP CHỈ MỤC: %1$d BÀI |
| `lib_capacity` / `lib_free` | CAPACITY USED: %1$s / %2$s / %1$s FREE | ĐÃ DÙNG: %1$s / %2$s / CÒN %1$s |
| `lib_search_hint` | QUERY: ARTIST / TITLE / FILE... | TÌM: CA SĨ / TÊN BÀI / FILE... |
| `lib_tab_all` / `lib_tab_folders` / `lib_tab_artists` / `lib_tab_albums` / `lib_tab_recent` | ALL TRACKS (%1$d) / FOLDERS / ARTISTS (%1$d) / ALBUMS (%1$d) / RECENT | TẤT CẢ (%1$d) / THƯ MỤC / CA SĨ (%1$d) / ALBUM (%1$d) / MỚI THÊM |
| `lib_perm_title` / `lib_perm_body` / `lib_perm_grant` / `lib_perm_settings` | STORAGE ACCESS REQUIRED / Canh Amp reads audio files on this phone to build your library. Nothing leaves the device. / GRANT ACCESS / OPEN SETTINGS | CẦN QUYỀN TRUY CẬP / Canh Amp đọc file nhạc trên máy để tạo thư viện. Không có dữ liệu nào rời khỏi máy. / CẤP QUYỀN / MỞ CÀI ĐẶT |
| `lib_empty` / `lib_import` | NO AUDIO FOUND / + IMPORT FOLDER / SCAN | KHÔNG TÌM THẤY NHẠC / + THÊM THƯ MỤC / QUÉT |
| `lib_add_queue` / `lib_play_next` / `lib_details` | ADD TO QUEUE / PLAY NEXT / TRACK DETAILS & TAGS | THÊM VÀO DANH SÁCH / PHÁT TIẾP THEO / CHI TIẾT BÀI |
| `lib_imported` / `lib_import_empty` / `lib_import_dup` | IMPORTED %1$d TRACKS / NO AUDIO IN FOLDER / FOLDER ALREADY IMPORTED | ĐÃ THÊM %1$d BÀI / THƯ MỤC KHÔNG CÓ NHẠC / THƯ MỤC ĐÃ CÓ |
| `lib_add_n` | ADD %1$d TO QUEUE | THÊM %1$d BÀI |

## Accessibility

- A card is one focusable node ("Daft Punk, Around The World, MP3, 320 kilobits, 9.4 megabytes") with custom actions Play, Add to queue, Play next and Details.
- The scan progress is announced on a polite live region at start and at the end only.
- The permission rationale is read before the system dialog appears.
