#!/usr/bin/env python3
"""Real-input ATR classification harness (R2.0b).

Mirrors `AtrTaxonomy.classifyHistogram` in Kotlin. Loads histogram fixtures from
`scripts/fixtures/atr/*.json` (synthetic worn-photo shapes today; replace with
Pixel classMap exports when available).

Usage:
  python3 scripts/test_atr_classify.py
  python3 scripts/test_atr_classify.py --fixtures scripts/fixtures/atr
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

CLASS_COUNT = 18
BACKGROUND = 0
HAT = 1
HAIR = 2
UPPER = 4
SKIRT = 5
PANTS = 6
DRESS = 7
FACE = 11
LEFT_LEG = 12
RIGHT_LEG = 13
LEFT_ARM = 14
RIGHT_ARM = 15
SCARF = 17


def classify_histogram(h: list[float]) -> str:
    def f(i: int) -> float:
        return h[i] if i < len(h) else 0.0

    scarf = f(SCARF)
    hat = f(HAT)
    face = f(FACE)
    hair = f(HAIR)
    upper = f(UPPER)
    dress = f(DRESS)
    pants = f(PANTS)
    skirt = f(SKIRT)
    legs = f(LEFT_LEG) + f(RIGHT_LEG)
    arms = f(LEFT_ARM) + f(RIGHT_ARM)
    head_cover = scarf + hat
    lower = pants + skirt
    torso = upper + dress

    if scarf > 0.08 and face < 0.035 and head_cover > 0.10:
        return "NIQAB"
    if head_cover > 0.10 and torso < 0.14 and lower < 0.08:
        return "HIJAB" if scarf >= hat else "HEADSCARF"
    if scarf > 0.07 and torso > 0.12 and lower < 0.12:
        return "DUPATTA"

    if torso + lower >= 0.32:
        if dress > 0.18 and skirt > 0.08 and pants < 0.06:
            return "LEHENGA"
        if dress > 0.20 and lower < 0.12:
            return "DRESS"
        if torso > 0.26 and (arms + legs) > 0.10 and face < 0.09 and lower > 0.08:
            return "ABAYA"
        if upper > 0.12 and pants > 0.10:
            return "SHALWAR_KAMEEZ"
        if torso > 0.22 and arms > 0.12 and lower > 0.06:
            return "JILBAB"
        if torso > 0.18 and arms > 0.08 and lower > 0.05:
            return "KAFTAN"

    if dress > 0.18:
        return "LEHENGA" if skirt > pants and skirt > 0.07 else "DRESS"
    if lower > torso and lower > 0.16:
        return "LOWER_BODY"
    if upper > 0.16 and pants > 0.09:
        return "SHALWAR_KAMEEZ"
    if upper > 0.14 and lower < 0.10:
        return "KURTA" if arms > 0.07 else "UPPER_BODY"
    if skirt > 0.14:
        return "LEHENGA"
    if head_cover > 0.06 or (hair > 0.12 and scarf > 0.04):
        return "HIJAB"
    return "ABAYA"


def load_hist(path: Path) -> tuple[str, list[float]]:
    data = json.loads(path.read_text())
    expected = data["expected"]
    hist = data.get("histogram")
    if hist is None and "fractions" in data:
        hist = [0.0] * CLASS_COUNT
        for key, val in data["fractions"].items():
            hist[int(key)] = float(val)
    if hist is None:
        raise ValueError(f"{path}: need histogram or fractions")
    if len(hist) < CLASS_COUNT:
        hist = list(hist) + [0.0] * (CLASS_COUNT - len(hist))
    return expected, hist[:CLASS_COUNT]


def main() -> int:
    parser = argparse.ArgumentParser(description="ATR classify fixture harness")
    parser.add_argument(
        "--fixtures",
        type=Path,
        default=Path(__file__).resolve().parent / "fixtures" / "atr",
    )
    args = parser.parse_args()
    files = sorted(args.fixtures.glob("*.json"))
    if not files:
        print(f"No fixtures in {args.fixtures}", file=sys.stderr)
        return 2

    failed = 0
    for path in files:
        expected, hist = load_hist(path)
        got = classify_histogram(hist)
        ok = got == expected
        status = "PASS" if ok else "FAIL"
        print(f"{status}  {path.name}: expected={expected} got={got}")
        if not ok:
            failed += 1

    print(f"\n{len(files) - failed}/{len(files)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
