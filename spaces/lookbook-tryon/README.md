---
title: The Lookbook Cloud Try-On
emoji: 👗
colorFrom: gray
colorTo: indigo
sdk: gradio
sdk_version: 5.9.1
app_file: app.py
pinned: false
suggested_hardware: zero-a10g
---

# The Lookbook — free Cloud try-on (ZeroGPU)

Gradio Space that generates a photoreal on-model shot from a person image + a
garment image, at **no per-image cost** within ZeroGPU's daily quota. The app's
Supabase Edge Function calls its `/tryon` API.

## Two switchable engines

| `model` | What | License | Use for |
|---|---|---|---|
| `clean` *(default)* | SD1.5 + ControlNet-Depth + IP-Adapter-Plus (Realistic Vision V5.1) | **commercially clean** (not VITON-HD/DressCode trained) | paid B2B output |
| `idmvton` | routes to a duplicated `yisol/IDM-VTON` Space | VITON-HD (non-commercial) | max fidelity, preview/personal |

`upscale=True` adds a Real-ESRGAN 2× pass (Lanczos fallback) — resolution up, no
extra diffusion cost.

## Deploy (one-time)

> **Requires HF PRO ($9/mo)** to attach ZeroGPU hardware to a personal Space.
> This is a small *fixed* cost, not per-image. The only true-$0 path is the
> app's on-device Pro engine.

1. **New Space** → SDK **Gradio** → Hardware **ZeroGPU**. Push these files.
2. (Optional, for `idmvton`) Duplicate https://huggingface.co/spaces/yisol/IDM-VTON,
   then set this Space's secret `IDM_VTON_SPACE = <your-user>/IDM-VTON` and
   `HF_TOKEN` (a read token).
3. First run downloads the models (~5 GB) — the initial call is slow, then warm.
4. Copy the Space URL and point the backend at it (see `docs/CLOUD_ZEROGPU.md`).

## API

`POST` via `gradio_client` or HTTP to `/tryon` with:
`(person_image, garment_image, model="clean", upscale=True, seed=42)` → image.
