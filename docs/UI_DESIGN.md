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

## Redesign: one chatbox (current shell)

The shell was measured against the Gemini app, and lost on the thing that matters most: how many
controls stand between opening the app and sending a message.

**Before:** a top bar with a greeting block, a five-chip modality row, and a composer carrying an
"Attach Reference" chip row *and* a leading attach button *and* a model chip. In Image mode both
attach affordances rendered at once, with the chip overlapping the placeholder, and the model
chip read `FLUX.1 Schnell · Ready · verified 6m ago` in about 150dp. Above all of it, an empty
state stacking a hero card, a 2×2 capability grid, three starters and a history list.

**After:** one `+`, one field, one send button.

| Concern | Where it lives now |
|---|---|
| Attach a photo, camera shot or file | The `+` sheet's source row |
| Switch generator (Chat/Images/Videos/Canvas/Audio) | The `+` sheet's tool list |
| Which generator is active | A dismissible chip in the composer — absent for Chat |
| Which model runs it | The top bar's selector |
| Start over | The top bar's new-chat icon — hidden while the thread is empty |
| Why generation is gated | Its own hint row in the composer |

The rule that produced this: **a control the user changes rarely does not get permanent screen
space.** The chip row cost 40dp on every screen to expose a choice most messages never make; the
capability grid offered the same five generators the `+` sheet does, in different words, from a
second place. Both are gone. What is left is on screen because it is used, or because hiding it
would hide state (the active tool, an attachment, a blocked reason).

Every source in the sheet is wired to something real: Photos through the photo picker, Camera
through the existing `FileProvider` capture behind the same permission gate as garment capture,
Files with a persistable read grant so the URI still resolves when generation runs. Dictation
uses the system recogniser (`RecognizerIntent`), which needs no `RECORD_AUDIO` grant — and the
mic is hidden rather than disabled where no recogniser is installed.

**Replies changed shape too.** Assistant turns have no bubble: a reply is the longest content the
app renders and the bubble was spending 28dp of horizontal padding plus a 32dp avatar gutter on
it. What gives the turn its identity instead is the model name above and an action row below —
copy, regenerate, read aloud (platform TTS), share. User turns stay as short right-aligned pills.

**Markdown renders** (`ui/components/Markdown.kt`). Every reply in the app previously showed its
markers literally — the first assistant message of every conversation arrived as
`- **Fashion try-on** features and tips`. The parser covers headings, bullets, ordered items,
quotes, rules and inline emphasis; fenced code still splits out to `CodeBlock` first. Unmatched
markers degrade to literal text rather than swallowing the rest of the line, and the tests pin
that no visible text is ever lost — a wrong colour is cosmetic, a dropped clause is damage.

**Settings is a hub and only a hub.** The previous pass added four navigable rows and then left
appearance, storage, permissions, safety, four API-key fields, durable-storage status, about and
memory inline *around* them — the same six screen-heights, now with navigation stranded in the
middle. There are ten rows now, grouped GENERATION / APP / YOUR DATA, and every setting is on a
page: `ApiKeysScreen`, `SafetyScreen`, `AppearanceScreen`, `StoragePrivacyScreen`, `MemoryScreen`,
`AboutScreen`. Each page calls the same `LazyListScope` section function the hub used to call
inline, so the split moved no setting logic at all — which is what makes it reviewable against
"nothing was lost", the failure mode the previous restructure actually hit.

### Result cards, and the keyboard

Two defects the one-chatbox pass did not touch, both found on a real device rather than in a
render.

**A result is a picture.** The thread wrapped every generated image in a card: a "RESULT" label,
an "AI-generated" pill, an "In looks gallery" pill, and Save / Share / Privacy blur / Report as
four full-width buttons. That is ~300dp of chrome around 320dp of image, and it pushed the next
turn off screen. All four actions moved into `FullScreenImageViewer` — tap the image to get them.

The one thing that stayed is the AI-generated marker, redrawn as a small translucent badge on the
image itself. The distinction that decides it: **the buttons are controls, the marker is a
disclosure.** A control can live one tap away; a label on a synthetic image of a person cannot,
because the person who needs it is the one scrolling past, not the one who opened the viewer.

**The keyboard.** `enableEdgeToEdge()` was on and the activity declared no
`windowSoftInputMode`, so two layers each handled the IME: the window resized, and Compose's
`safeDrawingPadding()` (which includes `WindowInsets.ime`) applied the inset again on top. The
composer came to rest roughly one keyboard-height above the keyboard, and the thread was squeezed
hard enough that its content ran under the status bar.

`android:windowSoftInputMode="adjustNothing"` makes Compose the only layer that moves anything.
The cost, paid once: a `Dialog` or `ModalBottomSheet` is its own window and does not inherit the
host screen's padding, so any sheet with a text field needs an explicit `imePadding()` —
`ModelPickerSheet` and `PromptDirectorSheet` have one. Screens are already covered, because
`GlassScreen` and every hub page use `safeDrawingPadding()`.

## Redesign: glassmorphism (post-professional-UI pass)

The app already had `Glass*` components, but on a light-blue palette with ~95%-opaque fills over
a near-black ground. That is a dark theme wearing glass component names: a blur has nothing to
work with when everything behind it is one flat colour.

Three changes make it actually glassmorphic, in dependency order:

1. **Palette.** One violet/magenta/teal family across both themes, and glass fills dropped from
   `0xF2` to `0x8C`/`0xA6` alpha so surfaces are see-through.
2. **`SpatialBackground` → aurora mesh.** Five overlapping radial blobs drifting on independent
   periods, over a lifted indigo canvas, under a vertical scrim at the bar/composer edges. This
   is what the translucent fills reveal.
3. **`GlassUiKit.kt`** — one file holding every new surface (greeting header, hero card,
   capability tile, history row, chat status header, author chip, code block, badge pill, app
   mark, social-proof row). They share one treatment deliberately; the previous UI ended up with
   three different card styles precisely because that language lived in three places.

Screen-level: Home opens on a hero card and a capability grid rather than an empty canvas; Chat
renders fenced replies as **syntax-highlighted code blocks** (`CodeHighlighter` +
`MessageSegment.split`, both unit-tested on the invariant that they never alter the source);
Onboarding opens on badge → app mark → wordmark → trust line.

A verification gap this exposed: `RedesignScreenshotTest`'s `shoot()` painted a flat
`colorScheme.background`, so the first violet render showed the palette but *not* the mesh — a
card can look right there and flat in the app. `shoot()` now takes `spatial = true` to render
inside the real `SpatialBackground`, and every case judging glass uses it.

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
The engine-tier dropdown and the Prefer NNAPI toggle sit at the top of Models, above the
on-device catalog — they briefly had no UI at all when the old engine section was retired, which
an audit of every `AppSettings` setter for a call site caught. `Routes.USAGE` now resolves to the
API-monitor screen instead of a near-duplicate `UsageScreen`.

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
