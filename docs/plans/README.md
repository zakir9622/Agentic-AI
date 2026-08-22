# Lookbook planning docs

Active roadmaps live in separate directories so parallel workstreams do not collide.

| Directory | Source | Baseline | Scope | Status |
|-----------|--------|----------|-------|--------|
| [`true-local/`](true-local/) | Pixel-true offline engines | **v3.1.0-rc5** | System TTS + SD-Turbo ORT engine; pack weights still needed for Image | **Active** |
| [`big-release-r2/`](big-release-r2/) | True-limits release (ATR + UI + on-device scaffold) | **v3.1.0-rc1…rc4** | Full ATR Auto, fixture harness, Loom Ink UI, Audio Studio | Merged to main |
| [`stable-release/`](stable-release/) | Cursor plan audit (post v3.0.3) | v3.0.4 → v3.0.16 | Honest stable cut + sideload | R0/R1 done; handoff to R2 |
| [`lookbook-v3-followup/`](lookbook-v3-followup/) | Cursor Cloud Agent review (post v3 ship) | v2.9.16 → **v3.0.x** | Finish v3 gaps: diagnostics, CI gates, dead-code cleanup, composer depth | Mostly done; E3/E4 open |
| [`claude-code-expansion/`](claude-code-expansion/) | Claude Code improvement plan | v2.9.5 → **v3.0.x** | Models, tools, UI — HF discovery, quality packs, QNN/LCM, model health | cycle1–3 done; cycle4 partial (QNN/LCM honesty) |
| [`generation-stability/`](generation-stability/) | Claude Code read-only audit | v2.9.16 → **v3.0.2+** | Typed cloud failures, live model health, Gradio schemas, local image gen, harness | M1–M3/M6 done; M2 UI @3.0.5; M4 weights deferred; M5 harness dry-run |

## How to use

- **Big release R2** — start here for v3.1.0 (ATR Auto, UI overhaul, true on-device limits).
- **Stable release** — historical R0/R1 through v3.0.16.
- **v3 follow-up** — A1–E4 checklist (historical + remaining E3/E4).
- **Claude Code expansion** — longer-horizon product/engine roadmap (`cycle1`–`cycle4`).
- **Generation stability** — M1–M6 gated milestones for cloud/local generation.

When items overlap (e.g. BiRefNet packs, settings split), implement once and mark done in both plans.

**Scorecard:** [`COMPLETION.md`](COMPLETION.md) — Claude cycles + v3 follow-up A1–E4 vs current tree.

## Iterative UX cycles (historical)

Short atelier polish loops on `iterative-*` branches (v2.7.1–2.7.5): cancel, a11y, Share/Report, preflight. All merged into `main` — **complete**.

## Archived / external

- Original v3 overhaul plan: Cursor artifacts (`lookbook_v3_overhaul_32292744.plan.md`) — do not edit.
