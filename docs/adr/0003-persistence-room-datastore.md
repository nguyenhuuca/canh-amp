# ADR-0003: Room for library, queue and presets; DataStore for settings

**Status:** Accepted (2026-11-18) · **Deciders:** project owner · **Tags:** data

## Context

The app keeps four kinds of local state:

- a library index of a few thousand tracks, searchable and grouped by folder, artist and album (R1, R3, R4);
- the play queue and position, which must survive a process kill (R17);
- user EQ presets (R21);
- scalar settings: theme, shuffle, repeat, EQ bands, balance and similar (R12, R20, R24).

The reference repo `D:\DO\android` already uses Room 2.6.1 with KSP (`gradle/libs.versions.toml`, `roomVersion`). The
app has no server, and nothing leaves the device (R30).

## Options

### A. Room for collections, Preferences DataStore for scalars
- **How:** Room holds `tracks`, `saf_folders`, `queue_items` and `eq_presets`, with schema export and versioned migrations. DataStore holds `settings` as typed keys.
- **Cost:** low. This is the known stack from the reference repo.
- **Risk:** two stores, so code has to know which holds what. The split rule is "lists in Room, single values in DataStore".
- **Reversibility:** moderate. The schema is versioned, and migrations are tested.

### B. Everything in Room (settings as a key-value table)
- **Cost:** low.
- **Risk:** you lose DataStore's `Flow`-first typed API for settings and write more boilerplate.

### C. Re-query MediaStore every time, keep no index
- **Cost:** lowest.
- **Risk:** cannot hold SAF-imported folders (R5) or cached bitrate and sample rate. Grouping and search become slow at about 2,000 tracks (G2), and the queue still needs storage.

## Decision

**A.** It is the reference repo's stack, it fits each data shape, and G2 needs an index. We give up a single store.

## Consequences

- Dependencies: `androidx.room` (runtime, ktx, compiler via KSP) and `androidx.datastore:datastore-preferences`, both from the version catalog.
- `CanhAmpDatabase` version 1, `exportSchema = true`, schemas in `app/schemas/`. Every later change ships a `Migration` plus a migration test. Never use `fallbackToDestructiveMigration`, because it would wipe user presets.
- The library index is a cache: it can be rebuilt by Rescan, so a failed migration of `tracks` may clear and rescan. That fallback is allowed only for `tracks`.
- Personal data (file names, titles) stays in app-private storage. Android Auto Backup stays on with default rules, so settings, presets and the queue come back on a new phone. The `tracks` index may be stale after a restore, and the first scan revalidates it (spec B5).

## Changelog

- 2026-11-18: created.
