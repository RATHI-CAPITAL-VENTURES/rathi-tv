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

  /**
   * Pause on Home if a film or programme (or an unlabelled media player) is playing and
   * no music is. The Pause key goes to whichever session Android routes media buttons
   * to, usually the most recent player; with music also playing it could pause the
   * music instead and leave the film running, so then nothing is sent (review, PR #3).
   * Only USAGE_MEDIA players are passed in: Assistant replies and UI sounds don't count.
   */
  static boolean shouldPause(List<Integer> playingContentTypes) {
    boolean video = false;
    for (int t : playingContentTypes) {
      if (t == MUSIC) return false;
      if (t == MOVIE || t == UNKNOWN) video = true;
    }
    return video;
  }
}
