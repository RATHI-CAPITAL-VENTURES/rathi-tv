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
- **Memory:** at most two 8 MB bitmaps. The outgoing picture is released when the fade
  ends. Otherwise it would sit under the new one, drawn every frame for 20 s and
  still held while the next car decodes (3 bitmaps at peak).
- **Stopping:** `onDreamingStopped` cancels both animators. A 21.8 s push-in left
  running keeps ticking on a detached window and holds the bitmaps.
  `onDetachedFromWindow` clears the views, and a `generation` counter drops decodes
  posted by an earlier session. Checked on the TV: four start/stop cycles in a row,
  no crash, native heap ~57 MB.
- **Failure:** a failed decode (low memory, a bad file) backs off 2, 4 … 20 s, and
  stops once every car has failed in a row. The last picture stays up. The first
  build retried at 0 ms and would have spun a core.
- **Rejected: moving the clock against burn-in.** The X90L is an LED panel, not
  OLED. On an OLED this would need revisiting.
- **Settings:** `install.sh` sets `secure screensaver_components` to
  `com.rathi.tv.home/.CarsDream`, first saving the previous component *and*
  `screensaver_enabled` (Sony's Living Decor, on) as two lines in
  `/data/local/tmp/rathi-tv-screensaver-prev.txt`, once only. `undo.sh` restores
  them only if the cars are still the screensaver; a choice made in Settings since
  then wins. A saved `null` is restored by deleting the key, not by writing the word.
  Round trip checked on the TV: undo put Living Decor back (enabled 1, record removed),
  then reinstall saved it again. When it
  starts (`system screen_off_timeout` = 10 min) and when the TV sleeps
  (`secure sleep_timeout` = 30 min) are left as they were.
- **Test it without waiting:** `adb shell am start -n com.android.systemui/.Somnambulator`.

## Things kept from upstream on purpose

`candidates.sh`, `check.sh` and `docs/debloat.md` are unchanged apart from the package
rename. The cleanup of this TV (33 apps off) was done with them and is recorded in
`~/sony-bravia-detox/` on the Mac.
