# ADR-0005: Docs site with MkDocs Material on GitHub Pages

**Status:** Accepted (2026-11-23) · **Deciders:** project owner · **Tags:** docs, ci, dependencies

## Context

The PRD, specs, ADRs and plans live in `docs/` as Markdown. The owner wanted the same published docs site as the
sibling `assessment` repo: MkDocs Material, a hook that reads GitHub-style lists, Mermaid diagrams with zoom, and a
workflow that deploys on every docs push. This adds a Python toolchain to an Android repo and a third-party script
that every docs visitor's browser fetches.

## Decision

- **Toolchain:** MkDocs Material, pinned in `docs/requirements.txt`. CI installs from that file; the pip cache is keyed on its hash. Bump it deliberately, as with any dependency.
- **Publish:** `.github/workflows/docs.yml` builds, fails on lists rendered as paragraphs (`scripts/check-docs-lists.py`), then runs `mkdocs gh-deploy` to the `gh-pages` branch. Pages serves that branch at `https://nguyenhuuca.github.io/canh-amp/`. The job needs `contents: write` to push `gh-pages`.
- **Mermaid from a CDN:** pages load `mermaid.min.js` from `unpkg.com` at an exact version (`mkdocs.yml`, `extra_javascript`).
  - Egress: the visitor's browser requests the script from unpkg. No data from the app or the repo is sent; unpkg sees the visitor's IP and the docs page as referrer.
  - The owner approved this third-party fetch on 2026-11-23 (architecture review of the docs site).

## Options Considered

- **Vendor `mermaid.min.js` into `docs/javascripts/`:** no egress and no CDN trust, but about 3 MB in git history per version bump. Rejected for now; revisit if the docs ever hold private content or unpkg becomes unreliable.
- **No docs site, read Markdown on GitHub:** zero tooling, but no search, no nav and no diagram zoom.

## Consequences

- The docs are public, like the repo. Nothing private belongs in `docs/`.
- A Mermaid upgrade is an edit of one version string in `mkdocs.yml`.
- Docs-only pushes also run `build.yml`, which has no path filter; harmless, a few CI minutes.
