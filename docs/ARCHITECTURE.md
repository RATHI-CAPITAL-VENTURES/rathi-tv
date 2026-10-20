# Architecture

## What runs on the TV

```
Home (com.rathi.tv.home)
├─ MainActivity   upstream tv-detox launcher, renamed: app tiles, inputs, clock, shortcuts
│   ├─ Shelf      the focused app's blurred art + colour behind the page (Ambient picks the colour)
│   └─ tiles      lean in from the direction of travel (Tilt) and a shine sweeps across
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

## The top shelf (v0.3.0)

Apple TV's top shelf shows the highlighted app's *content* (shows, trailers). That
content comes from each app's preview channels, and reading another app's channels
needs `ACCESS_ALL_EPG_DATA`, which is system-only. That's the same wall as *Continue
watching*. A Plex-only row was possible with the owner's token, and was **declined**:
the shelf had to work for every app or not at all (owner, 2026-10-20).

So the shelf is the tvOS 26 backdrop instead: with 15 apps the grid already fills the
screen up to the clock, and there's no room for a banner above it.

- **Art:** the focused app's banner is drawn at **10×6** and stretched back to full
  screen with bilinear filtering. That's a heavy blur for free, and it works on any API
  level. At 24×14, NETFLIX was still readable as blocky letters. At alpha 0.45; 0.55
  let a white banner wash the screen pale.
- **Colour:** `Ambient.colourOf` weights each pixel by saturation² × brightness, so a
  small red logo on a white field gives red, not the grey a plain average gives.
  Too little colour gives back null, and no tint. It's drawn as a radial glow from the
  top right.
- **Scrim:** a top-to-bottom dark gradient keeps the white clock and labels readable
  over any art.
- **Timing:** changes wait 140 ms for focus to stop moving, then crossfade over
  450 ms, so holding Right across a row doesn't strobe. Glass tiles (inputs,
  Settings, Add) clear it back to the wallpaper.
- **Tiles:** a tile taking focus starts 7° rotated toward the side focus came from
  (from the last D-pad key in `dispatchKeyEvent`) and settles flat over 300 ms while
  it scales up. A light streak sweeps across it once. Pointer-driven parallax like
  Apple's touch surface has no equivalent on a D-pad remote.

Checked by screenshot on the TV: Netflix (red), Disney+ (teal), Hulu (green), Apple
TV (dark, as its tile is).

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
