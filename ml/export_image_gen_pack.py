#!/usr/bin/env python3
"""Export local-sdturbo-v1 ONNX pack for offline Create Studio.

Produces pack layout for AndroidTxt2ImgEngine:
  text_encoder.onnx, unet.onnx, vae_decoder.onnx, vocab.json, merges.txt, config.json

Full weight export needs a GPU host / Colab — this script writes the contract
scaffold. See ml/colab_export_sdturbo_pack.ipynb (when present) or:
  diffusers SD-Turbo / LCM-SD1.5 → ONNX (opset 17) → copy into exports/local-sdturbo-v1/

Usage:
  python ml/export_image_gen_pack.py --out exports/local-sdturbo-v1
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description="Export local SD-Turbo image-gen pack scaffold")
    parser.add_argument("--out", type=Path, default=Path("exports/local-sdturbo-v1"))
    args = parser.parse_args()
    out = args.out
    out.mkdir(parents=True, exist_ok=True)

    config = {
        "version": 1,
        "tier": "LOCAL_IMAGE",
        "displayName": "SD-Turbo local",
        "description": "Offline Create Studio — 512×512, 1–4 steps (AndroidTxt2ImgEngine)",
        "minSpec": {"minRamMb": 6144, "requiresNpu": False, "minSdk": 35},
        "lcmDistilled": True,
        "graphs": {
            "text_encoder": "text_encoder.onnx",
            "unet": "unet.onnx",
            "vae_decoder": "vae_decoder.onnx",
        },
        "scheduler": {"type": "lcm", "steps": 4, "guidance": 1.0},
        "resolution": 512,
    }
    (out / "config.json").write_text(json.dumps(config, indent=2))
    (out / "README.md").write_text(
        "# local-sdturbo-v1\n\n"
        "Required files (each ONNX ≥ 1 MB for the app to treat graphs as real):\n\n"
        "- `text_encoder.onnx` — CLIP text encoder\n"
        "- `unet.onnx` — 4-channel SD-Turbo / LCM UNet (not Pro 9-ch inpaint)\n"
        "- `vae_decoder.onnx`\n"
        "- `vocab.json` + `merges.txt` — CLIP BPE (copy from SD1.5 tokenizer)\n\n"
        "App engine: `AndroidTxt2ImgEngine` (SAMPLER_WIRED=true). "
        "Publish via `scripts/publish-packs.py` / HF `Iamzakirzr/vestra-packs`.\n"
    )
    print(f"Wrote scaffold to {out}")
    print("TODO: export real ONNX weights on GPU/Colab, then publish to HF packs manifest")


if __name__ == "__main__":
    main()
