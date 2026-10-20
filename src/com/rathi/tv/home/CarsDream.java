package com.rathi.tv.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextClock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

/**
 * The screensaver: the car collection (assets/cars), one at a time, crossfading with
 * a slow push-in, and a small clock. Each wallpaper already carries its year and
 * name, so there is no other text.
 *
 * Images are decoded on a background thread while the current one is on screen, so
 * a 1080p JPEG decode (~60 ms on this TV) never stalls the animation.
 */
public class CarsDream extends DreamService {
  static final long SHOW_MS = 20_000;
  static final long FADE_MS = 1_800;
  // The push-in. Small on purpose: the caption sits ~10% from the bottom edge and a
  // larger zoom would start to crop it.
  static final float ZOOM = 1.045f;

  private final Handler main = new Handler(Looper.getMainLooper());
  private HandlerThread worker;
  private Handler io;
  private CarDeck deck;
  private ImageView front, back;
  private TextClock clock;
  private boolean running;
  // Bumped on every start and stop: a decode posted by an earlier session compares
  // its number and drops itself, instead of relying on `running` alone.
  private int generation;
  private int failures;
  private ObjectAnimator fade, push;

  @Override public void onAttachedToWindow() {
    super.onAttachedToWindow();
    setInteractive(false);   // any key wakes it, as a screensaver should
    setFullscreen(true);
    setScreenBright(true);

    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.BLACK);
    back = image();
    front = image();
    root.addView(back);
    root.addView(front);

    clock = new TextClock(this);
    clock.setFormat12Hour("h:mm");
    clock.setFormat24Hour("H:mm");
    clock.setTextColor(0xCCFFFFFF);
    clock.setTextSize(28);
    clock.setShadowLayer(12, 0, 2, 0x99000000);
    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
    lp.setMargins(0, dp(36), dp(56), 0);
    root.addView(clock, lp);
    setContentView(root);

    String[] files = new String[0];
    try { files = getAssets().list("cars"); } catch (IOException ignored) {}
    deck = new CarDeck(Arrays.asList(files == null ? new String[0] : files), new Random());
  }

  @Override public void onDreamingStarted() {
    super.onDreamingStarted();
    running = true;
    generation++;
    failures = 0;
    worker = new HandlerThread("cars");
    worker.start();
    io = new Handler(worker.getLooper());
    loadNext();
  }

  @Override public void onDreamingStopped() {
    running = false;
    generation++;
    main.removeCallbacksAndMessages(null);
    if (worker != null) worker.quitSafely();
    // A push-in runs 21.8 s; left alone it keeps ticking on a detached window and
    // holds both bitmaps until it ends.
    if (fade != null) fade.cancel();
    if (push != null) push.cancel();
    super.onDreamingStopped();
  }

  @Override public void onDetachedFromWindow() {
    if (front != null) front.setImageDrawable(null);
    if (back != null) back.setImageDrawable(null);
    super.onDetachedFromWindow();
  }

  /** Decode the next car off the main thread, then show it. */
  private void loadNext() {
    final String name = deck.next();
    if (name == null || io == null) return;
    final int gen = generation;
    io.post(() -> {
      final Bitmap bmp = decode("cars/" + name);
      main.post(() -> {
        if (!running || gen != generation) return;
        if (bmp != null) {
          failures = 0;
          show(bmp, name);
          main.postDelayed(this::loadNext, SHOW_MS);
          return;
        }
        // A failed decode (low memory, a bad file) must not spin: back off, and stop
        // trying once every car has failed in a row. The last picture stays up.
        failures++;
        if (failures >= deck.size()) return;
        main.postDelayed(this::loadNext, Math.min(SHOW_MS, 1000L << Math.min(failures, 5)));
      });
    });
  }

  private void show(Bitmap bmp, String name) {
    // The incoming picture goes on the back view, fades in over the front one,
    // then the two swap roles.
    ImageView incoming = back;
    incoming.setImageBitmap(bmp);
    incoming.setContentDescription(CarDeck.title(name));
    incoming.setAlpha(0f);
    incoming.setScaleX(1f);
    incoming.setScaleY(1f);
    incoming.bringToFront();
    clock.bringToFront();   // the new picture would otherwise cover it
    // Two animators, not two view.animate() calls: those share one animator, and the
    // second duration would replace the fade's.
    final ImageView outgoing = front;
    fade = ObjectAnimator.ofFloat(incoming, View.ALPHA, 0f, 1f);
    fade.setDuration(FADE_MS);
    // Once the new picture covers it, drop the old one: it would otherwise be drawn,
    // hidden, every frame for 20 s and held while the next car decodes (3 bitmaps).
    fade.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator a) {
        if (outgoing != front) outgoing.setImageDrawable(null);
      }
    });
    push = ObjectAnimator.ofPropertyValuesHolder(incoming,
        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, ZOOM),
        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, ZOOM));
    push.setDuration(SHOW_MS + FADE_MS);
    push.setInterpolator(new LinearInterpolator());
    fade.start();
    push.start();
    back = front;
    front = incoming;
  }

  private Bitmap decode(String asset) {
    try (InputStream in = getAssets().open(asset)) {
      return BitmapFactory.decodeStream(in);
    } catch (IOException | OutOfMemoryError e) {
      return null;
    }
  }

  private ImageView image() {
    ImageView v = new ImageView(this);
    v.setScaleType(ImageView.ScaleType.CENTER_CROP);
    v.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
    return v;
  }

  private int dp(int v) {
    return Math.round(v * getResources().getDisplayMetrics().density);
  }
}
