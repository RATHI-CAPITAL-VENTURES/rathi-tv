package com.rathi.tv.home;

/**
 * How far a tile leans as it takes focus, by the D-pad key that moved focus there.
 * It leans in from the side focus came from (pressing Right: it arrives from the
 * left, so its left edge comes forward), then settles flat. Plain Java; key codes
 * are android.view.KeyEvent's.
 */
final class Tilt {
  static final float DEG = 7f;
  static final int UP = 19, DOWN = 20, LEFT = 21, RIGHT = 22;

  private Tilt() {}

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
