package com.rathi.tv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;

/**
 * The top shelf, the way tvOS 26 does its backdrop: the highlighted app's banner,
 * blurred to a wash of colour, fills the screen behind the clock and tiles, tinted
 * with the app's own colour, and crossfades as focus moves.
 *
 * Two layers, each the art plus its own tint, crossfade as a pair, so the colour
 * fades with the picture. One shared tint view swapped its colour in a single frame.
 *
 * The blur is a downscale, not a blur filter: the banner is drawn at 64x36, halved in
 * filtered steps down to 10x6, and stretched back up with bilinear filtering. Stepping
 * down averages every part of the banner; one jump to 10x6 sampled ~60 points and
 * could miss a thin logo. Works on any Android version (RenderEffect needs 12+).
 *
 * The work happens once per app, in prepare(), when the tiles are built. Focus only
 * passes a key, so the tile listeners don't keep full-size banners alive.
 *
 * Sits between the window's wallpaper and the page. Glass tiles and the clock-row
 * icons clear it, so the wallpaper shows again.
 */
final class Shelf {
  static final long SETTLE_MS = 140;   // wait for focus to stop moving before changing
  static final long FADE_MS = 450;
  // 0.55 let white banners (Netflix) wash the screen pale grey-pink; 0.45 keeps it dark.
  static final float ART_ALPHA = 0.45f;
  // 24x14 still showed blocky letters (NETFLIX was readable); 10x6 is a wash of colour.
  static final int SMALL_W = 10, SMALL_H = 6;
  static final int SAMPLE_W = 64, SAMPLE_H = 36;

  private final Context context;
  private final FrameLayout view;
  private final FrameLayout[] layer = new FrameLayout[2];
  private final ImageView[] art = new ImageView[2];
  private final View[] tint = new View[2];
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Map<String, Bitmap> blurred = new HashMap<>();
  private final Map<String, Integer> colours = new HashMap<>();
  private final View scrim;
  private int shown;            // index of the layer on screen
  private String showingKey;

  Shelf(Context context) {
    this.context = context;
    view = new FrameLayout(context);
    for (int i = 0; i < 2; i++) {
      layer[i] = new FrameLayout(context);
      layer[i].setAlpha(0f);
      art[i] = new ImageView(context);
      art[i].setScaleType(ImageView.ScaleType.FIT_XY);
      art[i].setAlpha(ART_ALPHA);
      layer[i].addView(art[i], new FrameLayout.LayoutParams(-1, -1));
      tint[i] = new View(context);
      layer[i].addView(tint[i], new FrameLayout.LayoutParams(-1, -1));
      view.addView(layer[i], new FrameLayout.LayoutParams(-1, -1));
    }
    // Keeps the white clock and labels readable over any banner: darker at the top
    // (the clock) and the bottom (the inputs row), lighter through the middle.
    scrim = new View(context);
    scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
        new int[] {0xB0000000, 0x60000000, 0x40000000, 0x90000000}));
    scrim.setAlpha(0f);
    view.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
  }

  View view() { return view; }

  /** Work out an app's blurred art and colour, once, while its tile is built. */
  void prepare(String key, Drawable banner) {
    if (banner == null || blurred.containsKey(key)) return;
    Bitmap sample = draw(banner, SAMPLE_W, SAMPLE_H);
    colours.put(key, colourOf(sample));
    Bitmap b = sample;
    while (b.getWidth() / 2 >= SMALL_W) {
      Bitmap half = Bitmap.createScaledBitmap(b, b.getWidth() / 2, b.getHeight() / 2, true);
      if (b != sample) b.recycle();
      b = half;
    }
    Bitmap small = Bitmap.createScaledBitmap(b, SMALL_W, SMALL_H, true);
    if (b != sample && b != small) b.recycle();
    if (sample != small) sample.recycle();
    blurred.put(key, small);
  }

  /** The app with this key has focus. */
  void show(final String key) {
    main.removeCallbacksAndMessages(null);
    main.postDelayed(() -> apply(key), SETTLE_MS);
  }

  /** Something without a banner has focus: back to the wallpaper. */
  void clear() {
    main.removeCallbacksAndMessages(null);
    main.postDelayed(() -> apply(null), SETTLE_MS);
  }

  /** The tiles are being rebuilt: drop everything, including a change still queued. */
  void forget() {
    main.removeCallbacksAndMessages(null);
    blurred.clear();
    colours.clear();
    showingKey = null;
  }

  /** The activity is going: nothing queued or animating may outlive it. */
  void release() {
    main.removeCallbacksAndMessages(null);
    for (View v : new View[] {layer[0], layer[1], scrim}) v.animate().cancel();
  }

  private void apply(String key) {
    if (key == null ? showingKey == null : key.equals(showingKey)) return;
    Bitmap b = key == null ? null : blurred.get(key);
    if (b == null) {
      showingKey = null;
      fade(layer[0], 0f);
      fade(layer[1], 0f);
      fade(scrim, 0f);
      return;
    }
    showingKey = key;
    FrameLayout out = layer[shown];
    shown = 1 - shown;
    FrameLayout in = layer[shown];
    art[shown].setImageBitmap(b);
    Integer c = colours.get(key);
    if (c != null) {
      GradientDrawable g = new GradientDrawable();
      g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
      g.setGradientCenter(0.85f, 0.15f);
      g.setGradientRadius(context.getResources().getDisplayMetrics().widthPixels * 0.9f);
      g.setColors(new int[] {Ambient.toned(c, 200) & 0x80FFFFFF, 0x00000000});
      tint[shown].setBackground(g);
    } else {
      tint[shown].setBackground(null);
    }
    in.bringToFront();
    scrim.bringToFront();
    fade(in, 1f);
    fade(out, 0f);
    fade(scrim, 1f);
  }

  // view.animate() cancels that view's previous alpha animation itself, so a quick
  // third move never leaves two animators fighting over one view.
  private static void fade(View v, float to) {
    v.animate().alpha(to).setDuration(FADE_MS).start();
  }

  private Bitmap draw(Drawable banner, int w, int h) {
    // A copy: setBounds on the tile's own drawable would resize the tile's picture.
    Drawable.ConstantState cs = banner.getConstantState();
    Drawable d = cs != null ? cs.newDrawable(context.getResources()).mutate() : null;
    Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    if (d == null) return out;   // no safe copy: an empty shelf beats a shrunken tile
    Canvas c = new Canvas(out);
    d.setBounds(0, 0, w, h);
    d.draw(c);
    return out;
  }

  private static Integer colourOf(Bitmap b) {
    int[] px = new int[b.getWidth() * b.getHeight()];
    b.getPixels(px, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
    return Ambient.colourOf(px);
  }
}
