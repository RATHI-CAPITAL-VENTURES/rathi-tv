package com.rathi.tv.home;

import java.util.Arrays;

/** Plain-Java checks for HomeCars (the home screen's car index) and Greeting. */
public class HomeTest {
  private static int failed;

  private static void check(String what, boolean ok) {
    System.out.println((ok ? "  ok    " : "  FAIL  ") + what);
    if (!ok) failed++;
  }

  public static void main(String[] args) {
    System.out.println("HomeCars — cars.tsv");
    HomeCars h = HomeCars.parse(Arrays.asList(
        "ford_model_t_1908.jpg\tFord Model T\t1908\t1",
        "aston_martin_db5_1963.jpg\tAston Martin DB5\t1963\t37",
        "",
        "broken line without tabs",
        "x.jpg\tName\t1999\tnot-a-number",
        "bugatti_chiron_2016.jpg\tBugatti Chiron\t2016\t83"));
    check("good lines kept, bad ones skipped (3 of 6)", h.all().size() == 3);
    HomeCars.Car db5 = h.byFile("aston_martin_db5_1963.jpg");
    check("fields by file", db5 != null && db5.name.equals("Aston Martin DB5") && db5.year.equals("1963") && db5.number == 37);
    check("number line counts the cars it has", h.numberLine(db5).equals("No. 37 of 3"));
    check("unknown file -> null", h.byFile("nope.jpg") == null);
    check("empty file -> no cars", HomeCars.parse(Arrays.asList()).all().isEmpty());
    check("names with quotes and slashes survive", HomeCars.parse(Arrays.asList(
        "b.jpg\tBentley 4.5 Litre 'Blower'\t1927\t3")).all().get(0).name.equals("Bentley 4.5 Litre 'Blower'"));

    System.out.println("Greeting — by the hour");
    check("4:59 is still evening", Greeting.partOfDay(4).equals("Good evening"));
    check("5:00 morning", Greeting.partOfDay(5).equals("Good morning"));
    check("11:59 morning", Greeting.partOfDay(11).equals("Good morning"));
    check("12:00 afternoon", Greeting.partOfDay(12).equals("Good afternoon"));
    check("16:59 afternoon", Greeting.partOfDay(16).equals("Good afternoon"));
    check("17:00 evening", Greeting.partOfDay(17).equals("Good evening"));
    check("midnight evening", Greeting.partOfDay(0).equals("Good evening"));
    check("with the name: \"Good evening, Ishan\"", Greeting.forHour(20).equals("Good evening, Ishan"));

    System.out.println("Playback — what Home pauses");
    check("nothing playing -> nothing to pause", !Playback.shouldPause(java.util.Arrays.<Integer>asList()));
    check("a film (Netflix, YouTube) -> pause", Playback.shouldPause(java.util.Arrays.asList(Playback.MOVIE)));
    check("music (Spotify) -> keeps playing", !Playback.shouldPause(java.util.Arrays.asList(Playback.MUSIC)));
    check("unlabelled player -> pause, to be safe", Playback.shouldPause(java.util.Arrays.asList(Playback.UNKNOWN)));
    check("music plus a film -> pause", Playback.shouldPause(java.util.Arrays.asList(Playback.MUSIC, Playback.MOVIE)));
    check("speech or UI sounds -> no", !Playback.shouldPause(java.util.Arrays.asList(Playback.SPEECH, Playback.SONIFICATION)));

    if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    System.out.println("all passed");
  }
}
