package com.rathi.tv.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The home screen's backdrop: a car from the collection (assets/home, caption-free)
 * filling the first screen, and a cream museum card naming it. While the home screen
 * sits in front, it crossfades to another car every few minutes; it stops while an
 * app is open. Order comes from CarDeck, so every car shows before any repeats.
 *
 * Decoding happens on a HandlerThread. At most two 8 MB bitmaps are held: the one
 * fading out is released when its fade ends.
 */
final class Gallery {
  static final long IDLE_MS = 3 * 60_000;   // owner's choice: "slowly while idle"
  static final long FADE_MS = 1_800;

  private final Context context;
  private final FrameLayout backdrop;
  private final ImageView[] art = new ImageView[2];
  private final View shade;
  private final View dim;   // over everything: darkens the car when the page shows all apps
  private final LinearLayout card;
  private final TextView number, name, year;
  private final HomeCars cars;
  private final CarDeck deck;
  private final Handler main = new Handler(Looper.getMainLooper());
  private HandlerThread worker;
  private Handler io;
  private int front;
  private int generation;
  private boolean running;
  private boolean hasPicture;
  private boolean dimmed;
  private ObjectAnimator fade;

  Gallery(Context context, float scale) {
    this.context = context;
    cars = HomeCars.parse(readLines("home/cars.tsv"));
    List<String> files = new ArrayList<>();
    for (HomeCars.Car c : cars.all()) files.add(c.file);
    deck = new CarDeck(files, new Random());

    backdrop = new FrameLayout(context);
    for (int i = 0; i < 2; i++) {
      art[i] = new ImageView(context);
      art[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
      art[i].setAlpha(0f);
      backdrop.addView(art[i], new FrameLayout.LayoutParams(-1, -1));
    }
    // Dark at the top for the clock, clear through the car, dark again at the bottom
    // for the tiles, ending near the wallpaper's own darkness so the page continues.
    shade = new View(context);
    shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
        new int[] {0x80000000, 0x00000000, 0x00000000, 0xB8000000, 0xF0050405}));
    backdrop.addView(shade, new FrameLayout.LayoutParams(-1, -1));
    dim = new View(context);
    dim.setBackgroundColor(0xFF050405);
    dim.setAlpha(0f);
    backdrop.addView(dim, new FrameLayout.LayoutParams(-1, -1));

    card = new LinearLayout(context);
    card.setOrientation(LinearLayout.VERTICAL);
    int padH = Math.round(33 * scale), padV = Math.round(27 * scale);
    card.setPadding(padH, padV, padH, padV);
    GradientDrawable paper = new GradientDrawable();
    paper.setColor(0xFFF4EFE6);
    paper.setCornerRadius(6 * scale);
    card.setBackground(paper);
    card.setElevation(24 * scale);
    Typeface serif = Typeface.create("serif", Typeface.NORMAL);
    number = label(18 * scale, 0xFF8A8173, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
    number.setLetterSpacing(0.16f);
    name = label(39 * scale, 0xFF1B1A17, Typeface.create("serif", Typeface.BOLD));
    year = label(25 * scale, 0xFF4F4A42, serif);
    card.addView(number);
    card.addView(name);
    card.addView(year);
    card.setAlpha(0f);
  }

  /** The car, sized to one screen; the page puts it at the top so it scrolls away. */
  View backdrop() { return backdrop; }

  /** The museum card; the page places it top right. */
  View card() { return card; }

  int size() { return cars.all().size(); }

  /** The page shows all apps: the car steps back (and the card with it). */
  void setDimmed(boolean dimmed) {
    this.dimmed = dimmed;
    dim.animate().alpha(dimmed ? 0.82f : 0f).setDuration(200).start();
    card.animate().alpha(cardTarget()).setDuration(200).start();
  }

  // The card shows only over a car, and not on the all-apps page. Every card animation
  // aims here, so one cancelling another can't leave it in the wrong state.
  private float cardTarget() { return !dimmed && hasPicture ? 1f : 0f; }

  /** Home is in front: show a car now if none is up, then change every IDLE_MS. */
  void resume() {
    if (running || cars.all().isEmpty()) return;
    running = true;
    generation++;
    worker = new HandlerThread("gallery");
    worker.start();
    io = new Handler(worker.getLooper());
    if (hasPicture) main.postDelayed(this::next, IDLE_MS);
    else next();
  }

  /** An app is in front: no timers, no decoding. The current car stays for next time. */
  void pause() {
    running = false;
    generation++;
    main.removeCallbacksAndMessages(null);
    if (worker != null) worker.quitSafely();
    worker = null;
    io = null;
  }

  /**
   * An app is in front and may need the memory: drop the car pictures (8 MB each).
   * The next resume decodes a car again (~60 ms) and fades it in. The TV runs with
   * ~250 MB free and swap full while Plex alone holds 330 MB (measured 2026-10-20).
   */
  void trim() {
    if (fade != null) fade.cancel();
    card.animate().cancel();
    for (ImageView v : art) {
      v.animate().cancel();
      v.setAlpha(0f);
      v.setImageDrawable(null);
    }
    hasPicture = false;
    card.setAlpha(0f);
  }

  void release() {
    pause();
    for (ImageView v : art) {
      v.animate().cancel();
      v.setImageDrawable(null);
    }
  }

  private void next() {
    final String file = deck.next();
    if (file == null || io == null) return;
    final int gen = generation;
    io.post(() -> {
      final Bitmap bmp = decode("home/" + file);
      main.post(() -> {
        if (!running || gen != generation) return;
        if (bmp != null) show(bmp, cars.byFile(file));
        // A failed decode tries the next car after a short wait, not at once.
        main.postDelayed(this::next, bmp != null ? IDLE_MS : 5_000);
      });
    });
  }

  private void show(Bitmap bmp, HomeCars.Car car) {
    final ImageView out = art[front];
    front = 1 - front;
    final ImageView in = art[front];
    in.animate().cancel();
    in.setAlpha(0f);
    in.setImageBitmap(bmp);
    in.bringToFront();
    shade.bringToFront();   // the new picture would otherwise cover the shade
    dim.bringToFront();
    if (fade != null) fade.cancel();
    fade = ObjectAnimator.ofFloat(in, View.ALPHA, 0f, 1f);
    fade.setDuration(hasPicture ? FADE_MS : 600);
    fade.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator a) {
        if (out == art[front]) return;   // a later show() made it the front picture again
        out.setAlpha(0f);
        out.setImageDrawable(null);
      }
    });
    fade.start();
    hasPicture = true;
    if (car != null) {
      in.setContentDescription(car.year + " " + car.name);
      updateCard(car);
    }
  }

  // The words fade, not the card: setDimmed() animates the card itself, and a new
  // animation on the card would cancel this one's end action (and the text change with it).
  private void updateCard(final HomeCars.Car car) {
    final View[] lines = {number, name, year};
    for (View l : lines) l.animate().cancel();
    number.animate().alpha(0f).setDuration(card.getAlpha() > 0 ? 250 : 0).withEndAction(() -> {
      number.setText(cars.numberLine(car).toUpperCase());
      name.setText(car.name);
      year.setText(car.year);
      for (View l : lines) l.animate().alpha(1f).setDuration(400).start();
    }).start();
    name.animate().alpha(0f).setDuration(card.getAlpha() > 0 ? 250 : 0).start();
    year.animate().alpha(0f).setDuration(card.getAlpha() > 0 ? 250 : 0).start();
    card.animate().alpha(cardTarget()).setDuration(400).start();
  }

  private TextView label(float px, int colour, Typeface tf) {
    TextView t = new TextView(context);
    t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
    t.setTextColor(colour);
    t.setTypeface(tf);
    t.setMaxWidth(Math.round(420 * px / 39));
    return t;
  }

  private Bitmap decode(String asset) {
    try (InputStream in = context.getAssets().open(asset)) {
      return BitmapFactory.decodeStream(in);
    } catch (IOException | OutOfMemoryError e) {
      return null;
    }
  }

  private List<String> readLines(String asset) {
    List<String> lines = new ArrayList<>();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(context.getAssets().open(asset), "UTF-8"))) {
      for (String l; (l = r.readLine()) != null; ) lines.add(l);
    } catch (IOException ignored) {
      // No index: no cars, and the wallpaper shows as before.
    }
    return lines;
  }
}
