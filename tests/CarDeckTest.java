package com.rathi.tv.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Plain-Java checks for CarDeck: `make test`, no Android, no JUnit. */
public class CarDeckTest {
  private static int failed;

  private static void check(String what, boolean ok) {
    System.out.println((ok ? "  ok    " : "  FAIL  ") + what);
    if (!ok) failed++;
  }

  public static void main(String[] args) {
    List<String> cars = new ArrayList<>();
    for (int i = 0; i < 83; i++) cars.add("car_" + i + "_19" + (10 + i % 90) + ".jpg");

    System.out.println("CarDeck — order");
    for (long seed = 0; seed < 200; seed++) {
      CarDeck d = new CarDeck(cars, new Random(seed));
      Set<String> firstRound = new HashSet<>();
      String prev = null;
      boolean repeatInRound = false, backToBack = false;
      for (int i = 0; i < 83 * 3; i++) {
        String c = d.next();
        if (i < 83 && !firstRound.add(c)) repeatInRound = true;
        if (c.equals(prev)) backToBack = true;
        prev = c;
      }
      if (repeatInRound || firstRound.size() != 83 || backToBack) {
        check("seed " + seed + ": every car once per round, never twice in a row", false);
        break;
      }
      if (seed == 199) check("200 seeds x 3 rounds: every car once per round, never twice in a row", true);
    }
    // The reshuffle edge case on its own: two cars, so a shuffle putting the last car
    // first again is likely; it must still alternate.
    CarDeck two = new CarDeck(Arrays.asList("a.jpg", "b.jpg"), new Random(1));
    String p = null; boolean alternates = true;
    for (int i = 0; i < 50; i++) { String c = two.next(); if (c.equals(p)) alternates = false; p = c; }
    check("two cars always alternate across reshuffles", alternates);
    CarDeck one = new CarDeck(Arrays.asList("only.jpg"), new Random(1));
    check("one car repeats (nothing else to show)", "only.jpg".equals(one.next()) && "only.jpg".equals(one.next()));
    check("no cars: next() is null", new CarDeck(new ArrayList<>(), new Random(1)).next() == null);

    System.out.println("CarDeck — titles");
    check("aston_martin_db5_1963.jpg", CarDeck.title("aston_martin_db5_1963.jpg").equals("1963 Aston Martin Db5"));
    check("no year", CarDeck.title("ford_gt.jpg").equals("Ford Gt"));
    check("year in the middle", CarDeck.title("bmw_328_mille_miglia_touring_1939.jpg").equals("1939 Bmw 328 Mille Miglia Touring"));

    if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    System.out.println("all passed");
  }
}
