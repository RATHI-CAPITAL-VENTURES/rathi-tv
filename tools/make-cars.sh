#!/bin/sh
# Rebuilds assets/cars/ from the car wallpaper collection.
#   sh tools/make-cars.sh [SRC]   SRC defaults to ~/Pictures/Cars/wallpapers/print_5120x2880
#
# The originals are 5120x2880 (~3 MB each). The TV is 1080p for the UI and a decoded
# 4K bitmap is 33 MB, so the screensaver gets 1920x1080 JPEGs: ~25 MB for all 83.
# Each wallpaper already carries its year and name, so nothing else is needed.
set -e
SRC=${1:-$HOME/Pictures/Cars/wallpapers/print_5120x2880}
cd "$(dirname "$0")/.."
rm -f assets/cars/*.jpg
for f in "$SRC"/*.jpg; do
  name=$(basename "$f" | sed 's/_5120x2880//')
  sips -s format jpeg -s formatOptions 82 -z 1080 1920 "$f" --out "assets/cars/$name" >/dev/null
done
ls assets/cars | wc -l | xargs echo "cars:"
du -sh assets/cars
