package com.rathi.tv.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The home screen's cars, read from assets/home/cars.tsv (written by
 * tools/make-home-cars.py): one line per car, "file \t name \t year \t number".
 * Plain Java, so tests/HomeTest.java checks the parsing without a TV.
 */
final class HomeCars {
  static final class Car {
    final String file, name, year;
    final int number;

    Car(String file, String name, String year, int number) {
      this.file = file;
      this.name = name;
      this.year = year;
      this.number = number;
    }
  }

  private final List<Car> cars;

  private HomeCars(List<Car> cars) { this.cars = Collections.unmodifiableList(cars); }

  /** Lines that don't have four fields, or whose number isn't a number, are skipped. */
  static HomeCars parse(List<String> lines) {
    List<Car> out = new ArrayList<>();
    for (String line : lines) {
      String[] f = line.split("\t");
      if (f.length < 4 || f[0].isEmpty()) continue;
      try {
        out.add(new Car(f[0].trim(), f[1].trim(), f[2].trim(), Integer.parseInt(f[3].trim())));
      } catch (NumberFormatException ignored) {}
    }
    return new HomeCars(out);
  }

  List<Car> all() { return cars; }

  Car byFile(String file) {
    for (Car c : cars) if (c.file.equals(file)) return c;
    return null;
  }

  /** The card's top line: "No. 37 of 83". */
  String numberLine(Car c) { return "No. " + c.number + " of " + cars.size(); }
}
