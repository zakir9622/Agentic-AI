# Exact UI parity with lookbookweb.lovable.app

> **Source of truth:** `zakir9622/lookbookweb` (TanStack Start + React 19 + Tailwind v4 +
> shadcn/ui/Radix, live at lookbookweb.lovable.app), commit `6209953` as cataloged
> 2026-08-25. Every visual/interaction claim in this plan is backed by a direct read of that
> repo's `src/styles.css`, `src/components/AppShell.tsx`, all 16 `src/routes/*.tsx` files, all 8
> non-`ui` components, and its `.lovable/plan/` design-intent docs — not guessed from the live
> site's rendered pixels. `zakir9622/LookbookWebUI` was also checked and is an empty placeholder
> repo (README + LICENSE only) — nothing to pull from it.
>
> **Superseded in 3.1.4, by direct user request, not lookbookweb research:** this plan's bottom
> dock (Home/Library/**Create FAB**/Chat/Settings) and `QuickCreateSheet` tool-picker dialog are
> gone. The real user of this app explicitly rejected the tabbed Image/Video/Audio/Code pager and
> the "+"-FAB popup pattern this plan called for — "why keep tabs and dock button", "main home
> page view should be the view that clicking + icon should be showing" — so the shell is now
> Home/Library/Settings only, and Home's own content is the tool grid that used to live behind
> the Create dialog. Every reference below to the Create FAB, the 5-item dock, or
> `QuickCreateSheet` describes retired behavior — see `CHANGELOG.md`'s 3.1.4 entry for what
> replaced it. Treat those sections as historical record of what lookbookweb does, not as a
> target this app should still match.
>
> **This plan replaces every previous plan doc in `docs/plans/`.** All prior plan directories
> (`big-release-r2`, `claude-code-expansion`, `five-star-quality`, `generation-stability`,
> `generation-transparency`, `litert-lm-integration`, `local-first-mode`,
> `lookbook-v3-followup`, `lovable-parity-local-first`, `stable-release`, `true-local`) and
> `COMPLETION.md` are deleted. Their historical value is fully absorbed into this repo's git
> history and `CHANGELOG.md`; nothing in them was still-open work that this plan doesn't
> either supersede or explicitly carry forward (see "Carried forward" below).

## The one deliberate, non-negotiable deviation

lookbookweb is a **cloud-backed, account-gated web app** (Supabase auth, Postgres tables,
row-level security, a hosted AI gateway). This app (**The Lookbook** / `com.zakir.vestra`) is
**local-first by design** — no accounts, no server, generation runs on-device or through
free-tier APIs the user brings their own key for. That is a standing product decision from
every prior phase of this project, not something this plan is authorized to reverse.

So: **every visual pattern below is ported. Every backend behavior is re-grounded in local
storage.** Concretely:
- "Signed in as {email}" states → replaced with local-only equivalents (no email, no
  account concept) or simply omitted where the web app's *only* reason for the state was auth
  (e.g. `/auth` itself is not ported — there is nothing to sign into).
- Supabase `projects`/`memories`/`messages` tables → Room/local-file equivalents already in
  place (`WardrobeRepository`, `LocalJobStore`, chat history) or newly added as local-only
  stores (see Part B, memory).
- Cross-device sync → does not exist and is not being added.
- The `admin` role / seeded admin account from `.lovable/plan/android-apk-on-device-edge-models-2026-08-24.md`
  → not applicable; there is no multi-user backend to administer.

Everything else — every color, every radius, every card treatment, every animation curve,
every icon, every copy string, every route's layout — is an exact-match target.

## Carried forward from deleted plans (still real, still open)

A few genuinely-open items from the deleted plans have no home elsewhere now, so they're
restated here rather than lost:
- Real on-device benchmark numbers (RAM/latency) captured on an actual Pixel and committed to
  `docs/BENCHMARKS.md` — every prior plan flagged this as blocked on device access; still true.
- A live Appium/UiAutomator run against a real device or emulator — the suite exists
  (`appium/`) and is now fairly comprehensive (see `docs/DRAWBACKS.md`), but has never
  executed.
- QNN execution-provider packaging and an ONNX NSFW classifier model (`claude-code-expansion`
  cycle4) — still genuinely open, still blocked on artifacts this environment can't produce.
- iOS target — `EpochClock`/`LogEntry` formatting are now `expect`/`actual`-clean (3.1.2), but
  no `iosArm64`/`iosSimulatorArm64` target exists and most engine code is Android-only.

None of these block this plan; they're independent and can resume whenever device access
exists.

---

## Part A — Exact UI parity

### A0. Design tokens (do this first — every later phase depends on it)

**The single biggest gap:** Agentic-AI's current palette (`Theme.kt`'s `VestraPalette`) is
"Loom Ink atelier" — brass-on-deep-ink, explicitly designed to avoid generic AI-tool looks.
lookbookweb's actual shipped palette is a completely different family: light airy canvas,
near-black primary, one electric-blue accent. These cannot coexist — this phase **replaces**
`LightPalette`/`DarkPalette` in `Theme.kt` wholesale.

lookbookweb's tokens are defined in OKLCH (`src/styles.css:64-177`). Converted to sRGB hex via
the CSS Color 4 OKLab→linear-sRGB matrices (not eyeballed):

| Token | Light OKLCH | Light hex | Dark OKLCH | Dark hex |
|---|---|---|---|---|
| `background` | `oklch(0.975 0.008 240)` | `#F2F8FC` | `oklch(0.16 0.008 265)` | `#0C0D11` |
| `foreground` | `oklch(0.19 0.012 265)` | `#111419` | `oklch(0.97 0.003 260)` | `#F4F5F7` |
| `card` | `oklch(1 0 0)` | `#FFFFFF` | `oklch(0.21 0.01 265)` | `#16181D` |
| `surface` | `oklch(0.945 0.004 260)` | `#EBEDEF` | `oklch(0.26 0.011 265)` | `#21242A` |
| `primary` | `oklch(0.19 0.012 265)` | `#111419` | `oklch(0.97 0.003 260)` | `#F4F5F7` |
| `primary-foreground` | `oklch(0.99 0 0)` | `#FCFCFC` | `oklch(0.18 0.01 265)` | `#0F1216` |
| `secondary` | `oklch(0.945 0.004 260)` | `#EBEDEF` | `oklch(0.28 0.012 265)` | `#26292F` |
| `secondary-foreground` | `oklch(0.28 0.012 265)` | `#26292F` | `oklch(0.96 0.003 260)` | (≈`#F4F5F7`) |
| `muted-foreground` | `oklch(0.47 0.012 265)` | `#575B62` | `oklch(0.74 0.012 265)` | (≈`#B8BCC2`) |
| `accent` / `ring` | `oklch(0.58 0.15 250)` | `#1F7DCF` | `oklch(0.7 0.17 265)` | `#6A99FF` |
| `destructive` | `oklch(0.55 0.21 25)` | `#D01C29` | — | (keep existing `Danger`) |
| `brand-image` | `oklch(0.58 0.15 250)` | `#1F7DCF` | `oklch(0.72 0.17 265)` | `#709FFF` |
| `brand-video` | `oklch(0.62 0.18 30)` | `#DD503F` | `oklch(0.75 0.15 45)` | `#FA8C58` |
| `brand-voice` (→ `modalityAudio`) | `oklch(0.62 0.25 350)` | `#E8179B` | `oklch(0.72 0.2 350)` | `#FC65B6` |
| `brand-chat` (→ `modalityCode`, chat surfaces) | `oklch(0.6 0.15 175)` | `#009C7B` | `oklch(0.74 0.13 175)` | `#2DC5A6` |

Implementation:
- Rewrite `LightPalette`/`DarkPalette` in `Theme.kt` with the hex values above, mapped onto the
  existing `VestraPalette` fields (`canvas`→background, `surfaceRaised`→card, `ink`→foreground,
  `accent`→accent, `modalityImage`→brand-image, `modalityVideo`→brand-video,
  `modalityAudio`→brand-voice, `modalityCode`→brand-chat). Drop the atelier-specific fields
  (`atelierCanvas`, `atelierContainer`, `saffronDeep`, `silkMist`, `ivory`, `ivoryMuted`) that
  have no lookbookweb equivalent, or repoint them at the closest real token so nothing that
  reads them breaks — audit call sites with `git grep` before deleting a field outright.
- `RadiusTokens`: lookbookweb's base `--radius: 1.5rem` (24px) with `calc(var(--radius) ± N)`
  offsets (`styles.css:14-20`) yields sm=12dp, md=16dp, lg=20dp, xl=24dp, **2xl=30dp, 3xl=36dp,
  4xl=44dp**. Agentic-AI's current scale (sm=12, md=16, lg=24, xl=32) only goes to `xl` and its
  `lg`/`xl` don't match lookbookweb's `lg`/`xl` — extend the object with `xl2`/`xl3`/`xl4` at
  30/36/44dp and correct `lg`→20dp, `xl`→24dp to match exactly (audit existing `RadiusTokens.lg`
  call sites — `GlassCard` currently uses it and this changes its corner radius).
- Shadows: lookbookweb's `--shadow-float`/`--shadow-lift` are multi-layer soft shadows tuned per
  theme (`styles.css:121-126`, `173-176`) — port as two `Modifier.shadow()`-equivalent Compose
  elevation/tint pairs (Compose can't do CSS multi-shadow directly; approximate with a single
  soft shadow at the outer layer's blur/spread/color, since Android's shadow rendering is
  GPU-cost-sensitive — matches this repo's own existing `SpatialElevation` comment "prefer flat
  glass — shadows are GPU-risky").
- Typography: **already correct** — Syne (display) / Outfit (body) already loaded in `Type.kt`,
  matching `styles.css:21-22` exactly. No change needed.

### A1. Motion/interaction primitives — exact match

lookbookweb's utility classes (`styles.css`) map to Compose modifiers. Current state vs. target:

| lookbookweb utility | Exact spec | Agentic-AI today | Action |
|---|---|---|---|
| `press-3d` | hover `translateY(-2px)`, active `translateY(1px) scale(0.98)`, 0.25s | **missing** | New `Modifier.press3d()` in a new `PressModifier.kt` — mirrors `TiltModifier.kt`'s pattern but Y-translate + scale only, no rotation |
| `tilt-3d` | hover `translateY(-6px) rotateX(6°) rotateY(-6°) scale(1.015)` | `Modifier.tilt3d(maxDegrees=6f)` exists | Verify translateY(-6px) component is present — current impl may be rotation-only; add the Y-lift if missing |
| `lift-3d` | hover `translateY(-3px) scale(1.008)`, no rotation | **missing** | New `Modifier.lift3d()` — for list rows (jobs, history, model rows), gentler than tilt |
| `shimmer` | `::after` gradient band, `translateX(-100% → 100%)`, 1.6s ease-in-out infinite | `ShimmerBlock.kt` exists | Verify sweep duration matches 1.6s; adjust if the ported version used a different value |
| `float-slow` | 9s ease-in-out infinite, `±14px` vertical | not ported as a standalone modifier | Add `Modifier.floatSlow()` for the home hero orb |
| `drift-slow` | 22s ease-in-out infinite, translate `3%,-4%` + scale to 1.08 | not ported | Add `Modifier.driftSlow()` for the 3 ambient background orbs |
| `gradient-pill` | `linear-gradient(135deg, accent1, accent2)` fill + inset highlight + drop shadow | `CreateFab` already uses a radial gradient close to this | Adjust `CreateFab`'s brush to the two-stop 135° linear gradient using the new accent tokens; reuse for the dock's *active item* fill (see A2) |
| `gradient-text` | accent-gradient `background-clip:text` | not ported | Compose `Brush` + `TextStyle(brush=...)` (API 33+; this app's `minSdk=35` so it's available) — for the home H1's "next creation?" span |
| Reduced motion | `.reduce-motion` class + OS `prefers-reduced-motion` both neutralize every transform-based utility | `rememberReduceMotion()` already exists per `TiltModifier`'s gate | Extend the same gate to every new modifier above |

All new modifiers go in `composeApp/.../ui/components/` alongside `TiltModifier.kt`, follow its
exact pattern (pointerInput-driven, `animateFloatAsState`, reduced-motion identity fallback),
and get the same class of Robolectric smoke test `TiltModifierTest.kt` already established.

### A2. App shell — top bar + ambient background + bottom dock

Source: `AppShell.tsx:38-178`.

**Ambient background layer** (new — nothing like this exists today):
- Root screen background: `sky-canvas` = flat background color + a 3-layer radial gradient
  wash (`--gradient-sky`, `styles.css:106-112` light / `162-164` dark). Port as a
  `Modifier.background(Brush.radialGradient(...))` composed of the 3 stops, or a custom
  `Canvas` draw for exact positioning (`120% 90% at 15% -10%`, `100% 80% at 95% 0%`,
  `120% 100% at 50% 110%`). This is distinct from `SpatialBackground` (which currently renders
  the atelier look) — repoint `SpatialBackground` at this new wash instead of building a
  parallel component.
- 3 blurred "orb" shapes, `drift-slow` animated, tinted `brand-image/40`, `brand-voice/25`,
  `brand-chat/25` at roughly 72dp/80dp/96dp, positioned at off-canvas corners, `blur(60px)`
  (`orb` utility, `styles.css:328-334`) — Compose `Box` with a radial-gradient fill and a
  large `blur()` `Modifier.graphicsLayer` / `RenderEffect`, driven by `Modifier.driftSlow()`.

**Top bar** (`AppShell.tsx:51-83`):
- `sticky top-0`, 64dp height, translucent `background/60` + heavy blur (`backdrop-blur-2xl`
  ≈ 40px), bottom border in `glass-border`.
- Left: a **Back pill** (`soft-card press-3d rounded-full`, text "Back") when navigating deeper,
  else the **home mark** — a 36×36dp `gradient-pill` rounded-2xl square with a bold "S" plus
  "Studio" wordmark. Agentic-AI's current top bar (`GlassTopBar`) doesn't have this exact
  logo-mark-vs-back-pill switch — add it.
- Center/left-adjacent: page title in display font, bold, truncated.
- Right: an account-style pill — since there's no auth, repoint this at **Settings** directly
  (icon + "Settings" label, `soft-card press-3d`) rather than an auth-gated account link.

**Bottom dock** (`AppShell.tsx:89-178`) — Agentic-AI's `LookbookBottomBar.kt` is already
structurally very close (Home/Library left, center Create FAB, Chat/Settings right — same 4+1
arrangement). Concrete gaps to close:
- **Active-item treatment**: lookbookweb fills the *entire* active dock item with a
  `gradient-pill` background (`!text-accent-foreground gradient-pill shadow-none`), not just an
  icon-color change + 4dp dot. Change `DockItem`'s selected branch to draw a
  `RoundedCornerShape` gradient-pill background behind the icon+label column when `selected`,
  matching `CreateFab`'s gradient treatment, and remove the small dot indicator (it doesn't
  exist in the source).
- **Container**: lookbookweb's dock pill uses `rounded-4xl` (44dp, per the radius table above)
  — Agentic-AI's currently uses `RadiusTokens.xl`; update to the new 4xl token once A0 lands.
- **Shadow**: `dock-shadow` = the heavier `--shadow-lift`, not the lighter default — apply the
  lift-tier shadow specifically to this component.
- **Center FAB**: switch from the current 3-stop radial gradient to the exact 2-stop 135°
  linear `gradient-pill` gradient (accent tones from A0), 56×56dp (lookbookweb: `h-14 w-14` =
  56px — Agentic-AI's `52.dp` should become `56.dp` for an exact match), press micro-interaction
  already present and correct.
- **Create picker**: lookbookweb opens a **centered Dialog** (title "Create", description "Pick
  a tool to start something new.", tool rows as `soft-card press-3d`), not a bottom sheet.
  `QuickCreateSheet.kt` currently uses `ModalBottomSheet` — change to a centered `AlertDialog`/
  `Dialog` with `rounded-3xl` styling to match exactly (a bottom sheet is arguably more
  Android-idiomatic, but the user's ask is exact parity, so match the dialog pattern).

**Skip link**: no Android equivalent needed (this is a web-only a11y pattern for keyboard
users bypassing repeated nav) — covered instead by correct TalkBack focus order (Part A12).

### A3. Card/surface primitives — exact match

| lookbookweb class | Spec | Agentic-AI equivalent | Gap |
|---|---|---|---|
| `soft-card` | translucent glass fill (`card-color 55%` mixed with transparent), 1px `glass-border`, `shadow-float`, `blur(22px) saturate(160%)`, gradient rim highlight via `::before` mask trick | `GlassCard` | Verify blur radius (22px→~22dp `RenderEffect` blur) and the rim-highlight trick; `GlassCard` may already approximate this — audit against exact opacity (55%) and add the top-edge gradient highlight if missing |
| `glass-tile` | lighter/nested variant, `blur(14px)`, `42%` opacity | `GlassTile` | Verify against exact opacity/blur values above |
| `solid-card` | opaque variant, same border/shadow, no blur | **missing as a named variant** | Add `SolidCard` (or a `GlassCard(translucent=false)` param) — used for dense reading surfaces (chat bubbles, transcript boxes) |
| `dock-shadow` | heavier `shadow-lift` | see A2 | apply to `LookbookBottomBar` container |

### A4. Route-by-route exact layout

Each subsection cites the exact source file/lines cataloged from `lookbookweb`. Where
Agentic-AI's local-first architecture means the underlying data source differs, that's called
out explicitly — the **visual layout is still the exact-match target**.

**A4.1 — Home: real architecture gap found during audit, re-scoped as its own sub-phase (A4.1a).**
lookbookweb's `index.tsx` is a distinct **landing page** (hero + 2×2 tool-tile grid + Recent
Projects list) that is *separate* from `studio.tsx` (the tabbed Image/Video/Chat/Audio working
surface). In Agentic-AI today, the bottom dock's Home destination goes **directly** into
`HomeScreen.kt`'s tabbed pager (`UnifiedStudioPane`/`AudioStudioPane` behind
`ScrollableTabRow`+`HorizontalPager`) — there is no separate landing page; Home *is* the studio
pager. Building the landing page for real means: a new composable, repointing the dock's Home
destination at it, and moving the pager one level deeper (behind a tile tap or the existing
Create-FAB dialog) — a navigation-architecture change, not a visual tweak, and one that touches
code several existing tests assert against directly (`HomeTabVisibilityTest`,
`StudioIsolationAfterNavTest`, `BottomBarNavigationTest`, `ScreenshotTest`'s
`studio-header-and-dock` case, `appium/test_bottom_bar.py`). Doing this safely needs its own
focused pass — auditing and updating every one of those tests alongside the change — not a
rushed edit folded into an already-large phase. **Scoped as A4.1a, sequenced before the rest of
A4.1's content below** (which describes the landing page's own layout once it exists). Recent
Projects sourcing has its own honest gap too: `WardrobeRepository` (the natural local-first
analog to lookbookweb's cross-tool `projects` table) only receives entries from the Image and
Video paths (confirmed via `GenerativeViewModel.ingestCreateImage`, called from both the
Create-image and Video result branches) — Code and Audio results never reach it. Until a unified
local "recent activity across all tools" store exists, the landing page's Recent list will
under-represent Code/Audio activity; note this honestly rather than fabricate coverage or
silently expand `WardrobeRepository`'s scope beyond what its name implies.

**A4.1 — Home page layout** (`routes/index.tsx`, → new landing composable once A4.1a lands)
- Hero card: `soft-card` with a single `float-slow` orb (brand-image tint, 160×160dp,
  top-right), H1 "What's your next creation?" with "next creation?" rendered in
  `gradient-text`, subhead "Images, motion, live voice, code and chat with memory — one spatial
  studio." (adjust copy — no accounts, so "with memory" claim only holds once Part B's local
  memory ships).
- 2-column tool grid (`scene-3d` perspective wrapper + `tilt-3d` per tile): icon chip (44dp,
  brand-colored per modality) + display-font bold label + blurb. Maps to Image/Video/Audio/Code
  — 4 tiles, 2×2 grid (lookbookweb's 5th "tool", Chat, is dock-accessible not grid-accessible
  per its own `TOOLS` list vs. dock destinations — mirror that: don't put Chat in this grid).
- "Live sources" promo row → maps to News/Chat's existing headline-sync concept (A4.8).
- "Recent projects": Agentic-AI's equivalent is Wardrobe's recent entries — same visual
  treatment (`soft-card press-3d` rows, icon chip, title, tool·date, chevron), sourced from
  `WardrobeRepository` instead of a Supabase query, capped at 6, linking into the existing
  look-detail dialog instead of a `/project/$id` route.

**A4.2 — Chat** (`routes/chat.tsx`, → `NewsChatScreen.kt`)
- Header pill row: "Remembering N things" (`Brain` icon). **Done** — Part B.1's local memory
  count now exists, so `MemoryPill` wires to `MemoryRepository.facts`, hidden entirely at zero
  facts. `Brain` → Material Symbols `Psychology` (A5's icon-intent audit). "N live items" /
  "Add live sources" pill → maps to the existing News headline-sync affordance.
- Message list: user bubbles `rounded-br-lg` (tail bottom-right) filled `primary`/
  `primary-foreground`; assistant messages plain text, code fences split into `pre` blocks with
  `foreground`/`background` inverted colors. Agentic-AI's `ChatMessageBubble` (ported in 3.1.1)
  — audit against this exact tail-corner and fill treatment.
- Citation disclosure (`&lt;details&gt;` "Information used (N synced items)") → maps to the news
  source attribution already surfaced in some form; make it a collapsible `glass-tile` row if
  not already.
- Composer: `sticky bottom-28` (sits above the dock, not overlapping it) — verify
  `PromptComposer`'s placement in `NewsChatScreen` sits clear of `LookbookBottomBar`.

**A4.3 — Studio (tabbed multi-modal)** (`routes/studio.tsx`) — **this is a second UX pattern
lookbookweb runs *in addition to* the dedicated `/create/*` routes**: one tabbed page
(Image/Video/Chat/Audio tabs) with routing/token-budget/safety/effects/history all in one
place. Agentic-AI's `UnifiedStudioPane` + `HorizontalPager` tab pattern (Image/Video/Audio/Code)
is architecturally the closer analog to *this* route, not to the separate `/create/*` pages —
treat `UnifiedStudioPane` as the `studio.tsx` port target and the dedicated screens in A4.5-A4.7
as covering the same ground through `/create/*`'s presentation instead. Don't build both;
pick `UnifiedStudioPane` as the single source (matches this app's existing architecture) and
make sure every distinct thing `studio.tsx` shows — token-budget line, safety-preset row,
background-jobs list, per-modality history with Load-params/Re-run/Export/Delete — is present
somewhere in the pager, even if split across `PromptComposer` + `ResultPane` rather than one
mega-page.
- **Token-budget line** (real gap — see Part B) — "≈N prompt tokens + N reserved … of N
  available", debounced 250ms, turns `destructive`-colored when it will truncate.
- **Safety controls** (real gap — see Part B) — on/off switch + preset pill row + confirm-before-run
  for presets that need it.
- **Background jobs list** — Agentic-AI's `InterruptedJobsBanner`/`LocalJobStore` cover the
  "resumed after a kill" case; lookbookweb's version is a live *while-running* list with
  per-row progress bars and Cancel — audit whether `GenerativeState.Running` surfaces this as a
  list (multiple concurrent jobs) or just the single active one, and whether that gap matters
  given Agentic-AI's one-generation-at-a-time model.

**A4.4 — Library** (`routes/library.tsx`, → `WardrobeScreen.kt`)
- Search bar + upload-dialog trigger (Agentic-AI has no upload-to-library flow distinct from
  generation — this may not apply; audit whether "import an existing image into the gallery"
  is a wanted feature or an artifact of lookbookweb's account-based storage model).
- Filter pills: All / Images / Videos (`aria-pressed`) — `WardrobeScreen` currently has
  All/Favorites (per 3.1.2's new tags); lookbookweb's is by-type not by-favorite. Consider
  adding type filters alongside the existing favorites filter rather than replacing it —
  Agentic-AI's favorites concept has no lookbookweb equivalent to conflict with.
  Also add a **tag-filter pill row** if any per-entry tagging exists or gets added.
  A dynamic `#tag` chip row (`border-accent bg-accent` when selected) is a genuinely new,
  addable piece of UI if Wardrobe entries ever get freeform tags — not currently modeled;
  scope as optional/deferred until a tagging data model exists.
- **Demo-data banner concept**: lookbookweb shows sample content with a "Sample" badge + an
  explanatory banner when the real library is empty. Agentic-AI's empty-Wardrobe state
  (`GlassEmptyState`) is plainer — consider matching the tone (a `soft-card` banner explaining
  what's shown and how to make it go away) but **do not fabricate sample generated images** —
  that would violate this project's own no-fabricated-content discipline (`docs/DRAWBACKS.md`).
  If ported, sample content must be clearly-labeled real static assets, not claimed generations.

**A4.5 — Create → Image** (`routes/create.image.tsx`)
- Reference-image thumbnails (up to 3, 64×64dp, X-remove overlay) — Agentic-AI's image-edit
  entry point currently supports one reference image (`REFERENCE_IMAGE_THUMB` tag, 3.1.1's
  audit); extending to 3 is a real capability gap worth scoping only if the underlying
  generators (SD-Turbo edit, Bonsai) can actually consume multiple references — check before
  committing to the UI change; don't add UI for an input the engine ignores.
- Style/aspect-ratio chip rows exactly as cataloged — Agentic-AI likely already has some of
  this; audit `PromptComposer`'s style/aspect controls against lookbookweb's exact chip set
  (none/photo/3d/illustration/cinematic/poster; 1:1/16:9/9:16/4:5).
- **Progressive blur-to-sharp reveal** on the result image as it streams — Agentic-AI's local
  image generators already report live per-step progress (session history: Bonsai/SD-Turbo
  `LocalImageStreamEvent`); wire that into a `blur(24dp)→blur(0dp)` animated `Modifier` on the
  in-progress preview instead of (or alongside) the existing progress bar — genuinely portable
  since the underlying live-step data already exists.

**A4.6 — Create → Video** (`routes/create.video.tsx`)
- Quality/Orientation/Length chip rows — audit against existing video studio controls.
- **Version-chain "Prompt history"** with re-run-as-new-version — Agentic-AI's B2 (version
  lineage, already shipped) is the direct analog; verify the UI surfaces it with the same
  chip-row / re-run affordance lookbookweb uses, not just the Wardrobe history dialog.
- Export options panel (Format/Resolution/Bitrate chips, client-side re-encode with a safe
  fallback) — audit whether Agentic-AI's video export already offers format/resolution choice;
  if it's a fixed export today, this is a real, scoped feature gap.

**A4.7 — Create → Voice** (`routes/create.voice.tsx`) — the richest single route in
lookbookweb (57KB). Compare against Agentic-AI's already-substantial B6 voice-studio DSP work:
- 3-way Live/Record/Clone segmented control — Agentic-AI's `AudioStudioPane` structure; audit
  whether "Clone" (enrollment-sample capture for a custom voice) exists — if not, this is a
  real, scoped new capability (not just UI — needs an actual voice-cloning DSP/model path,
  out of scope for a UI-parity phase; flag as a separate future capability, not silently faked).
- Preset grid with technical readout lines (semitone offset, timbre, reverb/ring-mod/distortion
  flags) — Agentic-AI's B6 grouped-persona chips are the closer analog; audit exact readout
  copy/format.
- Latency-mode chips (Lowest/Balanced/Smoothest) with estimated-ms figures, plus
  "Auto-calibrate latency" — Agentic-AI's `AndroidLatencyCalibrator`/"Calibrate mic latency"
  chip (B6) is the direct match; verify the 3-mode selector exists alongside the calibrate
  action, not just the raw calibrate button.
- `LiveScope` waveform+spectrum — this is exactly `SpectrumScope` + `AudioLevelMeter` (now
  wired to real data per 3.1.2's `AndroidPlaybackVisualizer`); the *live-mic-monitoring* variant
  (real-time pass-through with a visible input+output dual trace) may not be fully ported —
  audit whether Agentic-AI's voice changer does true live monitoring (processed audio audible
  in real time while speaking) vs. record-then-transform; this is a genuine architecture
  question (real-time audio graph vs. buffer-based DSP), not a small UI tweak — scope carefully.
- Voice-balance quota meter (`role="meter"`, remaining-minutes, reset-date) — **not applicable**;
  this exists because lookbookweb's cloud TTS/voice has a paid-tier quota. Agentic-AI's local
  DSP has no such quota — omit rather than fabricate one.

**A4.8 — Sources** (`routes/sources.tsx`) — genuinely new UI pattern. Agentic-AI's News/Chat
already syncs headlines from feeds; lookbookweb exposes a full **source-management** screen:
add-source-by-URL form with sync-interval picker (Hourly/6h/Daily/Weekly), per-source
Sync/Remove actions, and a "latest synced items" list. If Agentic-AI's news feed is currently a
fixed, non-configurable source list, this is a real, scoped feature: let the user add/remove/
adjust-interval on their own feed URLs. Purely additive — doesn't conflict with anything
existing.

**A4.9 — Settings** (`routes/settings.tsx`, → `SettingsScreen.kt`)
Section order to match exactly: Appearance & accessibility (Dark/Reduce motion/Larger text) →
device/engine lab → provider/cloud settings → diagnostics → "What the assistant remembers"
(Part B) → Account (n/a, omit) → sample-data toggle (n/a unless A4.4's demo banner ships) →
About/Changelog link. Audit `SettingsScreen`'s current section order against this and reorder
if it drifted from this sequence during earlier phases.

**A4.10 — Changelog** (`routes/changelog.tsx`) — genuinely missing screen. Simple to add: an
"upgrade instructions" card (not applicable the same way — Agentic-AI ships as an APK, not a
git-pull; adapt to "install the latest release" instructions instead) + a release list sourced
directly from this repo's own `CHANGELOG.md` (parse it at build time or hand-maintain a parallel
`Release` list — prefer parsing the real file so this can't drift out of sync with reality,
matching this project's own anti-fabrication discipline).

**A4.11 — Auth** — **not ported.** No accounts exist in this app. If Settings' "Account"
section needs *something* where lookbookweb has a sign-in CTA, it should say plainly that this
app is fully local and there is no account — not a dead button.

**A4.12 — Project detail** (`routes/project.$id.tsx`) — maps to Wardrobe's existing look-detail
dialog; audit the media block / detail card / download+delete action-row layout against it.
**Audited.** `WardrobeScreen.kt`'s `LookDetailDialog` already covers every element this item
names: a media block (the look's own thumbnail/clip, `MediaThumb`), a detail card (tier · still-
or-clip · version count), a version-history chain (B2, tap-to-view earlier attempts), and an
action row (favorite, share, save-to-Photos/save-clip — the download equivalent, delete). No gap
found against the structure this item describes. Caveat: this session has no live browser access
to lookbookweb.lovable.app, so this is a structural audit against the item's own description, not
a pixel-level screenshot comparison the way A0–A4.4/A4.9 were verified — if a future session gets
screenshot access, worth a follow-up visual pass.

### A5. Iconography

lookbookweb uses `lucide-react`. Agentic-AI uses `androidx.compose.material.icons` (Material
Symbols). These are different icon families — **exact glyph match isn't achievable**, but
*intent* match is: audit the icon table in the research catalog against Agentic-AI's current
icon choices per surface and swap any Material icon whose *meaning* diverges (e.g. lookbookweb
uses `Brain` for the memory pill, `Radar` for live-sources, `Gauge` for latency calibration —
pick the closest Material Symbols equivalent for each, not a mismatched one).

### A6. Responsive behavior

Not applicable in the same sense — Android has no desktop-breakpoint concept the way
lookbookweb's `max-w-3xl`-centered-on-wide-viewport pattern does. The relevant takeaway instead:
lookbookweb is **phone-width-shaped by design** (floating bottom dock, single column, 44px+
touch targets) even where it renders wider — confirms the target this whole app already aims
for. No action beyond ensuring touch targets stay ≥44dp everywhere (fold into A12).

### A7. Toasts

**Done.** lookbookweb uses `sonner`, top-center, for all success/error/info/warning notices.
Every `Toast.makeText(...).show()` call site in the app (13 files, ~40 call sites, including
`MediaExport` — a plain Kotlin object with no Composable scope) is replaced with `GlassSnackbar`
(`composeApp/.../ui/components/GlassSnackbar.kt`): a global `MutableSharedFlow` message bus any
code can post to, plus a single `GlassSnackbarHost` mounted once at the app root
(`VestraNavHost`), positioned top-center, styled per lookbookweb's 4-level convention
(success/error/warning/info — distinct icon + accent color each; warning's amber is a
conventional choice, not sourced from lookbookweb's exact `sonner` hex, which wasn't captured in
this session's research).
Code-review caught and fixed three real bugs before landing: (1) the exit animation cleared the
displayed message in the same frame it started, so the card blinked off instead of animating out
— fixed by keeping the last-shown request in a separate `displayed` state that outlives `visible`
turning false. (2) `collect` processed messages strictly one at a time, so a newer message queued
behind whatever was already showing for up to 3.2s instead of pre-empting it — fixed by switching
to `collectLatest`. (3) the `SharedFlow`'s default `BufferOverflow.SUSPEND` meant a burst of 9+
calls would silently drop the 9th+ — fixed with `BufferOverflow.DROP_OLDEST`, matching the
"latest wins" semantics `collectLatest` already implements.

### A8. No page-transition animation

Confirmed: lookbookweb has zero route-transition animation (instant swaps). Agentic-AI should
likewise **not** add elaborate screen-transition animation in the name of parity — matching
"none" is still matching exactly.

---

## Part B — Non-UI capabilities worth pulling from lookbookweb

Everything below is architecture/product logic, independent of the UI parity work above, and
independently valuable. Each is graded for portability given the local-first constraint.

1. **Persistent chat memory** (`src/routes/api/memory.ts`, `lib` memory handling). After each
   conversation turn, an LLM call extracts durable facts ("stated preferences, projects, tools,
   constraints, names, recurring goals") as a capped JSON array (≤5 new facts/extraction,
   deduped against existing memory), stored and re-injected into future system prompts, with a
   user-editable "Memory" panel (view/edit/delete). **Fully portable on-device**: run the
   extraction prompt through the local chat model itself (Qwen3/Gemma) instead of a cloud
   gateway, store facts in a small local table (Room or a flat JSON file, matching this app's
   existing local-storage patterns), inject into `NewsChatScreen`'s system prompt. This is the
   single most valuable non-UI pull — a real capability gap, not just cosmetic, and doesn't
   touch cloud/accounts at all.

2. **Tokenizer-aware context budgeting** (`src/lib/tokens.ts`). Real per-model context-window
   table, a token-count estimate shown live in the composer, and a hard "will truncate" warning
   before sending — using the *real* tokenizer when available, a calibrated character heuristic
   otherwise. Agentic-AI has no equivalent today; local models' tokenizers are already loaded
   (BPE for Qwen3, etc.) so this is a straightforward, real, on-device port — surfaces exactly
   the kind of honest pre-failure warning this project's whole design philosophy already favors
   over silent truncation.

3. **Prompt-level safety presets** (`src/lib/safety.ts`). Off/Standard/Blur-identities/Redact
   presets that inject **prompt-guard wording** before generation (not just post-process),
   with a confirm-before-run step for the stronger presets, plus a client-side redact
   (pixelate) post-process alongside the existing blur. Agentic-AI already has post-process
   privacy blur (B7, ML Kit face detection); this adds the *pre-generation* prompt-guard layer
   it's missing, plus a distinct redact/pixelate mode. Fully local — no new dependency beyond
   string concatenation into the existing prompt pipeline and a `Bitmap` pixelation function
   (same complexity class as the existing `BoxBlur`).

4. **Creeping-progress + fallback-path job pattern** (`src/lib/jobs.ts`, `src/lib/retry.ts`) —
   **audited, partially closed.** Cloud video/audio (HF Space polling) already had a real
   creeping-progress formula (poll-index eased toward a ceiling) duplicated inline at two call
   sites in `GenerativeCloudService.kt` — not a gap, just untested duplication. Extracted into
   `CreepingProgress.forPoll()` (`shared/commonMain/cloud`), a single tested primitive both
   call sites now share (7 unit tests, exact-regression-checked against the original formulas
   so the emitted fractions are byte-identical). The three local blocking calls with no
   progress signal at all (video still-clip encode, system TTS, voice-changer DSP) were
   audited and found to be sub-2.5s in practice — a concurrent progress-ticker for a window
   that short was judged not worth the added complexity, so left as-is (their progress bar
   sits at a fixed low value for that brief window rather than creeping). The
   retry-exhaustion "gentler path" (lower resolution/relaxed constraints) is **deferred, not
   built**: a real fallback would need per-provider parameter tuning for each Gradio Space's
   actual API, which can't be verified without live access to test against — building it
   without that verification risks shipping fabricated, unverified fallback behavior, which
   this project's own discipline rules out. Noted here and in `docs/DRAWBACKS.md` as a
   deliberate scope decision, not a silent drop.

5. **Resumable, range-request pack downloads** (`native/android/EdgeLlmPlugin.kt`'s
   `downloadModel`) — **audited, no gap found.** `PackDownloadWorker.downloadOneFile` already
   implements real `Range: bytes=N-` resume per file against on-disk staging that survives
   process death and app restart (`ModelPackManager.stagingDir`), with `doWork()` recomputing
   `doneBytes` from actual on-disk file lengths on restart rather than assuming zero, and
   WorkManager persisting the enqueued job across restarts with exponential backoff. Nothing
   to port — this item is closed as already-done.

6. **Preflight-report pattern generalized**. lookbookweb's `runPreflight()` (browser
   capability checks) and Agentic-AI's own `AppSettings.preflight()`/`PacksScreen`'s
   INCOMPATIBLE checklist (B4) are already the same *shape* (a list of `{id, label, level,
   detail, fix}` checks with a summary) — no new capability here, just confirms the pattern
   already adopted is sound; no action needed beyond noting the convergence.

7. **Explicitly not portable / not worth porting**: Supabase auth & row-level security (no
   accounts), the admin-role seed account (no multi-user backend), voice-balance quota
   metering (no paid tier to meter), Google OAuth sign-in, cross-device sync. Listed here so
   they're a documented decision, not an oversight.

---

## Phase plan

Each phase ends with the standing gate: `./gradlew :shared:testDebugUnitTest
:composeApp:testSideloadDebugUnitTest :composeApp:lintSideloadDebug
:composeApp:assembleSideloadDebug`, plus new Robolectric screenshot tests for every visibly
changed surface (this repo's established, load-bearing way of proving a UI change actually
renders correctly without a device) and unit tests for every new non-UI capability in Part B.

1. **A0 — Design tokens.** Full palette/radius replacement. Highest blast radius (every screen
   repaints), do it first and alone so any regression is obviously attributable to this phase.
2. **A1 — Motion primitives.** New `press3d`/`lift3d`/`floatSlow`/`driftSlow` modifiers,
   `gradient-text`, verify `tilt3d`/shimmer against exact specs.
3. **A2 — App shell.** Ambient background, top bar, dock active-state fill, Create dialog
   (sheet→dialog).
4. **A3 — Card primitives.** `SolidCard`, blur/opacity audit on `GlassCard`/`GlassTile`.
5. **A4.1–A4.2 — Home + Chat.** Highest-traffic screens, do together.
6. **A4.3 — Studio/pager audit.** Done: token-budget bar (Code tab) + safety-preset pill row
   (Image/Video) now live inline in `UnifiedStudioPane`, wired to Part B.2/B.3's real state.
   Also closed a gap the audit surfaced: `generateVideo()` never applied the safety guard —
   fixed to match `generateImage()`. Background-jobs-list and citation-disclosure sub-items
   audited as not applicable to this app's one-generation-at-a-time architecture.
7. **A4.4 — Library.** Done: media-type filter (All/Images/Videos) added alongside the
   existing Favorites filter, pure-function-tested. Upload-to-library and the demo-data banner
   audited and left unbuilt — lookbookweb artifacts of its account-based cloud storage with no
   honest local-first equivalent. **A4.9 — Settings order.** Done: reordered to match
   Appearance → Engines → Cloud → Diagnostics → Memory; gates unchanged, order-only change,
   code-review clean. **A4.10 — Changelog screen.** Done: `ChangelogParser` (pure, tested)
   parses the real `CHANGELOG.md`, bundled into the APK as a build-time asset via a new
   `copyChangelogAsset` Gradle task so it can't drift from what shipped; `ChangelogScreen`
   renders the release list, linked from Settings → About. Code-review caught and fixed two
   real bugs before landing: a non-version `## CI / releases` heading being mis-parsed as a
   fake release, and synchronous asset I/O blocking the composition thread.
   **A4.2/A5 — Chat memory pill.** Done: "Remembering N things" header pill wired to Part B.1's
   real `MemoryRepository.facts` count, hidden entirely at zero facts; `Brain` → Material Symbols
   `Psychology` (A5's icon-intent-match rule). Code-review found the first pass hand-duplicated
   `GlassPill`'s container styling a third time — fixed by adding an optional `leadingIcon` slot
   to `GlassPill` itself.
8. **A4.5–A4.7 — Create screens** (Image/Video/Voice), scoping each engine-dependent item
   honestly per the notes above rather than adding UI for capabilities that don't exist yet.
9. **A4.8 — Sources** (new source-management screen).
10. **A7 — Toast repositioning.** Done: every `Toast.makeText` call site (13 files) replaced with
    `GlassSnackbar`, a top-center Compose message bus reachable from Composable and plain-Kotlin
    call sites alike (see A7 section above for the three code-review-caught bugs fixed before
    landing). **A5/A6/A8** — icon-intent audit (partially covered by the A4.2 pill above; a
    fuller pass folds into A12), responsive/touch-target audit (folds into A12), and the
    no-page-transition-animation confirmation (already matches, no action needed) remain.
11. **Part B.1 — Local chat memory.** Independent of UI phases; can run in parallel with any
    of the above once A4.2 (Chat) has landed, since it needs the chat surface to inject into.
12. **Part B.2 — Token budgeting.** Independent; feeds into A4.3.
13. **Part B.3 — Safety presets.** Independent; feeds into A4.3.
14. **Part B.4 — Job progress/fallback pattern.** Done (partial, deliberately): audited every
    generator, extracted+tested the shared creeping-progress primitive, deferred the
    gentler-path fallback with a documented reason. See Part B write-up above.
15. **Part B.5 — Resumable pack downloads.** Done (audit-only, no gap): `ModelPackManager`
    already resumes via HTTP Range + on-disk staging. See Part B write-up above.
16. **A12 — Full accessibility pass** (last, so it audits the final state of every other
    phase rather than an intermediate one): 44dp touch targets everywhere, TalkBack labels on
    every icon-only control, live-region equivalents (`liveRegion` semantics) for generation
    progress and voice state, reduced-motion coverage across every new modifier from A1.

## Definition of done

- Every design token in `Theme.kt` traces to a cataloged lookbookweb value, not a guess.
- Every route/screen in the catalog above has either a landed Compose equivalent or an explicit,
  reasoned note in this doc for why it doesn't (auth) or isn't yet (engine-dependent gaps).
- Part B's five portable items are shipped with real unit tests, or explicitly deferred with a
  reason in `docs/DRAWBACKS.md`.
- Full test/lint/build gate green after every phase, screenshots captured for every visibly
  changed surface per this repo's established Robolectric `GraphicsMode.NATIVE` pattern.
- `docs/DRAWBACKS.md` updated phase-by-phase the same way every prior phase in this project's
  history has been — closed items get a "closed in X, verified by Y" note, not silent deletion.
