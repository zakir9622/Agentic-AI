# The Lookbook — Project Status

> Local-only modest-wear AI try-on for Android. Last updated: 2026-08-19.

## What this app is

**The Lookbook** is a fully on-device Android app for virtual try-on of modest wear — abaya, hijab, niqab, shalwar kameez, kurta, and more. Upload a garment photo, set casting parameters (ethnicity, body type, scenario), and generate photorealistic model shots offline.

- **Package:** `com.zakir.vestra`
- **Target device:** Pixel 9 class (10 GB+ RAM)
- **License:** GPL-3.0

## Current status

| Component | Status |
|---|---|
| Pro on-device engine (SD1.5 + ControlNet + IP-Adapter) | ✅ |
| Casting parameters + prompt builder | ✅ |
| CLIP tokenizer on-device | ✅ (ships in Pro pack) |
| Spatial Material 3 UI | ✅ |
| Sideload release APK | ✅ `assembleSideloadRelease` |
| Pro pack INT8 (`pro-v2-int8`) | Script ready; host on HF |
| Cloud tier | ❌ Removed (100% local) |

## Build

```bash
./gradlew :composeApp:assembleSideloadRelease   # signed sideload APK
./gradlew :shared:testDebugUnitTest
```

Install on Pixel 9, then download Pro pack from **Model packs** inside the app.

## Architecture

```
composeApp/     Spatial UI, casting studio, pack downloads
shared/         EngineRouter (Auto/Lite/Pro), DiffusionEngine, ModelPackManager
ml/             convert_pro_pack.py, quantize_pro_pack.py
```

## Model packs

Hosted at `Iamzakirzr/vestra-packs`:
- `pro-v1` — FP16 (~4.3 GB)
- `pro-v2-int8` — INT8 UNet/ControlNet (~2 GB, Pixel 9 optimized)

Rebuild: see [README.md](../README.md).
