# Project History — what this app is and how it got here

This is the narrative companion to `CHANGELOG.md` (which has the full version-by-version
detail) and `git log`. It exists so a new contributor — human or AI — can understand *why*
the codebase looks the way it does without reading a thousand commits. Every claim below is
backed by `CHANGELOG.md`, `docs/DRAWBACKS.md`, or a merged PR; nothing here is aspirational.

## What the app is today

**The Lookbook** (`com.zakir.vestra`) is a local-first, multi-modal AI content studio for
Android. It generates images, video clips, code, and audio/voice — mostly on-device, with
optional free cloud fallback the user opts into. It began life as a single-purpose virtual
try-on app (garment photo → photorealistic model wearing it) and grew into a general creative
studio; try-on itself is still in the codebase but is currently unreachable from the main
navigation (see [Where try-on stands today](#where-try-on-stands-today)).

## Timeline

### Era 1 — Local-first virtual try-on (through v3.0.x)

The original product: upload a garment photo, get a photorealistic image of a model wearing
it. Three generation tiers:

- **Lite** — on-device ONNX compositor (garment segmentation + human parsing + warp +
  harmonize), works on any supported device, ~68 MB pack.
- **Pro** — on-device SD1.5 + ControlNet-Depth + IP-Adapter diffusion, gated to 8GB+ RAM
  flagships (Pixel 9 class).
- **Cloud** — uploaded to a Supabase Edge Function that called Replicate. **This backend no
  longer exists** — it was fully replaced in Era 2 by free-tier Hugging Face Spaces / Inference
  Providers, Groq, and OpenRouter, called directly from the app with no server component. Any
  doc or comment still mentioning Supabase/Replicate describes this retired architecture.

Model licensing was a running theme even then — most published try-on weights (IDM-VTON,
CatVTON, StableVITON) are non-commercial (CC BY-NC), which is why the shipped Pro pack uses a
from-scratch SD1.5 + ControlNet-Depth + IP-Adapter chain built entirely from commercially-usable
weights instead (see `MODEL_LICENSES.md`).

### Era 2 — Multi-modal Create Studio (v3.0.x)

The app grew beyond try-on: **Image, Video, Code, and News/Chat** studios were added, all
running against free cloud endpoints (HF Spaces, HF Inference Providers, Groq, OpenRouter).
This era shipped the infrastructure that still underpins cloud generation today:

- Typed `CloudFailure` errors and a real fallback chain that advances through candidate models
  instead of getting stuck on the first failure (`CloudModelRouting`).
- `ModelHealthTracker` — exponential cooldown for a model that just failed, so the app stops
  retrying a Space that's down.
- Gradio schema discovery (`GradioSchemaClient`) instead of guessing a Space's call signature.
- Quality post-processing packs — BiRefNet matting, Real-ESRGAN upscaling — downloadable
  add-ons that improve local-generation output.

### Era 3 — On-device LLM integration (LiteRT-LM)

Google's LiteRT-LM SDK was integrated for genuinely local text/code generation and chat,
independent of the ONNX-based try-on engines:

- `LiteRtLmEngine` wraps the SDK; `LiteRtLmEngineCache` keeps warm engines resident across tab
  switches (so a background generation survives navigating away) with GPU→CPU fallback on
  delegate-init failure.
- On-device model catalog: Qwen3 0.6B (Apache-2.0), Gemma 4 E2B, Gemma 3 1B, FunctionGemma
  270M (Gemma Terms of Use) — see `MODEL_LICENSES.md` for the full, currently-accurate table.
- **Bonsai Image** (`local-bonsai-image-v1`) — a ternary-weight FLUX.2-klein-architecture
  on-device text-to-image DiT model, ported from the Apache-2.0 `hf-to-litertlm` reference app.
  The math (`BonsaiMath`: sigma schedule, position ids, noise, unpatchify) and tokenizer
  (`BonsaiTokenizer`, Qwen3 BPE) were ported and unit-tested; real end-to-end generation was
  verified on desktop CPU LiteRT before shipping.
- Streaming generation (`generateTextStream`) wired through Code and Chat so tokens appear
  incrementally instead of waiting for a full response.

### Era 4 — lookbookweb design-parity attempt (v3.1.3) — superseded

A plan existed to exactly match the visual design of a sibling web app,
`zakir9622/lookbookweb` (color tokens, radii, motion, a 5-item bottom dock with a center
Create FAB). Phase 1 (color/radius token replacement) shipped in 3.1.3. The rest was
**explicitly abandoned in 3.1.4 by direct user request** — "why keep tabs and dock button" —
in favor of a simpler 3-item dock and isolated per-modality screens (Era 5). The
`docs/plans/lookbookweb-exact-ui-parity/` plan document that tracked this was removed as part
of this documentation pass since it no longer reflects any actual direction; its still-open,
non-UI carried-forward items (see [Known gaps](#known-gaps--carried-forward-open-items) below)
were preserved here instead of being lost.

### Era 5 — IA restructure + reliability hardening (v3.1.4 – v3.1.8)

Driven by real device logs and direct user feedback rather than speculation:

- **Retired the tabbed pager.** Image/Video/Audio/Code became fully isolated routes instead of
  sharing a `HorizontalPager` — each modality only ever needs one model resident, and sharing a
  pager risked keeping more than one loaded.
- **Bottom dock cut from five items to three** — Home / Library / Settings. The center Create
  FAB and its tool-picker popup were deleted; Home's own content became the tool grid that used
  to live behind that popup.
- **All four studios redesigned as conversations** — a scrolling prompt→result history per
  studio (like a chat thread) instead of overwriting a single result card, with typing
  indicators and auto-scroll.
- **In-studio Advanced/Safety settings removed** in favor of a single Settings-level control,
  removing a duplicate surface.
- Several real crashes fixed from actual device log bundles a user sent via Settings →
  Diagnostics: a native SIGSEGV from unsynchronized concurrent LiteRT-LM calls, a silent
  low-memory process kill that never touched the LiteRT-LM engine cache, a Compose `NaN` crash
  from a clamp function that didn't actually filter NaN, and a voice-changer WAV-format
  rejection that now auto-converts instead of failing.
- A full LiteRT-LM integration audit against Google's own guide (v3.1.7) added opt-in NPU
  backend support and speculative decoding, both off by default pending real-device
  verification; a follow-up (v3.1.8) turned GPU/NPU/speculative-decoding on by default once
  their fallback safety was established, leaving only NNAPI (a confirmed SIGSEGV risk on
  Pixel 9) as an explicit opt-in.

Full per-item detail for this era is in `CHANGELOG.md` (3.1.4 through 3.1.8) and
`docs/DRAWBACKS.md`.

### Era 6 — GoogleLookBookUI cross-repo port (merged PR #80)

`zakir9622/GoogleLookBookUI` turned out to be an earlier development snapshot of this same
codebase (same package, architecture frozen around v3.1.0-rc23) rather than a separate
product. A structured, phased comparison found real, additive features it had gained
independently and ported them across in six gated phases (each with its own
implement → test → lint → build → code-review → commit cycle):

1. **Quick wins** — a periodic model-prewarm `WorkManager` job, a visible-watermark setting
   flip, audio export to the Music app, model-picker readiness/offline-metadata UI, and a real
   ZIP-bundle diagnostics export (system info, troubleshooting report, run history, logs, pack
   status) replacing the old flat JSON export.
2. **CI/quality infra** — a non-blocking detekt step, lint-evidence artifact upload, and a
   deterministic-screenshot CI re-run.
3. **Moderate UI features** — searchable Wardrobe with recipe reuse, an onboarding copy
   refresh.
4. **Creative Studio V2** — `GenerationBatch`/`GenerationCandidate`, generating 1–4 image
   candidates per request sharing a batch id and lineage, rendered in an `ImageCandidateGrid`.
5. **Prompt Director** — a structured prompt-building sheet (subject/setting/mood/lighting/
   composition/finish + 12 style-modifier presets), plus a Gemini-style pinch-zoom full-screen
   image viewer as the entry surface for capability-gated image-edit intents.
6. **Voice-cloning / vocal-editor pipeline** — the largest and most novel piece (~1,900+ lines):
   raw `AudioRecord`-based voice-sample capture, a serialized custom voice profile, a DSP engine
   (`AudioEditorEngine`, 825 lines) doing decode/trim/fade plus vocal-remover/karaoke/bass-boost
   via stereo center-channel cancellation and MP3/WAV/M4A export, a waveform/spectrum audio
   player view, and a consolidated permission checklist screen.

One deliberate exclusion: `ModelConfigScreen.kt`'s per-provider connectivity "ping" in the
source repo was **fake** (`delay(600)` then a random 65–115ms number presented as a real
measurement) — not ported. A genuine replacement (`ProviderConnectivityChecker`, a real HTTP
call against the same host the app's generation code actually calls) had already shipped in
3.1.2, independently, and is what the app uses today.

### Era 7 — Generation audit follow-through (merged PR #81)

A full audit of model selection, image-generation parameter support, and the cloud/on-device
setting (published as the "Lookbook Generation Audit" artifact) drove four concrete fixes:

1. **Wired five dead image-generation parameters.** Steps, guidance scale, seed, img2img
   strength, and 1–4 candidate batching were fully implemented in the local engines
   (`AndroidTxt2ImgEngine`, `BonsaiImageEngine`) and computed in `GenerativeViewModel`, but
   never reached a UI control or the actual generation call. A new "Advanced" section in the
   Image studio (sliders + a candidate-count picker) fixes this for local generation; cloud
   generation is unaffected since Gradio Space/HF Inference payloads don't accept sampler
   overrides — a real API constraint, not an oversight.
2. **Declined a global "unlock explicit/uncensored content" toggle.** The user asked for one
   across every generator; it was refused because this app captures and processes real photos
   of the user and other people (garment reference, virtual try-on), and a filter-bypass toggle
   combined with that pipeline is a direct path to non-consensual imagery of real people — a
   property of combining the two features, not something a warning label fixes. Built instead:
   a narrower, honestly-scoped "reduce fashion false positives" toggle (default off) that only
   softens prompt phrasing for legitimate fashion/beauty content that safety filters
   over-flag — it never disables a provider's own moderation and never touches the separate
   `SafetyPresets` "no explicit content" guard.
3. **In-composer model quick switcher.** The model chip in the prompt composer used to only
   open a full bottom sheet. A compact `DropdownMenu` popup anchored at the chip now covers the
   common case (on-device entries + top 4 ranked cloud models); the full sheet stays reachable
   via "Browse all models…" for search and metadata.
4. **Removed the manual cloud/on-device processing-mode setting.** This was the largest single
   change — see [Cloud consent model](#cloud-consent-model-replacing-the-manual-toggle) below.

## Cloud consent model (replacing the manual toggle)

The old design had a single settings switch — "Auto / On-device / Cloud" — controlling whether
cloud generation could run at all. It was removed entirely; model availability is now purely
**credential-based**: a keyless free model (e.g. FLUX.1 Schnell on an HF Space) is always
listed and selectable, a keyed one (Groq, OpenRouter, HF Inference) appears once its API key is
saved. This matches how `FreeCloudDiscovery` already worked for Hugging Face specifically —
this change generalized the pattern to every provider/capability and removed the old blanket
switch.

Removing the switch created a real gap that two rounds of code review caught: with no manual
gate, a fresh install with zero configuration would reach a keyless cloud provider on its very
first generation attempt, silently breaking the app's "nothing leaves the device until you say
so" promise. The fix is an implicit **cloud consent** flag
(`AppSettings.cloudConsentGranted`), granted only by a genuinely interactive user action:

- picking a cloud model in the model picker or quick switcher, or
- saving an API key via Settings' "Save" button specifically.

It is **not** granted by `TokenSidecar`'s automatic boot-time token restoration (reading a
previously-saved key back from disk on app start) — that path calls the same
`setHfToken`/`setGroqApiKey`/`setOpenRouterApiKey` setters but runs before the user has done
anything in the current session, so it must not be mistaken for consent. An install that
already had the old master switch on migrates straight to consented, so nobody's cloud access
silently disappears on upgrade. Consent can be revoked from a small "Stop using cloud models"
action in the Settings → API Keys card, which reverts to local-only without touching any saved
key or model selection.

Every cloud-reachability check in the codebase — `AppSettings.cloudUsable()`,
`CloudModelRouting`'s fallback-chain filtering, `GenerativeCloudService`'s runtime gates, and
the model picker's readiness indicator — routes through this one flag, so there is a single
place that decides whether a network call is allowed to happen.

## Where try-on stands today

Try-on (the original garment→photo feature) is still fully implemented in the codebase
(`GARMENT`/`CASTING` routes, the Lite/Pro/Cloud pipeline documented in
`docs/PIPELINE.md`/`docs/BACKEND_PIPELINE.md`) but is **not reachable from the current
navigation** — it was temporarily disabled while the app's identity shifted toward the
multi-modal Create Studio. Re-enabling it (restoring a Home entry point, then extending the
credential-based cloud availability and consent model this doc describes to its cloud tier) is
tracked as open follow-up work, not started.

## Known gaps / carried-forward open items

Carried forward here from the now-removed `lookbookweb-exact-ui-parity` plan (still genuinely
open, not lost):

- **Real on-device benchmark numbers** (RAM/latency, captured on an actual Pixel and committed
  to `docs/BENCHMARKS.md`) — blocked on device access in every development session so far.
- **A live Appium/UiAutomator run** against a real device or emulator — the suite exists
  (`appium/`) and is fairly comprehensive, but has never executed; see `docs/DRAWBACKS.md`.
- **QNN execution-provider packaging and an ONNX NSFW classifier model** — still open, still
  blocked on artifacts that can't be produced in a sandboxed dev environment.
- **iOS target** — `EpochClock`/`LogEntry` formatting are `expect`/`actual`-clean, but no
  `iosArm64`/`iosSimulatorArm64` target exists and most engine code is Android-only; see
  `docs/IOS_PORT.md`.

Plus items already tracked as pending in this repo's own working set:

- Camera capture for the Image studio's reference picker (currently photo-picker only).
- Text-file attachment support in Chat.
- Re-enabling Try-on in the main navigation, and extending automatic on-device/cloud
  availability plus HF-token auto-discovery to it.

For a plain list of current, verified limitations (not aspirational, not "planned"), see
`docs/DRAWBACKS.md` — it is maintained separately from this history and updated whenever a
drawback is actually fixed, not when it's merely reworded.

## Where to look for more

| Question | Answer lives in |
|---|---|
| Exact version-by-version change list | `CHANGELOG.md` |
| Current, honest limitations | `docs/DRAWBACKS.md` |
| Current UI/navigation design and its redesign history | `docs/UI_DESIGN.md` |
| Current functional/backend architecture | `docs/FUNCTIONALITY.md` and `docs/ARCHITECTURE.md` |
| Model licenses | `MODEL_LICENSES.md` |
| Building/publishing model packs | `docs/HUGGINGFACE_SETUP.md`, `ml/README.md` |
| Play Store compliance checklist | `docs/PLAY_COMPLIANCE.md` |
