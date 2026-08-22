# Lookbook planning docs

Active roadmaps live in separate directories so parallel workstreams do not collide.

| Directory | Source | Baseline | Scope |
|-----------|--------|----------|-------|
| [`lookbook-v3-followup/`](lookbook-v3-followup/) | Cursor Cloud Agent review (post v3 ship) | **v2.9.16** | Finish v3 gaps: diagnostics, CI gates, dead-code cleanup, composer depth |
| [`claude-code-expansion/`](claude-code-expansion/) | Claude Code improvement plan | v2.9.5 (update as you iterate) | Models, tools, UI expansion — HF discovery, quality packs, QNN/LCM, model health |
| [`generation-stability/`](generation-stability/) | Claude Code read-only audit | **v2.9.16** | Fix image/video/code generation failures: typed cloud failures, live model health, self-healing Gradio schemas, local on-device image gen, visual test harness |

## How to use

- **v3 follow-up** — execution order for stabilizing and completing the shipped v3 overhaul (`A1`–`E4` todos).
- **Claude Code expansion** — longer-horizon product/engine roadmap (`cycle1`–`cycle4`); reconcile UI sections with `HomeScreen` (v3 replaced `StudioScreen`).
- **Generation stability** — evidence-backed audit of why cloud generation fails on device (`A`–`Q` findings, `M1`–`M6` gated milestones). Start here before touching `shared/.../cloud/`.

When items overlap (e.g. BiRefNet packs, settings split), implement once and mark done in both plans.

## Archived / external

- Original v3 overhaul plan: Cursor artifacts (`lookbook_v3_overhaul_32292744.plan.md`) — do not edit.
