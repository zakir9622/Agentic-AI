# Functionality — what the app actually does, end to end

The functional/backend counterpart to `docs/UI_DESIGN.md`. For the historical narrative of how
this architecture came to be, see `docs/PROJECT_HISTORY.md`. For the still-accurate module/DI
overview, see `docs/ARCHITECTURE.md` — this doc goes one level deeper into the generation
pipeline and its recent redesign (consent-based cloud gating).

## The five capabilities

`AiCapability` (shared/commonMain) enumerates what the app can generate:

| Capability | Local engine | Cloud fallback | Status |
|---|---|---|---|
| `IMAGE_GEN` | Bonsai Image (LiteRT-LM DiT) or SD-Turbo-class ONNX | HF Spaces (FLUX Schnell etc.) / HF Inference | Shipping |
| `IMAGE_EDIT` | SD-Turbo-class img2img | HF Spaces (Qwen Image Edit, InstructPix2Pix) | Shipping |
| `VIDEO` | — (cloud only) | HF Spaces (LTX-Video etc.) | Shipping |
| `CODE` | LiteRT-LM (Qwen3/Gemma) | Groq, OpenRouter, HF Inference | Shipping |
| `AUDIO` | System TTS / local voice changer | HF Spaces (Edge-TTS) | Shipping |
| *(chat)* | LiteRT-LM (Qwen3/Gemma) | same chain as Code | Shipping, not a separate `AiCapability` — routed through `chatWithFallback` |
| *(try-on)* | Lite/Pro ONNX pipeline | *(retired Supabase/Replicate backend)* | Implemented, not reachable from navigation — see `docs/PROJECT_HISTORY.md` |

`GenerativeCloudService` (shared/commonMain/cloud) is the single dispatcher for all of these —
one function per capability, each returning a `Flow<GenerativeState>` so the UI can render every
stage (preparing → running → ready/failed) without polling.

## Local-first, cloud-fallback dispatch

Every capability's generate function follows the same shape:

1. If the user has selected a **local** model for this capability (`AppSettings.prefersLocal`),
   try it first. Local engines never throw for expected failures — they report a typed
   `GenerativeState.Failed` reason so the UI can show it, rather than silently falling through.
2. If local isn't selected, or the local attempt fails and the device has network, compute the
   **fallback chain** — an ordered list of candidate cloud providers for this capability
   (`CloudModelRouting.fallbackChain` for image/video/audio, `codeFallbackChain` for code/chat).
   The chain always tries the originally-selected provider first, then ranked alternates,
   skipping any candidate currently in a health-based cooldown.
3. Each candidate is tried in order; a typed `CloudFailure` on one candidate advances to the
   next rather than giving up. `ModelHealthTracker` records failures with an exponential
   cooldown so a known-bad Space isn't retried on every single request.
4. If every candidate is unusable — no network, or (as of the consent redesign below) cloud
   consent hasn't been granted and no local pack is ready — the flow emits a single, specific
   `GenerativeState.Failed` explaining exactly why, not a generic error.

## Cloud consent: how availability is decided now

This is the most recent architectural change (Era 7 of `docs/PROJECT_HISTORY.md`), so it's
worth spelling out precisely since it replaced a much simpler mental model (a single on/off
switch).

**There is no manual "enable cloud" setting anymore.** Two independent things gate whether a
given cloud provider can actually be used, both exposed as one function:

```kotlin
fun AppSettings.cloudUsable(provider: CloudModelProvider): Boolean =
    cloudConsentGranted && (!provider.requiresApiKey || apiKeyFor(provider) != null)
```

1. **Credential** — does this specific provider need an API key, and if so, is one saved?
   Unchanged from before; several providers (FLUX Schnell on HF Spaces, Qwen Image Edit, Edge
   TTS) are genuinely free and keyless.
2. **Consent** (`AppSettings.cloudConsentGranted`) — a boolean that gates *every* cloud
   provider, keyless or not. It is granted only by:
   - picking a cloud model in the model picker or quick switcher (`setImageGenProvider` and its
     siblings grant consent internally when the selected id isn't a local one), or
   - saving an API key through Settings' "Save" button
     (`AppSettings.confirmCloudConsentFromApiKeyEntry()`, called explicitly by the Settings
     screen after a successful save).

   It is **never** granted by `TokenSidecar`'s automatic boot-time token restoration — reading a
   previously-saved key back from disk when the app starts calls the same
   `setHfToken`/`setGroqApiKey`/`setOpenRouterApiKey` setters, but that path runs before the
   user has done anything in the current session, so treating it as consent would let a fresh
   install silently reach the network based on a file that predates any user action.

An install that already had the old master switch on migrates straight to `consented` on first
launch after upgrade, so nobody's working cloud setup silently breaks. Consent can be revoked
(`AppSettings.revokeCloudConsent()`, wired to a small action in Settings' API Keys card) without
touching any saved key or model selection — it only flips the gate back off; re-picking a cloud
model or re-saving a key grants it again.

Every reachability check in the app routes through `cloudUsable()`: `AppSettings.preflight()`
(the UI's "can I even try this" check before generation starts),
`CloudModelRouting.fallbackChain`/`codeFallbackChain`'s per-candidate filtering,
`GenerativeCloudService`'s runtime gates right before a network call, and the model picker's
readiness indicators. There is exactly one source of truth, not several places that could drift
out of sync — a design deliberately chosen after the switch's removal was found to have created
a real gap (see below).

**Why this exists at all**: removing the manual switch without adding consent would have meant
a fresh install, with zero configuration, could reach a keyless free cloud provider on its very
first generation attempt — silently breaking the app's standing "nothing leaves the device
until you say so" privacy promise. Two rounds of code review during this redesign caught this
gap and a related one (the consent-granting hook originally lived inside the shared secret-
setter function, which meant it fired even from the automatic boot-time restore path) before
either shipped.

## Generation parameters

Local image generation exposes the parameters the underlying engines always supported but that
never reached a UI control until Era 7:

- **Steps** — diffusion step count.
- **Guidance scale** — how strongly the prompt steers generation (CFG).
- **Seed** — for reproducible/variant generation.
- **Strength** — img2img denoising strength (how much of the reference image survives).
- **Candidate count** (1–4) — generates that many variants per request in one batch, sharing a
  `batchId` and lineage (`GenerationBatch`/`GenerationCandidate`, ported in Era 6's Creative
  Studio V2 work), rendered in an `ImageCandidateGrid`.

These are local-only. Cloud payloads (Gradio Space calls, HF Inference requests) don't accept
sampler overrides — this is a real constraint of those APIs, not a gap in this app's UI, so the
Advanced parameter section hides itself entirely when a cloud model is selected.

## Safety

Two independent, non-overlapping systems:

1. **`SafetyPresets`** (shared/commonMain/safety) — prompt-level guard clauses, including a "no
   explicit content" instruction appended to every prompt by default. An "Off" preset removes
   that specific guard clause; it does **not** touch a cloud provider's own server-side content
   moderation, which still applies regardless of this app's setting.
2. **The "reduce fashion false positives" assist** (Era 7) — a narrow, opt-in, default-off
   toggle that softens prompt phrasing specifically for legitimate fashion/beauty content
   (swimwear, lingerie, editorial catalog shots) that safety filters tend to over-flag. It never
   claims to disable a provider's moderation and is intentionally *not* the broad "unlock
   explicit/uncensored generation" toggle that was requested and declined during this audit —
   see `docs/PROJECT_HISTORY.md`'s Era 7 section for the full reasoning.

Reports on generated content are recorded locally (`LocalReportStore`); there is no backend
service to deliver them to, since the app has no server component. This is different from an
earlier design (documented in some now-corrected docs) that assumed a Supabase Edge Function
existed to receive reports — it doesn't, and hasn't since the Supabase backend was retired.

## Model catalogs

Two independent, static catalogs plus one dynamic one:

- **`LocalModelCatalog`** — every on-device model entry, keyed by id, with `runnable`/`packId`/
  `displayName`/capability metadata. `isSelectableStudioId()` distinguishes a local id from a
  cloud one, used throughout the settings/routing code to branch behavior.
- **`CloudModelCatalog`** — every known cloud provider per capability, with `requiresApiKey` and
  a per-capability default. Several defaults are genuinely free/keyless (FLUX Schnell, Qwen
  Image Edit, LTX-Video on ZeroGPU, Edge-TTS); Code's default (Llama 3.3 70B on Groq) requires a
  key.
- **`FreeCloudDiscovery`** — dynamic, credential-based model listing. Currently Hugging-Face-only
  (Code/Image-Gen/Image-Edit): once an HF token is saved, it queries which router models are
  actually usable with that account and refreshes the picker's cloud entries accordingly. This
  is the precedent the "no more manual toggle, just list what tokens make available" redesign
  (Era 7) generalized — see `docs/PROJECT_HISTORY.md`.

## Diagnostics

Settings → Diagnostics → Export produces a ZIP bundle (Era 6, upgraded from a flat JSON/text
export): `system_info.json`, a troubleshooting report, `run_history.json`, app logs, and
`packs_status.json`. Every generation attempt is tagged with one correlation id threaded through
`RunDiagnostics`, `LocalJobStore`, and `EngineLogHook` (unified in v3.1.5), so an interrupted
job, its diagnostics record, and any crash-log entry for the same incident can be found by the
same id instead of matched by eyeballing timestamps.

## What's genuinely not built (functional side)

- **Advanced generation techniques** (ControlNet-guided edits beyond try-on's own pipeline,
  inpainting, LoRA model-swapping) were analyzed as part of the Era 7 audit and deliberately
  scoped out for now — a cloud-provider-integration decision for ControlNet/inpainting, with
  LoRA-swap noted as a smaller, separate future on-device project.
- **No "gentler path" retry-exhaustion fallback** for cloud video/audio — when every fallback
  candidate fails, the app surfaces the last error rather than retrying at lower resolution or
  relaxed constraints; building one honestly requires per-provider parameter tuning that hasn't
  been verified against each Space's real behavior.
- Try-on's cloud tier has no working backend (see above) — its local Lite/Pro tiers still run,
  but the feature as a whole is unreachable from navigation regardless.

See `docs/DRAWBACKS.md` for the complete, continuously-maintained list including what's real but
unverified on actual hardware (device I/O for the voice-editor pipeline, ML Kit face-detection
accuracy, GPU/NPU delegate behavior across real devices).
