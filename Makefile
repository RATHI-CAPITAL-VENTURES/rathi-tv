TV ?= 192.168.68.65

.PHONY: build test install undo cars guards changelog-archive

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

guards:
	bash .github/scripts/guards.sh all

changelog-archive:
	python3 tools/changelog-archive.py
