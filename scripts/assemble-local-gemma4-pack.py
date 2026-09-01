#!/usr/bin/env python3
"""Assemble local-gemma-4-e2b-v1 from litert-community HuggingFace export.

Downloads gemma-4-E2B-it.litertlm and writes manifest_gen-ready layout under ml/exports/.

ROOT-CAUSE HISTORY (3.1.5): this script used to hardcode `"vision": True, "audio": True`
in config.json without ever loading the downloaded .litertlm file through a real
litertlm-android Engine to confirm those capabilities actually validate. That produced a
published pack whose vision encoder the SDK's native validator rejects at runtime — every
app install hit "The Vision Encoder model must have exactly one signature but got …" the
first time offline vision assist ran. The live published pack was also independently
observed shipping "audio": false despite this script asserting True for it — direct proof
the two can drift. Neither flag is trustworthy without a real on-device smoke test, so both
now default to False and require an explicit, human-confirmed opt-in (see --assume-vision /
--assume-audio below). Real signature introspection of a .litertlm file isn't practical from
pure Python (undocumented container format) — see --probe-signatures.

Usage:
  HF_TOKEN=… python3 scripts/assemble-local-gemma4-pack.py
  python3 scripts/assemble-local-gemma4-pack.py --functiongemma
  # After a REAL on-device smoke test confirms the capability actually works:
  python3 scripts/assemble-local-gemma4-pack.py --assume-vision --assume-audio
  cd ml && python3 manifest_gen.py exports/ && python3 ../scripts/publish-packs.py
"""
from __future__ import annotations

import argparse
import getpass
import json
import os
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPO = "litert-community/gemma-4-E2B-it-litert-lm"
PRIMARY_FILE = "gemma-4-E2B-it.litertlm"
FUNCTION_REPO = "litert-community/functiongemma-mobile-actions_q8_ekv1024.litertlm"
FUNCTION_FILE = "mobile-actions_q8_ekv1024.litertlm"


def download(repo: str, filename: str, dest: Path, token: str | None) -> None:
    from huggingface_hub import hf_hub_download

    path = hf_hub_download(repo_id=repo, filename=filename, token=token)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(path, dest)
    print(f"  {dest.name}: {dest.stat().st_size / 1e9:.2f} GB")


def git_sha() -> str:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True,
        ).strip()
    except Exception:
        return "unknown"


def write_provenance(out: Path, assumed: dict[str, bool]) -> None:
    """Sidecar recording which capability flags were asserted without real validation, and
    by whom/when — so a future audit doesn't have to reverse-engineer publish history the
    way the vision-encoder bug did."""
    (out / "manifest_provenance.json").write_text(
        json.dumps(
            {
                "gitSha": git_sha(),
                "invoker": getpass.getuser(),
                "assembledAtUtc": datetime.now(timezone.utc).isoformat(),
                "assumedNotValidated": assumed,
                "note": "Flags above were set via --assume-* after a manual on-device smoke "
                "test, not automated signature validation (none exists yet — see "
                "--probe-signatures in this script).",
            },
            indent=2,
        )
        + "\n",
    )


def write_gemma4_pack(out: Path, assume_vision: bool, assume_audio: bool) -> None:
    out.mkdir(parents=True, exist_ok=True)
    (out / "config.json").write_text(
        json.dumps(
            {
                "runtime": "litert-lm",
                "primaryFile": PRIMARY_FILE,
                "capability": "code",
                # Neither flag is trustworthy without a real on-device smoke test — see the
                # ROOT-CAUSE HISTORY note at the top of this file. Defaults to False; pass
                # --assume-vision/--assume-audio only after manually confirming the capability
                # actually works (e.g. via this app's own Diagnostics screen or warm-up flow).
                "vision": assume_vision,
                "audio": assume_audio,
                "tools": False,
                "backendDefault": "cpu",
            },
            indent=2,
        )
        + "\n",
    )
    (out / "pack.json").write_text(
        json.dumps(
            {
                "version": 1,
                "tier": "LITE",
                "displayName": "Gemma 4 E2B (LiteRT-LM)",
                "description": "Gallery-class Gemma 4 for Code, vision assist, and audio transcribe.",
                "minSpec": {"minRamMb": 8192, "requiresNpu": False, "minSdk": 35},
            },
            indent=2,
        )
        + "\n",
    )
    if assume_vision or assume_audio:
        write_provenance(out, {"vision": assume_vision, "audio": assume_audio})


def write_functiongemma_pack(out: Path) -> None:
    out.mkdir(parents=True, exist_ok=True)
    (out / "config.json").write_text(
        json.dumps(
            {
                "runtime": "litert-lm",
                "primaryFile": FUNCTION_FILE,
                "capability": "tools",
                "vision": False,
                "audio": False,
                "tools": True,
                "backendDefault": "cpu",
            },
            indent=2,
        )
        + "\n",
    )
    (out / "pack.json").write_text(
        json.dumps(
            {
                "version": 1,
                "tier": "LITE",
                "displayName": "FunctionGemma 270M tools",
                "description": "Experimental local tool calling (Mobile Actions class).",
                "minSpec": {"minRamMb": 4096, "requiresNpu": False, "minSdk": 35},
            },
            indent=2,
        )
        + "\n",
    )


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--out", type=Path, default=ROOT / "ml" / "exports" / "local-gemma-4-e2b-v1")
    parser.add_argument("--repo", default=DEFAULT_REPO)
    parser.add_argument("--functiongemma", action="store_true")
    parser.add_argument(
        "--assume-vision",
        action="store_true",
        help="Write config.json with \"vision\": true. Pass this ONLY after manually confirming "
        "a real on-device smoke test of offline vision assist passes with the downloaded "
        ".litertlm file — this script cannot verify that itself. Writes a "
        "manifest_provenance.json sidecar recording that the flag was asserted, not validated.",
    )
    parser.add_argument(
        "--assume-audio",
        action="store_true",
        help="Write config.json with \"audio\": true. Same caveat as --assume-vision: pass only "
        "after a real on-device smoke test, not on faith.",
    )
    parser.add_argument(
        "--probe-signatures",
        action="store_true",
        help="Placeholder for real .litertlm signature validation — not implemented. Real "
        "signature introspection needs either a documented container format spec or running "
        "the downloaded file through the actual litertlm-android SDK, neither of which this "
        "Python script can do today. Use --assume-vision/--assume-audio after a manual "
        "on-device check instead.",
    )
    args = parser.parse_args()
    if args.probe_signatures:
        print(
            "--probe-signatures is not implemented yet — no automated way to validate a "
            ".litertlm file's signatures from Python exists in this script. Confirm the "
            "capability works via a real on-device smoke test, then re-run with "
            "--assume-vision/--assume-audio.",
        )
        return
    token = (
        os.environ.get("HF_TOKEN")
        or os.environ.get("LOOKBOOK_HF_TOKEN")
        or os.environ.get("HUGGING_FACE_HUB_TOKEN")
    )

    if args.functiongemma:
        out = ROOT / "ml" / "exports" / "local-functiongemma-v1"
        write_functiongemma_pack(out)
        dest = out / FUNCTION_FILE
        if not dest.is_file() or dest.stat().st_size < 100_000_000:
            print(f"Downloading {FUNCTION_FILE}…")
            download(FUNCTION_REPO, FUNCTION_FILE, dest, token)
        else:
            print(f"  skip {FUNCTION_FILE} ({dest.stat().st_size / 1e6:.0f} MB)")
        return

    out = args.out
    write_gemma4_pack(out, assume_vision=args.assume_vision, assume_audio=args.assume_audio)
    if not args.assume_vision and not args.assume_audio:
        print(
            "config.json written with vision=false, audio=false (safe default — neither "
            "capability is validated by this script). Pass --assume-vision/--assume-audio "
            "after a real on-device smoke test to enable them.",
        )
    dest = out / PRIMARY_FILE
    if dest.is_file() and dest.stat().st_size > 500_000_000:
        print(f"  skip {PRIMARY_FILE} ({dest.stat().st_size / 1e9:.2f} GB)")
    else:
        print(f"Downloading {PRIMARY_FILE} from {args.repo}…")
        download(args.repo, PRIMARY_FILE, dest, token)
    print(f"Ready: {out}")


if __name__ == "__main__":
    main()
