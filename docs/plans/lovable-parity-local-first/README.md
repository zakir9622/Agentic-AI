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

## Status: implementation started (3.1.0-rc21), design system + nav complete (3.1.0-rc25)

- **A0 (color tokens) — done (3.1.0-rc24).** `VestraColors.modalityAccent(AiCapability)` resolves
  the right per-modality tint everywhere a studio surface needs one — the Studio header label
  (unchanged from rc21), plus `PromptComposer` (border/model-chip dot/reference icon), `ResultPane`
  (loading spinner/progress bar, result pills), `HomeScreen`'s tab row, `ModelPickerSheet`
  (search field, section headers, selection state, status dots), and `AudioStudioPane`'s
  voice-changer knob readouts. Also added `SpacingTokens` to replace ad hoc `18.dp` literals.
- **A1 (typography) — already done**, found already in place when this phase started: `Type.kt`
  already pairs Syne (display) with Outfit (body), matching lookbookweb's split exactly.
- **A2 (glass/spatial interaction) — done (3.1.0-rc24).** `GlassCard` has a subtle press-lift
  (scale to ~97% on press, spring back on release), gated by `rememberReduceMotion()` —
  lookbookweb's `press-3d`/`lift-3d` language ported at Compose-native cost. Added
  `Modifier.tilt3d()` (pointer-driven 3D perspective tilt, also reduced-motion-gated) applied to
  the try-on hero card, and `GlassTile` for future nested-content rows.
- **A3 (nav pattern) — done (3.1.0-rc25).** Decided in favor of a bottom dock (`LookbookBottomBar`)
  with five destinations — Home (the studio pager), Library (Wardrobe), a raised center Create FAB
  (returns to the studio pager), Chat (News/Chat, promoted from a pager tab to its own top-level
  route), and Settings. `VestraNavHost` wraps its `NavHost` in a `Scaffold`; the bar only renders
  on those five destinations (hidden on the try-on capture flow, nested Settings sections, Packs,
  Usage, Help, Privacy). Per-tab session isolation was the real risk here — verified safe by
  construction: `GenerativeViewModel` lives in `VestraNavHost`'s own composable scope, not inside
  any `NavBackStackEntry`, so `StudioBag`/`bindStudio` state is untouched by bottom-bar navigation
  regardless of back-stack save/restore behavior. Regression-covered by
  `appium/test_bottom_bar.py`'s round-trip test and `BottomBarNavigationTest.kt`.
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
- **B2 (version lineage) — done.** `WardrobeEntry.parentGenerationId` chains consecutive
  generations in the same studio tab as retries; the look-detail dialog shows a HISTORY section
  of earlier attempts, tap to view any of them. While wiring this, found and fixed a real,
  in-pattern bug: `WardrobeEntry.tier` was hardcoded to `CLOUD` for every Create Studio result
  regardless of how it was actually generated — the exact "Tier: CLOUD" mislabeling class already
  fixed for diagnostics in an earlier cycle, at a call site that fix didn't reach.
- **B7 (safety post-process) — reassessed, not attempted this pass.** The plan describes an
  "optional on-device blur/redact pass" — but this app has no face/region detector anywhere in
  its model catalog, and building or bundling one is a materially larger undertaking than "wire
  into the existing `QualityPostProcessor` insertion point" (which is model-pack-driven for
  upscale/matte-refine, not a fit for manual region redaction either). A user-drawn manual-blur
  tool is possible without a new model, but it's gesture/canvas UI this remote session cannot
  visually verify, so it isn't included here rather than shipped unverified. Scope this as its
  own follow-up once either a lightweight face detector is added to the local-model catalog, or
  a manual-region tool is explicitly requested and can be verified on a device.
- **B6 (voice studio DSP depth) — mostly done (3.1.0-rc26), one piece unwired.** The DSP
  algorithms (`PitchDetector`, `PitchMatcher`, `LatencyCalibrator`, `SimpleFft`) are real and
  unit-tested against synthetic signals — not stubs. Wired into the UI: a live RMS `AudioLevelMeter`
  during recording, grouped voice personas (Female/Male/Neutral & character), a "Match voice"
  chip, and a "Calibrate mic latency" chip. Not wired: `SpectrumScope` (the playback-side FFT bar
  visualizer) exists and is smoke-tested but no screen calls it — a live spectrum needs Android's
  `Visualizer` API on an active playback session, which this environment has no device to verify.
  The `AudioTrack`/`AudioRecord` I/O in `AndroidLatencyCalibrator` and the amplitude stream in
  `AndroidMicRecorder` are unverified on real hardware for the same reason — the math they call is
  tested, the device timing around it is not.
- **Not yet started:** B7's remaining face-detection/manual-blur build-out, and Part D's
  real-model output-quality testing for code/audio.
