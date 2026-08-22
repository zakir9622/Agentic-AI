#!/usr/bin/env python3
"""Export Real-ESRGAN ONNX pack (realesrgan-v1) for 2× upscale after try-on/create.

Usage:
  python ml/export_realesrgan_pack.py --out exports/realesrgan-v1

Requires: torch, basicsr/realesrgan (run in Colab or GPU env).
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description="Export Real-ESRGAN ONNX quality pack")
    parser.add_argument("--out", type=Path, default=Path("exports/realesrgan-v1"))
    parser.add_argument("--scale", type=int, default=2, choices=[2, 4])
    args = parser.parse_args()

    out = args.out
    out.mkdir(parents=True, exist_ok=True)

    try:
        import torch
    except ImportError as exc:
        raise SystemExit(
            "Install torch to export Real-ESRGAN. "
            "Use ml/colab_convert_pro_pack.ipynb or a GPU machine."
        ) from exc

    # Placeholder graph — replace with realesrgan arch export in CI/Colab.
    class UpscaleStub(torch.nn.Module):
        def forward(self, x: torch.Tensor) -> torch.Tensor:
            return torch.nn.functional.interpolate(x, scale_factor=args.scale, mode="bilinear")

    model = UpscaleStub().eval()
    dummy = torch.randn(1, 3, 256, 256)
    onnx_path = out / f"realesrgan_x{args.scale}.onnx"
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
        "displayName": f"Real-ESRGAN {args.scale}×",
        "description": "Upscale try-on and Create outputs for listing-ready stills.",
        "minSpec": {"minRamMb": 4096, "requiresNpu": False, "minSdk": 26},
        "kind": "QUALITY",
        "scale": args.scale,
    }
    (out / "pack.json").write_text(json.dumps(meta, indent=2))
    print(f"Wrote {onnx_path} ({onnx_path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
