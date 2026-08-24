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
  `press-3d`/`lift-3d` language ported at Compose-native cost. No 3D perspective tilt, no shimmer/
  skeleton component yet (B8 still open).
- **B5 (processing mode) — verified already correct, not a new mechanism.** `cloudModelsEnabled`
  defaults `false` app-wide (confirmed in `AppSettings.kt`); every studio and the News/Chat
  window already hide cloud model rows and generate on-device only until a user explicitly
  opts in. Studio subtitle copy that said "Cloud by default" regardless of this setting was
  corrected to reflect the real state (on-device-only vs. cloud-until-you-install-a-pack).
- **Appium/chat testability** — see `docs/DRAWBACKS.md` and `TestTags.kt`; the News/Chat window
  (refresh button, headline cards, chat message bubbles) now carries stable tags alongside the
  rest of the generation flow tagged in the prior phase.
- **Not yet started:** A3 (nav pattern — needs the open question in `PLAN.md` answered first),
  B1–B4, B6–B8, and Part D's real-model output-quality testing for code/audio.
