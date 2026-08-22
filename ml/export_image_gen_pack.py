#!/usr/bin/env python3
"""Export local-sdturbo-v1 ONNX pack for offline Create Studio (generation-stability M4).

Produces a pack layout mirroring ProPackConfig. Full weight export needs a GPU
host / Colab — this script writes the config + placeholder graph so CI and
integration smoke can validate the contract.

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
        "description": "Offline Create Studio — 512×512, 1–4 steps",
        "minSpec": {"minRamMb": 6144, "requiresNpu": False, "minSdk": 35},
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
        "Replace placeholder ONNX graphs with INT8/FP16 SD-Turbo or LCM-SD1.5 exports.\n"
        "Reuse `shared/.../engine/pro` OrtGraph / UnetRunner / LatentCodec / DdimScheduler.\n"
    )
    print(f"Wrote scaffold to {out}")
    print("TODO: export real ONNX weights on GPU host, then scripts/publish-packs.py")


if __name__ == "__main__":
    main()
