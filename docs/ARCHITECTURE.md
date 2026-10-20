# Architecture

## What runs on the TV

```
Home (com.rathi.tv.home)
├─ MainActivity   upstream tv-detox launcher, renamed: app tiles, inputs, clock, shortcuts
└─ CarsDream      DreamService: assets/cars/*.jpg, CarDeck order, crossfade + push-in
```

Google TV's own home (`launcherx`) is **disabled**, which is what lets this one be
Home on a Google TV (`GOOGLE_TV=1` in upstream terms). The cost is its *Continue
watching* row, which needs a system-only permission no sideloaded launcher can hold
(rathi-studios-intro `docs/ARCHITECTURE.md` has the evidence).

On this Sony the first switch attempt failed: with `launcherx` off, Home went to
Google's setup app (`setupwraith/.RecoveryActivity`), and tv-detox put everything back
as designed. A second run with `YES_SETUP=1` didn't need the setup app turned off:
Home went straight to the new launcher. Only `launcherx` is disabled
(`/data/local/tmp/tvhome-disabled.txt`). The cause of the difference wasn't found;
`undo.sh` covers both.

## The screensaver

- **Images:** 83 JPEGs at 1920×1080 (~18 MB) made by `tools/make-cars.sh` from the
  5120×2880 originals. A decoded 4K frame is 33 MB, and the TV's UI is 1080p anyway.
  Each wallpaper already has its year and name baked in, so the dream draws no text
  except a small clock.
- **Order:** `CarDeck` shuffles all 83 and shows each once before any repeats, and
  never shows the same car twice in a row across a reshuffle (tested over 200 seeds).
- **Motion:** a 1.8 s crossfade and a linear 1.0 → 1.045 push over 20 s. The zoom is
  capped by the caption, which sits ~10% from the bottom. More zoom starts cutting it.
  Two `ObjectAnimator`s, because two `view.animate()` calls share one animator and the
  second duration overwrites the first.
- **Z-order:** each incoming picture is `bringToFront()`ed, so the clock has to be
  brought front after it. The first build hid the clock behind the second picture.
- **Decoding** happens on a `HandlerThread` while the current car is on screen.
- **Settings:** `install.sh` sets `secure screensaver_components` to
  `com.rathi.tv.home/.CarsDream` and saves the previous value (Sony's Living Decor,
  `com.sony.dtv.livingfit/...MainDreamService`) to
  `/data/local/tmp/rathi-tv-screensaver-prev.txt`; `undo.sh` restores it. When it
  starts (`system screen_off_timeout` = 10 min) and when the TV sleeps
  (`secure sleep_timeout` = 30 min) are left as they were.
- **Test it without waiting:** `adb shell am start -n com.android.systemui/.Somnambulator`.

## Things kept from upstream on purpose

`candidates.sh`, `check.sh` and `docs/debloat.md` are unchanged apart from the package
rename. The cleanup of this TV (33 apps off) was done with them and is recorded in
`~/sony-bravia-detox/` on the Mac.
