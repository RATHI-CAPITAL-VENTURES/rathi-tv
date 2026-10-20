package com.rathi.tv.home;

import java.util.List;

/**
 * Whether opening Home should pause what's playing. Plain Java on
 * android.media.AudioAttributes content types, so tests/HomeTest.java checks it.
 *
 * Google TV's own home takes audio focus when it opens, and that focus loss is what
 * makes Netflix or YouTube pause; a home screen that never asks lets them play on
 * behind it (seen on the parents' TV, 2026-10-20). Music is left alone: Home from
 * Spotify should keep the music going, as it does on an Apple TV. Android reports
 * only the kind of sound, not the app playing it, so no permission is needed.
 */
final class Playback {
  // android.media.AudioAttributes.CONTENT_TYPE_*, copied so this stays plain Java.
  static final int UNKNOWN = 0, SPEECH = 1, MUSIC = 2, MOVIE = 3, SONIFICATION = 4;

  private Playback() {}

  /** Pause on Home if anything playing is a film or programme (or can't say). */
  static boolean shouldPause(List<Integer> playingContentTypes) {
    for (int t : playingContentTypes) {
      if (t == MOVIE || t == UNKNOWN) return true;   // most video apps; and unlabelled players
    }
    return false;                                     // music, speech, UI sounds
  }
}
