# Free Cloud tier on Hugging Face ZeroGPU

Run the Cloud engine at **no per-image cost** by hosting the try-on model on a
Hugging Face **ZeroGPU** Space instead of paying Replicate per generation. The
`tryon` Edge Function uses the Space when `HF_SPACE_URL` is set and falls back to
Replicate otherwise — so wiring this up never breaks the current live path.

## Honest cost & quality

- **Quality:** no compromise. ZeroGPU runs the same models at full resolution on
  an RTX Pro 6000 Blackwell (48/96 GB). The `clean` engine is our commercially-safe
  SD1.5 + ControlNet-Depth + IP-Adapter; `idmvton` matches the max-fidelity model.
  Both are **image-conditioned** (faithful to the actual garment) and `upscale`
  adds a Real-ESRGAN 2× pass.
- **Cost:** hosting a ZeroGPU Space needs **HF PRO ($9/month)** — a small *fixed*
  cost, not per-image. Included quota is **40 GPU-min/day** (PRO), then **$1 per
  10 min** overflow (~6–10¢/image). Free (non-PRO) accounts can't host reliably.
  The only true-$0 photoreal path is the app's **on-device Pro engine**.
- **Reliability:** Spaces cold-start and share a queue — great for MVP/preview and
  early customers, not an enterprise SLA. Keep Replicate as the paid overflow.

## Deploy

### 1. Create the Space
1. New Space → SDK **Gradio** → Hardware **ZeroGPU** (requires PRO).
2. Push `spaces/lookbook-tryon/*` (app.py, requirements.txt, README.md).
3. First launch downloads ~5 GB of weights; the first call is slow, then warm.

### 2. (Optional) enable the `idmvton` engine
1. Duplicate https://huggingface.co/spaces/yisol/IDM-VTON (also ZeroGPU/PRO).
2. On the lookbook-tryon Space, set secrets:
   `IDM_VTON_SPACE = <your-user>/IDM-VTON` and `HF_TOKEN = <read token>`.

### 3. Point the backend at it
Set Edge Function secrets (Supabase → Project → Edge Functions → Secrets, or SQL Vault):
```
HF_SPACE_URL   = https://<your-user>-lookbook-tryon.hf.space
HF_SPACE_TOKEN = <hf read token>   # optional; better rate limits on the Space
```
Redeploy `tryon` (or it picks up secrets on next cold start). Done — the Cloud tier
now uses the free Space, `model:"clean"` by default.

### 4. Choose the engine per request
The app / caller can send `"model": "clean"` (commercial-safe, default) or
`"idmvton"` (max fidelity, preview). Unset = `clean`.

## Validate on first deploy
The Edge Function talks to Gradio via `/gradio_api/call/tryon`. Gradio's FileData
shape varies across versions; if the first call fails, check the Space logs and
adjust `callHfSpace` in `supabase/functions/tryon/index.ts` (the image-input and
SSE-parsing lines are commented). The Replicate fallback keeps the tier working
meanwhile.

## Licensing reminder
`clean` is safe for paid B2B. `idmvton` (and Kolors/CatVTON/Leffa/FitDiT) trace to
the VITON-HD/DressCode research datasets — treat their output as preview/personal
until you license a commercial model or fine-tune on a commercial dataset. See
`docs/OPENSOURCE_OPPORTUNITIES.md`.
