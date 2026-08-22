# Generation stability — image / video / code

**Status:** audit complete, execution not started
**Baseline:** v2.9.16 (source audited @ `06a24f1`, re-checked @ `5ad009b`)
**Canonical plan:** [`PLAN.md`](PLAN.md)

Read-only audit of the generation stack, triggered by repeated image-generation failures on
device: unresolvable Space hostnames, `HTTP 400 Model not supported by provider nscale`, empty
Gradio `event: error / data: null`, and a Model Health card reporting `Ready` for models that
had just failed.

17 findings (**A–Q**) with `file:line` evidence. Root cause of the reported failures is **A** —
in `GenerativeCloudService.generateImage` the fallback `continue` statements target the
prompt-variant loop instead of the model-candidate loop, so a dead model is retried three times
with softer prompts rather than advancing to the next model.

Six milestones, each with a hard gate:

| # | Theme | Closes |
|---|-------|--------|
| M1 | Typed failures + correct fallback | A, B, E, G, J, K |
| M2 | Live model health + generation budget | C, D, I |
| M3 | Self-healing Gradio schema contracts | F |
| M4 | Local on-device image generation (offline Create Studio) | L |
| M5 | Test + visual verification harness | O |
| M6 | Cleanup, changelog, KMP portability | H, M, N, P, Q |

`PLAN.md` ends with a self-contained, copy-pasteable execution prompt: findings table,
milestones, gates, an eight-step iteration loop, and the invariants that must not be weakened
(AUTO never selects cloud; free-tier only; watermark + EXIF provenance always; no secrets in
release builds; Pro try-on depends on `lite-v1`).

**Related:** [`../lookbook-v3-followup/`](../lookbook-v3-followup/) — overlaps on model health,
quality packs, and CI gates. Implement once, mark done in both.
