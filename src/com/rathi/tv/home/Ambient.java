package com.rathi.tv.home;

/**
 * The one colour that stands for an app, taken from its banner: Netflix red, Hulu
 * green. Plain Java on ARGB ints, so tests/AmbientTest.java runs without Android.
 *
 * A plain average is wrong for banners: most are a white or black field with a small
 * coloured logo, and the average is grey. So each pixel is weighted by how colourful
 * it is (saturation x brightness), and near-black, near-white and transparent pixels
 * barely count. A banner with no real colour gives back null.
 */
final class Ambient {
  /** Below this share of colourful weight, a banner has no colour of its own. */
  static final double MIN_COLOUR = 0.04;

  private Ambient() {}

  /** The banner's colour as opaque 0xFFRRGGBB, or null if it is grey all over. */
  static Integer colourOf(int[] argb) {
    double r = 0, g = 0, b = 0, weight = 0;
    int counted = 0;
    for (int p : argb) {
      int a = p >>> 24;
      if (a < 128) continue;
      counted++;
      int pr = (p >> 16) & 0xFF, pg = (p >> 8) & 0xFF, pb = p & 0xFF;
      int max = Math.max(pr, Math.max(pg, pb)), min = Math.min(pr, Math.min(pg, pb));
      if (max == 0) continue;
      double sat = (max - min) / (double) max;
      double w = sat * sat * (max / 255.0);
      r += pr * w;
      g += pg * w;
      b += pb * w;
      weight += w;
    }
    if (counted == 0 || weight / counted < MIN_COLOUR) return null;
    return 0xFF000000 | clamp(r / weight) << 16 | clamp(g / weight) << 8 | clamp(b / weight);
  }

  /** The colour scaled so its brightest channel is `peak`: vivid enough to tint, never glaring. */
  static int toned(int rgb, int peak) {
    int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
    int max = Math.max(1, Math.max(r, Math.max(g, b)));
    double k = peak / (double) max;
    return 0xFF000000 | clamp(r * k) << 16 | clamp(g * k) << 8 | clamp(b * k);
  }

  private static int clamp(double v) {
    return (int) Math.max(0, Math.min(255, Math.round(v)));
  }
}
