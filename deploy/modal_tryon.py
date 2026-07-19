"""The Lookbook — free Cloud try-on on Modal (serverless GPU, free credits).

One-command deploy of the commercially-clean SD1.5 + ControlNet-Depth +
IP-Adapter-Plus pipeline as an autoscaling HTTPS endpoint. No fixed subscription
— Modal's Starter tier includes monthly credits and scales to zero when idle.

Deploy (on YOUR machine — Modal's CLI needs gRPC, which the agent sandbox can't
carry, so this one step is yours):

    pip install modal
    modal token new                 # or: modal token set --token-id ... --token-secret ...
    # (recommended) lock the endpoint with a shared key:
    modal secret create lookbook-api LOOKBOOK_API_KEY=$(openssl rand -hex 16)
    modal deploy deploy/modal_tryon.py

`modal deploy` prints the endpoint URL (…/generate). Give it to the app owner /
set it as the Edge Function secret MODAL_URL (and MODAL_KEY = the same key). See
docs/CLOUD_MODAL.md.

Request (POST JSON):  {person_b64, garment_b64, model?, seed?, api_key?}
Response:             image/jpeg bytes
"""
import modal

app = modal.App("lookbook-tryon")

image = (
    modal.Image.debian_slim(python_version="3.11")
    .pip_install(
        "torch==2.4.0",
        "diffusers==0.31.0",
        "transformers==4.44.2",
        "accelerate",
        "safetensors",
        "pillow",
        "numpy",
        "fastapi[standard]",
    )
)

HF_CACHE = "/cache"
cache_vol = modal.Volume.from_name("lookbook-hf-cache", create_if_missing=True)

REALVIS_URL = (
    "https://huggingface.co/SG161222/Realistic_Vision_V5.1_noVAE/resolve/main/"
    "Realistic_Vision_V5.1_fp16-no-ema.safetensors"
)
POSITIVE = (
    "professional studio fashion photograph, photorealistic, a person wearing the "
    "garment, natural skin texture, soft studio lighting, sharp focus, high detail, "
    "editorial catalog quality, 85mm lens"
)
NEGATIVE = (
    "deformed, distorted, disfigured, extra limbs, bad anatomy, blurry, lowres, "
    "cartoon, 3d render, watermark, text, mannequin, plastic skin"
)


@app.cls(
    gpu="A10G",
    image=image,
    volumes={HF_CACHE: cache_vol},
    timeout=600,
    scaledown_window=300,  # keep warm 5 min after a request, then scale to zero
)
class TryOn:
    @modal.enter()
    def load(self):
        import os

        os.environ["HF_HOME"] = HF_CACHE
        import torch
        from diffusers import (
            AutoencoderKL,
            ControlNetModel,
            StableDiffusionControlNetPipeline,
            UniPCMultistepScheduler,
        )
        from transformers import pipeline as hf_pipeline

        self.depth = hf_pipeline(
            "depth-estimation",
            model="depth-anything/Depth-Anything-V2-Small-hf",
            device=0,
        )
        controlnet = ControlNetModel.from_pretrained(
            "lllyasviel/control_v11f1p_sd15_depth", torch_dtype=torch.float16
        )
        vae = AutoencoderKL.from_pretrained(
            "stabilityai/sd-vae-ft-mse", torch_dtype=torch.float16
        )
        pipe = StableDiffusionControlNetPipeline.from_single_file(
            REALVIS_URL, controlnet=controlnet, vae=vae,
            torch_dtype=torch.float16, safety_checker=None,
        )
        pipe.scheduler = UniPCMultistepScheduler.from_config(pipe.scheduler.config)
        pipe.load_ip_adapter(
            "h94/IP-Adapter", subfolder="models",
            weight_name="ip-adapter-plus_sd15.safetensors",
        )
        pipe.set_ip_adapter_scale(0.75)
        self.pipe = pipe.to("cuda")

    @modal.fastapi_endpoint(method="POST")
    def generate(self, item: dict):
        import base64
        import io
        import os

        import torch
        from fastapi import HTTPException, Response
        from PIL import Image

        expected = os.environ.get("LOOKBOOK_API_KEY")
        if expected and item.get("api_key") != expected:
            raise HTTPException(status_code=401, detail="unauthorized")

        try:
            person = Image.open(io.BytesIO(base64.b64decode(item["person_b64"]))).convert("RGB").resize((512, 512))
            garment = Image.open(io.BytesIO(base64.b64decode(item["garment_b64"]))).convert("RGB").resize((512, 512))
        except Exception:
            raise HTTPException(status_code=400, detail="person_b64 and garment_b64 required")

        depth = self.depth(person)["depth"].convert("RGB").resize((512, 512))
        gen = torch.Generator("cuda").manual_seed(int(item.get("seed", 42)))
        out = self.pipe(
            prompt=POSITIVE, negative_prompt=NEGATIVE,
            image=depth, ip_adapter_image=garment,
            num_inference_steps=28, guidance_scale=7.0,
            controlnet_conditioning_scale=0.9, generator=gen,
        ).images[0]
        buf = io.BytesIO()
        out.save(buf, format="JPEG", quality=92)
        return Response(content=buf.getvalue(), media_type="image/jpeg")
