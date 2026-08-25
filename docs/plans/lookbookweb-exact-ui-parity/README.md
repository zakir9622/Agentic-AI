# lookbookweb exact UI parity

**Status:** Planned — not started.

**Source:** `zakir9622/lookbookweb` (TanStack Start + React + Tailwind v4 + shadcn/ui,
live at lookbookweb.lovable.app), cataloged directly from its source (`src/styles.css`,
`AppShell.tsx`, all 16 routes, all 8 non-`ui` components, `.lovable/plan/`) — not guessed from
rendered pixels. `zakir9622/LookbookWebUI` was checked too and is an empty placeholder repo.

**Scope:** Replace this app's current "Loom Ink" (brass-on-deep-ink) design language with an
exact match of lookbookweb's shipped design system — colors, radii, shadows, motion primitives,
app shell, and every route's layout — while keeping this app's local-first architecture
unchanged (no accounts, no cloud sync; that's a standing, non-negotiable decision, not
something this plan touches). Also identifies five genuinely portable non-UI capabilities
(persistent local chat memory, tokenizer-aware context budgeting, prompt-level safety presets,
a creeping-progress/fallback job pattern, resumable pack downloads) worth pulling in
independent of the UI work.

**This is the only active plan in `docs/plans/`.** Every previous plan directory was deleted —
see `PLAN.md`'s "Carried forward" section for the handful of still-open items absorbed from
them (device benchmarks, a live Appium run, QNN/NSFW packaging, iOS target).

See [`PLAN.md`](PLAN.md) for the full token tables, per-route breakdown, and phase order.
