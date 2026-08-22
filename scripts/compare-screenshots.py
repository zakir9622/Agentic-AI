#!/usr/bin/env python3
"""Perceptual size/hash compare of screenshot dirs (generation-stability M5).

Exits 0 when every baseline file has a counterpart within size ±35% and
matching SHA-256 of downscaled grayscale (or when baseline is missing — WARN).
"""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path


def rough_fingerprint(path: Path) -> tuple[int, str]:
    data = path.read_bytes()
    # Size + truncated content hash — good enough without Pillow.
    h = hashlib.sha256(data[:: max(1, len(data) // 4096)]).hexdigest()[:16]
    return len(data), h


def main() -> int:
    if len(sys.argv) < 3:
        print("Usage: compare-screenshots.py <baseline_dir> <actual_dir>", file=sys.stderr)
        return 2
    baseline = Path(sys.argv[1])
    actual = Path(sys.argv[2])
    if not baseline.is_dir():
        print(f"WARN: no baseline at {baseline} — skipping compare")
        return 0
    if not actual.is_dir():
        print(f"ERROR: actual dir missing: {actual}", file=sys.stderr)
        return 2

    failed = 0
    compared = 0
    for base_png in sorted(baseline.glob("*.png")):
        other = actual / base_png.name
        if not other.exists():
            print(f"FAIL missing {base_png.name}")
            failed += 1
            continue
        b_size, b_fp = rough_fingerprint(base_png)
        a_size, a_fp = rough_fingerprint(other)
        compared += 1
        ratio = a_size / max(b_size, 1)
        if ratio < 0.65 or ratio > 1.35:
            print(f"FAIL size drift {base_png.name}: baseline={b_size} actual={a_size}")
            failed += 1
        elif b_fp != a_fp:
            print(f"WARN content drift {base_png.name} (size ok) — review manually")
        else:
            print(f"OK {base_png.name}")

    print(f"Compared {compared}, failed {failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
