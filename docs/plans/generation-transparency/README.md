# Generation transparency

**Status:** rc15 — A0 harness + A1/A3/B2/B3 shipped; Pixel 9 on-device numbers optional/TBD  
**Baseline:** v3.1.0-rc14 → rc15  
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

### rc15 shipped

| # | Theme | Deliverable |
|---|-------|-------------|
| A0 | Measure | Instrumented harness + `scripts/benchmark-on-device.sh`; [`docs/BENCHMARKS.md`](../../BENCHMARKS.md) with desktop reference — **Pixel 9 numbers when device available** |
| A1 | Cache | `OrtSessionCache.openGraph()` wired into `SdControlNetPipeline` |
| A3 | Cleanup | `DeviceSpec.minSdk` enforced in `deviceMeets()` |
| B1 | Log | Per-tab `_liveLog` + `LiveGenConsole` (prior trains) |
| B2 | Timer | Elapsed-time counter alongside remaining budget |
| B3 | A11y | `rememberReduceMotion()` on `GenerationScreen` + `DevelopShader` |

### Still open

| # | Theme | Gap |
|---|-------|-----|
| A0 | Measure | Committed **on-device** Pixel 9 table in `docs/BENCHMARKS.md` (harness ready) |
| B4 | Quality | `CloudOutputValidator` quality-rating retry (composer sliders already closed) |
| B1 | Try-on | Unify `GenerationScreen` log with cloud studio if still divergent |

**Related:** [`../generation-stability/`](../generation-stability/) (R1/R2), [`../five-star-quality/`](../five-star-quality/)
(active — check Q3/Q4 before duplicating work).
