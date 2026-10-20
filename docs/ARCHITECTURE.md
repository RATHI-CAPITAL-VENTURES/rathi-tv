# Architecture

## What runs on the TV

```
Home (com.rathi.tv.home)
├─ MainActivity   upstream tv-detox launcher, renamed and re-laid-out (v0.4.0, below)
│   ├─ Gallery    the car (assets/home) filling the first screen + its museum card
│   ├─ Shelf      the focused app's blurred art + colour, behind the all-apps page
│   └─ tiles      yellow focus ring, lean in from the direction of travel (Tilt), shine
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

## The gallery home (v0.4.0)

Chosen by the owner from a set of mockups (an Artifact, 2026-10-20): the "museum
card" direction with big tiles, no names under them, and every app one press down.

- **Two positions, one page.** The first screen is a car from the collection with
  the clock, "Good evening, Ishan", the date, a cream museum card ("No. 9 of 83 ·
  Bugatti Type 57 Imaginaire · 1934"), and the home row: the first five apps.
  Pressing Down scrolls the page so the first app row sits 138 px from the top. The car
  dims to near black (`Gallery.setDimmed`), and everything else fades in: the other
  app rows, the inputs (wrapping like the apps, so all four HDMI inputs show), and the
  shortcut icons. `ScrollView`'s own focus scrolling is switched off
  (`computeScrollDeltaToGetChildRectOnScreen` returns 0) because it fought the two
  positions. `showAll()` picks them, and scrolls further only for a tile below the fold.
- **Below the home row is invisible at rest** (alpha 0), not just off screen: with
  five tiles a row and the home row 114 px above the bottom edge, the second row
  peeked over the car.
- **Home always lands on the car** with the first app focused (`onNewIntent` with
  `CATEGORY_HOME`). Back from an app keeps focus where it was.
- **The cars are the caption-free renders** (`~/Pictures/Cars/work/*_big.png`,
  6144×4096) cropped to 16:9 by `tools/make-home-cars.py`, which also writes
  `assets/home/cars.tsv`: the name (title-cased from the catalogue's capitals, with
  a keep-as-is list: DB5, NSX, McLaren, R/T …), the year, and the number in the
  collection (oldest first). The screensaver keeps the captioned set in `assets/cars`.
- **The car changes slowly while idle:** every 3 minutes while Home is in front (owner's
  choice over "every time Home opens"). It doesn't change while an app is open, and
  the decode thread stops in `onPause`.
- **Greeting:** `Greeting.forHour` gives "Good morning/afternoon/evening, Ishan". The
  name was the owner's request.
- **Pausing what plays behind Home.** The owner saw playback carry on behind this
  launcher after Home. Plex, it turned out, keeps playing in the background; Google
  TV's home stopped it.
  - **Now:** `pauseVideoBehind()` sends a media **Pause** key
    (`AudioManager.dispatchMediaKeyEvent`) when a film-type stream is playing
    (`Playback`). That is the same as pressing Pause on the remote, and it goes to the
    playing session. Music is exempt, so Spotify keeps playing.
  - **Rejected, after shipping it to the TV:** taking audio focus (`AUDIOFOCUS_GAIN`,
    handed straight back). It did stop Plex, but Plex treats a permanent focus loss
    as "stop" and tears its player down. Going back left a spinner, and the film had
    to be restarted (owner, 2026-10-20). Transient focus would make the app resume as
    soon as it is handed back, behind Home.
  - **What's verified:** YouTube pauses on Home with *or* without this, so YouTube
    doesn't need it.
  - **What isn't:** which app needs it is not yet confirmed.
  - **A testing trap:** starting YouTube with `am start … VIEW <watch url>` opens
    the player with its buffer stuck at 0. It reports `state=3` (playing) forever and
    shows the title card under a spinner. A video picked by hand plays fine. Don't use
    that intent to test playback.

## The top shelf (v0.3.0)

Apple TV's top shelf shows the highlighted app's *content* (shows, trailers). That
content comes from each app's preview channels, and reading another app's channels
needs `ACCESS_ALL_EPG_DATA`, which is system-only. That's the same wall as *Continue
watching*. A Plex-only row was possible with the owner's token, and was **declined**:
the shelf had to work for every app or not at all (owner, 2026-10-20).

So the shelf is the tvOS 26 backdrop instead: with 15 apps the grid already fills the
screen up to the clock, and there's no room for a banner above it.

- **Art:** each app's banner is drawn once, when tiles are built (`Shelf.prepare`),
  at 64×36, halved in filtered steps down to **10×6**, and stretched back to full
  screen with bilinear filtering. That's a heavy blur for free, on any API level.
  - At 24×14, NETFLIX was still readable as blocky letters.
  - One jump straight to 10×6 samples only ~60 points and can miss a thin logo
    (review), hence the steps. From 64×36 down every step averages its whole area;
    the first draw to 64×36 still samples, at ~2,300 points.
  - A banner with no `ConstantState` (nothing safe to copy) gets no entry, so its
    focus clears the shelf rather than fading in an empty, darkening layer.
  - Alpha 0.45: at 0.55 a white banner washed the screen pale.
  - The colour is taken from the 64×36 sample.
  - Focus passes only the app's key. Holding the banner in each tile's listener would
    keep full-size bitmaps alive that upstream's `fit()` exists to drop.
- **Colour:** `Ambient.colourOf` weights each pixel by saturation² × brightness, so a
  small red logo on a white field gives red, not the grey a plain average gives.
  Too little colour gives back null, and no tint. It's drawn as a radial glow from the
  top right.
- **Scrim:** a top-to-bottom dark gradient keeps the white clock and labels readable
  over any art.
- **Timing:** changes wait 140 ms for focus to stop moving, then crossfade over
  450 ms, so holding Right across a row doesn't strobe. Two layers, each the art plus
  its own tint, crossfade as a pair. With one shared tint view, the colour cut from
  red to blue in a single frame while the art faded (review).
  `view.animate()` cancels a view's previous fade, so a quick third move never leaves
  two animators on one view. Glass tiles and the clock-row icons clear it back to the
  wallpaper.
- **Lifecycle:** `forget()` (on rebuild) and `release()` (in `onDestroy`) also drop a
  queued change, so a stale banner can't be cached after a rebuild and nothing posted
  outlives the activity. `forget()` also fades the shelf out. `apply(null)` used to
  return early when `showingKey` was already null, so after a rebuild the
  uninstalled app's art could stay on screen (re-review).
- **Browsing speed:** at ~3 presses a second, the incoming layer can still be fading
  out from two moves back. The incoming layer is whichever is **dimmer now**, not the
  other index (140–225 ms apart, the other one is still the brighter). It's snapped to
  0 before its picture is swapped. A fade already at its last target is skipped (the
  target is kept as a view tag), so clears between glass tiles cost nothing. Fades use
  `withLayer()`, so the full-screen layer is drawn once, not every frame.
- **Tiles:** a tile taking focus starts 7° rotated toward the side focus came from
  (from the last D-pad key in `dispatchKeyEvent`) and settles flat over 300 ms while
  it scales up. Only a key under 250 ms old counts (`Tilt.keyFor`), so focus restored
  by a rebuild minutes later doesn't lean. Focus-gain runs without `withLayer()`,
  because the shine moves inside the card and a layer would be redrawn every frame.
  **Not verified by screenshot:** whether the lean goes the right way on screen. A
  capture over adb lands after the 300 ms animation even at 10× animator scale; it
  needs an eye on the TV. A light streak sweeps across it once. Pointer-driven parallax like
  Apple's touch surface has no equivalent on a D-pad remote.

Checked by screenshot on the TV for all 15 apps:
- red: Netflix, YouTube, ESPN;
- blue: Prime Video, Paramount+, NFL;
- teal: Disney+;
- green: Hulu, Spotify;
- orange: Crunchyroll;
- dark, as their tiles are: Max, Peacock, Apple TV, Plex, Sling.

None came out grey, and the HDMI row clears it.

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
