"""The Lookbook — free Cloud tier on Hugging Face ZeroGPU.

A Gradio Space that generates a photoreal on-model shot from a person image + a
garment image, at no per-image cost (within ZeroGPU's daily quota). It exposes a
single `tryon(...)` API the app's Supabase Edge Function calls.

Two backends, switchable per request via `model`:
  • "clean"   — commercially-clean SD1.5 + ControlNet-Depth + IP-Adapter-Plus
                (Realistic Vision V5.1). Not trained on VITON-HD/DressCode, so
                it's safe for a paid B2B product. Highest fidelity we can ship
                license-clean.
  • "idmvton" — routes to a separate duplicated `yisol/IDM-VTON` Space for
                maximum garment fidelity (VITON-HD-trained → treat as
                preview/personal until licensing is resolved). Set IDM_VTON_SPACE.

Optional Real-ESRGAN 2× upscale (`upscale=True`) raises output resolution with
no extra diffusion cost — quality up, not down.

Hosting note: ZeroGPU hardware requires an HF PRO account on the Space owner.
Select "ZeroGPU" in Space settings; models are placed on cuda at module level and
GPU work is wrapped in @spaces.GPU (see docs/CLOUD_ZEROGPU.md).
"""
from __future__ import annotations

import os

import gradio as gr
import numpy as np
import spaces
import torch
from PIL import Image

DTYPE = torch.float16
IDM_VTON_SPACE = os.environ.get("IDM_VTON_SPACE", "").strip()  # e.g. "you/IDM-VTON"

# ── commercially-clean pipeline (loaded on cuda at module level per ZeroGPU) ──
from diffusers import (
    AutoencoderKL,
    ControlNetModel,
    StableDiffusionControlNetPipeline,
    UniPCMultistepScheduler,
)
from transformers import pipeline as hf_pipeline

_depth = hf_pipeline(
    "depth-estimation", model="depth-anything/Depth-Anything-V2-Small-hf",
)

_controlnet = ControlNetModel.from_pretrained(
    "lllyasviel/control_v11f1p_sd15_depth", torch_dtype=DTYPE,
)
_vae = AutoencoderKL.from_pretrained("stabilityai/sd-vae-ft-mse", torch_dtype=DTYPE)
_pipe = StableDiffusionControlNetPipeline.from_single_file(
    "https://huggingface.co/SG161222/Realistic_Vision_V5.1_noVAE/resolve/main/"
    "Realistic_Vision_V5.1_fp16-no-ema.safetensors",
    controlnet=_controlnet, vae=_vae, torch_dtype=DTYPE, safety_checker=None,
)
_pipe.scheduler = UniPCMultistepScheduler.from_config(_pipe.scheduler.config)
_pipe.load_ip_adapter(
    "h94/IP-Adapter", subfolder="models",
    weight_name="ip-adapter-plus_sd15.safetensors",
)
_pipe.set_ip_adapter_scale(0.75)
_pipe.to("cuda")

POSITIVE = (
    "professional studio fashion photograph, photorealistic, a person wearing the "
    "garment, natural skin texture, soft studio lighting, sharp focus, high detail, "
    "editorial catalog quality, 85mm lens"
)
NEGATIVE = (
    "deformed, distorted, disfigured, extra limbs, bad anatomy, blurry, lowres, "
    "cartoon, 3d render, watermark, text, mannequin, plastic skin"
)


def _upscale(img: Image.Image, enabled: bool) -> Image.Image:
    if not enabled:
        return img
    try:  # Real-ESRGAN if available; otherwise high-quality Lanczos fallback.
        from RealESRGAN import RealESRGAN
        model = RealESRGAN("cuda", scale=2)
        model.load_weights("weights/RealESRGAN_x2.pth", download=True)
        return model.predict(img.convert("RGB"))
    except Exception:
        return img.resize((img.width * 2, img.height * 2), Image.LANCZOS)


@spaces.GPU(duration=90)
def _generate_clean(person: Image.Image, garment: Image.Image, seed: int) -> Image.Image:
    person = person.convert("RGB").resize((512, 512))
    garment = garment.convert("RGB").resize((512, 512))
    depth_map = _depth(person)["depth"].convert("RGB").resize((512, 512))
    generator = torch.Generator("cuda").manual_seed(int(seed))
    out = _pipe(
        prompt=POSITIVE, negative_prompt=NEGATIVE,
        image=depth_map,                       # ControlNet-Depth structure
        ip_adapter_image=garment,              # garment appearance (IP-Adapter)
        num_inference_steps=28, guidance_scale=7.0,
        controlnet_conditioning_scale=0.9, generator=generator,
    ).images[0]
    return out


def tryon(person, garment, model="clean", upscale=True, seed=42):
    """Main API. person/garment: PIL images. Returns a PIL image."""
    if person is None or garment is None:
        raise gr.Error("Both a person image and a garment image are required.")

    if model == "idmvton":
        if not IDM_VTON_SPACE:
            raise gr.Error(
                "IDM-VTON backend not configured. Duplicate yisol/IDM-VTON and set "
                "the IDM_VTON_SPACE secret, or use model='clean'."
            )
        from gradio_client import Client, handle_file
        client = Client(IDM_VTON_SPACE, hf_token=os.environ.get("HF_TOKEN"))
        # yisol/IDM-VTON's /tryon signature: (dict{background,layers,composite},
        # garm_img, prompt, is_checked, is_checked_crop, denoise_steps, seed)
        import tempfile
        with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as pf, \
             tempfile.NamedTemporaryFile(suffix=".png", delete=False) as gf:
            person.convert("RGB").save(pf.name)
            garment.convert("RGB").save(gf.name)
            result = client.predict(
                {"background": handle_file(pf.name), "layers": [], "composite": None},
                handle_file(gf.name), "garment", True, False, 30, int(seed),
                api_name="/tryon",
            )
        out = Image.open(result[0]) if isinstance(result, (list, tuple)) else Image.open(result)
    else:
        out = _generate_clean(person, garment, seed)

    return _upscale(out, upscale)


demo = gr.Interface(
    fn=tryon,
    inputs=[
        gr.Image(type="pil", label="Model / person"),
        gr.Image(type="pil", label="Garment"),
        gr.Dropdown(["clean", "idmvton"], value="clean", label="Engine"),
        gr.Checkbox(value=True, label="Upscale 2×"),
        gr.Number(value=42, label="Seed"),
    ],
    outputs=gr.Image(type="pil", label="Result"),
    title="The Lookbook — Cloud try-on (ZeroGPU)",
    description="Free-tier garment photoshoot generation. API: /tryon",
)

if __name__ == "__main__":
    demo.queue().launch()
