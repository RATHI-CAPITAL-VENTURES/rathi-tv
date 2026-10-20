package com.rathi.tv.home;

/** Plain-Java checks for Ambient (an app's colour) and Tilt (the focus lean). */
public class AmbientTest {
  private static int failed;

  private static void check(String what, boolean ok) {
    System.out.println((ok ? "  ok    " : "  FAIL  ") + what);
    if (!ok) failed++;
  }

  private static int[] field(int background, int logo, int logoShare, int n) {
    int[] px = new int[n];
    for (int i = 0; i < n; i++) px[i] = i % 100 < logoShare ? logo : background;
    return px;
  }

  private static boolean reddish(Integer c) {
    if (c == null) return false;
    int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
    return r > 150 && g < 80 && b < 80;
  }

  public static void main(String[] args) {
    int netflixRed = 0xFFE50914, white = 0xFFFFFFFF, black = 0xFF000000;
    System.out.println("Ambient.colourOf — an app's colour from its banner");
    check("Netflix: red logo on white -> red, not the grey a plain average gives",
        reddish(Ambient.colourOf(field(white, netflixRed, 25, 140))));
    check("a small logo still wins: 8% red on black -> red",
        reddish(Ambient.colourOf(field(black, netflixRed, 8, 140))));
    Integer hulu = Ambient.colourOf(field(0xFF0B0C0F, 0xFF1CE783, 30, 140));
    check("Hulu: green on near-black -> green", hulu != null && ((hulu >> 8) & 0xFF) > 180 && ((hulu >> 16) & 0xFF) < 80);
    check("all white -> null (no colour of its own)", Ambient.colourOf(field(white, white, 0, 140)) == null);
    check("all black -> null", Ambient.colourOf(field(black, black, 0, 140)) == null);
    check("greys only -> null", Ambient.colourOf(field(0xFF333333, 0xFFAAAAAA, 50, 140)) == null);
    check("a few colourful pixels below the floor -> null",
        Ambient.colourOf(field(white, netflixRed, 1, 140)) == null);
    // A COLOURFUL transparent field: transparent white weighs nothing anyway, so it
    // couldn't tell whether transparency is skipped at all.
    check("transparent pixels are ignored, even colourful ones", reddish(Ambient.colourOf(field(0x000000FF, netflixRed, 10, 140))));
    check("fully transparent -> null", Ambient.colourOf(field(0x00000000, 0x00FF0000, 50, 140)) == null);
    check("empty -> null", Ambient.colourOf(new int[0]) == null);
    check("result is opaque", (Ambient.colourOf(field(black, netflixRed, 50, 140)) >>> 24) == 0xFF);

    System.out.println("Ambient.toned — tint strength");
    int t = Ambient.toned(0xFF102040, 200);
    check("brightest channel lifted to the peak", (t & 0xFF) == 200);
    check("hue kept (ratios)", Math.abs(((t >> 8) & 0xFF) - 100) <= 1 && Math.abs(((t >> 16) & 0xFF) - 50) <= 1);
    check("black stays black, no divide by zero", (Ambient.toned(0xFF000000, 200) & 0xFFFFFF) == 0);

    System.out.println("Tilt — leans in from where focus came from");
    check("Right: arrives from the left, rotationY negative", Tilt.yFor(Tilt.RIGHT) < 0 && Tilt.xFor(Tilt.RIGHT) == 0);
    check("Left: rotationY positive", Tilt.yFor(Tilt.LEFT) > 0);
    check("Down: rotationX positive", Tilt.xFor(Tilt.DOWN) > 0 && Tilt.yFor(Tilt.DOWN) == 0);
    check("Up: rotationX negative", Tilt.xFor(Tilt.UP) < 0);
    check("OK or first focus: flat", Tilt.yFor(23) == 0 && Tilt.xFor(23) == 0 && Tilt.yFor(0) == 0);
    check("a fresh key counts", Tilt.keyFor(Tilt.RIGHT, 40) == Tilt.RIGHT);
    check("boundary: exactly FRESH_MS still counts", Tilt.keyFor(Tilt.RIGHT, Tilt.FRESH_MS) == Tilt.RIGHT);
    check("a key from minutes ago (focus restored by a rebuild) doesn't lean", Tilt.keyFor(Tilt.RIGHT, 180_000) == 0);
    check("1 ms past FRESH_MS doesn't", Tilt.keyFor(Tilt.RIGHT, Tilt.FRESH_MS + 1) == 0);

    if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    System.out.println("all passed");
  }
}
