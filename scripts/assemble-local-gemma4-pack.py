#!/usr/bin/env python3
"""Assemble local-gemma-4-e2b-v1 from litert-community HuggingFace export.

Downloads gemma-4-E2B-it.litertlm and writes pack layout under ml/exports/.

Usage:
  HF_TOKEN=… python3 scripts/assemble-local-gemma4-pack.py
  python3 scripts/assemble-local-gemma4-pack.py --vision   # vision assist pack stub
  python3 scripts/assemble-local-gemma4-pack.py --audio    # audio scribe stub
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPO = "litert-community/gemma-4-E2B-it-litert-lm"
PRIMARY_FILE = "gemma-4-E2B-it.litertlm"


def download(repo: str, filename: str, dest: Path, token: str | None) -> None:
    from huggingface_hub import hf_hub_download

    path = hf_hub_download(repo_id=repo, filename=filename, token=token)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(path, dest)
    print(f"  {dest.name}: {dest.stat().st_size / 1e9:.2f} GB")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def write_pack(out: Path, pack_id: str, display: str, desc: str, primary: str, *, vision: bool = False, audio: bool = False, tools: bool = False, min_ram: int = 8192) -> None:
    model = out / primary
    if not model.is_file():
        print(f"Missing {model} — run download first", file=sys.stderr)
        sys.exit(1)
    digest = sha256(model)
    size = model.stat().st_size
    config = {
        "runtime": "litert-lm",
        "primaryFile": primary,
        "capability": "code" if not audio else "audio",
        "vision": vision,
        "audio": audio,
        "tools": tools,
        "backendDefault": "cpu",
    }
    (out / "config.json").write_text(json.dumps(config, indent=2) + "\n")
    pack = {
        "id": pack_id,
        "version": 1,
        "displayName": display,
        "description": desc,
        "minSpec": {"minRamMb": min_ram, "requiresNpu": False, "minSdk": 35},
        "primaryFile": primary,
        "sha256": digest,
        "bytes": size,
    }
    (out / "pack.json").write_text(json.dumps(pack, indent=2) + "\n")
    print(f"Wrote {out}/pack.json · sha256={digest[:16]}…")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, default=ROOT / "ml" / "exports" / "local-gemma-4-e2b-v1")
    parser.add_argument("--repo", default=DEFAULT_REPO)
    parser.add_argument("--vision", action="store_true", help="Build vision assist pack (same weights)")
    parser.add_argument("--audio", action="store_true", help="Stub audio scribe pack layout only")
    args = parser.parse_args()
    token = (
        os.environ.get("HF_TOKEN")
        or os.environ.get("LOOKBOOK_HF_TOKEN")
        or os.environ.get("HUGGING_FACE_HUB_TOKEN")
    )

    if args.audio:
        out = ROOT / "ml" / "exports" / "local-audio-scribe-v1"
        out.mkdir(parents=True, exist_ok=True)
        config = {
            "runtime": "litert-lm",
            "primaryFile": "whisper-large-v3-turbo.litertlm",
            "capability": "audio",
            "audio": True,
        }
        (out / "config.json").write_text(json.dumps(config, indent=2) + "\n")
        print(f"Audio scribe stub at {out} — download whisper .litertlm from litert-community manually.")
        return

    if args.vision:
        out = ROOT / "ml" / "exports" / "local-gemma-4-vision-v1"
    else:
        out = args.out

    out.mkdir(parents=True, exist_ok=True)
    dest = out / PRIMARY_FILE
    if dest.is_file() and dest.stat().st_size > 500_000_000:
        print(f"  skip {PRIMARY_FILE} ({dest.stat().st_size / 1e9:.2f} GB)")
    else:
        print(f"Downloading {PRIMARY_FILE} from {args.repo}…")
        download(args.repo, PRIMARY_FILE, dest, token)

    if args.vision:
        write_pack(
            out,
            "local-gemma-4-vision-v1",
            "Gemma 4 E2B vision assist",
            "Multimodal Gemma 4 for offline reference photo analysis.",
            PRIMARY_FILE,
            vision=True,
            min_ram=10240,
        )
    else:
        write_pack(
            out,
            "local-gemma-4-e2b-v1",
            "Gemma 4 E2B Code",
            "LiteRT-LM Gemma 4 for offline Code Studio.",
            PRIMARY_FILE,
        )


if __name__ == "__main__":
    main()
