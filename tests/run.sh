#!/bin/sh
# Plain-Java tests: only the Android-free classes are compiled.
set -e
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
javac -nowarn -d "$OUT" src/com/rathi/tv/home/CarDeck.java src/com/rathi/tv/home/Ambient.java \
  src/com/rathi/tv/home/Tilt.java tests/CarDeckTest.java tests/AmbientTest.java
java -cp "$OUT" com.rathi.tv.home.CarDeckTest
java -cp "$OUT" com.rathi.tv.home.AmbientTest
rm -rf "$OUT"
