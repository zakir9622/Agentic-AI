# Generation transparency

**Status:** A0 harness ready — awaiting Pixel 9 device numbers  
**Baseline:** v3.1.0-rc14 (plan text originally rc9; re-verified post DoD pass)  
**Canonical plan:** [`PLAN.md`](PLAN.md)

Supplements [`../five-star-quality/`](../five-star-quality/) — the active plan — rather than
replacing it. Written after a full re-check found that local image, code, video, and audio
generation, Real-ESRGAN, Model Health UI, Settings decomposition, and a full visual re-theme
("Loom Ink") had all shipped since the last audit round. This plan does not propose another
visual redesign — `five-star-quality/PLAN.md` already lists that as a non-goal.

### Already closed elsewhere (do not redo)

| Item | Closed by |
|------|-----------|
| A2 Pro-pack export collision | rc14 DoD — colliding CatVTON exporter quarantined; live HF `pro-v1` verified fully-conditioned |
| B4 composer Steps/CFG/Seed | rc14 DoD — removed (never reached model payloads) |

### Still open

| # | Theme | Gap |
|---|-------|-----|
| A0 | Measure | Instrumented harness + EP probe shipped; **committed Pixel 9 `docs/BENCHMARKS.md` numbers still needed** |
| A1 | Cache | `OrtSessionCache` not yet wired into `SdControlNetPipeline` (needs A0 before/after) |
| A3 | Cleanup | `DeviceSpec.minSdk` / `EpochClock` — low priority |
| B1–B3 | UI transparency | Accumulating per-tab log, elapsed timer, reduce-motion on GenerationScreen |

**Related:** [`../generation-stability/`](../generation-stability/) (R1/R2), [`../five-star-quality/`](../five-star-quality/)
(active — check Q3/Q4 before duplicating work).
