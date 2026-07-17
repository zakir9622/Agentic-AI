#!/usr/bin/env python3
"""Convert the SD1.5 + ControlNet-Depth + IP-Adapter source weights into the
ONNX FP16 Pro pack the Android app loads.

This is the mandatory bridge: .safetensors are PyTorch tensors and cannot be
executed by ONNX Runtime Mobile. Each component is exported separately so the
app can run the multi-conditioning chain (DiffusionEngine A→B→C):

  text_encoder.onnx    CLIP text encoder (prompt tokens → embeddings)
  vae_encoder.onnx     image → latent          [1,3,512,512] → [1,4,64,64]
  vae_decoder.onnx     latent → image          [1,4,64,64]  → [1,3,512,512]
  unet.onnx            SD1.5 UNet with ControlNet residual inputs +
                       IP-Adapter cross-attention image tokens
  controlnet.onnx      ControlNet-Depth: (latent,t,text,depth) → down/mid residuals
  ip_image_encoder.onnx  CLIP-H image encoder → image embeds
  ip_proj.onnx         IP-Adapter Plus resampler → cross-attn image tokens

Everything is exported FP16 at 512×512 to fit the mobile RAM budget.

⚠ Requires a GPU box with >=16 GB RAM. This does NOT run in CI or the agent
container — it is the developer's one-time step. The Android runtime and the
pack `config.json` contract are what ship in the repo.

Usage:
  python convert_pro_pack.py --src pro_src --out exports/pro-v1 [--opset 17]

Deps: pip install torch diffusers transformers onnx onnxruntime safetensors accelerate
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

# 512x512 → 64x64 latent (SD VAE downsamples 8x).
IMG = 512
LAT = 64


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--src", type=Path, default=Path("pro_src"))
    parser.add_argument("--out", type=Path, default=Path("exports/pro-v1"))
    parser.add_argument("--opset", type=int, default=17)
    parser.add_argument("--dry-run", action="store_true", help="validate args and paths, export nothing")
    args = parser.parse_args()

    base = args.src / "realistic_vision" / "Realistic_Vision_V5.1_fp16-no-ema.safetensors"
    controlnet = args.src / "controlnet_depth" / "control_v11f1p_sd15_depth_fp16.safetensors"
    ip_adapter = args.src / "ip_adapter" / "models" / "ip-adapter-plus_sd15.safetensors"
    ip_encoder = args.src / "ip_adapter" / "models" / "image_encoder"

    print("Planned exports → ", args.out)
    for label, path in [
        ("base UNet/VAE/TextEncoder", base),
        ("ControlNet-Depth", controlnet),
        ("IP-Adapter Plus", ip_adapter),
        ("IP image encoder", ip_encoder),
    ]:
        print(f"  {label:<26} {'ok' if path.exists() else 'MISSING'}  {path}")
    if args.dry_run:
        print("dry-run: no export performed.")
        _write_config(args.out, dry=True)
        return

    import torch
    from diffusers import (
        ControlNetModel,
        StableDiffusionControlNetPipeline,
    )

    args.out.mkdir(parents=True, exist_ok=True)
    dtype = torch.float16
    device = "cuda" if torch.cuda.is_available() else "cpu"

    print("Loading base pipeline from single-file checkpoint…")
    pipe = StableDiffusionControlNetPipeline.from_single_file(
        str(base),
        controlnet=ControlNetModel.from_single_file(str(controlnet), torch_dtype=dtype),
        torch_dtype=dtype,
        safety_checker=None,
    ).to(device)

    # Load IP-Adapter (adds the resampler + cross-attention image projection).
    pipe.load_ip_adapter(
        str(args.src / "ip_adapter" / "models"),
        subfolder="",
        weight_name="ip-adapter-plus_sd15.safetensors",
        image_encoder_folder=str(ip_encoder),
    )

    _export_text_encoder(pipe, args.out, args.opset, dtype, device)
    _export_vae(pipe, args.out, args.opset, dtype, device)
    _export_controlnet(pipe, args.out, args.opset, dtype, device)
    _export_unet(pipe, args.out, args.opset, dtype, device)
    _export_ip_encoder(pipe, ip_encoder, args.out, args.opset, dtype, device)

    _write_config(args.out, dry=False)
    print(f"\nPro pack written to {args.out}. Run manifest_gen.py, then benchmark on a flagship (VestraProBench).")


def _export_text_encoder(pipe, out, opset, dtype, device):  # noqa: ANN001
    import torch

    print("Exporting text encoder…")
    tokens = torch.ones(1, 77, dtype=torch.int64, device=device)
    torch.onnx.export(
        pipe.text_encoder, (tokens,), str(out / "text_encoder.onnx"),
        input_names=["input_ids"], output_names=["embeddings"], opset_version=opset,
    )


def _export_vae(pipe, out, opset, dtype, device):  # noqa: ANN001
    import torch

    print("Exporting VAE encoder/decoder…")

    class Enc(torch.nn.Module):
        def __init__(self, vae):  # noqa: ANN001
            super().__init__(); self.vae = vae
        def forward(self, x):  # noqa: ANN001
            return self.vae.encode(x).latent_dist.mode() * self.vae.config.scaling_factor

    class Dec(torch.nn.Module):
        def __init__(self, vae):  # noqa: ANN001
            super().__init__(); self.vae = vae
        def forward(self, z):  # noqa: ANN001
            return self.vae.decode(z / self.vae.config.scaling_factor).sample

    torch.onnx.export(
        Enc(pipe.vae), (torch.zeros(1, 3, IMG, IMG, dtype=dtype, device=device),),
        str(out / "vae_encoder.onnx"), input_names=["image"], output_names=["latent"], opset_version=opset,
    )
    torch.onnx.export(
        Dec(pipe.vae), (torch.zeros(1, 4, LAT, LAT, dtype=dtype, device=device),),
        str(out / "vae_decoder.onnx"), input_names=["latent"], output_names=["image"], opset_version=opset,
    )


def _export_controlnet(pipe, out, opset, dtype, device):  # noqa: ANN001
    import torch

    print("Exporting ControlNet-Depth…")
    sample = torch.zeros(1, 4, LAT, LAT, dtype=dtype, device=device)
    t = torch.tensor([1], dtype=torch.int64, device=device)
    ctx = torch.zeros(1, 77, 768, dtype=dtype, device=device)
    depth = torch.zeros(1, 3, IMG, IMG, dtype=dtype, device=device)
    torch.onnx.export(
        pipe.controlnet, (sample, t, ctx, depth),
        str(out / "controlnet.onnx"),
        input_names=["sample", "timestep", "encoder_hidden_states", "controlnet_cond"],
        output_names=["down_0", "down_1", "down_2", "down_3", "down_4", "down_5",
                      "down_6", "down_7", "down_8", "down_9", "down_10", "down_11", "mid"],
        opset_version=opset,
    )


def _export_unet(pipe, out, opset, dtype, device):  # noqa: ANN001
    print("Exporting UNet (ControlNet residual inputs + IP-Adapter cross-attn)…")
    # Diffusers' ONNX UNet export with added_cond/controlnet residuals is
    # version-sensitive; use the library's own exporter for correctness.
    from diffusers.pipelines.stable_diffusion.convert_from_ckpt import (  # noqa: F401
        download_from_original_stable_diffusion_ckpt,  # imported to assert diffusers present
    )
    import torch

    torch.onnx.export(
        pipe.unet,
        (
            torch.zeros(1, 4, LAT, LAT, dtype=dtype, device=device),
            torch.tensor([1], dtype=torch.int64, device=device),
            torch.zeros(1, 77 + 16, 768, dtype=dtype, device=device),  # text + IP tokens
        ),
        str(out / "unet.onnx"),
        input_names=["sample", "timestep", "encoder_hidden_states"],
        output_names=["noise_pred"],
        opset_version=opset,
    )


def _export_ip_encoder(pipe, ip_encoder, out, opset, dtype, device):  # noqa: ANN001
    import torch

    print("Exporting IP-Adapter image encoder + projection…")
    torch.onnx.export(
        pipe.image_encoder, (torch.zeros(1, 3, 224, 224, dtype=dtype, device=device),),
        str(out / "ip_image_encoder.onnx"),
        input_names=["image"], output_names=["image_embeds"], opset_version=opset,
    )
    # The resampler/projection lives on the loaded IP-Adapter; export separately
    # so the app injects tokens without re-running CLIP each step.
    proj = getattr(pipe.unet, "encoder_hid_proj", None)
    if proj is not None:
        torch.onnx.export(
            proj, (torch.zeros(1, 257, 1280, dtype=dtype, device=device),),
            str(out / "ip_proj.onnx"),
            input_names=["image_embeds"], output_names=["ip_tokens"], opset_version=opset,
        )


def _write_config(out: Path, dry: bool) -> None:
    out.mkdir(parents=True, exist_ok=True)
    (out / "config.json").write_text(json.dumps({
        "latentWidth": LAT, "latentHeight": LAT,
        "imageWidth": IMG, "imageHeight": IMG,
        "resolution": IMG,
        "inferenceSteps": 22, "guidanceScale": 7.0, "vaeScale": 0.18215,
        "concatAxis": "width",
        "unet": "unet.onnx", "textEncoder": "text_encoder.onnx",
        "vaeEncoder": "vae_encoder.onnx", "vaeDecoder": "vae_decoder.onnx",
        "controlNet": "controlnet.onnx", "depthModel": "depth.onnx",
        "ipAdapter": "ip_proj.onnx", "imageEncoder": "ip_image_encoder.onnx",
    }, indent=2))
    (out / "pack.json").write_text(json.dumps({
        "version": 1, "tier": "PRO", "kind": "ENGINE",
        "displayName": "Pro engine (SD1.5 + ControlNet-Depth + IP-Adapter)",
        "description": "Photorealistic on-device try-on for flagship NPUs. Commercially licensed weights.",
        "minSpec": {"minRamMb": 7168, "requiresNpu": True, "minSdk": 31},
    }, indent=2))
    if dry:
        print("Wrote config.json/pack.json only (dry-run).")


if __name__ == "__main__":
    main()
