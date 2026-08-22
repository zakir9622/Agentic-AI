# Local model research (v3)

Open-source models evaluated for on-device use in The Lookbook.

## Try-on (shipping)

| Pack | Role | Size | Status |
|------|------|------|--------|
| `lite-v1` | Garment seg + human parse (ONNX) | ~68 MB | **Required** — masks for Lite and Pro |
| `pro-v1` | SD1.5 FP16 + ControlNet | ~4.3 GB | Shipping on HF manifest |
| `pro-v2-int8` | SD1.5 INT8 | ~2 GB | Shipping — Pixel-class 8 GB+ RAM |

**Constraint:** Pro cannot run without `lite-v1` human parsing. Do not remove lite pack until a replacement mask pipeline ships.

## Quality post-steps (optional packs)

| Pack | Model | Status |
|------|-------|--------|
| `birefnet-v1` | BiRefNet matting | ONNX export pending — wired, inactive |
| `realesrgan-v1` | Real-ESRGAN 2×/4× | ONNX export pending — wired, inactive |
| `gfpgan-v1` | GFPGAN face restore | Planned |

## Create / Code / Video (not local yet)

| Direction | Candidates | Blocker |
|-----------|------------|---------|
| Image gen | FLUX Schnell, SD Turbo, LCM | 1–3 GB + NNAPI/DSP variance on Android |
| Code LLM | Qwen2.5-Coder 1.5B, Gemma 2B | ExecuTorch / MediaPipe integration not in build |
| Video | — | Not practical on phones; cloud LTX-Video only |

**Current approach:** Hide non-runnable catalog entries from pickers; use cloud HF / Groq / OpenRouter for Create Studio.

## Session caching

`OrtSessionCache` reuses ONNX sessions per model path to cut cold-start latency on repeat try-on shots. Invalidate when pack root changes (re-download / verify).

## lite-v2 (research)

Potential improvements for a future `lite-v2` pack:

- Smaller garment seg (MobileSAM-class) for faster first layer
- Updated human parser (SCHP / Graphonomy export) for better abaya/hijab regions
- Quantized INT8 graphs where ORT mobile EP supports them

Export pipeline: train/export on desktop → validate with `scripts/benchmark-local.py` → publish to `Iamzakirzr/vestra-packs` manifest.

## References

- [ONNX Runtime Android](https://onnxruntime.ai/docs/get-started/with-java.html)
- [CreativeML OpenRAIL-M](https://huggingface.co/spaces/CompVis/stable-diffusion-license) (SD1.5)
- HF manifest: `https://huggingface.co/datasets/Iamzakirzr/vestra-packs`
