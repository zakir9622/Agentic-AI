# The Lookbook — local-first multi-modal AI studio

A local-first Android AI content studio: generate images, video clips, code, and audio/voice —
mostly **on-device**, with optional free cloud fallback you opt into per model. It started as a
single-purpose virtual try-on app (garment photo → photorealistic model wearing it) and grew
into a general creative studio; try-on is still in the codebase but currently has no entry point
in the main navigation. See `docs/PROJECT_HISTORY.md` for the full story.

> Internal package: `com.zakir.vestra` · Product name: **The Lookbook**

## What it does

| Studio | On-device | Cloud (opt-in, free) |
|---|---|---|
| **Image** — generate & edit | Bonsai Image (LiteRT-LM) / SD-Turbo-class ONNX | HF Spaces / HF Inference |
| **Video** | — | HF Spaces (LTX-Video etc.) |
| **Code** | LiteRT-LM (Qwen3 / Gemma), streaming | Groq, OpenRouter, HF Inference |
| **Chat** | LiteRT-LM, streaming | same fallback chain as Code |
| **Audio** | System TTS, local voice changer/DSP editor | HF Spaces (Edge-TTS) |
| **Try-on** *(implemented, not currently reachable in the UI)* | Lite (ONNX compositor) / Pro (SD1.5+ControlNet+IP-Adapter diffusion) | — (its old cloud backend was retired) |

Everything the user does day-to-day works **fully offline** once a model pack is downloaded.
The network is only used to download packs, and — only once the user opts in by picking a cloud
model or saving an API key — for cloud generation. There is no manual "enable cloud" switch:
availability is automatic and credential-based (a free model is always listed; a keyed one
appears once its key is saved), gated by an implicit consent flag that's only ever granted by a
genuine user action. See `docs/FUNCTIONALITY.md` for exactly how that works.

## Build & install

Requires JDK 17+, Android SDK (platform 36), and a device/emulator on **Android 15 (API 35)+**.

```bash
# Sideload release build (unrestricted local generation, no cloud dependency)
./gradlew :composeApp:assembleSideloadRelease
adb install -r composeApp/build/outputs/apk/sideload/release/*.apk
```

After install: open the app → **Home → Model Packs** → download a local pack over Wi-Fi → start
generating. Cloud generation is entirely optional — add a free API key in Settings, or just pick
a cloud model in any studio's model picker, to opt in.

## Tests

```bash
./gradlew :shared:testDebugUnitTest :composeApp:testSideloadDebugUnitTest
./gradlew :composeApp:lintSideloadDebug
```

## Project layout

- `composeApp/` — Android UI (Jetpack Compose, Material 3 + custom design tokens)
- `shared/` — Kotlin Multiplatform core: generation dispatch, model catalogs/routing, settings,
  safety, diagnostics (`commonMain`, reusable if an iOS target is ever added; engine
  implementations live in `androidMain`)
- `ml/` — Python tooling to export/quantize/publish model packs (not shipped in the APK)
- `appium/` — end-to-end UI test suite (Python/Appium)
- `docs/` — architecture, UI design, functionality, project history, compliance, model research

## Documentation

Start with [`docs/PROJECT_HISTORY.md`](docs/PROJECT_HISTORY.md) for how the app got to its
current shape, then:

| Doc | Covers |
|---|---|
| [`docs/PROJECT_HISTORY.md`](docs/PROJECT_HISTORY.md) | What's been built, era by era, and why |
| [`docs/UI_DESIGN.md`](docs/UI_DESIGN.md) | Current navigation/screen design and its redesign history |
| [`docs/FUNCTIONALITY.md`](docs/FUNCTIONALITY.md) | Generation pipeline, cloud-consent model, safety, diagnostics |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Module layout, DI, engine routing |
| [`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md) | Current build/version status at a glance |
| [`docs/DRAWBACKS.md`](docs/DRAWBACKS.md) | Honest, continuously-updated list of real limitations |
| [`CHANGELOG.md`](CHANGELOG.md) | Full version-by-version change history |
| [`MODEL_LICENSES.md`](MODEL_LICENSES.md) | Every model shipped/downloaded and its license |
| [`docs/HUGGINGFACE_SETUP.md`](docs/HUGGINGFACE_SETUP.md) | Hosting your own model-pack manifest |
| [`docs/PLAY_COMPLIANCE.md`](docs/PLAY_COMPLIANCE.md) | Google Play submission checklist |

## Model packs

On-device model packs (LiteRT-LM Qwen3/Gemma, Bonsai Image, and the legacy try-on Lite/Pro ONNX
packs) are hosted on Hugging Face (`Iamzakirzr/vestra-packs`). See
[`docs/HUGGINGFACE_SETUP.md`](docs/HUGGINGFACE_SETUP.md) to host your own manifest, or
[`MODEL_LICENSES.md`](MODEL_LICENSES.md) for what's shipped and under what license.

## License

GPL-3.0 — see [LICENSE](LICENSE).
