"""Builds assets/home/: the home screen's cars, without captions, plus cars.tsv.

    python3 tools/make-home-cars.py [CARS_DIR]     CARS_DIR defaults to ~/Pictures/Cars

The home screen draws its own label card, so it uses the caption-free renders
(work/<slug>_big.png, 6144x4096, 3:2) cropped to 16:9 at 1920x1080. The screensaver
keeps the captioned wallpapers in assets/cars/.

cars.tsv has one line per car: file, display name, year, and its number in the
collection (oldest first, the order of the collection's own decade table). Names
come from the catalogue (pipeline/catalog.json), which writes them in capitals;
title_case() turns "ASTON MARTIN DB5" into "Aston Martin DB5".
"""
import json
import pathlib
import re
import sys

from PIL import Image

CARS = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else pathlib.Path.home() / "Pictures/Cars")
OUT = pathlib.Path(__file__).resolve().parent.parent / "assets/home"
W, H = 1920, 1080
# Where the crop sits in the 3:2 render: the cars are photographed slightly below
# centre, so a centred crop clips wheels. 0.55 keeps the floor reflection.
FOCUS_Y = 0.55

# Words that stay as written: model codes and marques that are acronyms.
UPPER = {"BMW", "GT", "GTO", "GTB", "GTS", "SL", "SLR", "RS", "CSL", "SS", "XK", "AMG",
         "GNX", "DB", "SC", "LP", "SV", "SVJ", "TT", "MG", "II", "III", "IV", "V", "DBS",
         "NSX", "FD", "MM", "GSX", "STI", "DS", "R/T", "R", "RX", "DMC"}
# Names whose capitals sit mid-word, which no rule produces.
SPECIAL = {"MCLAREN": "McLaren", "LAFERRARI": "LaFerrari", "DELOREAN": "DeLorean"}
SMALL = {"de", "di", "von", "and", "et"}


def word_case(w, first):
    if w in SPECIAL:
        return SPECIAL[w]
    if w in UPPER:
        return w
    if any(ch.isdigit() for ch in w):
        # 7TH -> 7th; DB5, GT40, 2900B, 57SC stay as written.
        return re.sub(r"(\d)(ST|ND|RD|TH)$", lambda m: m.group(1) + m.group(2).lower(), w)
    if w.lower() in SMALL and not first:
        return w.lower()
    return w[:1].upper() + w[1:].lower()


def title_case(name):
    out = []
    for i, w in enumerate(name.split()):
        quote = w[0] if w[0] in "'\"" else ""
        core = w.strip("'\"")
        # GT-R, TALBOT-LAGO, T150-C: case each hyphenated part on its own.
        cased = "-".join(word_case(part, i == 0) for part in core.split("-"))
        out.append(f"{quote}{cased}{quote}" if quote else cased)
    return " ".join(out)


def main():
    rows = [r for r in json.load(open(CARS / "pipeline/catalog.json")) if not r.get("skip")]
    rows.sort(key=lambda r: (int(r["year"]), r["slug"]))
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.jpg"):
        old.unlink()
    lines = []
    for n, r in enumerate(rows, 1):
        src = CARS / "work" / f"{r['slug']}_big.png"
        im = Image.open(src).convert("RGB")
        w, h = im.size
        ch = round(w * 9 / 16)
        top = max(0, min(h - ch, round(h * FOCUS_Y - ch / 2)))
        im = im.crop((0, top, w, top + ch)).resize((W, H), Image.LANCZOS)
        im.save(OUT / f"{r['slug']}.jpg", quality=82, optimize=True, progressive=False)
        lines.append(f"{r['slug']}.jpg\t{title_case(r['name'])}\t{r['year']}\t{n}")
    (OUT / "cars.tsv").write_text("\n".join(lines) + "\n")
    print(f"{len(lines)} cars -> {OUT}")


if __name__ == "__main__":
    main()
