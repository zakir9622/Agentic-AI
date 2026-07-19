# The Lookbook — Project Status & Handoff

> Single source of truth for picking this project back up (with any AI or
> developer). Covers what the app is, everything built so far, the live
> infrastructure, the architecture, known issues, and the roadmap.
> Last updated: 2026-07-18.

---

## 1. What this app is

**The Lookbook** is an Android **AI garment photoshoot studio**. Input: one photo
of a garment (flat-lay, hanger, or catalog shot). Output: a photorealistic image
of a **model wearing that garment**, staged on a studio backdrop across one or
more poses ("a shoot" = several shots).

- **Audience:** B2B modest-wear sellers first (abaya / hijab / kurta / kaftan are
  first-class categories), western wear supported too.
- **Privacy-first:** on-device generation is the default and works fully offline.
- **Internal namespace:** `com.zakir.vestra` (kept from the first iteration; the
  product name is "The Lookbook").
- **License:** GPL-3.0 (repo). Model licenses tracked in `ml/MODEL_LICENSES.md`.

---

## 2. What is LIVE right now

| Thing | Status | Where |
|---|---|---|
| **Cloud tier (Replicate IDM-VTON)** | ✅ Live | Supabase project `lookbook-cloud` (ref `todzunpexvvmbxpvdyap`, region ap-south-1) |
| — `tryon` + `report` Edge Functions | ✅ Deployed | `verify_jwt=on`; app sends anon JWT |
| — Replicate token | ✅ In Supabase Vault | read by a `service_role`-only accessor (migration 0002) |
| — App wired to it | ✅ | `VestraApp.SUPABASE_URL` / `SUPABASE_ANON_KEY` set; `cloudConfigured=true` |
| **On-device Pro pack (SD1.5+ControlNet+IP-Adapter)** | ✅ Converted, validated, hosted | HF dataset `Iamzakirzr/vestra-packs`, `pro-v1/` (~4.3 GB fp16) |
| — App wired to it | ✅ | `VestraApp.PACKS_MANIFEST_URL` → that dataset's `manifest.json` |
| **Lite pack** | ⚠️ Not hosted (deprioritized) | Compositor quality was rejected; manifest currently lists only `pro-v1` |

**Requires the owner to act:** add a Replicate billing method (live cloud runs
need credit); on-device Pro pack downloads/runs only on **12 GB+ RAM flagship
phones** (`minSpec.minRamMb = 11000`).

---

## 3. Architecture

Kotlin Multiplatform. `shared/` is iOS-reusable pure Kotlin in `commonMain` with
Android actuals in `androidMain`; `composeApp/` is the Android UI.

```
composeApp/            Android app (Jetpack Compose, Material 3, cinematic dark UI)
  ui/                    screens (studio/garment/person/generate/result/wardrobe/settings/packs)
  VestraApp.kt           composition root — wires engines, packs, cloud config, constants
shared/
  commonMain/            domain models, TryOnEngine interface, EngineRouter, ModelPackManager,
                         CloudEngine, wardrobe repo, settings, safety, pipeline contracts
  androidMain/           LiteEngine, DiffusionEngine (Pro), ONNX wrappers (OrtModel/OrtGraph),
                         HumanParsing, LatentCodec/UnetRunner, SdControlNetPipeline, platform IO
ml/                     Python export/convert tooling (NOT shipped in the app)
supabase/              Edge Functions (tryon/report) + migrations for the cloud tier
docs/                  architecture, pipeline, compliance, privacy, iOS port, this file
```

### Engine routing (`shared/.../engine/EngineRouter.kt`)
`Settings → Auto | Lite | Pro | Cloud`.
- **Auto** = best installed **on-device** engine (Pro if pack installed + device
  capable, else Lite). **Auto never routes to Cloud** — a hard privacy invariant.
- **Cloud** only runs when explicitly selected and online.

### The three engines (all behind `TryOnEngine`)
1. **Lite** (`LiteEngine`) — u2netp garment segmentation + SCHP-ATR human parsing +
   contour-mesh warp + harmonization. A **compositor**, not a generator: fast,
   offline, every phone — but cannot synthesize a person (won't reach photoreal).
2. **Pro** (`DiffusionEngine` → `SdControlNetPipeline`) — SD1.5 + ControlNet-Depth
   + IP-Adapter-Plus, staged STRUCTURE→TEXTURE→SYNTHESIS. Photoreal, offline, free
   per image. **12 GB+ RAM flagship only** (~4.3 GB fp16 weights, ~30–60 s/image).
3. **Cloud** (`CloudEngine`) — Ktor → Supabase Edge Function → Replicate IDM-VTON.
   Any phone, online, ~2–5¢/image. Inputs deleted after every run.

### The on-device Pro ONNX contract (verified end-to-end on CPU)
Produced by `ml/convert_pro_pack.py`, consumed by `SdControlNetPipeline.kt`:
- `text_encoder.onnx`: `input_ids[1,77]` → `[1,77,768]`
- `vae_encoder/decoder.onnx`: `[1,3,512,512] ↔ [1,4,64,64]`
- `controlnet.onnx`: `(sample,t,text,depth[1,3,512,512])` → 12 `down_*` + `mid` residuals
- `ip_image_encoder.onnx`: garment `[1,3,224,224]` → CLIP-H penultimate `[1,257,1280]`
- `unet.onnx`: `(sample, t, text[1,77,768], image_embeds[1,1,257,1280], down_0..down_11, mid)` → `noise[1,4,64,64]`
- `depth.onnx`: `[1,3,518,518]` → `[1,1,518,518]`

**Key facts learned making it actually run:** opset **18** (17 breaks Split/Resize);
**no separate `ip_proj`** — the IP-Adapter Plus resampler + attention are baked into
`unet.onnx`; `image_embeds` is **4-D** `[batch,num_images,seq,dim]` via
`added_cond_kwargs` (not concatenated into `encoder_hidden_states`); FP16 packing
uses `disable_shape_infer` for >2 GB graphs (protobuf 2 GB serialize limit).

---

## 4. Build phases (all merged unless noted)

| Phase | What | Status |
|---|---|---|
| **M1** | Repo wipe (old ecommerce removed), KMP scaffold, Android SDK 36, CI | ✅ |
| **M2** | All screens, consent gate, wardrobe persistence, photo picker/camera, mock engine | ✅ |
| **M3** | Real Lite engine (ONNX) + ModelPackManager (sha256 atomic installs) + resumable WorkManager downloader | ✅ |
| **M4** | Pro diffusion engine (CatVTON-style concat, DDIM scheduler, device gating, benchmark logging) | ✅ |
| **M5** | Cloud tier — CloudEngine, EXIF-stripped uploads, ReportQueue, Supabase functions | ✅ |
| **M6** | Cinematic polish (AGSL develop shader, before/after slider, haptics) + Play compliance kit | ✅ |
| **P0** | Photoshoot pivot — shot sets, casting UX, copy rebrand to "The Lookbook" | ✅ |
| **P1** | Modest-wear-first garment taxonomy + Lite quality upgrades | ✅ |
| **P2** | Dev-pack support (non-commercial research weights, private-only) | ✅ |
| **P3** | Training-readiness kit + eval harness (`ml/train/`, `ml/eval/`) | ✅ |
| **P4** | Multi-garment outfits (layered), auto-orientation, manual rotate, HF setup guide | ✅ |
| **P5** | Worn-photo input guard + honest engine docs | ✅ |
| **P6** | Multi-conditioning pipeline architecture + photorealism prompt engineering + cyber-atelier UI | ✅ |
| **P7** | On-device SD1.5 + ControlNet-Depth + IP-Adapter pipeline (architecture) | ✅ |
| **Cloud provisioning** | Supabase project created, migrations applied, functions deployed, Vault token, app wired (PR #10) | ✅ |
| **Pro conversion** | Colab notebook (PR #11) → **actual on-CPU conversion + validation of all 7 ONNX components, fp16 pack built + hosted, runtime aligned to verified contract** (PR #12) | ✅ |

---

## 5. Key files map

- **Composition root:** `composeApp/src/main/kotlin/com/zakir/vestra/VestraApp.kt`
  — all constants (`PACKS_MANIFEST_URL`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`) live here.
- **Routing:** `shared/src/commonMain/.../engine/EngineRouter.kt`
- **Pro pipeline:** `shared/src/androidMain/.../engine/pro/SdControlNetPipeline.kt`,
  `DiffusionEngine.kt` (+ `ProPackConfig`), `OrtGraph.kt`, `DdimScheduler.kt`
- **Lite pipeline:** `shared/src/androidMain/.../engine/lite/*`
- **Packs:** `shared/src/commonMain/.../packs/ModelPackManager.kt`, `AndroidPackFileSystem`, `PackDownloadWorker`
- **Cloud:** `shared/src/commonMain/.../cloud/CloudEngine.kt`, `supabase/functions/{tryon,report}/index.ts`, `supabase/migrations/*`
- **ML tooling:** `ml/download_pro_models.sh`, `ml/convert_pro_pack.py`, `ml/export_depth.py`, `ml/manifest_gen.py`, `ml/colab_convert_pro_pack.ipynb`
- **Docs:** `docs/ARCHITECTURE.md`, `docs/PIPELINE.md`, `docs/CLOUD_SETUP.md`, `docs/HUGGINGFACE_SETUP.md`, `docs/PLAY_COMPLIANCE.md`, `docs/PRIVACY_POLICY.md`, `docs/IOS_PORT.md`

---

## 6. Build / test / run

Requires JDK 17+ and Android SDK (platform 36).

```bash
./gradlew :composeApp:assembleDebug     # build the APK
./gradlew :shared:testDebugUnitTest     # core unit tests
./gradlew :composeApp:lintDebug         # lint
```

CI: `.github/workflows/android-ci.yml` builds + tests on every PR. The debug APK
is ~129 MB (Lite pack bundled in debug via `DebugPackBootstrap`; release ships no pack).

**Regenerate/host the Pro pack** (no GPU needed, ~15 GB RAM):
```bash
cd ml && ./download_pro_models.sh pro_src
python convert_pro_pack.py --src pro_src --out exports/pro-v1
python export_depth.py --out exports/pro-v1/depth.onnx
python manifest_gen.py exports/ --base-url https://huggingface.co/datasets/<user>/vestra-packs/resolve/main
# upload exports/pro-v1 + manifest.json to the HF dataset (see docs/HUGGINGFACE_SETUP.md)
```

---

## 7. Known issues, limitations & risks

**None are known crashes** — the code is defensively guarded (no `!!` in
production, list access guarded, engines emit `GenerationState.Failed` on error).
The items below are quality/feasibility limitations:

1. **Pro pack is huge (~4.3 GB) and flagship-only.** IP-Adapter *Plus* pulls in a
   1.2 GB CLIP-H encoder. Runs only on 12 GB+ RAM phones, ~30–60 s/image. → **INT8
   quantization is the top follow-up** (~1.5–2 GB, 8 GB phones). Not yet done.
2. **On-device text prompt is neutral.** No on-device CLIP BPE tokenizer ships, so
   `encodePrompt` feeds zero ids — the PromptStyle photorealism tokens are NOT
   applied on-device (IP-Adapter image tokens carry appearance). A tokenizer pack
   would enable text guidance. (Cloud path passes the prompt through fine.)
3. **Depth preprocessing is approximate.** `ImageOps.toNormalizedChw` isn't the
   exact ImageNet mean/std Depth-Anything expects — minor structural-quality loss.
4. **On-device Pro has never been run on a real device.** Every ONNX component was
   validated on CPU (correct shapes), but end-to-end quality/latency/thermals on an
   actual NPU are unverified. First run may surface tuning needs (log tag `VestraProBench`).
5. **Cloud model is western-tuned.** Default `cuuupid/idm-vton` isn't modest-wear-
   specialized; abaya/hijab drape may need a better `REPLICATE_MODEL_VERSION` (env
   swap, no code change).
6. **Replicate billing required** for live cloud runs (token authenticates but
   needs account credit).
7. **Lite pack unhosted.** The manifest lists only `pro-v1`; the app's Lite/Auto
   path has no downloadable pack in production (debug bundles one). Decide whether
   to host Lite or drop it.
8. **No rate limiting on the cloud function** — add before public launch (per-IP,
   e.g. Supabase built-in or Upstash) to prevent token-cost abuse.

---

## 8. Next-steps roadmap (prioritized)

### Near-term (make the core experience solid)
1. **INT8-quantize the Pro pack** → ~1.5–2 GB, runs on 8 GB phones. Biggest unlock
   for "free on-device on normal phones". Use `onnxruntime.quantization` (static
   with a small calibration set for the UNet; dynamic for encoders). Re-validate
   on CPU, re-host, lower `minSpec.minRamMb`.
2. **Real device test pass** for Pro — install on a 12 GB+ phone, run the sample
   suits, capture `VestraProBench` timings + any ORT errors, fix.
3. **Modest-wear cloud model** — trial IDM-VTON alternatives / a fine-tune tuned for
   flowing covered garments; make `REPLICATE_MODEL_VERSION` a per-category choice.
4. **Cloud hardening** — per-IP rate limiting + a simple abuse guard on `tryon`.

### Mid-term (quality + reach)
5. **On-device CLIP tokenizer pack** so text guidance (PromptStyle) actually applies
   on-device — meaningfully improves Pro realism.
6. **Studio-model gallery pack** — real diverse model photos (`ml/build_models_pack.py`)
   replacing bundled silhouettes; host as a `MODELS` pack.
7. **Fix depth normalization** to Depth-Anything's exact preprocessing.
8. **Batch shoots UX polish** — progress per shot, retry a single failed layer,
   share/export a full shoot as a set.

### Longer-term
9. **iOS port** — `shared/commonMain` is ready; needs CoreML actuals + SwiftUI shell
   (`docs/IOS_PORT.md`). CoreML would also give a much better on-device perf story
   than ONNX on Apple silicon.
10. **Train a commercially-clean, mobile-first try-on model** (`ml/train/`) — an
    LCM/SD-Turbo-distilled, quantization-friendly UNet to cut steps (4–8) and size.
11. **Play Store submission** — data-safety form (`docs/PLAY_COMPLIANCE.md` is
    pre-filled), store listing assets, closed testing track.
12. **Monetization hooks** — billing scaffold exists in the architecture; wire a
    credits/subscription model when ready (free in v1).

### Engineering hygiene
13. **Instrumented tests** for the generate flow (currently unit-tested only).
14. **Crash/analytics** (opt-in) to catch on-device Pro failures in the wild.
15. **APK size** — release build strategy for the 129 MB debug (pack is downloaded,
    not bundled, in release; verify).
