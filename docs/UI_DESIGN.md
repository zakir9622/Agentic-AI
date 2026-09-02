# UI Design

What the app's navigation and per-screen design look like today, and the redesigns that got it
here. For the functional/backend side of each screen (what happens when you tap Generate), see
`docs/FUNCTIONALITY.md`. For the full chronological story, see `docs/PROJECT_HISTORY.md`.

## Navigation shape (current)

A 3-item bottom dock (`LookbookBottomBar`): **Home / Library / Settings**. This replaced an
earlier 5-item dock (Home/Library/**Create FAB**/Chat/Settings) — see
[Redesign: bottom dock, 5 items → 3](#redesign-bottom-dock-5-items--3) below.

- **Home** — a tool grid (Image, Video, Audio, Code, Chat) plus entry points to Model Packs and
  Help. Tapping a tool card navigates straight to that modality's isolated screen. This grid
  used to live behind a "+" FAB popup (`QuickCreateSheet`); it is now Home's own content.
- **Library** — the Wardrobe screen: generation history, searchable, with recipe reuse (pull a
  prior generation's parameters back into the composer).
- **Settings** — a hub of navigable rows (Models, Default models, Notifications, API monitor,
  Diagnostics) plus inline sections for API keys, appearance, storage, permissions, safety and
  memory. See "Redesign: professional-UI pass" below.

Each modality screen (Image/Video/Audio/Code, and Chat separately under `Routes.CHAT`) is a
fully isolated route wrapped in `IsolatedStudioScreen` — a back arrow + title, no shared chrome
between them. This is deliberate: each modality only ever has one model resident at a time, and
an earlier shared-tab-pager design risked keeping more than one loaded simultaneously (see
[Redesign: retire the tabbed pager](#redesign-retire-the-tabbed-pager) below).

Try-on's routes (`GARMENT`/`CASTING`) still exist in `VestraNavHost` but have no entry point
from Home or the bottom dock — see `docs/PROJECT_HISTORY.md`'s "Where try-on stands today".

## Per-studio screen anatomy (current)

Image, Video, Audio, and Code studios (`UnifiedStudioPane` for the first three + Code,
`AudioStudioPane` for audio-specific controls) share one layout pattern:

1. **A scrolling conversation history** — prompt → result turns, like a chat thread, instead of
   a single result card that gets overwritten each generation. A typing indicator shows while a
   turn is still running; new turns animate in and auto-scroll into view.
2. **The composer** — prompt text field, a model chip (tap for the quick switcher, long-press or
   "Browse all models…" for the full picker), a send button that doubles as the stop/cancel
   control while generating.
3. **A docked live log** (`DockedLiveLog`) — a single collapsed line next to the composer,
   expandable to the same scrollback a separate console card used to show. Replaces a duplicate
   status pill, a separate `Cancel` button on the result card, and (for Code) an animated status
   indicator that used to stack on top of all of that.
4. **An "Advanced" section** (Image studio only, local models only) — sliders for steps,
   guidance scale, seed, and img2img strength, plus a 1×–4× candidate-count picker. Hidden
   entirely while a cloud model is selected, since cloud payloads don't accept sampler
   overrides.

Chat (`NewsChatScreen`) follows the same conversation pattern independently, with its own
richer chat bubbles, a typing indicator, an empty state, a headlines bar, and a quick-prompt
carousel (all ported from the GoogleLookBookUI source in Era 6 of the project history).

## Redesign: professional-UI pass (post-3.1.8)

A review against the shipped build found the home screen reading as a debug console rather
than a product, and three of the four symptoms traced to concrete layout defects rather than
taste:

- **The usage dashboard was the hero.** `UnifiedMainScreen` pinned `ApiUsageDashboardCard`
  above the thread *and* rendered a second copy as the empty-state item, so a fresh install
  opened on a token counter above ~900px of dead space. Both call sites are gone; the monitor
  now lives at **Settings → API monitor**. The empty state is a greeting plus three one-tap
  starters (per composer mode) that fill the prompt and send, so a cold install reaches its
  first result in two taps.
- **Service tiles laid text out vertically.** A `FlowRow` gave all five service chips
  `weight(1f, fill = false)` with no `maxItemsInEachRow`, so they never wrapped and each got
  ~52dp on a 360dp phone; the five unweighted `Text`s inside had no `maxLines`, so
  `"0 reqs · 0 tok"` soft-wrapped one character per line. Now `maxItemsInEachRow = 2` with
  every `Text` bounded.
- **The composer chip showed an error, not a model.** It was fed
  `GenerativeViewModel.preflightLabel()`, which returns the *blocked reason* when cloud is
  gated — a 140-character consent sentence rendered as `Pick a cloud model in the model pi…`.
  That accessor is now split into `modelLabel()` and `blockedReason()`, and the composer has a
  dedicated hint row for the latter.
- **The top bar could not fit.** Three unweighted children under `SpaceBetween` demanded
  ~423dp of a 324dp content width. The service chip and its inline `DropdownMenu` were removed
  entirely (the composer already owns model selection), leaving a `weight(1f)` brand block and
  two icon buttons.

Two palette bugs surfaced during the same pass, both pre-existing and app-wide:

- `LightPalette`'s `glassBorder`/`glassFill` were white-on-white against white cards, so
  `GlassSecondaryButton` rendered as bare text and unbordered tiles floated. Light-mode glass
  tokens are now a low-alpha ink border over an off-white fill.
- `GlassTopBar` never set a content color, so every screen title inherited `LocalContentColor`'s
  black default and was invisible in dark mode. Titles are explicitly `VestraColors.Ink`, and
  `GlassCard` now provides ink as its content color so no child has to remember.

Settings became a **hub**: navigable rows for Models, Default models, Notifications, API
monitor and Diagnostics, with API keys, appearance, storage, permissions, safety and memory
staying inline. Model configuration — engine tier, packs, keys, and the five per-capability
dropdowns — moved out to `ModelsScreen` / `ProviderModelsScreen` / `DefaultModelsScreen`.
`ProviderModelsScreen` fetches the provider's own `/models` endpoint live
(`shared/cloud/ProviderModelDirectory.kt`, covering Groq, OpenRouter, Gemini and the HF router
behind a 1h TTL cache) and lists **everything the key returns**, marking rows Ready or
Not-usable from `CloudModelContracts` rather than hiding the ones the app cannot route.

Generations now post notifications (`notify/GenerationNotifier.kt`, `generation_results`
channel), gated on a per-category preference, the OS grant, and the app being backgrounded.

Verification for all of the above is `composeApp/src/test/.../RedesignScreenshotTest.kt` —
real Compose renders at **360dp and 411dp, light and dark**, since the vertical-text
regression was invisible at the 411dp-dark-only coverage that existed before.

## Model selection UI

Three ways to pick a model, in increasing order of how much you need to see:

1. **The composer's model chip** — shows the currently active model's name; always visible.
2. **The quick switcher** (`ModelQuickSwitcher`) — a `DropdownMenu` popup anchored at the chip.
   Shows on-device entries first, then up to 4 top-ranked cloud models (ranked by live model
   health + quality score), plus a trailing "Browse all models…" row. Added in Era 7 so
   switching models doesn't require leaving the composer for the common case.
3. **The full picker** (`ModelPickerSheet`) — a `ModalBottomSheet` with search, every model's
   full metadata (license, size, "Accepts:" capability badges), and per-row readiness
   indicators. This is what both error-recovery retry flows use, and what "Browse all
   models…" opens.

Every row's **readiness indicator** (a colored dot + optional checkmark) reflects real,
current reachability — `AppSettings.cloudUsable()` — not just "does this provider need a key
and do I have one". A provider that's selected but blocked on missing cloud consent shows as
not-ready, the same as one missing its API key; this was a real gap a code-review pass caught
after the manual cloud/on-device toggle was removed (Era 7) and fixed by routing every picker
readiness lambda through the same `cloudUsable()` check the actual generation gate uses.

## Design language and its history

### Redesign: "Loom Ink" → lookbookweb-parity tokens (v3.1.3, superseded)

The app's original palette, "Loom Ink," was brass-on-deep-ink — deliberately chosen to avoid a
generic AI-tool look. A plan to exactly match a sibling web app's (`lookbookweb`) shipped design
system replaced it in 3.1.3: light airy canvas, near-black primary, one electric-blue accent,
per-modality brand colors (image blue, video red, audio pink, code/chat teal), and an extended
radius token scale (`xl2`/`xl3`/`xl4`). This is the palette the app ships today.

**What did not survive**: the rest of that plan — matching lookbookweb's 5-item dock with a
center Create FAB, and its `QuickCreateSheet` tool-picker dialog — was explicitly rejected by
direct user request in 3.1.4 (see the next section) before the plan's remaining phases (every
per-route layout match, icon/toast/responsive audits) were started. The plan document that
tracked this was removed in this documentation pass as fully superseded; its still-open,
non-UI carried-forward items are listed in `docs/PROJECT_HISTORY.md`.

### Redesign: bottom dock, 5 items → 3 (v3.1.4)

Direct, verbatim user feedback drove this: *"why keep tabs and dock button"*, *"main home page
view should be the view that clicking + icon should be showing"*. The center Create FAB and
Chat dock slot were removed; `QuickCreateSheet`'s tool grid became Home's own content instead
of a popup behind a button; Chat gained its own back arrow since it lost its dock slot.

### Redesign: retire the tabbed pager (v3.1.4)

Image/Video/Audio/Code used to share a `ScrollableTabRow` + `HorizontalPager` (`HomeScreen`'s
`HomeTab` enum). Since each modality only ever needs one model loaded, sharing a pager risked
more than one staying resident across tab switches. Each became a fully isolated route instead.

### Redesign: conversation-style studios (v3.1.6)

All four studios and Chat moved from "one result card, overwritten each generation" to a
scrolling prompt→result history, matching how a chat interface reads. Landed alongside removing
duplicate in-studio Advanced/Safety controls (Settings already had the same controls) and
consolidating three separate loading/status surfaces (spinner, status pill, live-log card) into
one docked log next to the composer.

### Component ports from GoogleLookBookUI (Era 6, PR #80)

A structured comparison against an earlier development snapshot of this same codebase found
genuinely additive UI it had gained independently; the additive pieces were ported (not the
whole screen) — richer chat bubbles/typing indicator/empty state, a quick-prompt carousel, a
model-picker readiness/offline-metadata upgrade, an `ImageCandidateGrid` for 1–4 generated
candidates sharing a batch, a `PromptDirectorSheet` for structured prompt building
(subject/setting/mood/lighting/composition/finish + style-modifier presets), a Gemini-style
pinch-zoom `FullScreenImageViewer`, and a full voice-editor UI (waveform/spectrum player,
consolidated permission checklist). One thing was deliberately **not** ported: a fake
per-provider "ping" status UI in the source repo's `ModelConfigScreen.kt` — this app's own
`ProviderConnectivityChecker` (a real HTTP call, shipped independently in 3.1.2) replaced the
need for it entirely.

## Design tokens quick reference

- **Color**: light canvas, near-black primary, electric-blue accent; per-modality accent colors
  propagate across every studio surface (composer, results, tab row, model picker, voice knobs).
- **Radius**: an extended scale (`RadiusTokens` — includes `xl2`/`xl3`/`xl4` up to 44dp) beyond
  Material3 defaults.
- **Motion**: a press-lift micro-interaction and a reduced-motion-gated 3D tilt on cards
  (`press-3d`/`lift-3d`/`float-slow`/`drift-slow` primitives).
- **Loading**: a shared shimmer/skeleton component, used wherever a real gap was found rather
  than applied speculatively everywhere.

## What's not built yet (UI side)

- Camera capture for the Image studio's reference picker — photo-picker only today.
- Text-file attachment in Chat.
- A Home entry point for Try-on (routes exist, nothing navigates to them).

See `docs/DRAWBACKS.md` for the full, continuously-updated list of verified gaps and
unverified-on-real-hardware items (several UI pieces here — the voice-editor's real device I/O,
ML Kit face-blur detection accuracy — have real unit-tested logic but no confirmed real-device
run; that file has the honest, current status for each).
