# rathi-tv

The home screen and screensaver on the Sony BRAVIA at Ishan's parents' house. A fork
of [tv-detox](https://github.com/Ansub/tv-detox) (MIT, by Ansub Khan; its licence is
kept as `LICENSE.tv-detox`), taken at upstream `41a8a87` (v1.5.1).

- **Home screen:** the family's 15 apps, the HDMI inputs, a clock, and Bluetooth,
  Sound and Picture shortcuts by the clock. No ads, no recommendation rows.
- **Top shelf:** the highlighted app's colours fill the screen behind everything,
  like tvOS 26: Netflix turns it red, Disney+ blue. Tiles lean in as focus arrives
  and catch a streak of light.
- **Screensaver:** the car collection (`~/Pictures/Cars`, 83 cars from 1908 to 2016),
  one every 20 s, crossfading with a slow push-in, with a small clock.

The Rathi Studios intro on wake is a separate app,
[rathi-studios-intro](https://github.com/RATHI-CAPITAL-VENTURES/rathi-studios-intro).

## Use it

```sh
make install          # build, install, make it Home and the screensaver
make undo             # Google TV's home + the old screensaver back; remove this
make test             # the screensaver's shuffle, on the laptop
make cars             # re-make assets/cars from ~/Pictures/Cars (committed)
```

`TV=<ip>` points these at another TV. The TV needs network debugging on.

**Rearranging tiles:** hold OK on a tile (or press Menu) to move, replace or remove
it, all from the remote.

## Differences from upstream

| | tv-detox | rathi-tv |
| --- | --- | --- |
| Package | `com.ansub.tvhome` | `com.rathi.tv.home` |
| `install.sh` | downloads the upstream release | always builds and installs this repo |
| Signing key | `key.jks` in the repo dir | `~/.config/rathi-tv/key.jks`, outside the repo |
| Screensaver | none | `CarsDream`, set by `install.sh`, restored by `undo.sh` |
| Backdrop | the wallpaper | the focused app's blurred art and colour (`Shelf`) |
| Focus | scale up | scale up, lean in from the direction of travel, and a shine |

Upstream's README and debloat guide still apply to the cleanup half;
`docs/debloat.md` is kept from it.
