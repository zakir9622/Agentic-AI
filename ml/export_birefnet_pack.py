#!/usr/bin/env python3
"""Export BiRefNet ONNX pack (birefnet-v1) for Lite/Pro matte quality.

Usage:
  python ml/export_birefnet_pack.py --out exports/birefnet-v1

Requires: torch, transformers, onnx (run in Colab or GPU env).
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description="Export BiRefNet ONNX quality pack")
    parser.add_argument("--out", type=Path, default=Path("exports/birefnet-v1"))
    parser.add_argument("--model", default="ZhengPeng7/BiRefNet")
    args = parser.parse_args()

    out = args.out
    out.mkdir(parents=True, exist_ok=True)

    try:
        import torch
        from transformers import AutoModelForImageSegmentation
    except ImportError as exc:
        raise SystemExit(
            "Install torch + transformers to export BiRefNet. "
            "Use ml/colab_convert_pro_pack.ipynb or a GPU machine."
        ) from exc

    model = AutoModelForImageSegmentation.from_pretrained(args.model, trust_remote_code=True)
    model.eval()
    dummy = torch.randn(1, 3, 512, 512)
    onnx_path = out / "birefnet.onnx"
    torch.onnx.export(
        model,
        dummy,
        str(onnx_path),
        input_names=["input"],
        output_names=["output"],
        dynamic_axes={"input": {0: "batch", 2: "height", 3: "width"}},
        opset_version=17,
    )

    meta = {
        "version": 1,
        "tier": "LITE",
        "displayName": "BiRefNet matting",
        "description": "Cleaner garment/person mattes for Lite and Pro try-on.",
        "minSpec": {"minRamMb": 4096, "requiresNpu": False, "minSdk": 26},
        "kind": "QUALITY",
    }
    (out / "pack.json").write_text(json.dumps(meta, indent=2))
    print(f"Wrote {onnx_path} ({onnx_path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
