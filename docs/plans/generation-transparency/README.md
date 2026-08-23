# Generation transparency

**Status:** not started
**Baseline:** v3.1.0-rc9 (`b76c215`)
**Canonical plan:** [`PLAN.md`](PLAN.md)

Supplements [`../five-star-quality/`](../five-star-quality/) — the active plan — rather than
replacing it. Written after a full re-check found that local image, code, video, and audio
generation, Real-ESRGAN, Model Health UI, Settings decomposition, and a full visual re-theme
("Loom Ink") had all shipped since the last audit round. This plan does not propose another
visual redesign — `five-star-quality/PLAN.md` already lists that as a non-goal.

Two things were confirmed still open by reading the current code, not the docs:

| # | Theme | Gap |
|---|-------|-----|
| A | Measure, then resolve | No on-device latency has ever been measured (`benchmark-local.py` is desktop-CPU-only); ONNX execution-provider selection is silently swallowed by `runCatching` with no logging; `OrtSessionCache` isn't wired into the Pro/diffusion path; two export scripts (`export_diffusion_pack.py` / `convert_pro_pack.py`) still collide on the same output path with different capability |
| B | Generation transparency UI | `GenerativeViewModel` still overwrites its state on every emission — no accumulating per-tab log, no elapsed-time counter; reduced-motion doesn't reach the generation screen; composer sliders may still not affect the actual payload |

**Related:** [`../generation-stability/`](../generation-stability/) (shares findings R1/R2 on the
Pro-pack collision and phantom composer controls), [`../five-star-quality/`](../five-star-quality/)
(active plan — check its Q3/Q4 checklist before starting any item here to avoid duplicating work).
