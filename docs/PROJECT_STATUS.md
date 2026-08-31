# The Lookbook — Project Status

> Local-first multi-modal AI studio for Android (Image / Video / Code / Chat / Audio, plus a
> currently-unreachable Try-on feature). Last updated: 2026-08-31, after the merge of PR #81
> ("Generation audit follow-through") on top of PR #80 (the GoogleLookBookUI cross-repo port).
> App version at time of writing: **v3.1.8** (`versionCode 94`) per `composeApp/build.gradle.kts`
> — note the two PRs above landed after the last `CHANGELOG.md`-tracked version bump; see
> `docs/PROJECT_HISTORY.md` Eras 6–7 for what they contain.

- **Package:** `com.zakir.vestra`
- **Target device:** Pixel 9 class (10 GB+ RAM) for on-device Pro-tier generation; broader
  support on **Android 15+** (`minSdk = 35`)
- **License:** GPL-3.0

For the full narrative of how the app got here, see `docs/PROJECT_HISTORY.md`. For current UI
and functional design, see `docs/UI_DESIGN.md` and `docs/FUNCTIONALITY.md`. For a plain,
continuously-updated list of real limitations, see `docs/DRAWBACKS.md` — that file, not the
table below, is the source of truth for "is X actually verified."

## Current status

| Area | Status |
|---|---|
| Local generation — Image (Bonsai LiteRT-LM + SD-Turbo ONNX), Code/Chat (LiteRT-LM Qwen3/Gemma) | ✅ Shipping |
| Cloud generation — Image/Video/Code/Chat/Audio (free HF Spaces + HF Inference + Groq + OpenRouter) | ✅ Shipping |
| Cloud/on-device availability | ✅ Automatic, credential-based — no manual toggle (removed in PR #81) |
| Cloud reachability gate | ✅ Implicit consent (`AppSettings.cloudConsentGranted`), granted only by real user action |
| Image generation parameters (steps/guidance/seed/strength/batch) | ✅ Wired to local UI (PR #81) — dead code before that |
| In-composer model quick switcher | ✅ Shipping (PR #81) |
| "Reduce fashion false positives" safety assist | ✅ Shipping, opt-in, default off (PR #81) |
| Creative Studio V2 (1–4 image candidates per request) | ✅ Shipping (GoogleLookBookUI port, PR #80) |
| Prompt Director (structured prompt builder) | ✅ Shipping (PR #80) |
| Voice-cloning / vocal-editor pipeline | ✅ Shipping, unverified on real device I/O (PR #80) |
| Diagnostics export (ZIP bundle: system info, run history, logs, pack status) | ✅ Shipping (PR #80) |
| Isolated per-modality screens + conversation-style history | ✅ Shipping (v3.1.4–3.1.6) |
| 3-item bottom dock (Home/Library/Settings) | ✅ Shipping (v3.1.4) |
| GPU/NPU/speculative-decoding LiteRT-LM backends | ✅ Default on, with safe CPU fallback (v3.1.7–3.1.8) |
| Try-on (Lite/Pro on-device pipeline) | ⏳ Implemented, no entry point in current navigation |
| Try-on cloud tier | ❌ Retired (old Supabase/Replicate backend removed, no replacement) |
| `pro-v2-int8` (try-on) HF manifest | ⏳ Export ready; app prefers `pro-v1` until upload |
| Real on-device benchmark numbers | ❌ Not captured — blocked on device access |
| Appium suite execution on a real device/emulator | ❌ Suite exists (`appium/`), never executed |
| QNN execution-provider packaging / ONNX NSFW classifier | ❌ Not started |
| iOS target | ❌ `commonMain` is `expect`/`actual`-clean for JVM APIs; no iOS target declared |

## Build

```bash
./gradlew :composeApp:assembleSideloadRelease   # signed sideload APK
./gradlew :shared:testDebugUnitTest :composeApp:testSideloadDebugUnitTest
./gradlew :composeApp:lintSideloadDebug
python3 scripts/integration-local-models.py --skip-hf-download
python3 scripts/benchmark-local.py
python3 scripts/probe-models.py --quick          # live cloud smoke (needs tokens)
```

## Architecture (one-line map)

```
composeApp/     Screens, navigation, model picker/quick-switcher, settings UI
shared/         GenerativeCloudService (dispatch), AppSettings (consent + selection),
                CloudModelRouting (fallback chains), Local/CloudModelCatalog, safety, diagnostics
ml/             export_*.py, manifest_gen.py — model pack tooling
```

Full detail: `docs/ARCHITECTURE.md`.

## Model packs

Hosted at `Iamzakirzr/vestra-packs`:

- LiteRT-LM: `local-qwen3-06b-v1`, `local-gemma-4-e2b-v1`, `local-gemma-v1`,
  `local-functiongemma-v1` — **on HF manifest** ✅
- `local-bonsai-image-v1` (Bonsai Image, ternary-weight FLUX.2-klein-architecture DiT) —
  **on HF manifest** ✅
- Try-on: `lite-v1` (~68 MB, required for Lite/Pro masks), `pro-v1` (~4.3 GB FP16
  SD1.5+ControlNet+IP-Adapter) — **on HF manifest** ✅; `pro-v2-int8` (~2 GB INT8) export ready,
  upload pending
- Quality post-process: `birefnet-v1`, `realesrgan-v1` — **on HF manifest** ✅

Full license table (this is the current, accurate one — see `docs/PROJECT_HISTORY.md` for why
`ml/MODEL_LICENSES.md` was removed as a stale duplicate): `MODEL_LICENSES.md`.

Verify live manifest: `python3 scripts/verify-manifest.py`
Publish updates: `python3 scripts/publish-packs.py` (requires HF credentials)
