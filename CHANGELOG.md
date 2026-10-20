# Changelog

`MAJOR.MINOR.PATCH` in `VERSION`. The `changelog` guard enforces that the top
header equals `VERSION`, is new relative to the base branch, and increases
monotonically. A MINOR bump is a milestone and must ship a retro.

## 0.2.0 — 2026-10-20

### Added

- The home screen, forked from tv-detox v1.5.1 (MIT) as `com.rathi.tv.home`.
  `install.sh` builds and installs this repo instead of downloading upstream.
- `CarsDream`: a screensaver of the 83-car collection with a crossfade, a slow
  push-in and a clock, set by `install.sh` and restored by `undo.sh`.
- `CarDeck`, the screensaver's order (every car once per round, no back-to-back
  repeats), with tests.
