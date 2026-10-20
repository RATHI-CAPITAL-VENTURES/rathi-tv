package com.rathi.tv.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The order the screensaver shows the cars in: every car once, shuffled, before any
 * car repeats, and never the same car twice in a row across a reshuffle. Plain Java,
 * so tests/CarDeckTest.java runs it without a TV.
 */
final class CarDeck {
  private final List<String> cars;
  private final Random random;
  private final List<String> deck = new ArrayList<>();
  private String last;

  CarDeck(List<String> cars, Random random) {
    this.cars = new ArrayList<>(cars);
    this.random = random;
  }

  int size() { return cars.size(); }

  /** The next car, or null if there are none. */
  String next() {
    if (cars.isEmpty()) return null;
    if (deck.isEmpty()) refill();
    last = deck.remove(deck.size() - 1);
    return last;
  }

  private void refill() {
    deck.addAll(cars);
    Collections.shuffle(deck, random);
    // next() takes from the end: keep the car just shown away from it.
    if (deck.size() > 1 && deck.get(deck.size() - 1).equals(last)) {
      Collections.swap(deck, 0, deck.size() - 1);
    }
  }

  /** "aston_martin_db5_1963.jpg" -> "1963 Aston Martin Db5", for logs and accessibility. */
  static String title(String file) {
    String base = file.replaceAll("(?i)\\.[a-z]+$", "");
    String[] parts = base.split("_");
    StringBuilder name = new StringBuilder();
    String year = "";
    for (String p : parts) {
      if (p.matches("\\d{4}")) { year = p; continue; }
      if (p.isEmpty()) continue;
      if (name.length() > 0) name.append(' ');
      name.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
    }
    return year.isEmpty() ? name.toString() : year + " " + name;
  }
}
