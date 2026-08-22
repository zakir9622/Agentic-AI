# Generation stability — image / video / code

**Status:** M1–M3 + M6 core **shipped** through v3.0.2–v3.0.3; M4 local image-gen weights **deferred**; M2 live-health UI + M5 screenshot baselines **partial**. See [`../stable-release/`](../stable-release/) for the R0/R1 ship plan.  
**Baseline:** v2.9.16 (source audited @ `06a24f1`) · execution landed in v3.0.1–v3.0.3  
**Canonical plan:** [`PLAN.md`](PLAN.md)

Evidence-backed audit of the generation stack (findings **A–Q**). Root cause of the original image failures was **A** — fallback `continue` targeting the prompt-variant loop instead of the model-candidate loop.

| # | Theme | Status |
|---|-------|--------|
| M1 | Typed failures + correct fallback | **DONE** @ v3.0.1 |
| M2 | Live model health + generation budget | **PARTIAL** — tracker/budget done; picker UI still static Ready; blank-frame TBD |
| M3 | Self-healing Gradio schema contracts | **DONE** @ v3.0.2 |
| M4 | Local on-device image generation | **DEFERRED** — scaffold only (`local-sdturbo-v1`) |
| M5 | Test + visual verification harness | **PARTIAL** — scripts in CI; device baselines thin |
| M6 | Cleanup, changelog, KMP portability | **PARTIAL** — EpochClock/hooks done; minSdk docs + iOS open |

**Related:** [`../lookbook-v3-followup/`](../lookbook-v3-followup/), [`../stable-release/`](../stable-release/).
