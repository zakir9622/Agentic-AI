# True local on Pixel (v3.1.0-rc7+)

**Status:** Pack published — offline Create unlocked when installed  
**Device floor:** Pixel 8+ / 8 GB RAM · `minSdk 35`

## What genuinely works offline on-device today

| Surface | Status | How |
|---------|--------|-----|
| **Try-on Lite / Pro** | **Works** | `lite-v1` / `pro-v1` packs + ORT (rc6 R8 keep fixes Pixel SIGABRT) |
| **Audio Speak** | **Works** | Android **system TTS** (Google/OEM voices) |
| **Audio voice change** | **Works** | Mic record + DSP knobs |
| **Quality upscale/matte** | **Works** | Real-ESRGAN / BiRefNet packs |
| **Image Create** | **Works when pack installed** | `AndroidTxt2ImgEngine` + published `local-sdturbo-v1` (~994 MB tiny-SD ONNX) |
| **Video** | Cloud only | Not practical on phones |
| **Code / Chat LLM** | Cloud only | Gemma via LiteRT-LM planned |

## Unlock Create Studio offline

1. Install **3.1.0-rc7+**
2. Settings → Model packs → download **local-sdturbo-v1** (~994 MB)
3. Airplane mode → Image Studio → prompt → PNG

Pack source: assembled from public `RanaLLC/tiny-sd-onnx-fp16` via `scripts/assemble-local-sdturbo-pack.py`, published to `Iamzakirzr/vestra-packs`.

## Honesty rules

- Never claim Image Create is offline-ready without pack graphs ≥ 1 MB each + CLIP vocab  
- Never route Create Studio through Pro try-on UNet (9-ch inpaint ≠ txt2img)  
- System TTS is real offline Speak — not a neural Kokoro substitute in quality, but it works without downloads  

## Stretch

- LiteRT-LM Gemma 3 1B optional pack for Code/News  
- Optional neural TTS pack (`local-tts-v1`) as upgrade over system voices  
