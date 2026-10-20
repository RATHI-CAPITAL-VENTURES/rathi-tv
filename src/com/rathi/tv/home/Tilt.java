package com.rathi.tv.home;

/**
 * How far a tile leans as it takes focus, by the D-pad key that moved focus there.
 * It leans in from the side focus came from (pressing Right: it arrives from the
 * left, so its left edge comes forward), then settles flat. Plain Java; key codes
 * are android.view.KeyEvent's.
 */
final class Tilt {
  static final float DEG = 7f;
  /** Focus that moves this long after the last key (a rebuild restoring focus) doesn't lean. */
  static final long FRESH_MS = 250;
  static final int UP = 19, DOWN = 20, LEFT = 21, RIGHT = 22;

  private Tilt() {}

  /** The key that moved focus, or 0 if the last key press is too old to have done it. */
  static int keyFor(int lastKey, long ageMs) {
    return ageMs >= 0 && ageMs <= FRESH_MS ? lastKey : 0;
  }

  static float yFor(int key) {
    if (key == RIGHT) return -DEG;
    if (key == LEFT) return DEG;
    return 0f;
  }

  static float xFor(int key) {
    if (key == DOWN) return DEG;
    if (key == UP) return -DEG;
    return 0f;
  }
}
