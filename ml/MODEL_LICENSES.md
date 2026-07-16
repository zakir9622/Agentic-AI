# Model licenses

Every model that ships in a published pack MUST be recorded here with a license
that permits redistribution and commercial use. Research-only checkpoints stay
out of the production packs repo.

## Lite pack (lite-v1) — shipping

| Model | Source | License | Commercial use |
|---|---|---|---|
| `garment_seg.onnx` (U²-Net-P) | U²-Net release (mirrored: `tomjackson2023/rembg`) | Apache-2.0 | ✅ Yes |
| `human_parse.onnx` (SCHP, ATR) | Self-Correction Human Parsing (mirrored: `Longcat2957/humanparsing-onnx`) | MIT (code); weights trained on ATR dataset (academic) | ⚠️ Verify: ATR dataset terms are academic-oriented. Before Play release, either confirm with the dataset authors or retrain on a licensed dataset. |

## Pro pack (pro-v1) — BLOCKED for public distribution

| Candidate | License | Status |
|---|---|---|
| CatVTON weights | CC BY-NC-SA 4.0 | ❌ Non-commercial. Fine for personal/dev builds only. |
| IDM-VTON weights | CC BY-NC-SA 4.0 | ❌ Non-commercial. |
| StableVITON | CC BY-NC 4.0 | ❌ Non-commercial. |
| Base SD 1.5 inpainting | CreativeML OpenRAIL-M | ✅ Usable with use-restriction pass-through. |

**Resolution paths for a shippable Pro pack** (decide before Play launch):
1. Train/fine-tune try-on attention modules in-house on licensed data over the
   OpenRAIL-M SD-1.5-inpainting base (CatVTON's trainable footprint is small —
   ~50M params — this is the pragmatic route).
2. License weights commercially from a vendor (e.g. FASHN and similar offer
   commercial try-on models).
3. Ship Lite-only on-device + Cloud (Replicate's commercial API terms cover the
   hosted model) until 1 or 2 lands.

The Android runtime (`DiffusionEngine`) is model-agnostic within the documented
pack contract, so swapping weights requires no app change.
