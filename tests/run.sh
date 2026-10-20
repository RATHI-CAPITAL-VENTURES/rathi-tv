#!/bin/sh
# Plain-Java tests: only the Android-free classes are compiled.
set -e
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
javac -nowarn -d "$OUT" src/com/rathi/tv/home/CarDeck.java tests/CarDeckTest.java
java -cp "$OUT" com.rathi.tv.home.CarDeckTest
rm -rf "$OUT"
