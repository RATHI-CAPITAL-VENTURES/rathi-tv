TV ?= 192.168.68.65

.PHONY: build test install undo cars home-cars guards changelog-archive

build:
	sh build.sh

test:
	sh tests/run.sh

# Installs the home screen + screensaver and makes both the TV's defaults.
install:
	sh install.sh $(TV)

undo:
	sh undo.sh $(TV)

# Re-make assets/cars from ~/Pictures/Cars (the result is committed).
cars:
	sh tools/make-cars.sh

# Re-make assets/home (caption-free cars cropped to 16:9, plus cars.tsv; committed).
home-cars:
	python3 tools/make-home-cars.py

guards:
	bash .github/scripts/guards.sh all

changelog-archive:
	python3 tools/changelog-archive.py
