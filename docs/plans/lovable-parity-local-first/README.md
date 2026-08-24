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
