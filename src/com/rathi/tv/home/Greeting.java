package com.rathi.tv.home;

/**
 * "Good evening, Ishan": the part of the day by the hour (0-23), then the name.
 * Plain Java. The name is the owner's choice (2026-10-20), on his parents' TV.
 */
final class Greeting {
  static final String NAME = "Ishan";

  private Greeting() {}

  static String forHour(int hour) {
    return partOfDay(hour) + ", " + NAME;
  }

  static String partOfDay(int hour) {
    if (hour >= 5 && hour < 12) return "Good morning";
    if (hour >= 12 && hour < 17) return "Good afternoon";
    return "Good evening";   // 17:00 to 04:59: nobody wants "good night" from a TV
  }
}
