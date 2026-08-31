# Vestra Architecture

> **Note on scope**: this doc's module layout and Play-compliance invariants are current. Its
> try-on-specific pipeline description (below the fold) covers a real feature that is
> implemented but currently has no entry point in the app's navigation — see
> `docs/PROJECT_HISTORY.md`. For the architecture that actually drives the app's four active
> studios (Image/Video/Code/Audio) today — `GenerativeCloudService`, the cloud-consent gate,
> model routing — see `docs/FUNCTIONALITY.md`, which supersedes this file's old "CloudEngine →
> Supabase Edge Function" description below (that backend was retired; there is no
> server component anymore).

## Overview

The app generates images, video, code, and audio — mostly on-device, with an optional free
cloud fallback. It also still contains its original feature, virtual try-on (garment photo →
photorealistic model wearing it), described in the pipeline section below. Everything the user
does day-to-day — capture, generation, history — works **fully offline**; the network is used
only to download model packs once and, when the user explicitly opts in (see
`docs/FUNCTIONALITY.md`'s cloud-consent model), for cloud generation.

```
┌────────────────────────────── composeApp (Android) ──────────────────────────────┐
│  Jetpack Compose UI · navigation · CameraX capture · photo picker · AGSL shaders │
└───────────────┬───────────────────────────────────────────────────────────────────┘
                │
┌───────────────▼────────────────── shared (KMP) ───────────────────────────────────┐
│ commonMain (pure Kotlin — iOS-reusable)                                            │
│   domain/    TryOnRequest · TryOnResult · GenerationState · ModelPack · EngineTier │
│   engine/    TryOnEngine interface · EngineRouter (AUTO policy) — try-on only      │
│   cloud/     GenerativeCloudService (dispatch) · CloudModelRouting (fallback       │
│              chains) · CloudModelCatalog · FreeCloudDiscovery                      │
│   packs/     ModelPackManager: HF manifest, downloads, checksums                   │
│   wardrobe/  generation history (JSON index via TextFileStore seam)                │
│   settings/  AppSettings — selection, consent gate, safety presets                 │
│   safety/    SafetyPresets · content-filter orchestration                          │
│ androidMain                                                                        │
│   LiteEngine (LiteRT) / LiteRtLmEngine (LiteRT-LM) — local generation              │
│   DiffusionEngine (ONNX Runtime + QNN EP) — try-on Pro tier                        │
│   BonsaiImageEngine — local text-to-image (LiteRT-LM DiT)                          │
│   DeviceCapabilities probe                                                         │
└────────────────────────────────────────────────────────────────────────────────────┘
```

`GenerativeCloudService` (not `TryOnEngine`/`EngineRouter`) is the dispatcher for the app's four
active studios — see `docs/FUNCTIONALITY.md` for its full local-first/cloud-fallback design and
the consent model that gates every network call. `TryOnEngine`/`EngineRouter` below describe the
older, still-present but currently-unreachable try-on feature specifically.

## Module choices

- **`composeApp` is a plain Android module** using Jetpack Compose (androidx artifacts), not the JetBrains Compose Multiplatform plugin. Reason: maximum build reliability and full access to Android-only APIs the cinematic UI needs (AGSL `RuntimeShader`, haptics, CameraX). Compose code migrates to CMP nearly verbatim when the iOS port starts; the real portability boundary is `shared/`.
- **`shared` is Kotlin Multiplatform** with `commonMain` kept free of Android imports. ML engines are `androidMain` implementations of the common `TryOnEngine` interface; iOS later supplies CoreML `actual`s (see `IOS_PORT.md`).
- **Manual DI** in `VestraApp` — the graph is a handful of singletons; a DI framework adds no value at this size.

## Engine routing (try-on)

`EngineRouter.resolve`:

- `AUTO` → `PRO` if its pack is installed **and** the device passes the capability gate, else `LITE`.
- `CLOUD` is **never** selected implicitly — try-on's cloud tier additionally no longer has a
  working backend at all (see below). The Play data-safety declaration ("data collected: none
  by default") depends on the never-implicit invariant for whichever tiers *do* run; the app's
  four active studios enforce the equivalent invariant through the cloud-consent gate described
  in `docs/FUNCTIONALITY.md`, not through this router.

Engines never throw for expected failures; they emit `GenerationState.Failed(TryOnError)` so the UI can render every failure state cinematically instead of crashing.

## Try-on pipelines

> Try-on is fully implemented but has no entry point in the current navigation — see
> `docs/PROJECT_HISTORY.md`. The Lite and Pro tiers below still run if reached directly; the
> Cloud tier does not (see "Cloud (M5)" below).

### Lite (M3) — all devices, ~300 MB pack
1. Garment segmentation (U²-Net-class, INT8 LiteRT) → cutout + mask
2. Person analysis: pose landmarks + human parsing → body-region masks
3. TPS/appearance-flow warp of the garment onto the target regions
4. Harmonization net blends lighting/color at the seams

### Pro (M4) — flagships, ~2.5–4 GB pack
CatVTON-class single-UNet try-on diffusion, INT8-quantized, executed with ONNX Runtime + QNN EP (NPU) with CPU/GPU fallback. Reuses Lite's stage-1/2 outputs for the inpaint mask. Gated on `DeviceCapabilities` (RAM ≥ 8 GB, supported accelerator).

### Cloud (M5) — retired
This tier's backend (upload to Supabase Storage via short-TTL signed URLs → an Edge Function
calling Replicate → result streamed back, inputs deleted) **no longer exists** — the Supabase
project was retired when the app's cloud strategy moved to free-tier Hugging Face Spaces /
Inference Providers, Groq, and OpenRouter for its active studios (see `docs/FUNCTIONALITY.md`).
Try-on's Cloud tier has not been re-pointed at that architecture; only Lite and Pro run today.

### AI-model mode
"Generate on an AI model" = try-on onto a base image from a curated gallery of synthetic/licensed model photos (downloadable pack). Identical code path across all tiers; no on-device text-to-image model needed.

## Model packs (M3)

`manifest.json` on a Hugging Face Hub repo lists packs → files → sha256/bytes → `DeviceSpec` gate. Downloads are resumable (HTTP Range) via Ktor inside a WorkManager `dataSync` foreground worker; files verify against sha256 before an atomic move into `filesDir/packs/<id>/<version>/`.

## Play compliance invariants (full checklist in PLAY_COMPLIANCE.md)

- No `READ_MEDIA_*` permissions — system photo picker only.
- Person-photo mode is gated behind a one-time likeness-consent acknowledgement.
- Every generated image is watermarked + metadata-tagged as AI-generated, and every result screen exposes a Report action.
- Safety classifiers run on inputs before any engine (including Cloud) executes.
