# Changelog

`MAJOR.MINOR.PATCH` in `VERSION`. The `changelog` guard enforces that the top
header equals `VERSION`, is new relative to the base branch, and increases
monotonically. A MINOR bump is a milestone and must ship a retro.

## 0.3.0 — 2026-10-20

### Added

- The top shelf: the focused app's banner, blurred, plus its own colour fill the
  screen behind the page and crossfade as focus moves (`Shelf`, `Ambient`).
- Tiles lean in from the direction focus came from and a streak of light sweeps
  across them (`Tilt`).
