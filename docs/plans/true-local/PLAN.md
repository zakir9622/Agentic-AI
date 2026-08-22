# True local on Pixel (v3.1.0-rc5+)

**Status:** In progress on `main` / PR track  
**Device floor:** Pixel 8+ / 8 GB RAM · `minSdk 35`

## What genuinely works offline on-device today

| Surface | Status | How |
|---------|--------|-----|
| **Try-on Lite / Pro** | **Works** | `lite-v1` / `pro-v1` packs + ORT |
| **Audio Speak** | **Works** | Android **system TTS** (Google/OEM voices) |
| **Audio voice change** | **Works** | Mic record + DSP knobs (pitch/speed/formant/warmth/clarity) |
| **Quality upscale/matte** | **Works** | Real-ESRGAN / BiRefNet packs (post-step) |
| **Image Create** | **Engine wired** | `AndroidTxt2ImgEngine` — needs published `local-sdturbo-v1` ONNX pack |
| **Video** | Cloud only | Not practical on phones |
| **Code / Chat LLM** | Cloud only | Gemma via LiteRT-LM planned |

## Unlock Create Studio offline

1. Export ONNX on GPU/Colab: `ml/export_image_gen_pack.py --copy-tokenizer` documents required files
2. Validate layout: `python scripts/verify-local-sdturbo-pack.py exports/local-sdturbo-v1`
3. Publish pack `local-sdturbo-v1` to HF packs manifest (or sideload via adb / debug bootstrap)
4. User: Settings → Model packs → download
5. Airplane mode Image Studio should produce a PNG

## Honesty rules

- Never claim Image Create is offline-ready without pack graphs ≥ 1 MB each + CLIP vocab  
- Never route Create Studio through Pro try-on UNet (9-ch inpaint ≠ txt2img)  
- System TTS is real offline Speak — not a neural Kokoro substitute in quality, but it works without downloads  

## Next (stretch)

- Publish `local-sdturbo-v1` weights to HF  
- LiteRT-LM Gemma 3 1B optional pack for Code/News  
- Optional neural TTS pack (`local-tts-v1`) as upgrade over system voices  
