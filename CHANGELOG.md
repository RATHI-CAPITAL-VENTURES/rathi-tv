# Changelog

`MAJOR.MINOR.PATCH` in `VERSION`. The `changelog` guard enforces that the top
header equals `VERSION`, is new relative to the base branch, and increases
monotonically. A MINOR bump is a milestone and must ship a retro.

## 0.4.0 — 2026-10-20

### Changed

- The home screen is a gallery: a car from the collection fills the first screen
  with the time, "Good evening, Ishan", the date and a museum card naming it. Five big
  tiles sit at the bottom, without names, with a yellow focus ring. Down shows every
  app, all four HDMI inputs, Settings and the shortcuts, with the car dimmed.
- Home always returns to the car with the first app focused.

### Added

- `Gallery`, `HomeCars`, `Greeting`, `Playback` (with tests), `assets/home` (83
  caption-free cars + `cars.tsv`), `tools/make-home-cars.py`.
- Home pauses a film still playing behind it (music keeps playing).
