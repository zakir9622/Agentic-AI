# Claude Code — Expansion & improvement roadmap

**Source:** Claude Code planning session (`lookbook_expansion_roadmap`)  
**Baseline when written:** v2.9.5 — **update status notes as you implement**  
**Canonical plan:** [`PLAN.md`](PLAN.md)

Phased roadmap for cloud/local model expansion, quality tooling, UI cleanup, and infrastructure. Written before v3 `HomeScreen` shipped; some UI references (`StudioScreen`, `CreateStudioScreen`) are legacy — map to the home pager + `UnifiedStudioPane` instead.

**Related:** [`../lookbook-v3-followup/`](../lookbook-v3-followup/) — post-v3 gap completion (diagnostics, CI, dead code).

## Cycles (from plan frontmatter)

| ID | Focus | Status |
|----|-------|--------|
| cycle1-inference-discovery | HF router discovery + inference image-edit | Pending — partial work in v2.9.14–16 |
| cycle2-quality-packs | BiRefNet, Real-ESRGAN, pro-v2-int8 manifest | Pending |
| cycle3-ui-settings | Settings split, model health, generation feedback | Partial — settings hub shipped in v3 |
| cycle4-speed-compliance | QNN EP, LCM, safety classifier, UI tests | Pending |
