# Lookbook planning docs

| Directory | Source | Scope | Status |
|-----------|--------|-------|--------|
| [`lookbookweb-exact-ui-parity/`](lookbookweb-exact-ui-parity/) | `zakir9622/lookbookweb` (live at lookbookweb.lovable.app), cataloged directly from source | Exact-match this app's design system (colors, radii, shadows, motion, app shell, every route) to lookbookweb's, keeping the local-first architecture unchanged; plus five portable non-UI capabilities (local chat memory, token budgeting, safety presets, job progress/fallback, resumable downloads) | **Planned** |

This is the only active plan. Every prior plan directory (`big-release-r2`,
`claude-code-expansion`, `five-star-quality`, `generation-stability`,
`generation-transparency`, `litert-lm-integration`, `local-first-mode`, `lookbook-v3-followup`,
`lovable-parity-local-first`, `stable-release`, `true-local`) and `COMPLETION.md` were deleted
at the user's request. Their still-open items (device benchmarks, a live Appium device run,
QNN/NSFW packaging, iOS target) are restated in the new plan's "Carried forward" section rather
than lost — everything else in them is either done (see `CHANGELOG.md`, `git log`) or superseded
by the new plan.
