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
import shutil
import sys
import tempfile

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
    slugs = [r["slug"] for r in rows]
    dupes = {x for x in slugs if slugs.count(x) > 1}
    if dupes:
        sys.exit(f"duplicate slugs in the catalogue: {sorted(dupes)}")
    missing = [x for x in slugs if not (CARS / "work" / f"{x}_big.png").exists()]
    if missing:
        sys.exit(f"no render for: {missing}")
    rows.sort(key=lambda r: (int(str(r["year"])[:4]), r["slug"]))
    # Build beside the real folder and swap at the end, so a failure halfway never leaves
    # a cars.tsv naming pictures that were already deleted.
    tmp = pathlib.Path(tempfile.mkdtemp(dir=OUT.parent, prefix=".home-"))
    try:
        build(rows, tmp)
    except BaseException:
        shutil.rmtree(tmp, ignore_errors=True)   # no half-built folder left in assets/
        raise
    if OUT.exists():
        shutil.rmtree(OUT)
    tmp.rename(OUT)
    print(f"{len(rows)} cars -> {OUT}")


def build(rows, tmp):
    lines = []
    for n, r in enumerate(rows, 1):
        im = Image.open(CARS / "work" / f"{r['slug']}_big.png").convert("RGB")
        w, h = im.size
        if w * 9 >= h * 16:   # wider than 16:9: crop the sides, centred
            cw = round(h * 16 / 9)
            left = (w - cw) // 2
            box = (left, 0, left + cw, h)
        else:                 # taller (the 3:2 renders): crop top and bottom around FOCUS_Y
            ch = round(w * 9 / 16)
            top = max(0, min(h - ch, round(h * FOCUS_Y - ch / 2)))
            box = (0, top, w, top + ch)
        im.crop(box).resize((W, H), Image.LANCZOS).save(
            tmp / f"{r['slug']}.jpg", quality=82, optimize=True, progressive=False)
        lines.append(f"{r['slug']}.jpg\t{title_case(r['name'])}\t{r['year']}\t{n}")
    (tmp / "cars.tsv").write_text("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
