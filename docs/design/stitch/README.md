# Stitch design: source of truth for the UI

Project **Winamp Style MP3 Player** (Stitch id `5406549699141973212`), downloaded 2026-11-18.
Screenshots are 2x (780 px wide, i.e. a 390 dp phone).

| # | Screen | Stitch screen id | File |
|---|--------|------------------|------|
| 1 | Main Player | `b3753d1531ba4f648edffb4110f40948` | `01-main-player.png` |
| 2 | Graphic Equalizer | `5d41994d0bef454f843c49b57c4540f4` | `02-graphic-equalizer.png` |
| 3 | Skins Browser | `dd2deac93edf477d85e75bf7511048f4` | `03-skins-browser.png` |
| 4 | Playlist Queue | `7ec6e0492fbe40dd963988a9529b415c` | `04-playlist-queue.png` |
| 5 | Local MP3 Library | `a357615cbc4047cb879a93b3628ea370` | `05-local-library.png` |

The HTML/Tailwind for each screen is not stored here. Fetch it with the Stitch MCP
(`get_screen` → `download_file` on `htmlCode.name`). The hosted URLs need Google OAuth, so `curl` gets a 401.

## Design system "Titanium Deck" (summary)

- **Mode:** dark only. Zero corner radius per the design doc (the HTML uses 4 dp radius; the PNGs win).
- **Colors:** surface `#101417`, container-lowest `#0B0F11`, container `#1C2023`, container-high `#262A2D`,
  container-highest `#313538`, on-surface `#E0E3E7`, on-surface-variant `#B9CCB2`, outline `#84967E`.
  Phosphor green `#00FF41` (primary-container), amber `#FECB00` / `#F1C100` (secondary), orange `#FF8800` (clip),
  error `#FFB4AB` / `#93000A`.
- **Type:** Space Mono for display, labels and numbers (uppercase labels, wide tracking); Space Grotesk for body text.
  Scale: display 26/30, title 14/18, body 13/18, label 12, 10 and 9 sp.
- **Spacing:** 4 dp grid; 12 dp screen margin, 8 dp gutter.
- **Depth:** recessed wells (inner shadow) for displays, raised plates for modules, pressed state = inverted gradient + 1 dp shift.
- **Touch:** visuals can be 32x24 dp, but hit areas stay at 48 dp or more.
- **Shell:** top header, then a mini-player bar above the bottom navigation (MAIN · EQ · PLAYLIST · LIBRARY [· SKINS]).

## Branding note

The mockups contain third-party trademarks ("Winamp", "Nullsoft", "DJ Mike Llama", the llama slogan) and the remote
logo image. The app does **not** ship any of them (PRD R13). Keep the look and replace the words.
