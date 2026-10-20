package com.rathi.tv.home;

import android.animation.ObjectAnimator;
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
 * The blur is a downscale, not a blur filter: the banner is shrunk to 10x6 and
 * drawn back up with bilinear filtering. That reads as a heavy blur, costs nothing per
 * frame, and works on any Android version (RenderEffect needs 12+ and a GPU pass).
 *
 * Sits between the window's wallpaper and the page. A glass tile (inputs, Settings)
 * clears it, so the wallpaper shows again.
 */
final class Shelf {
  static final long SETTLE_MS = 140;   // wait for focus to stop moving before changing
  static final long FADE_MS = 450;
  // 0.55 let white banners (Netflix) wash the screen pale grey-pink; 0.45 keeps it dark.
  static final float ART_ALPHA = 0.45f;
  // 24x14 still showed blocky letters (NETFLIX was readable); 10x6 is a wash of colour.
  static final int SMALL_W = 10, SMALL_H = 6;

  private final Context context;
  private final FrameLayout view;
  private final ImageView[] art = new ImageView[2];
  private final View tint;
  private final View scrim;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Map<String, Bitmap> blurred = new HashMap<>();
  private final Map<String, Integer> colours = new HashMap<>();
  private int shown;            // index into art of the one on screen
  private String showingKey;
  private String pendingKey;

  Shelf(Context context) {
    this.context = context;
    view = new FrameLayout(context);
    for (int i = 0; i < 2; i++) {
      art[i] = new ImageView(context);
      art[i].setScaleType(ImageView.ScaleType.FIT_XY);
      art[i].setAlpha(0f);
      view.addView(art[i], new FrameLayout.LayoutParams(-1, -1));
    }
    tint = new View(context);
    tint.setAlpha(0f);
    view.addView(tint, new FrameLayout.LayoutParams(-1, -1));
    // Keeps the white clock and labels readable over any banner: darker at the top
    // (the clock) and the bottom (the inputs row), lighter through the middle.
    scrim = new View(context);
    scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
        new int[] {0xB0000000, 0x60000000, 0x40000000, 0x90000000}));
    scrim.setAlpha(0f);
    view.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
  }

  View view() { return view; }

  /** The app with this key and banner now has focus. */
  void show(final String key, final Drawable banner) {
    pendingKey = key;
    main.removeCallbacksAndMessages(null);
    main.postDelayed(() -> apply(key, banner), SETTLE_MS);
  }

  /** Something without a banner has focus: back to the wallpaper. */
  void clear() {
    pendingKey = null;
    main.removeCallbacksAndMessages(null);
    main.postDelayed(() -> apply(null, null), SETTLE_MS);
  }

  private void apply(String key, Drawable banner) {
    if (key == null ? showingKey == null : key.equals(showingKey)) return;
    showingKey = key;
    ImageView out = art[shown];
    if (key == null || banner == null) {
      fade(out, 0f);
      fade(tint, 0f);
      fade(scrim, 0f);
      return;
    }
    Bitmap b = blurred.get(key);
    if (b == null) {
      b = shrink(banner);
      blurred.put(key, b);
      colours.put(key, colourOf(b));
    }
    shown = 1 - shown;
    ImageView in = art[shown];
    in.setImageBitmap(b);
    in.bringToFront();
    tint.bringToFront();
    scrim.bringToFront();
    fade(in, ART_ALPHA);
    fade(out, 0f);
    Integer c = colours.get(key);
    if (c != null) {
      GradientDrawable g = new GradientDrawable();
      g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
      g.setGradientCenter(0.85f, 0.15f);
      g.setGradientRadius(context.getResources().getDisplayMetrics().widthPixels * 0.9f);
      g.setColors(new int[] {Ambient.toned(c, 200) & 0x80FFFFFF, 0x00000000});
      tint.setBackground(g);
      fade(tint, 1f);
    } else {
      fade(tint, 0f);
    }
    fade(scrim, 1f);
  }

  private static void fade(View v, float to) {
    ObjectAnimator a = ObjectAnimator.ofFloat(v, View.ALPHA, v.getAlpha(), to);
    a.setDuration(FADE_MS);
    a.start();
  }

  /** Draw the banner at 10x6: drawn back up with filtering, that is the blur. */
  private Bitmap shrink(Drawable banner) {
    // A copy: setBounds on the tile's own drawable would resize the tile's picture.
    Drawable.ConstantState cs = banner.getConstantState();
    Drawable d = cs != null ? cs.newDrawable(context.getResources()).mutate() : banner;
    Bitmap small = Bitmap.createBitmap(SMALL_W, SMALL_H, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(small);
    d.setBounds(0, 0, SMALL_W, SMALL_H);
    d.draw(c);
    return small;
  }

  private static Integer colourOf(Bitmap b) {
    int[] px = new int[b.getWidth() * b.getHeight()];
    b.getPixels(px, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
    return Ambient.colourOf(px);
  }

  /** Bitmaps are tiny (10x6), so caching every app costs nothing. */
  void forget() {
    blurred.clear();
    colours.clear();
    showingKey = null;
  }
}
