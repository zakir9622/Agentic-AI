# The Lookbook — Project Status

> Modest-wear AI studio for Android. Last updated: 2026-08-22.

## What this app is

**The Lookbook** is an Android app for virtual try-on and AI studios — abaya, hijab, niqab, shalwar kameez, and more. It runs **on-device** (Lite/Pro ONNX packs) and optional **free cloud** studios (HF Spaces + Inference Providers).

- **Package:** `com.zakir.vestra`
- **Target device:** Pixel 9 class (10 GB+ RAM) for Pro; Lite runs on Android 8+
- **License:** GPL-3.0

## Current status

| Component | Status |
|---|---|
| Lite on-device try-on (ONNX seg + parse) | ✅ |
| Pro on-device engine (SD1.5 + ControlNet + IP-Adapter) | ✅ |
| Cloud image / edit / code / video (free HF + Groq + OpenRouter) | ✅ |
| HF Inference Providers fallback (OpenCode-style) | ✅ |
| HF router model discovery on token save | ✅ |
| Quality packs (BiRefNet, Real-ESRGAN) export scripts | ✅ scripts ready |
| Settings hub + token wizard | ✅ |
| Input safety gate (prompt filter) | ✅ v1 |

## Build

```bash
./gradlew :composeApp:assembleSideloadRelease   # signed sideload APK
./gradlew :shared:testDebugUnitTest
python3 scripts/probe-models.py --quick          # live cloud smoke test
bash scripts/verify-lite-pack.sh                 # debug lite assets
```

## Architecture

```
composeApp/     Compose UI, studios, settings hub, pack downloads
shared/         EngineRouter, cloud clients, discovery, safety gate
ml/             export_lite_pack.py, export_birefnet_pack.py, manifest_gen.py
```

## Model packs

Hosted at `Iamzakirzr/vestra-packs`:
- `lite-v1` — Lite ONNX (~68 MB, bundled in debug)
- `pro-v2-int8` — INT8 UNet/ControlNet (~2 GB)
- `pro-v1` — FP16 fallback (~4.3 GB)
- `birefnet-v1`, `realesrgan-v1` — optional quality packs (export via ml/)

Rebuild: see [README.md](../README.md).
