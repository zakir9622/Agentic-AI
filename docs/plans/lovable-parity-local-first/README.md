# Lovable design/UX parity — local-first

Adopt what's genuinely worth adopting from the user's other app, `zakir9622/lookbookweb`
(Lovable-built web/PWA/Android studio) — its glass/spatial design system and its generation-
lifecycle UX (job status, resumability, lineage, diagnostics, storage management, a single
honest processing-mode setting) — applied only to this app's **local** generation surfaces.

Key finding that shapes the whole plan: lookbookweb's own image/video generation is cloud-only
(`CLOUD_ONLY_MODALITIES` in its `src/lib/providers.ts`, confirmed by its own test suite) — this
app already ships real on-device diffusion (tiny-SD/LCM + Bonsai Image 4B) that lookbookweb does
not have. So this plan borrows lookbookweb's *design and UX patterns*, not new generative
capability, and never routes anything to cloud that isn't already cloud-routed today.

See [`PLAN.md`](PLAN.md) for the full breakdown (Parts A–D), explicit out-of-scope items, and
open questions for the user before implementation starts.

## Status: implementation started (3.1.0-rc21)

- **A0 (color tokens) — partial.** Added four per-modality accent tokens (`VestraColors.Modality
  Image/Video/Code/Audio`) to `VestraPalette`, brass-family tints rather than lookbookweb's own
  hues (keeps the Loom Ink identity, per the plan's own instruction not to replace it), plus a
  derived `RadiusTokens` corner-radius scale. Wired into the Studio header label
  (`UnifiedStudioPane`'s `GlassSectionLabel`) so far — not yet propagated to every chip/progress
  accent the plan describes; extend call-by-call as those surfaces are touched next.
- **A1 (typography) — already done**, found already in place when this phase started: `Type.kt`
  already pairs Syne (display) with Outfit (body), matching lookbookweb's split exactly.
- **A2 (glass/spatial interaction) — partial.** `GlassCard` now has a subtle press-lift (scale to
  ~97% on press, spring back on release), gated by `rememberReduceMotion()` — lookbookweb's
  `press-3d`/`lift-3d` language ported at Compose-native cost. No 3D perspective tilt yet.
- **A3 (nav pattern) — not started, deliberately.** The plan itself flags this as needing an
  explicit design decision before coding (top tab/pager vs. a bottom dock + center Create
  action). Not decided unilaterally — per-tab session isolation is real, hard-won infrastructure
  built around the current pager, and a nav change is the one item in this plan big enough to
  risk regressing it without a decision.
- **B1 (resumable job state) — done.** `LocalJobStore` (Settings+JSON, same pattern as
  `RunDiagnostics`) records QUEUED/RUNNING/DONE/FAILED/CANCELLED per local generation. A row
  still RUNNING/QUEUED from a previous app process surfaces on Home as an "Interrupted" card
  with Dismiss — not a literal one-tap resume (ONNX/LiteRT sessions aren't checkpointable), just
  the honest memory that something didn't finish. Wired into image/video/audio/code; chat is
  intentionally excluded (its replies stream live, so they don't silently vanish the same way).
- **B3 (correlation-ID error UX) — done.** `RunDiagnostics.RunBuilder` exposes its `id` before
  completion; local-generation failure messages (image/video/audio/code, and local chat) now
  thread "(ref &lt;id&gt;)" so a failure on screen is look-up-able in Settings → Diagnostics.
  Cloud failures are untouched — `CloudFailure` already carries enough context.
- **B4 (storage/download management) — done.** PacksScreen now shows an aggregate "X GB used
  across N packs · Y GB free" header, and `PackStatus.INCOMPATIBLE` expanded from one terse line
  into a scannable RAM/Android-version/NPU checklist with real have/need numbers. No explicit
  pause distinct from cancel-and-resume — that already works today, just not reframed as "pause"
  (lower priority, not done this pass).
- **B5 (processing mode) — done.** Replaced the "Enable cloud models" `Switch` with a single,
  prominent Processing Mode card (On-device only / Cloud allowed) in the same plain-language
  framing lookbookweb uses. Honestly two states, not three: this app has no true "Auto" fallback
  router, and the card's own copy says so rather than implying a mode that doesn't exist.
- **B8 (shimmer loading) — done.** `ShimmerBlock`/`ShimmerRows` (gradient sweep, falls back to a
  static fill under reduced motion) wired into the one real gap found: News/Chat's headline list
  used to show blank space while the first refresh was in flight.
- **Appium/chat testability** — see `docs/DRAWBACKS.md` and `TestTags.kt`; the News/Chat window,
  the processing-mode card, and the interrupted-jobs banner all carry stable tags alongside the
  rest of the generation flow tagged in the prior phase.
- **Not yet started:** B2 (version lineage for local generations), B6 (voice studio DSP depth —
  meters/scope/latency calibration), B7 (safety post-process / local blur-before-save), and Part
  D's real-model output-quality testing for code/audio. B6/B7 both touch live audio/image
  pipelines this session cannot verify on a real device — treat as higher-risk than B1–B5/B8 and
  worth extra scrutiny before landing.
