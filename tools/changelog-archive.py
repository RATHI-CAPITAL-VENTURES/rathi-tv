"""Keeps one MINOR series in CHANGELOG.md; older series go to docs/changelog/<M.m>.md."""
import pathlib
import re

root = pathlib.Path(__file__).resolve().parent.parent
log = root / "CHANGELOG.md"
text = log.read_text()
parts = re.split(r"(?m)^(?=## \d+\.\d+\.\d+)", text)
head, entries = parts[0], parts[1:]
series = lambda e: ".".join(re.match(r"## (\d+)\.(\d+)", e).groups())
keep = series(entries[0])
moved = {}
for e in entries:
    if series(e) != keep:
        moved.setdefault(series(e), []).append(e)
out = root / "docs" / "changelog"
out.mkdir(parents=True, exist_ok=True)
for s, es in moved.items():
    f = out / f"{s}.md"
    old = f.read_text() if f.exists() else f"# Changelog — {s}.x\n\n"
    f.write_text(old + "".join(es))
    print(f"archived {len(es)} entr{'y' if len(es) == 1 else 'ies'} to {f.relative_to(root)}")
log.write_text(head + "".join(e for e in entries if series(e) == keep).rstrip() + "\n")
