# Changelog — The Lookbook

## Unreleased (post-3.1.8)

**A render pass over the whole app.** Eight defects, every one found by looking at a screenshot
rather than by reading code — including three on surfaces that had shipped without ever being
rendered at all.

- **Every switch in the app used Material's default colours.** Not one of the six call sites
  passed a palette, and M3's defaults derive from `surfaceVariant`/`outline`, which in this
  violet system land within a few percent of the white card a settings toggle sits on. In light
  mode the off state was a pale blob with no border, near-indistinguishable from on — a control
  whose only job is to show one bit of state was showing none. `VestraSwitch` makes on
  accent-filled and off a hollow track with a visible rim: fill versus no-fill survives a glance,
  a dim screen and colour-blindness, which hue alone does not.
- **Material's stock red had leaked into six places.** On a violet-and-teal screen it was the one
  colour from outside the system, so "Blocked" read as a rendering fault rather than as status.
  All six use `VestraColors.Danger` now.
- **The back arrow was centred on the eyebrow-plus-title block** in `GlassTopBar`, so on every
  sub-page in the app the eyebrow floated above it with empty space to its left. Both anchor to
  the title's baseline now.
- **"DataStore" hard-clipped to "DataStor"** — the badge had `maxLines = 1` with the default
  `Clip` overflow and lost the race for width against the card title. The title yields now.
- **Notifications offered two identical outlined buttons**, so nothing said which one grants the
  permission. The primary action is filled; the system-settings detour stays outlined.
- **The API monitor said "nothing recorded yet" twice**, in two cards, around a row of noughts.
  One empty state with one action.
- **The history drawer never displayed its own `preview`.** Rows read "title / 2h ago · 6
  messages", so a conversation was identified by the first ~28 characters of its opening
  question, and a message count is not a way to tell two threads apart. Title and last reply now,
  the way a mail app lists threads.
- **Five surfaces had no screenshot coverage at all** — the Settings hub (rewritten twice,
  rendered zero times), the conversation drawer, and switch states in both palettes. All five
  render now; the drawer's `preview` bug was found the moment it did.


**Conversations, streaming, and the thread affordances that were missing.**

*"New chat" was a delete button.* It shipped for two releases with no history behind it, so it
called `ChatRepository.clear()`, which did `settings.remove(KEY)` — an unconfirmed, permanent
delete of the only conversation the app could hold, one tap from the top bar. On top of that,
turn 81 silently evicted turn 1.

- **`ChatRepository` stores conversations.** Records with an id, a title derived from the first
  user turn, and their own message list. New chat *files* the current one; a `ChatHistoryDrawer`
  behind the top bar's menu button lists them, with search (above six conversations) and
  per-conversation delete. Migration wraps any pre-existing single-thread history as one
  conversation and **leaves the legacy key in place**, so a rollback costs the user nothing.
- **Two real bugs surfaced by the new tests before any of this shipped**: conversation ids were
  `"c${nowMs()}"` and collided for two conversations created in the same millisecond, so deleting
  one deleted the other; and the streaming client never sent a request at all, because
  `LlmClient` sets a `JsonObject` body that only serialises with `ContentNegotiation` installed.

**Cloud replies stream.** `LlmClient.chatStream` — one SSE reader for Groq, OpenRouter, Gemini and
the HF router, since all four speak the OpenAI chat-completions dialect. Streaming is a parameter
on `chatWithFallback`, not a second copy of its fallback chain. The app already streamed *local*
replies token by token and waited in silence for cloud ones, so a 70B cloud model felt slower than
a 0.6B on-device one.

**Thread affordances**, all of which the reference app has and this one did not:

- scroll-to-bottom, rendered only while the thread can actually scroll further
- follow-up chips under a settled reply — static per-mode copy, not a second model round trip
- edit a sent prompt and re-run, dropping the replies that answered the old wording
- long-press any turn for copy / share / delete
- share the whole conversation, from the drawer
- haptics on the thread, matching what the capture and generation screens already did


**Three defects found on device.**

- **A generated image is now just the image.** The thread wrapped every result in a card
  carrying a "RESULT" label, two provenance pills and four full-width buttons — roughly 300dp of
  chrome around 320dp of picture, on the one surface where the picture is the point. Save,
  Share, Privacy blur and Report moved into `FullScreenImageViewer`, which is where a user who
  wants to act on an image already is; tapping the image opens it. The AI-generated marker stays,
  redrawn as a small badge *on* the image: it is a disclosure rather than a control, and a
  synthetic image of a person has to be labelled where it is seen. The fixed 320dp `Fit` box also
  went — a portrait result rendered as a narrow strip between two grey bands.
- **The composer no longer floats above the keyboard.** `enableEdgeToEdge()` was on but the
  activity declared no `windowSoftInputMode`, so the window resized for the IME *and*
  `safeDrawingPadding()` applied the same inset again. The composer ended up roughly one
  keyboard-height too high, with the thread squeezed hard enough to run under the status bar.
  `android:windowSoftInputMode="adjustNothing"` makes Compose the single source of truth. A
  `Dialog`/`ModalBottomSheet` is its own window and does not inherit the host screen's padding,
  so `ModelPickerSheet` and `PromptDirectorSheet` take an explicit `imePadding()`.
- **The top-bar model name was still a compound.** `modelLabel()` returns
  "FLUX.1 Schnell · Ready · verified just now"; the previous fix only stripped a trailing
  `(Groq)`-style parenthetical, so the status tail still truncated the name. It now cuts at the
  first " · " too. Liveness is on the picker sheet the control opens, and the full label stays in
  `contentDescription`.


**One chatbox.** The shell had five always-visible modality chips, a composer that carried both
an "Attach Reference" row and a leading attach button at the same time (the chip sat on top of
the placeholder), a model chip that rendered "FLUX.1 Schnell · Ready · verified 6m ago" into
about 150dp, and an empty state stacking a hero card, a 2×2 capability grid, starters and history
above all of it. The reference this was measured against — the Gemini app — puts one `+`, one
field and one send button on screen and nothing else.

- **The composer is one control.** `PromptComposer` now renders a `+`, a field, an optional mic
  and a send button. The chip row is gone; the generator lives in the `+` sheet.
- **`ComposerToolsSheet`** — the `+` sheet: attachment sources (Photos / Camera / Files) across
  the top, generators below (Chat, Images, Videos, Canvas, Audio). One way to attach, one way to
  switch generator. The active generator shows as a small dismissible chip in the composer when
  it isn't plain Chat, and renders nothing at all when it is.
- **Attachment is real on all three sources.** Photos uses the photo picker, Camera captures
  through the existing `FileProvider` behind the same permission gate as garment capture, and
  Files takes a persistable read grant so the URI still resolves when generation runs.
- **Dictation** via the system recogniser (`RecognizerIntent`, so no `RECORD_AUDIO` grant). The
  mic is hidden — not disabled — on a device with no recogniser installed.
- **The model selector moved to the top bar**, where a name fits. **New chat** joined it, hidden
  while the thread is already empty.
- **Markdown renders.** `Markdown.kt` parses headings, bullets, ordered items, quotes, rules and
  inline `**bold**` / `*italic*` / `` `code` `` / `~~strike~~` / links. Every reply in the app was
  previously showing its markers literally — `- **Fashion try-on** features and tips` — on the
  first message of every conversation. Unmatched markers degrade to literal text rather than
  swallowing the rest of the line; the tests pin that no visible text is ever lost.
- **Assistant turns lost their bubble** and gained an action row: copy, regenerate (drops the
  stale reply so the model isn't fed its own answer), read aloud via platform TTS, and share.
  User turns stay as short right-aligned pills.
- **The empty state is a mark, a greeting and three starters.** The hero card and capability grid
  were removed — they offered the same five generators the `+` sheet now does, from a second
  place, in different words.
- **Settings is a hub and only a hub.** The previous pass added four hub rows and left appearance,
  storage, permissions, safety, four API-key fields, durable-storage status, about and memory
  inline around them. Those are now six more pages — API keys, Safety & content, Appearance,
  Storage & privacy, Memory, About & help — grouped under GENERATION / APP / YOUR DATA. Each page
  calls the same section function the hub used to call inline, so no setting logic changed.


**Glassmorphism redesign.** The app had a glass *component set* on a light-blue palette with
near-opaque cards over a near-black ground — which is a dark theme, not glassmorphism. Frosted
glass only reads as glass when something varied shows through it.

- **Palette → violet aurora.** Both themes move to one violet/magenta/teal family (dark canvas
  `#130C26`, accent `#A78BFA`; light `#F6F3FE`/`#7C3AED`). Glass fills become genuinely
  translucent (`0x8C`/`0xA6`, from `0xF2`) so the background comes through them.
- **Background → an aurora mesh.** `SpatialBackground`'s two accent orbs are replaced by five
  overlapping radial blobs on independent drift phases, with a vertical scrim keeping text
  legible at the bar and composer edges. Still fully gated on `rememberReduceMotion`.
- **New component kit** (`ui/components/GlassUiKit.kt`): badge pill, app mark, social-proof row,
  presence dot, glass icon button, greeting header, hero prompt card, section header with
  action, capability tile, history row, chat status header, author chip, and a code block.
- **Home** opens on a hero card ("What can I help you create?") with a primary CTA, then a
  2×2 capability grid, one-tap starters, and a HISTORY list derived from the existing thread
  (it hides itself when there is nothing to show rather than rendering an empty heading).
- **Chat** gains a status header with presence, author chips, and — the substantive one —
  **fenced code replies now render as syntax-highlighted code blocks** inside the bubble instead
  of wrapped prose. New `CodeHighlighter` (a small, presentation-only tokenizer) and
  `MessageSegment.split`, both covered by unit tests whose central assertion is that neither ever
  alters or drops the source.
- **Onboarding** opens on an eyebrow badge, a glass app mark, the wordmark and a trust line.
- The composer gains a leading attach affordance.

**CI no longer runs on every commit.** `pull_request` was using its default trigger set, which
includes `synchronize` — the event GitHub fires for each new commit on an open PR — so a
four-commit branch burned four full ~7-minute runs, three of them on work still in progress. The
workflow now triggers on `opened`, `reopened`, `ready_for_review` and `workflow_dispatch`, plus
the existing post-merge `push: [main]`. The intended loop is: push freely to a **draft** PR, run
the identical gate locally, then mark it ready — which runs CI once, against finished work.

**Appium.** New `appium/test_glass_ui.py` covers the redesigned structure, since the redesign
moved most of what the older files drove. It has **not been run**: the SDK is installed now, but
`/dev/kvm` is absent and the CPU reports no virtualisation flags, so no emulator can start and no
device is attached. `appium/README.md` records that precisely rather than the stale "no adb".

**Professional-UI pass.** A design review of the shipped build against real screenshots, plus
the first Compose renders this repo has had at 360dp and in light mode. Four visible defects,
all with concrete causes:

- **The API usage dashboard was the home screen's hero** — pinned above the thread *and*
  duplicated as the empty-state item, so a fresh install opened on a token counter above ~900px
  of nothing (and the top-bar analytics toggle was inert while the thread was empty, since the
  pinned copy was gated on it and the empty-state copy was not). Both call sites removed; the
  monitor moved to **Settings → API monitor**, where it also gained real success-rate and
  average-latency metrics derived from data `SessionUsageRecord` was already storing. Home's
  empty state is now a greeting plus three per-mode one-tap starters that fill and send.
- **Service tiles rendered text one character per line.** A `FlowRow` with
  `weight(1f, fill = false)` on all five children and no `maxItemsInEachRow` never wrapped, so
  each tile got ~52dp on a 360dp phone; the five unweighted `Text`s inside had no `maxLines`.
  Fixed with `maxItemsInEachRow = 2` and bounds on every `Text`.
- **The composer's model chip displayed an error sentence.** It received
  `GenerativeViewModel.preflightLabel()`, which returns the blocked *reason* when cloud is
  gated — 140 characters of consent copy ellipsized to `Pick a cloud model in the model pi…`.
  Split into `modelLabel()` and `blockedReason()`, with the reason given its own hint row. The
  composer also drops from three concentric borders to one, recesses its field by fill rather
  than outline, and hides the permanently inert `Layers 0` assist chip.
- **The top bar demanded ~423dp of a 324dp row** (three unweighted children under
  `SpaceBetween`, with the model chip capped at 130dp for a compound `service · model` string
  that therefore always truncated). The chip and its hand-rolled `DropdownMenu` are gone — the
  composer already owns model selection — leaving a `weight(1f)` brand block and two buttons.

Two **pre-existing palette bugs** surfaced by rendering in light mode and dark for the first
time: `LightPalette`'s glass border/fill were white-on-white against white cards (so
`GlassSecondaryButton` drew no button at all and unconfigured tiles had no rim), and
`GlassTopBar` never set a content color, so every screen title inherited black and was
invisible in dark mode. Both fixed at the token/component level, which repairs them app-wide.

**Settings is now a hub.** Navigable rows for Models, Default models, Notifications, API
monitor and Diagnostics; API keys, appearance, storage, permissions, safety and memory stay
inline. The single 443-line scroll that mixed engine tiers, pack downloads, four key fields and
five per-capability model dropdowns is gone.

Retiring that scroll initially orphaned three things, caught by auditing every `AppSettings`
setter for a call site: the **engine-tier dropdown** and the **Prefer NNAPI toggle** lost their
only UI while still driving real behaviour (which local try-on tier runs, and whether ONNX
attaches the NNAPI delegate), and **Cloud usage** became unreachable. Both engine controls now
live at the top of Settings → Models, above the on-device catalog rather than buried under it.
`Routes.USAGE` is kept — it is deep-linked and `scripts/visual-verify.sh` drives it — but
resolves to the one API-monitor screen, which absorbed the `UsageLedger` free-tier request
summary that was the only thing the retired `UsageScreen` showed and the monitor did not.
`SettingsEnginesSection.kt`, `UsageScreen.kt`, `settingsCloudCapabilitiesSection`,
`CloudCapabilityDropdown` and `PackDropdown` are deleted rather than left orphaned (~800 lines).

**Live per-provider model directories.** New `shared/cloud/ProviderModelDirectory.kt` fetches
Groq's, OpenRouter's, Gemini's and the HF router's own `/models` endpoints behind a 1h TTL
cache — two of which `ProviderConnectivityChecker` was already calling and discarding. Each
provider page shows a masked key field, a real connectivity test, and **every** model the key
returns with its context window, modalities, owner and pricing; rows the app has a payload
route for are marked Ready and are selectable, the rest are greyed with the reason rather than
hidden. Selecting writes the matching per-capability default.

**Generation notifications.** New `notify/GenerationNotifier.kt` and a `generation_results`
channel, hooked into `GenerativeViewModel`'s single terminal-state funnel and gated on three
things: the per-category preference (`AppSettings.notifyOnGeneration*`), the OS
`POST_NOTIFICATIONS` grant, and the app being backgrounded. Settings → Notifications controls
all of it; before this the only notification the app could post was pack-download progress.

**Typography roles that were silently missing.** `headlineSmall`, `titleSmall`, `bodySmall` and
`labelSmall` were used across the usage dashboard and model picker but never defined, so they
fell back to Material's Roboto at the wrong tracking — two typefaces on one screen. All four
are now defined in Outfit, and a `ControlTokens` scale fixes controls that shared a row at
three different heights.

**Verification.** New `RedesignScreenshotTest` renders 18 cases at 360dp and 411dp in both
palettes via the existing Robolectric harness, and `ProviderModelDirectoryTest` covers each
provider's response shape, `:free` filtering, Ready/Not-usable decoration, 401 handling and the
TTL cache against `MockEngine`.

Three large PRs merged since the 3.1.8 version bump; `versionName`/`versionCode` have not been
bumped again yet. Full detail in `docs/PROJECT_HISTORY.md` (Eras 6–7).

**CI fix: restored `composeApp` module after an external-tool module rename.** Three commits
pushed directly to `main` via Google AI Studio renamed the app module from `composeApp` to `app`
in `settings.gradle.kts`, which — combined with `gradlew` losing its executable bit — broke every
CI job (`Permission denied` on `./gradlew`, then `project 'composeApp' not found`). The rename
also silently dropped the entire previously-tracked `composeApp` module (133 files: all core
source, `AndroidManifest.xml`, `MainActivity.kt`, ~20 tests) from git, leaving only a 16-file
`app/` module carrying genuine new work — Gemini API model support and an API usage monitoring
dashboard — layered on an older copy of the same files. Fixed forward without touching the 3
existing commits: restored `composeApp` from its last-known-good state, re-pointed
`settings.gradle.kts` back to `:composeApp`, and reconciled the 16 `app/` files against the
restored originals, porting only the confirmed-safe/additive deltas (Gemini `BuildConfig` fields
and token wiring, the new `ApiKeyDataStore`/`ApiUsageDashboardCard`, cloud quick-switcher and
usage dashboard in `UnifiedMainScreen`, `PromptComposer` UX polish) into `composeApp`, then
deleted the now-redundant `app/` module. Deliberately not ported: `ResultPane.kt`'s swap to an
`AudioPlayerView` component that doesn't exist anywhere in this codebase (would break the build),
and `build.gradle.kts`'s removal of the `sideload`/`store` product flavors and `minSdk` 35→28
change — both unrelated to the Gemini work and would have broken the existing gate/CI task names
and device-support guarantees. `shared`'s own Gemini cloud-platform support (`CloudModelCatalog`,
`LlmClient`, `AppSettings.geminiApiKey`, etc.) was untouched by the deletion and required no
reconciliation.

**Gemini-style unified main screen.** Replaced the Home dashboard, the 3-item bottom dock, and
the four isolated per-modality screens (Image/Video/Audio/Code Studio, News & Chat) with a
single screen (`UnifiedMainScreen.kt`): one continuous, timestamp-merged conversation thread
mixing Chat replies with Image/Video/Code/Audio results, selected per-message via a composer
mode chip row. Library and Settings move to two icons at top right — there is no bottom dock any
more. `GenerativeViewModel.StudioTurn` now carries its own `capability` and is exposed as a
merged `allTurns` flow across every studio (not just the currently-bound one) so a generation
still running in the background keeps updating the thread live; retry/dismiss stay scoped to
each studio's own latest turn, not a single global latest. On low-RAM devices (<6 GB), switching
composer mode to a different local-model capability now proactively evicts the previous LiteRT-LM
engine before the next one cold-loads, trading a cold reload for not risking an OOM — devices
with headroom keep the existing "leave resident, evict only under real memory pressure" behavior.
Settings collapses from a hub-plus-three-sub-screens split into one flowing screen, and gains a
persisted "Prompt clarity assist" toggle (`AppSettings.bypassFilterEnabled`) for the existing
image/video prompt-reframing assist, which previously had no UI. Known trade-offs from this pass,
not yet re-ported: the in-composer per-generation Advanced tuning panel (steps/guidance/seed/
strength/candidate count, creative/pragmatic/detail-boost/fashion-context/quality-guard toggles),
on-device warm-up loading feedback, Audio Studio's recording/voice-change/transcribe/import UI
(Audio mode currently only does plain device TTS), and the News headlines bar in Chat — the
model picker itself (choosing which cloud or on-device model a generation uses) is restored.
The Appium suite's `bottom_bar_*`-tag tests are now stale and need a rewrite against the unified
screen's `modality_chip_*`/`unified_library_button`/`unified_settings_button` tags; see
`appium/README.md`.

**GoogleLookBookUI cross-repo port (PR #80).** A structured, phase-gated comparison against an
earlier development snapshot of this same codebase found and ported genuinely additive
features: a periodic model-prewarm worker, a real ZIP-bundle diagnostics export, searchable
Wardrobe with recipe reuse, Creative Studio V2 (1–4 image candidates per request, sharing a
batch id/lineage), a structured Prompt Director prompt-builder + Gemini-style pinch-zoom image
viewer, and a full voice-cloning/vocal-editor pipeline (capture, DSP effects, waveform/spectrum
player, permission checklist) — ~1,900+ new lines, the largest single addition in the port.
Deliberately not ported: a fake per-provider connectivity "ping" UI in the source repo, since a
genuine version (`ProviderConnectivityChecker`) already shipped independently in 3.1.2.

**Generation audit follow-through (PR #81).** Wired five previously-dead local
image-generation parameters (steps/guidance/seed/strength/batch) to a new Advanced UI section;
added an in-composer model quick switcher; added a narrow, opt-in "reduce fashion false
positives" safety assist (a broader "unlock explicit/uncensored content" toggle was requested
and explicitly declined — see `docs/PROJECT_HISTORY.md`'s Era 7); and removed the manual
cloud/on-device processing-mode setting entirely, replacing it with automatic, credential-based
model availability gated by an implicit consent flag granted only by genuine user action
(picking a cloud model, or saving an API key) — never by automatic token restoration at boot.
Two gaps a second code-review pass caught before merge: the model picker's readiness indicator
was still checking only credential presence (now checks real reachability via
`AppSettings.cloudUsable`), and an offline-fallback error message had a duplicated clause.

**Documentation pass.** Added `docs/PROJECT_HISTORY.md`, `docs/UI_DESIGN.md`, and
`docs/FUNCTIONALITY.md`; refreshed `README.md`, `docs/PROJECT_STATUS.md`, and
`docs/ARCHITECTURE.md` to describe the app's current architecture instead of the retired
Supabase/Replicate cloud backend; removed the stale duplicate `ml/MODEL_LICENSES.md` (folding
its still-relevant licensing caveat into the root `MODEL_LICENSES.md` first) and the
already-superseded `docs/plans/lookbookweb-exact-ui-parity/` plan.

## 3.1.8

Reduces the Settings surface and closes a leftover copy inconsistency, based on a direct
follow-up to the 3.1.7 LiteRT-LM audit.

- **GPU, NPU, and speculative decoding now default on, with their Settings toggles removed.**
  Both GPU and NPU already fall back automatically to a slower-but-working tier if the delegate
  fails to init (a real, previously-fixed failure mode), so trying the fastest available backend
  by default is safe — it either loads faster or transparently degrades to exactly the path this
  app always had. NNAPI is the one backend preference left as an opt-in Settings toggle: its
  documented failure mode (a process-killing SIGSEGV/OOM observed on Pixel 9) bypasses that
  fallback pattern entirely, so it stays off by default and user-controlled.
- **Fixed:** `preferSpeculativeDecoding`'s stored default was still `false` immediately after
  this change (caught by self code-review before commit) — a fresh install would have silently
  never enabled it despite the doc comments and this changelog entry saying otherwise.
- **Removed leftover "modest fashion" branding from the Home screen's Image Studio card** — the
  generator screens themselves were already clean of this copy from a prior pass; the Home
  tool-picker description was the one place it still showed up first.
- **Removed the top-of-screen EXAMPLES prompt row from Image, Video, Audio, and Code** — one
  less thing competing for space above the conversation timeline; the composer is where prompts
  actually get typed.

## 3.1.7

Audited LiteRT-LM Android usage against Google's own integration guide and closed the gaps
found — all opt-in and off by default, since none of this is verified on real hardware here.

- **NPU backend support, opt-in.** `LiteRtLmEngine` can now try the NPU backend before GPU
  (falling back to GPU, then CPU, on failure — the same defensive pattern already used for the
  GPU→CPU fallback). New "LiteRT-LM NPU" toggle in Settings, off by default and disabled unless
  GPU preference is also on — no device in this codebase's dev/test loop has confirmed an NPU
  delegate actually engages for the installed packs.
- **Speculative decoding, opt-in.** Sets the SDK's `ExperimentalFlags.enableSpeculativeDecoding`
  before a GPU/NPU engine initializes, for faster decode. New "Speculative decoding" toggle in
  Settings, off by default — this is an `@RequiresOptIn`-marked SDK flag Google's own docs call
  "experimental and temporary... may change or be removed without notice."
- Fixed a data race in the speculative-decoding wiring during implementation: the flag is a
  global SDK static, but `LiteRtLmEngineCache` deliberately lets cold-loads for different models
  run concurrently — two simultaneous cold-loads could interleave and silently apply the wrong
  flag value to each other's engine. Now scoped behind a dedicated lock around just the
  set-flag-then-initialize window.

## 3.1.6

Fixes four real crashes/issues from a device log bundle a user sent directly from
Settings → Diagnostics — a Compose animation crash, a native SIGSEGV, a silent
low-memory process kill, and a recurring voice-changer failure.

- **Native SIGSEGV during concurrent local generations, fixed.** The crash traced to
  `LiteRtLmEngineCache` having no lock spanning different model specs — only a per-spec init
  lock, never held during the actual native generate call. Two different `LiteRtLmEngine`
  instances (e.g. Code's model and Chat's model, both intentionally left resident so a
  background generation keeps running when you switch tabs) could issue concurrent native
  calls into `liblitertlm_jni.so` with nothing preventing it — the vendored SDK only
  synchronizes native lifecycle calls per-instance. A mutex now serializes just the native
  call moment (never engine construction/warm-up, so cold-loads still run in parallel),
  bounded by the same 90s timeout the native call itself uses — a second studio's request
  now fails with a clear "busy, try again" message instead of ever racing or hanging.
- **Silent low-memory kill during local generation, fixed.** `onLowMemory`/severe
  `onTrimMemory` used to clear only the ONNX Runtime session cache (Bonsai/SD-Turbo) — the
  LiteRT-LM engine cache (Code/Chat/vision) was never touched by any memory-pressure
  callback, so a heavy session (several models loaded across studios, none ever evicted on
  navigation) could still get silently killed by the OS. Both caches are now cleared
  together under memory pressure; neither can cut off a generation that's actively running.
- **Compose `"current must not be NaN"` crash on the Audio screen, hardened against.** The
  exact trigger couldn't be pinned from an R8-stripped stack, but the underlying gap is real:
  `Float.coerceIn` doesn't filter `NaN`/`Infinite` (both `x < min` and `x > max` are false for
  NaN), so `VoiceKnobs.sanitized()` wasn't actually NaN-safe despite looking like it. Added a
  NaN/Infinite-aware clamp used there and in the voice-knob sliders themselves, plus in the
  image generator's guidance-scale setter.
- **Voice changer's "needs mono 16-bit WAV" failure, fixed.** Cloud TTS commonly saves
  stereo or 24/32-bit WAV, which the local voice changer used to reject outright with
  "re-generate or convert." It now auto-converts (downmix + bit-depth normalize) before
  falling back to an error, so this only surfaces for a genuinely unreadable file.
- **Misleading top status box removed from every studio.** Image, Video, and Code showed a
  status line naming a fixed default model (e.g. "FLUX.1 Schnell · Hugging Face Space") even
  when a different model — like an offline local one — was actually running; Audio's
  equivalent header had the same problem. Removed across all four studios; the composer's own
  model chip, which already reflects the real active model, is unaffected and stays as the
  single source of truth.
- **In-studio Advanced/Safety settings removed.** The Editorial/Modest fashion/Detail
  enhance/Quality check pills and the in-studio Safety row (Image/Video) are gone — Safety
  already had an identical control in Settings, so this removes a duplicate rather than the
  only entry point. The one setting with no Settings-level equivalent, offline vision-assist
  ("Analyze reference"), moved to Settings next to the Safety preset picker instead of being
  dropped.
- **Default in-app audio playback.** Generated audio now plays inline (play/pause, progress,
  live spectrum) instead of only offering to open an external player.
- **All four studios now read like a conversation.** Image, Video, Audio, and Code each keep a
  scrolling history of prompt → result turns instead of overwriting a single result card, with
  a typing indicator while a turn is still generating, smooth auto-scroll to the newest turn,
  and per-item entry animation as turns append.

## 3.1.5

Closes the one item 3.1.4 left explicitly unresolved — the vision-encoder error — plus a
general pass on model-crash diagnostics, driven by the two research findings from this
session: what caused the vision-encoder failure, and why the app's own diagnostics export
couldn't have caught it.

- **Vision-encoder root cause found and mitigated.** The native
  `"Vision Encoder model must have exactly one signature but got …"` error traces to
  `scripts/assemble-local-gemma4-pack.py` hardcoding `"vision": true` in the published
  pack's `config.json` without ever loading the file through a real `Engine` to confirm it
  validates — nothing in the assemble → publish → on-device pipeline ever did. The app now
  detects this deterministic failure client-side: `LiteRtLmEngineCache` gained a durable,
  restart-surviving failure cache (`LiteRtLmFailureStore`, self-invalidating on pack file
  size change) on top of its existing in-memory one, and the "Analyze reference (offline
  vision)" toggle in Create Advanced now shows *why* it's disabled instead of silently
  skipping the assist on every attempt. The publish script itself now defaults `vision`/
  `audio` to `false` and requires an explicit `--assume-vision`/`--assume-audio` flag (with
  a `manifest_provenance.json` sidecar) — asserted only after a real on-device check, not on
  faith. The already-published pack on HF is unchanged; a real republish is out of scope
  for this pass, but the app no longer trusts its broken capability claim.
- **Model-crash diagnostics: engine-layer detail no longer gets discarded.** Every native
  init/generation catch block across `LiteRtLmEngineCache`, `BonsaiImageEngine`, and
  `LiteRtLmInference` previously kept only the exception's truncated `.message` — the full
  stack trace and exception type were discarded at that boundary, so a *caught* native
  failure had strictly less diagnostic detail persisted than an uncaught one. A new
  `EngineLogHook` bridge (mirroring the existing `DiagnosticsHook` pattern, since `shared`
  can't see `composeApp`'s `CrashReporter` directly) now routes these through
  `CrashReporter.recordNonFatal()` with the real `Throwable`, and existing engine-layer
  `Log.w`/`Log.i` calls (GPU-fallback reasons, cold-load timings) now also land in the
  exported `app_trace.log` instead of Logcat-only.
- **One correlation id instead of two.** `RunDiagnostics` and `LocalJobStore` previously
  minted independent ids for the same generation attempt, so an interrupted job, its
  diagnostics record, and any crash-log entry for the same incident could only be
  correlated by eyeballing timestamps. `GenerativeViewModel` now mints one id and threads
  it into both stores plus `EngineLogHook`'s current-run tracking.
- **A swallowed sub-step failure no longer hides inside a "success" record.** This is
  exactly why two rounds of real device logs sent this session never surfaced the
  vision-encoder error: it's reached through an opt-in toggle, caught non-fatally, and the
  overall generation still succeeds — so the failure was one ordinary-looking stage line in
  an otherwise-`success = true` diagnostics record. `RunStage`/`GenerativeState.Running`
  gained an `isWarning` flag; the Diagnostics screen now auto-expands and highlights such
  records instead of treating them identically to a clean run. `RunRecord` also gained an
  optional `stackTraceRef` pointer (`"ref=<id>"`) for failed or warning-flagged runs,
  greppable against the exported `crash_log.txt`/`app_trace.log`.

## 3.1.4

Real-device-driven UI/UX pass: isolated modality screens, a leaner generation surface, and two
diagnosed engine bugs — see-what-broke items came from actual screenshots and crash logs, not
speculation.

- **Retired the tabbed Image/Video/Audio/Code pager.** Each modality only ever loads one model
  at a time, so sharing a `ScrollableTabRow` + `HorizontalPager` across all four risked more than
  one staying resident. `HomeScreen.kt`'s pager and `HomeTab` enum are gone; Image/Video/Audio/
  Code are now fully isolated routes (`Routes.IMAGE/VIDEO/AUDIO/CODE`), each wrapped in the new
  `IsolatedStudioScreen` (back arrow + title, no shared chrome with the others).
- **Bottom dock cut from five items to three.** `LookbookBottomBar` is now Home / Library /
  Settings only — the center Create FAB and the Chat slot are gone. `QuickCreateSheet` (the
  "+" tool-picker dialog) is deleted outright: its card grid is now Home's own content, not a
  popup behind a button. Tapping a card in the grid navigates straight to that isolated screen;
  News & Chat's card opens Chat, which gained its own back arrow (`NewsChatScreen`'s new
  `onBack` param) since it no longer has a dock slot either.
- **Fixed model-selection staleness.** Selecting the local Bonsai image model previously still
  showed a generic "Local SD-Turbo"/try-on label in both the composer's model chip
  (`UnifiedStudioPane`) and the preflight banner (`GenerativeViewModel.preflightLabel`) — both
  now consult the real selection (`appSettings.selectionId`) the same way the Code studio already
  did, instead of a hardcoded default.
- **One loading/generating surface instead of four.** The model-loading spinner now lives on the
  composer's own send button (`PromptComposer`'s new `loading` param) rather than a separate
  status card, a duplicate pill, and — for Code — an animated `LiteRtGemmaStatusIndicator` on top
  of that; only a genuine failure still gets its own banner. Audio never had model warm-up wired
  at all — it now warms on model selection exactly like Image/Video/Code (correctly keyed on the
  audio provider id, not the unrelated voice-persona pick), and `GenerativeCloudService.
  warmUpLocal(AUDIO)` now actually consults the local TTS engine's readiness instead of silently
  no-opping.
- **Live log is docked, not a wall of text.** `ResultPane`'s `Preparing`/`Running` states no
  longer render their own `Cancel` button (redundant with the composer's send-button-as-stop) or
  a persistent scrollable `LiveGenConsole` card. A new `DockedLiveLog` composable sits right next
  to the composer instead — a single collapsed line, tap to expand the same scrollback — with a
  real per-second ticking clock so its elapsed-time header doesn't freeze between log lines.
- **Example prompts show once, then get out of the way.** Every studio's example-prompt row
  (`GenerativeViewModel.examplesDismissed`) hides itself once the model finishes loading or the
  first generation starts for that session, and the chips themselves are smaller
  (`ExamplePromptRow` down to `labelSmall`, single-line ellipsis).
- **"Interrupted: …" banner restyled to match the toast system.** `InterruptedJobsBanner` now
  uses the same warning-amber accent and icon language as `GlassSnackbarCard` (previously a
  generic `GlassCard` that blended into the background) plus a proper close (X) icon button
  instead of a text "Dismiss".
- **Removed the "Atelier" subtitle.** `LookbookCopy.STUDIO_HOME` ("Atelier") is no longer
  rendered under the product name on Home — it didn't communicate anything to users.
- **Onboarding copy rewritten** to describe the actual new IA (isolated studios, what the
  send-button spinner means, local-vs-cloud model choice, optional cloud keys) instead of
  try-on-centric copy for a flow that's currently unreachable.
- **Two real engine bugs, found from an actual device crash-log bundle:**
  - `LiteRtLmEngineCache` previously retried a failing LiteRT-LM engine init from scratch on
    every single call. A deterministic failure (e.g. a vision-encoder/backend mismatch) is now
    cached and fails fast after the first attempt — but an `OutOfMemoryError` is deliberately
    **not** cached, since that class of failure can be transient (another studio's model still
    resident) and deserves a real retry once memory frees up.
  - `BonsaiImageEngine`'s per-step DiT loop and `Graph.run()`'s output buffer were both
    allocating a fresh `ByteBuffer.allocateDirect(...)` every single iteration instead of reusing
    one — direct buffers are only reclaimed via GC-driven `Cleaner` references, which can lag
    badly with a ~2.9 GiB model already resident. Real crash logs showed per-step time degrading
    from 46s to 8m44s before the native process was killed; both loops now reuse pre-allocated
    buffers.
  - Still **unresolved**: the vision-encoder "must have exactly…" error's true root cause (a
    stale `config.json` flag vs. an incompatible bundled SDK version) — the real device logs
    gathered this pass captured the Bonsai crash above instead, not this specific failure.

## 3.1.3
Phase 1 (A0/A1/A2) of `docs/plans/lookbookweb-exact-ui-parity/PLAN.md` — the design-system
foundation for matching lookbookweb.lovable.app exactly, verified against real screenshots at
every step (not claimed from reading the code).

- **A0 — full color/radius token replacement.** `Theme.kt`'s "Loom Ink" (brass-on-deep-ink)
  palette is replaced with an exact port of lookbookweb's shipped design system: every color is
  a real sRGB conversion of that app's OKLCH tokens (CSS Color 4 OKLab→linear-sRGB matrices,
  computed directly, not eyeballed) — light canvas `#F2F8FC`, near-black primary `#111419`,
  electric-blue accent `#1F7DCF`, per-modality brand colors (image `#1F7DCF`, video `#DD503F`,
  audio `#E8179B`, code/chat `#009C7B`), full dark-theme equivalents. `RadiusTokens` extended
  with `xl2`/`xl3`/`xl4` (30/36/44dp) matching lookbookweb's `calc(var(--radius) ± N)` scale, and
  `lg`/`xl` corrected to exactly match (20dp/24dp, were 24dp/32dp). Material3 `ColorScheme`
  container colors (`primaryContainer`, `secondaryContainer`, etc.) recomputed to match the new
  blue/teal accent family instead of the old brass tints.
- **A1 — the missing motion primitives.** lookbookweb's `press-3d`/`lift-3d`/`float-slow`/
  `drift-slow`/`gradient-text` utilities didn't have Compose equivalents yet (only `tilt-3d` and
  `shimmer` did). Added `Modifier.press3d()`/`Modifier.lift3d()` (`PressModifier.kt`, new) and
  `Modifier.floatSlow()`/`Modifier.driftSlow()`/`gradientTextStyle()` (`AmbientMotion.kt`, new),
  following `TiltModifier.kt`'s exact pattern — reduced-motion identity fallback, real gesture/
  animation wiring otherwise. `tilt3d()` itself gained the `translateY(-6px)` lift component its
  CSS source has that the Compose port was missing (rotation-only before this). Since Android has
  no `:hover` state, "hover" is reinterpreted as "while pressed" throughout — documented in each
  modifier's own doc comment.
- **A2 — bottom dock exact match.** The active dock item now fills with the accent gradient-pill
  background exactly like lookbookweb's `DockLink` (`!text-accent-foreground gradient-pill`),
  replacing the old icon-color-change-plus-dot indicator. Dock container corner radius/shadow
  moved to the new `xl4` (44dp) token + a heavier "dock-shadow" elevation. Center Create FAB
  resized to the exact 56dp (`h-14 w-14`) and switched from a 3-stop radial gradient to the
  correct 2-stop 135° linear gradient. `QuickCreateSheet` (the Create tool picker) switched from
  a `ModalBottomSheet` to a centered `Dialog` — lookbookweb opens a centered dialog here, not a
  bottom sheet — title/description copy updated to match ("Pick a tool to start something new.").
- 8 new tests (`PressModifierTest`, `AmbientMotionTest`) covering the reduced-motion identity
  contract for all 4 new modifiers, matching `TiltModifierTest`'s existing pattern. All 20
  existing `ScreenshotTest` renders re-verified against the new palette by direct visual
  inspection (2 screenshots checked pixel-by-pixel: the dock's active-item fill and the Create
  FAB gradient both render correctly, not just compile).
- **A3 — `SolidCard`.** Added the exact-match opaque card variant (`solid-card` in
  lookbookweb: same border/shadow as `GlassCard` but no translucency) for dense reading
  surfaces — chat bubbles, transcript boxes. New screenshot confirms it renders correctly.
- **Fixed during review, before landing:** a real dark-mode bug where `AtelierCanvas` (the fixed
  dark scrim behind generation previews) and `Ivory` (the text drawn on top of it) collided to
  the same color in the dark palette, making that text invisible — both are theme-independent by
  original design and are now fixed correctly in both palettes. Also: `floatSlow()`/
  `driftSlow()` now use a real CSS-equivalent ease-in-out curve instead of linear (their doc
  comments already claimed ease-in-out; the implementation didn't match), and the
  press-gesture-tracking code duplicated across `tilt3d`/`press3d`/`lift3d` is now shared via
  `rememberPressedState()` for the latter two.
- **A4.2 — Chat bubble exact match.** The user chat bubble now uses a solid accent fill with
  white text (`ChatMessageBubble` in `ChatComponents.kt`) — exact match of lookbookweb's
  `rounded-br-lg bg-primary text-primary-foreground` user bubble, replacing the previous
  translucent-glass treatment. The assistant bubble deliberately keeps its richer glass-card +
  model-badge + timestamp header (lookbookweb's assistant side is plain text with no bubble at
  all) — a reasoned deviation, not a compromise: same accent color, tail shape, and radius
  tokens either way, just more information density on the side that benefits from it. Verified
  against the re-rendered `12-chat-bubbles` screenshot.
- **Found during the same audit, deliberately not built yet:** the Home screen (A4.1) surfaced a
  real architecture gap — see `docs/plans/lookbookweb-exact-ui-parity/PLAN.md`'s A4.1a — that's
  being scoped as its own dedicated phase rather than rushed.
- **Part B.3 — prompt-level safety presets.** Exact port of lookbookweb's `src/lib/safety.ts`
  guard presets (`SafetyPresets` in `shared/commonMain`): Off, Standard (family-friendly guard
  clause, the default), Blur identities, Redact details. The active preset's guard clause is
  appended to the actual prompt sent to `generative.generateImage(...)` in
  `GenerativeViewModel` — a real behavior change to what the model receives, not a cosmetic
  setting — while leaving the visible/editable composer prompt untouched, so the guard text never
  leaks into what the user sees or can accidentally re-edit. Persisted via a new
  `AppSettings.safetyPresetId` StateFlow. New Settings → "Image generation safety" section
  (`SettingsSafetySection.kt`) mirrors the existing Processing Mode card's visual pattern. 9 new
  unit tests (`SafetyPresetsTest`) plus a new `22-safety-presets` screenshot, both confirming the
  real behavior — not just that it compiles.
- **Part B.2 — tokenizer-aware context budgeting.** Exact-port of lookbookweb's
  `src/lib/tokens.ts`: a per-model context-window table (`ContextBudget` in
  `shared/commonMain`, real published native-context values for every chat-capable local
  and cloud model — Qwen3 0.6B/Gemma 4 E2B/legacy Gemma 3 1B at 32,768, Llama 3.3 70B Groq at
  128,000; a documented, honest 8,192-token fallback for OpenRouter's free router, whose
  underlying model rotates and isn't individually published) and a calibrated
  chars-per-token heuristic when no real tokenizer is wired in. A live `ContextBudgetBar`
  now sits above the News/Chat composer, showing a running "used / window" count that updates
  on every keystroke and switches to a hard, red "won't fit and will be truncated" warning
  before the user can send something the model will actually cut off — not a cosmetic
  counter, a real pre-send check against exactly what `ChatViewModel.send()` would compose
  (system prompt + last 10 turns + the live draft). 12 new unit tests (`ContextBudgetTest`)
  plus two new screenshots (`23-context-budget-under`, `24-context-budget-truncate`)
  confirming both visual states render correctly.
- **Fixed during review, before landing:** a code-review pass on Part B.2/B.3 found two real
  gaps. First, `SafetyPreset.confirm` (true for Blur identities/Redact details) was declared
  and unit-tested but never actually checked — generation ran immediately regardless of the
  active preset. Fixed with a real `SafetyConfirmDialog` (new, `ui/components`) that
  `UnifiedStudioPane` now shows before dispatching an image generation whenever the active
  preset requires confirmation, verified by 3 new interaction tests
  (`SafetyConfirmDialogTest`) since Robolectric can't rasterize `AlertDialog`'s own platform
  window for a screenshot — the same class of limitation `PrivacyBlurFlowTest` already
  documents for `ModalBottomSheet`. Second, the new Settings safety section was gated behind
  the cloud-only section filter even though the guard applies to local generation too; it's
  now visible from both the Cloud and Engines section entry points.
- **Part B.4/B.5 — audited, partially closed.** B.5 (resumable pack downloads): audited
  `PackDownloadWorker`/`ModelPackManager` and found real HTTP `Range` resume + on-disk staging
  that survives app restart already in place — closed as a no-op, nothing to port. B.4
  (creeping progress / retry-exhaustion fallback): the cloud video/audio poll-progress formula
  already existed but was duplicated inline at two call sites — extracted into a single tested
  `CreepingProgress.forPoll()` primitive (`shared/commonMain/cloud`, 7 new unit tests,
  exact-regression-checked so the emitted progress fractions are unchanged). The
  retry-exhaustion "gentler path" fallback and progress-ticking the three sub-2.5s local
  blocking calls (video encode, system TTS, voice-changer DSP) are deliberately **not**
  built — reasoned and documented in `docs/DRAWBACKS.md` rather than silently dropped.
- **Part B.1 — persistent local chat memory, "what the assistant remembers."** After a
  News/Chat reply, an extraction prompt runs through the *local* chat model only — never
  cloud, regardless of which model answered the visible reply — asking for up to 5 durable
  facts (stated preferences, projects, tools, constraints, names, recurring goals) as a JSON
  array; `MemoryExtraction.parseFacts()` parses it tolerantly but never fabricates a fact when
  the output doesn't parse. `MemoryRepository` (new, `shared/commonMain/chat`) stores them
  locally (same `Settings` backing every other repository — nothing leaves the device),
  deduped case-insensitively, capped at 50. Facts are re-injected into future system prompts
  and shown in a new Settings → "What the assistant remembers" panel
  (`SettingsMemorySection.kt`) with per-fact delete, a "Clear all," and an on/off switch
  (`AppSettings.memoryEnabled`, default on) that gates both new extraction and re-injection of
  what's already stored. Extraction runs strictly after the primary reply completes, awaited
  inline in the same coroutine rather than fire-and-forget — the local LiteRT-LM engine isn't
  safe for concurrent generate calls, and a background task racing a fast second `send()` would
  either corrupt that call or silently drop the extraction. 22 new unit tests
  (`MemoryRepositoryTest`, `MemoryExtractionTest`) plus 2 new screenshots
  (`26-memory-empty`, `27-memory-with-facts`).
- **A4.3 — Studio pager audit: surfaced Part B.2/B.3 inline in Create, closed a real safety
  gap.** lookbookweb's `studio.tsx` shows its token-budget line and safety-preset row directly
  in the create surface, not only in a separate settings screen — `UnifiedStudioPane` (the
  Image/Video/Audio/Code tabbed pager, the actual architectural analog of `studio.tsx`) now
  does too. A compact safety-preset pill row (`GlassOptionToggle` chips, mirroring the existing
  Pragmatic/Creative pattern) sits in the Image/Video Advanced section, wired to the same
  `AppSettings.safetyPresetId` Settings already reads. A live token-budget bar sits above the
  Code tab's composer — the one Studio capability that's actually LLM-context-window-shaped
  (Image/Video/Audio are diffusion/TTS, not chat-context-bounded); its effective-model-id
  resolution is hoisted to `Dispatchers.IO` and keyed on pack/readiness state rather than the
  live prompt, so typing never re-triggers the disk stats `RoutingLocalCodeGenerator` needs to
  resolve which local pack is actually active. **Closed a real gap found during the audit**:
  `generateVideo()` never applied the safety-preset guard clause at all — video's local path is
  a still-clip from the same tiny-SD keyframe pipeline `generateImage()` already guards, so the
  same visual-content concern applies; fixed to match, and the confirm-before-run dialog now
  gates Video generation the same way it already gated Image.
- **Fixed during review, before landing:** a code-review pass on the above found two real bugs.
  First, the initial Code-tab token-budget wiring re-derived the effective model id from
  `viewModel.currentCodeModelId()` inside a `remember(prompt, ...)` block — since resolving
  that id (when no explicit local pick exists) walks `RoutingLocalCodeGenerator`'s delegate
  chain and stats each pack's files on disk, this reintroduced exactly the main-thread
  file-system-probe problem `produceLocalReadiness` already exists in this same file to avoid,
  now running on every keystroke. Moved the id resolution into its own `produceState` hoisted
  to `Dispatchers.IO`, keyed on pack/readiness signals instead of the prompt. Second,
  `currentCodeModelId()`'s local-vs-cloud decision only checked "local ready and explicitly
  selected," missing the "offline with a ready local pack" branch `generateCode()` itself
  actually routes through — fixed to mirror `generateCode()`'s exact `bypassPreflight`
  condition so the budget bar always evaluates against the model that will actually run.
- **A4.4 — Library: added a real media-type filter to Wardrobe, alongside (not replacing) the
  existing Favorites filter.** lookbookweb's `library.tsx` filters by type (All/Images/Videos);
  `WardrobeScreen` gets the same as an independent second filter row — "All types (n)/Images
  (n)/Videos (n)" — that composes with the existing Favorites toggle (e.g. Favorites + Videos
  shows only favorited video looks). The filtering itself (`filterWardrobeEntries`) and the
  empty-state message logic (`wardrobeEmptyMessage`) are extracted as pure functions and
  directly unit-tested (15 tests) rather than only reachable through a full-screen Compose
  harness this repo doesn't have for `WardrobeScreen` (it needs a real `WardrobeRepository`
  backed by device file storage). The remaining A4.4 items — an upload-to-library flow and a
  demo-data/sample-content banner — are lookbookweb artifacts of its account-based cloud
  storage model with no honest local-first equivalent (a demo banner would mean either
  fabricating sample generations, which `docs/DRAWBACKS.md`'s own discipline rules out, or
  showing nothing meaningful); left unbuilt as a reasoned scope decision, not an oversight.
- **Fixed during review, before landing:** the empty-state message for a combined
  Favorites+type filter (e.g. "Favorites" + "Videos" with no matches) fell back to a plain "No
  favorites yet" even when favorites did exist — just none of the selected type — misreporting
  why the list was empty. Fixed with two more precise messages
  (`EMPTY_FAVORITE_IMAGES`/`EMPTY_FAVORITE_VIDEOS`) and a regression test asserting the combined
  case never falls back to the plain favorites message.
- **A4.9 — Settings section order.** Audited `SettingsScreen`'s section order against
  lookbookweb's `settings.tsx` and found it had drifted — General/diagnostics and "what the
  assistant remembers" sat first, Appearance was split across two non-adjacent positions, and
  Cloud/Engines sections were interleaved rather than grouped. Reordered to match: Appearance &
  accessibility → device/engine lab → provider/cloud settings → diagnostics → "what the
  assistant remembers." Every section's own visibility gate (which `SettingsSection` route shows
  it) is untouched — only the relative order within the combined `ALL` page changed. Account and
  a sample-data toggle stay omitted, matching A4.11's "no accounts exist" and A4.4's "no
  demo-content banner to gate" decisions.
- **A4.10 — Changelog screen.** lookbookweb's `changelog.tsx` shows release history; added the
  equivalent read directly from this app's real `CHANGELOG.md`, not a hand-maintained parallel
  list — `copyChangelogAsset` (new Gradle task in `composeApp/build.gradle.kts`) bundles the
  actual root `CHANGELOG.md` into the APK as an asset at build time, so the in-app list can never
  drift out of sync with what actually shipped. `ChangelogParser` (`shared/commonMain`) splits it
  into per-version sections; `ChangelogScreen` renders them and links from Settings → About
  ("Changelog" button, alongside Help/Privacy) with adapted "install the latest release manually"
  copy in place of lookbookweb's git-pull instructions, since this app ships as an APK.
  Fixed during review, before landing: (1) the parser treated every `## ` heading as a release,
  including this file's own non-version `## CI / releases` note — it would have rendered as a
  fabricated release card between two real versions; now only headings starting with a digit are
  treated as releases, and non-version headings are dropped rather than folded into whichever
  version happens to precede them. (2) the screen read and parsed the asset file synchronously
  inside `remember { }`, blocking the composition/UI thread; moved to `produceState` on
  `Dispatchers.IO`, matching this codebase's established IO-hoisting pattern.
- **A4.2/A5 — Chat header "Remembering N things" pill.** lookbookweb's chat header shows a
  memory-count pill with a `Brain` icon; Part B.1 (this app's on-device chat memory) shipped
  earlier without the header affordance surfacing it. Added `MemoryPill` to `ChatComponents.kt`,
  wired into `NewsChatScreen` from the real `MemoryRepository.facts` count — hidden entirely at
  zero facts rather than showing a "Remembering 0 things" pill with no informational value, never
  a fabricated nonzero count. Icon is Material Symbols `Psychology`, the closest available intent
  match for lookbookweb's `Brain` (A5's icon audit: exact `lucide-react` glyphs aren't portable,
  intent-matching is). Code-review flagged the first pass for hand-duplicating `GlassPill`'s
  container styling a third time (`GlassPill`, `ProTierPill`, now this) — fixed by adding an
  optional `leadingIcon` slot to `GlassPill` itself and having `MemoryPill` reuse it, so the
  pill's shape/fill/border styling has one source of truth again.
- **A7 — Toast repositioning.** lookbookweb uses `sonner`, top-center, for every success/error/
  warning/info notice; Android's native `Toast` is bottom-anchored and can't be repositioned or
  restyled to match. Replaced every `Toast.makeText(...).show()` call site in the app — 13 files,
  ~40 sites, including `MediaExport` (a plain Kotlin object with no Composable scope) — with
  `GlassSnackbar` (`ui/components/GlassSnackbar.kt`): a global `MutableSharedFlow` message bus
  any code can post to, plus one `GlassSnackbarHost` mounted at the app root (`VestraNavHost`),
  top-center, styled with distinct icon+accent per level (success/error/warning/info; warning's
  amber is a conventional choice, not sourced from lookbookweb's exact unseen `sonner` hex).
  Fixed during review, before landing: (1) the exit animation cleared the displayed message the
  same frame it started, so the card blinked off instead of animating away — fixed by keeping the
  last-shown request in a `displayed` state that outlives `visible` turning false. (2) `collect`
  processed messages strictly one at a time, so a newer message queued behind whatever was
  already showing for up to 3.2s instead of pre-empting it — switched to `collectLatest`. (3) the
  flow's default `BufferOverflow.SUSPEND` meant a burst of 9+ calls would silently drop the 9th+
  — fixed with `BufferOverflow.DROP_OLDEST`, matching the "latest wins" semantics `collectLatest`
  already implements.
- See `docs/plans/lookbookweb-exact-ui-parity/PLAN.md` for the full remaining phase list
  (route-by-route layout parity, non-UI capabilities) — this is phase 1 of ~16.

## 3.1.2
Two follow-ups requested after 3.1.1 shipped: real screenshots confirming the ported UI actually
renders correctly, and a genuine replacement for the one piece of GoogleLookBookUI's UI that was
deliberately not ported (its fake connectivity ping).

- **Real "Test connection" checks in Settings → Cloud, replacing the fake ping this app never
  had.** `ProviderConnectivityChecker` (new, `shared/commonMain`) makes an actual read-only HTTP
  request per provider — `GET /models` (Groq), `GET /auth/key` (OpenRouter), `GET
  /api/whoami-v2` (Hugging Face) — against the exact same hosts this app's real generation code
  already calls (`LlmClient`, `FreeCloudDiscovery`). A "Test Hugging Face/Groq/OpenRouter key"
  button now sits under each API key field in Settings → Cloud → API Keys; the result pill shows
  a real measured round-trip latency on success, or the real HTTP status meaning on failure
  (unauthorized, rate-limited, unreachable) — never a `delay()` and a random number. This is the
  real version of the check GoogleLookBookUI's `ModelConfigScreen.kt` faked and that 3.1.1
  explicitly declined to port as-is (see that entry, and `docs/DRAWBACKS.md`).
- **Real pixel screenshots of the 3.1.1 UI port, rendered on the JVM.** Extended the existing
  `ScreenshotTest` suite (Robolectric `GraphicsMode.NATIVE` — genuine Skia rasterization, no
  device/emulator/KVM needed) with 9 new screenshots covering every piece shipped in 3.1.1: the
  floating-pill bottom dock, both chat bubble roles, the typing indicator, the empty state, the
  headlines bar, the quick-prompt carousel, and all three `LiteRtStatusIndicator` states, plus one
  more for this release's new connectivity-test row. All are real, non-blank, correctly-styled
  renders confirmed by direct visual inspection — not claimed from reading the code.
- 13 new tests: `ProviderConnectivityCheckerTest` (10, mock-HTTP-engine tests covering every real
  status-code branch and exception path — success, 401/403, 429, 5xx, thrown exceptions, and the
  exact Bearer-auth header/host per provider) and `ConnectivityTestRowTest` (3, the UI wiring:
  three test buttons render, no stale result before a test runs, and a tap drives the real code
  path without crashing). One honesty note in `ConnectivityTestRowTest`'s own doc comment: this
  environment's Robolectric Compose harness could not reliably observe the *async-completed*
  click-to-result state within a test (the same class of coroutine/idle-timing limitation
  documented for `PrivacyBlurFlowTest` earlier in this project) — the underlying network logic
  that actually matters is still fully covered by the 10 `ProviderConnectivityCheckerTest` cases.
- **Live spectrum scope in Audio Studio playback, closing the one gap the lovable-parity plan
  left open.** `SpectrumScope` existed as a rendering component with a smoke test but nothing fed
  it real data. `AndroidPlaybackVisualizer` (new, `shared/androidMain`) attaches
  `android.media.audiofx.Visualizer` to whichever clip's `MediaPlayer` session is currently
  playing in `AudioClipList` and streams its FFT output into the scope live. The byte→magnitude
  conversion (`magnitudesFromFft`, `shared/commonMain`) is a pure function — no `android.media`
  dependency — so it's unit-tested directly (6 new tests: DC/Nyquist bins, packed real/imaginary
  middle bins, all-zero input, output length, and a direct sqrt cross-check) rather than only
  smoke-tested through Compose.
- **Narrowed the iOS-target blocker in commonMain.** `EpochClock.System`'s wall-clock source and
  `LogEntry.formatDisplay()`'s `HH:mm:ss` formatting were the two remaining direct
  `java.text`/`java.util`/`java.lang.System` calls in `shared/commonMain` — moved behind
  `expect`/`actual` (`wallClockMs()`, `formatHms()` in `shared/src/commonMain/.../time/`, Android
  actuals alongside), mirroring the existing `createQualityPostProcessor` pattern. Incidentally
  fixes a latent thread-safety bug: the old code shared one `SimpleDateFormat` instance (not
  thread-safe) across every `LogEntry`; the new Android actual uses a `ThreadLocal`. iOS itself is
  still not a declared target — this closes two concrete, named instances of the blocker, not the
  blocker itself.
- **Extended Appium/UiAutomator `testTag` coverage** into the areas `docs/DRAWBACKS.md`
  explicitly flagged as untagged: Settings' clear-API-keys button and confirm dialog, the three
  new cloud connectivity-test buttons, the durable-storage-access prompt in Model Packs, the
  report-content dialog (every trigger button, every reason, Cancel), and the Wardrobe gallery
  (per-look tap target, Favorite/Delete buttons, delete-confirm dialog, All/Favorites filter
  chips). All new tags follow the existing `TestTags.kt` per-entity-id pattern.
- **Extended the Appium suite** (`appium/`) to cover the three gaps `docs/DRAWBACKS.md` named
  explicitly: video and audio (TTS-first) generation reaching a real terminal state, Model Packs
  (screen reachability, the durable-storage prompt, and an already-installed pack's real
  handshake verification — `test_model_packs.py`, new), and Wardrobe (gallery browsing,
  favoriting, opening a look's version history, delete-confirm/cancel — `test_wardrobe.py`, new).
  Still unexecuted in this environment (no device/emulator/Appium server) — same honesty note as
  the rest of this suite.

## 3.1.1
UI pieces ported over from `zakir9622/GoogleLookBookUI` (a Google AI Studio–generated build of
this same app, frozen around v3.1.0-rc23). That repo turned out to be an earlier snapshot of this
codebase, not a separate product — most of it is behind what shipped in 3.1.0, but a real
file-level diff found five genuinely distinct, additive pieces worth bringing forward:

- **Richer News/Chat UI.** Replaced the plain "YOU"/"ASSISTANT" label-and-text rows with real
  chat-tail bubble shapes, an AI avatar, per-message timestamps, and a copy-to-clipboard action
  (`ChatComponents.kt`). Added a proper empty state with tap-to-start conversation starters
  (`ChatEmptyState`), a pulsing typing indicator while a reply streams in (`ChatTypingIndicator`),
  and a collapsible live-headlines strip (`NewsHeadlinesBar`) replacing the old plain headline
  list. Dropped the source repo's token-throughput metrics block (TTFT/duration/tokens-per-second)
  since our `ChatMessage` doesn't carry that data — not faked in.
- **Quick-prompt carousel.** `PromptComposer` now takes an optional `quickPrompts` row of one-tap
  starter chips, wired into News/Chat with the two most recent headlines plus a generic
  "What can this app do on-device?" prompt.
- **Import an existing audio file into the voice changer.** `AudioStudioPane` was mic-only;
  `AudioImportHelper.copyUriToCache` + a new "Import audio" chip let a user pick any audio file
  from device storage and run it through the same voice-change/transcribe pipeline as a
  recording.
- **Real on-device model status chip.** `LiteRtStatusIndicator`/`LiteRtGemmaStatusIndicator` show
  installed/warm/loading/error state for the local Gemma/Qwen/FunctionGemma packs directly in
  Code Studio, bound to the actual `GenerativeViewModel.warmup` state — not simulated.
- **A real "Create" tool picker.** The bottom dock's center FAB used to jump straight to the
  last-used studio tab. `QuickCreateSheet` now opens a 2-column grid of every local generation
  surface (Image/Video/Code/Audio, News & Chat, and Try-On when that flag is re-enabled) with a
  short description and capability badge each — closing a gap the original A3 bottom-dock work
  left open (the plan's own research had called for exactly this "one obvious button starts
  anything" pattern).
- **Bottom dock restyled** from a full-width bar to a floating glass pill with a radial-gradient
  center FAB and spring-animated item selection, matching the reference app's dock language. Pure
  visual change — navigation logic, `BottomBarDestination`, and every existing test/testTag are
  unchanged; `BottomBarNavigationTest` passes against the restyled bar unmodified.
- **Not ported, flagged instead:** the source repo's `ModelConfigScreen.kt` (a unified cloud
  provider/API-key settings screen) shows connectivity "ping" status per provider, but the check
  is fake — `delay(600)` followed by a random 65–115ms latency presented as a real measurement.
  That conflicts with this project's own no-fabricated-status discipline (see `DRAWBACKS.md`), so
  it wasn't imported as-is.
- **Live chat event console.** A final verification pass (checking `shared`, `AndroidManifest.xml`,
  and `build.gradle.kts` too, not just the UI layer already covered) turned up one more real, wired
  piece the first pass missed: `LogStateManager` — a transient, capped, timestamped event log
  (LiteRT/Cloud API/System sourced) that the source repo's `ChatViewModel` populates and its
  `ChatPersistentInputBar` renders as an expandable console. Ported the log-collecting engine into
  `shared/commonMain` and wired matching log calls into our own `ChatViewModel` at the same points
  (preflight-blocked, dispatching, LiteRT stream start/done/fallback, cloud connect/reply, errors,
  cancel) — but instead of porting their separate console UI, reused our own already-shipped
  `LiveGenConsole` component (used elsewhere for image/video/code generation) to render it in
  News/Chat, keeping one console implementation instead of two.
- 23 new tests covering every ported/wired piece: `QuickPromptCarouselTest`,
  `LiteRtStatusIndicatorTest`, `ChatComponentsTest`, `AudioImportHelperTest`, `LogStateManagerTest`
  — all real interaction/render/state tests, no stubs, all passing alongside the existing
  `BottomBarNavigationTest` (unmodified, still green against the restyled dock).

## 3.1.0 (stable)
This is the stable release closing the lovable-parity local-first plan
(`docs/plans/lovable-parity-local-first/PLAN.md`) — every item A0–A3, B1–B8, D1–D2 is now
either shipped with test evidence or explicitly and honestly documented as unverified-on-device
in `docs/DRAWBACKS.md`. No item is left "deferred" or "pending" without a stated reason.

- **D2 — audio DSP verified against the real shipped pipeline, and a real bug found and fixed
  as a result.** `AudioDspVerificationTest` (JVM/Robolectric, `shared/androidUnitTest`) exercises
  `AndroidLocalVoiceChanger.transform()` itself — not new test-only math — with synthetic tone
  fixtures: +12 semitones measures ~880Hz from a 440Hz input, -12 measures ~220Hz, extreme knobs
  never exceed the 16-bit PCM range, and default knobs preserve both pitch and sample count.
  Writing the speed tests (2x should roughly halve duration, 0.5x should roughly double it)
  surfaced a genuine production bug: `applyPitchAndSpeed()` was *dividing* `readStep` by `speed`
  instead of multiplying, so the "Speed" knob's effect was inverted — a 2.00× setting played
  audio *slower* (longer), and a 0.50× setting played it *faster* (shorter), the opposite of
  what the UI's label promised. Fixed in `AndroidLocalVoiceChanger.kt`; all 6 new tests pass
  against the corrected pipeline, and the full 293-test `shared` suite plus the full
  `composeApp` suite were re-run afterward to confirm no other behavior depended on the old
  (wrong) direction.
- **D1 — local code-generation output-quality test suite.** `LiteRtLmOutputQualityTest`
  (`composeApp/androidTest`) runs three representative prompts (Kotlin quicksort, a StateFlow-vs-
  Flow explanation, a Jetpack Compose counter button) against the real installed Gemma 4 pack via
  `LiteRtLmEngine`, asserting the output is non-empty, substantive, free of leaked `<think>`
  blocks, and contains prompt-appropriate markers (`fun`, `pivot`/`partition`, `@Composable`,
  `remember`/`mutableStateOf`). Follows `LiteRtLmBenchmarkTest`'s graceful-skip pattern when no
  pack is installed on the device. Compiles cleanly (`compileSideloadDebugAndroidTestKotlin`);
  like the rest of this app's `androidTest` suite, it has not been run on a physical device in
  this environment — see `docs/DRAWBACKS.md`'s Testability section.
- **`SettingsTierSmokeTest`** — removed its stale `HomeTabRoute.NEWS` mirror constant, left over
  from before A3 (3.1.0-rc25) moved Chat out of the `HomeTab` pager and into the bottom dock.
- Version: drops the `-rc` suffix — this is the stable release the `-rc24`..`-rc27` cycle was
  building toward.

## 3.1.0-rc27
- **B7 — privacy blur post-process.** Fully offline face detection via ML Kit's bundled
  face-detection model (~6MB, no network call, no Play Services dependency — see
  `libface_detector_v2_jni.so` now packaged into the APK). `FaceBlurProcessor.detectAndBlur()`
  detects faces and applies a real box-blur (`BoxBlur` — no RenderScript, several passes
  approximating gaussian) to each region. `RegionBlurOverlay` adds a drag-to-draw manual blur
  tool for anything the detector misses. `PrivacyBlurSheet` (opened via the new "Privacy blur"
  button on every `GenerativeState.ImageReady` result) combines both: an auto-blur toggle, a
  blur-strength slider, drawn regions, and "Save original"/"Save blurred" actions. Blurred output
  keeps the same EXIF provenance tag as every other generated image (`Provenance.ensureImageFile`).
- 11 new tests: `BoxBlurTest` (real pixel-level blur math on actual `Bitmap`s — a sharp edge
  measurably smooths, a uniform region stays uniform, out-of-bounds/zero-radius/empty-region
  inputs don't crash), `RegionBlurOverlayTest` (a real drag adds a region, a tiny drag doesn't,
  clearing renders correctly), `PrivacyBlurFlowTest` (the auto-blur toggle and "Save original"
  pass-through, exercised against `PrivacyBlurContent` directly rather than through
  `ModalBottomSheet` — Robolectric's Compose harness doesn't reliably dispatch clicks into a live
  bottom sheet's window layer, and a real device-size root window is needed too, or every button
  in the sheet measures to zero size and silently swallows clicks with no exception; both
  findings are documented in the test file for the next time this pattern is needed).
- **Honesty note**: `FaceBlurProcessor`'s ML Kit detector itself is not exercised on a real image
  with real faces in this environment (no device, and ML Kit's on-device model behavior isn't
  meaningfully testable under Robolectric) — the blur *math* it calls (`BoxBlur`) is real and
  tested against actual bitmaps, not stubbed.

## 3.1.0-rc26
- **B6 — voice studio DSP depth.** Real, unit-tested signal-processing core added to `shared`:
  `PitchDetector` (autocorrelation-based fundamental-frequency detection), `PitchMatcher`
  (computes the semitone shift to move a recorded clip's pitch onto a target), `LatencyCalibrator`
  (cross-correlation round-trip latency estimation), and `SimpleFft` (radix-2 FFT for spectrum
  magnitude). All four are pure functions over `FloatArray`/`ShortArray`, verified with synthetic
  sine/chirp signals — 21 new tests, all passing on real math, not mocks.
- **Wired into Audio Studio:** `AndroidMicRecorder` now exposes a live `StateFlow<Float>` RMS
  amplitude, driving a new `AudioLevelMeter` (rolling-history bar visualization, reduced-motion
  gated) shown while recording. Voice personas are grouped into Female/Male/Neutral & character
  sections (`VoiceCatalog.groupedByVariety()`) using the new `GlassTile` inside the picker. A
  "Match voice" chip runs `PitchMatcher` against the recorded clip and the selected persona's
  typical pitch range, auto-setting `VoiceKnobs.pitchSemitones`. A "Calibrate mic latency" chip
  runs `AndroidLatencyCalibrator` (plays a tone, records it, cross-correlates) and displays the
  estimated round-trip latency as an informational readout.
- **Honesty note on hardware verification**, matching this app's established pattern (see the
  GPU-delegate fallback in `LiteRtLmEngine`): the DSP *algorithms* are real and tested against
  synthetic signals. The Android I/O around them — simultaneous `AudioTrack`/`AudioRecord` in
  `AndroidLatencyCalibrator`, and `AndroidMicRecorder`'s new amplitude stream — has not been
  exercised on a real device in this environment. `SpectrumScope` (playback-side spectrum bars)
  is built and smoke-tested but not yet wired to a live data source anywhere in the app — no
  screen calls it yet, since that would require Android's `Visualizer` API on a real playback
  session this environment cannot verify. Extracted `WavIo` (mono 16-bit PCM read/write) out of
  `AndroidLocalVoiceChanger`/`AndroidMicRecorder` to remove duplication now that three call sites
  need it.

## 3.1.0-rc25
- **A3 — bottom dock navigation.** Added `LookbookBottomBar` (Home / Library / a raised center
  Create FAB / Chat / Settings), wired into `VestraNavHost` via a `Scaffold`. The in-studio pager
  (Image/Video/Audio/Code) is unchanged — it's a second, lower level of navigation nested inside
  the Home destination, exactly as before. News/Chat is promoted from a pager tab to its own
  top-level route (`Routes.CHAT`), reachable via the dock's Chat item instead of a `HomeTab.NEWS`
  entry; `NewsChatScreen` now wraps itself in `SpatialBackground`/`.safeDrawingPadding()` since
  it's no longer nested inside `HomeScreen`'s own background. The header's Wardrobe and Settings
  icon buttons were removed from `HomeScreen` (redundant with the dock's Library/Settings items);
  Help stays in the header since it has no dock slot.
- **Session isolation verified safe by construction, not by luck.** `GenerativeViewModel` is
  created once in `VestraNavHost`'s own composable scope and passed down as a parameter — it is
  never scoped to a `NavBackStackEntry`, so `StudioBag`/`bindStudio` per-tab prompt state is
  unaffected by bottom-bar navigation regardless of the back stack's save/restore behavior. The
  dock itself uses the standard `popUpTo(startDestination) { saveState = true }` +
  `restoreState = true` pattern so the studio pager's own position (`rememberPagerState`, which is
  `rememberSaveable`-backed) survives a round trip through Library/Chat/Settings too.
- Added `appium/test_bottom_bar.py` (dock visibility, per-item navigation, Create FAB, and a
  studio-prompt round-trip regression guard) and `BottomBarNavigationTest.kt` (Robolectric).
  Updated `test_prompt_isolation.py` and `test_generation_flows.py` for Chat's new location, and
  `test_processing_mode.py` for Settings now opening via the dock's `bottom_bar_settings` tag.

## 3.1.0-rc24
- **A0 completion — modality accents now reach every studio surface**, not just the header
  label: `VestraColors.modalityAccent(AiCapability)` resolves the right per-modality tint (brass
  for Image/Edit/Try-on, copper for Video, teal for Code, dusty rose for Audio) and is now
  threaded through `PromptComposer` (border, model chip, reference-image icon), `ResultPane`
  (loading spinner/progress bar, result pills), `HomeScreen`'s tab row (selected-tab color),
  `ModelPickerSheet` (search field, section headers, selection state, status dots), and
  `AudioStudioPane`'s voice-changer knob readouts. Image/Video/Code studios pick this up via
  `UnifiedStudioPane`; Audio wires its own `VestraColors.ModalityAudio` since it isn't routed
  through that shared pane.
- **Added `SpacingTokens`** (`xxs`…`xxl`, plus `section` for the historical 18.dp card padding) —
  replaces ad hoc `18.dp` literals in `GlassCard`, `HomeScreen`, and `UnifiedStudioPane`/
  `AudioStudioPane`'s outer padding.
- **A2 completion — `Modifier.tilt3d()`**: a lightweight 3D perspective-tilt micro-interaction
  (pointer-driven `rotationX`/`rotationY` via `graphicsLayer`, springs back to flat on release),
  gated by `rememberReduceMotion()` like every other animation in this app — an exact no-op
  Modifier when reduced motion is on. Applied to the try-on hero card.
- **Added `GlassTile`** — a lighter nested-content variant of `GlassCard` (stronger fill,
  `RadiusTokens.md`, no press-lift/shadow) for future list-row use inside existing glass cards.
- New tests: `SpacingTokensTest`, `ModalityAccentTest`, `TiltModifierTest` (Robolectric).

## 3.1.0-rc23
- **Fixed a real prompt-leak bug, found directly from a user report**: typing a prompt in one
  studio tab (Image/Video/Code/Audio), then visiting News/Chat and tapping a headline, could
  overwrite that prompt with the headline's text. Root cause: `HomeScreen.openNewsChat()` and
  `VestraNavHost`'s `onOpenNewsChat` callback both wrote the headline into
  `GenerativeViewModel.prompt` — the single `StateFlow` every studio tab reads — even though
  `NewsChatScreen` already manages its own separate local chat-input state and never reads that
  flow. Both dead writes deleted; the per-tab isolation mechanism itself
  (`GenerativeViewModel.bindStudio`/`StudioBag`) was already correct.
- **Wired the image-edit/img2img entry point for Appium**: the "Add reference image" button and
  its attached-photo thumbnail on the Image tab (`composer_add_reference`,
  `composer_reference_thumb`) now carry stable `testTag`s — these existed as constants but were
  never actually applied to the composables. Also tagged Home's Settings entry button
  (`home_open_settings`).
- **Added a real Appium test suite** (`appium/`) covering prompt isolation across tabs (a direct
  regression test for the leak above), local image/code/chat generation reaching a genuine
  terminal state, the image-edit flow end to end, and the Processing Mode card. Honestly
  documented as unexecuted: no device, emulator, or Appium server exists in the environment that
  wrote it — see `appium/README.md`.

## 3.1.0-rc22
- **Started porting lookbookweb's design/UX language, local-only, per
  `docs/plans/lovable-parity-local-first/PLAN.md`.** Added four per-modality accent color tokens
  (`VestraColors.ModalityImage/Video/Code/Audio`, brass-family tints — Loom Ink's identity stays)
  and a derived `RadiusTokens` corner-radius scale; wired the Studio header label to its
  modality's accent. Added a subtle press-lift micro-interaction to `GlassCard` (scale to ~97%
  on press, gated by reduced-motion) — lookbookweb's `press-3d` language ported at Compose-native
  cost. Confirmed the Syne/Outfit typography pairing this plan called for was already in place.
- **Fixed misleading "Cloud by default" studio copy.** The Image/Video/Code studio subtitle said
  "Cloud by default" regardless of whether cloud models were actually enabled — since
  `cloudModelsEnabled` defaults to `false` app-wide, that text was simply wrong for most users.
  Now reads "On-device only (cloud is off)" when the master toggle is off, or names the local
  pack to install either way.
- **The News/Chat window is now Appium-testable**: refresh button, headline cards, and chat
  message bubbles carry stable `testTag`s, alongside the generation-flow coverage from rc21.
- Updated `docs/DRAWBACKS.md` and the plan's own README with an honest status: this is a slice
  of the full lookbookweb-parity plan, not the whole thing — see those docs for exactly what's
  landed and what's still open.

## 3.1.0-rc21
- **Local LiteRT-LM models now fall back to CPU automatically if the GPU delegate fails to
  initialize**, found via a user's Pixel 9 screenshot: `Local Qwen3 0.6B (fast) could not load:
  Failed to create engine: INTERNAL: ERROR: [...litert_compiled_model_executor.cc...]`. Before
  this fix, a failed GPU init had no fallback, so tapping "Retry load" repeated the identical
  failing GPU path forever. `LiteRtLmEngine.initialize()` now tries GPU first when requested,
  catches a GPU init failure, logs it, and retries on CPU — the model still loads, just slower.
- **The app is now testable with Appium/UiAutomator and similar external automation tools.**
  Compose's `Modifier.testTag` is invisible outside Compose's own UI-test framework unless the
  app opts in via `testTagsAsResourceId`; that flag is now set once at the composable root
  (`MainActivity.kt`). A new `TestTags` catalog
  (`composeApp/src/main/kotlin/com/zakir/vestra/ui/TestTags.kt`) gives every core interactive
  and result element in the generation flow a stable id: prompt input, model chip, assist
  toggle, send/stop, each home tab, every `GenerativeState` result card (image/video/audio/code
  streaming and ready/transcription/failed), the live generation console, retry/cancel, model
  pack install/handshake buttons, and each row in the model picker sheet (cloud and on-device).
- **Added `docs/DRAWBACKS.md`** — an honest, non-marketing list of this app's current real
  limitations (local model quality tradeoffs, partial NNAPI offload, no committed on-device
  benchmark yet, testability coverage gaps, no iOS target), kept up to date as items close.

## 3.1.0-rc20
- **Fixed a real on-device crash in local Create Studio**, found via a user's Pixel 9 screenshots:
  `ORT_INVALID_ARGUMENT — Invalid rank for input: timestep Got: 0 Expected: 1`. The local
  txt2img engine built the timestep tensor with no shape (defaulting to a scalar); the published
  `local-sdturbo-v1/unet.onnx` requires rank 1. Reproduced the exact error against the real graph
  before and after the fix to confirm.
- **Fixed local generations being mislabeled as cloud**, found via a user's diagnostics export:
  a CHAT run recorded `modelId: "llama33-70b-groq"` while its own note field said
  `local-qwen3-06b-v1` actually ran (cloud was off). The live console showed "Connecting to FLUX.1
  Schnell" / "Connecting to Llama 3.3 70B (Groq)" immediately before local generation actually
  started. Fixed across image/code/video/audio generation, the Chat and Code Studio diagnostics
  records, and the Diagnostics screen's "Tier" field (was hardcoded to CLOUD for every run).
- **Video now hard-stops offline** like image/code/audio already did, instead of a soft "Network
  probe uncertain — trying cloud anyway…" that burned time with no network to reach.
- **Local still-clip video holds its pack in use** through both the still-image generation and
  the MediaCodec encode that follows it, matching the pattern used everywhere else a local pack
  backs a multi-stage operation.
- **Pack handshake toasts no longer leak machine ACK strings** (`HANDSHAKE_OK`) — use the existing
  human-readable summary everywhere a handshake result reaches the user.

## 3.1.0-rc19
- **Live generation output, everywhere:** tapping Generate now streams real model output as
  it's produced — News Chat and Code Studio append tokens live (`GenerativeState.CodeStreaming`,
  `ChatViewModel.streamLocalReply`), local image generation (tiny-SD/LCM and Bonsai) reports
  live per-step progress instead of a single static "please wait". No stage is simulated.
- **Two real, on-device-model-verified bugs fixed in local Create Studio** — found and confirmed
  by running the actual published `local-sdturbo-v1` ONNX weights end-to-end on real hardware
  math (not just code review), per the standing "test the models, don't trust the code" rule:
  - `OrtGraph.timestepTensor` was missing an FP16 branch; the pack's `unet.onnx` declares
    `timestep tensor(float16)`, so every generation threw `ORT_INVALID_ARGUMENT`.
  - `LcmScheduler.step()` combined the UNet's raw noise prediction directly instead of first
    converting it to a predicted denoised sample, and never re-injected noise between steps —
    both required by the model's LCM distillation. Rewritten to match diffusers'
    `scheduling_lcm.py` exactly; verified against a real 4-step generation that produces a
    genuine, if soft, image instead of statistical noise.
  - Local image-to-image edit additionally ignored `strength`: the denoise loop always started
    from the schedule's highest timestep even though the reference image was only noised to a
    partial level. Fixed to slice the timestep schedule to match, mirroring diffusers'
    `get_timesteps()` — img2img now denoises from the correct noise level instead of collapsing
    to a near-black frame.
- **Real-ESRGAN quality upscale now reaches local Create Studio.** `realesrgan-v1`'s own catalog
  description already promised "auto-upscale after try-on or Create" — it only ever ran for
  try-on. Wired the same `QualityPostProcessor` into `AndroidTxt2ImgEngine` and
  `BonsaiImageEngine` so an installed pack now upscales locally-generated images too.

## 3.1.0-rc18
- **Bonsai Image 4B (LiteRT):** second on-device text-to-image engine, `local-bonsai-image-v1` —
  ternary-weight FLUX.2-klein-architecture DiT via LiteRT `Interpreter`/XNNPACK (~4 GB, text-to-image
  only). Selectable alongside tiny-SD in the Create Studio ON-DEVICE picker; Edit always uses tiny-SD.
- Plain `com.google.ai.edge.litert:litert` runtime added alongside the existing LiteRT-LM engine.

## 3.1.0-rc17
- **LiteRT-LM deep integration:** warm engine cache (no per-shot cold load), 90s inference timeout
- **Offline hard-stop:** Code Studio and Chat fail closed when offline without local pack
- **FunctionGemma:** selectable in Code ON-DEVICE picker; tool callbacks wired to studio prompt/tier
- **Audio scribe picker:** Generate transcribes attached clip when scribe model selected
- **Vision assist:** feedback when reference photo cannot be read
- **Per-pack readiness:** Gemma 3 / Gemma 4 / FunctionGemma show independent install state in picker

## 3.1.0-rc14
- **DoD stability pass:** live HF `pro-v1` verified fully-conditioned; CatVTON exporter quarantined off `pro-v1`
- Composer honesty: remove Steps/CFG/Seed UI (never reached cloud); audio fashion assist enriches speech
- Model Health dropdown uses runtime `effectiveSupport` (cooldown/failures), not static catalog
- Video + audio Gradio `predict` honor wall `deadlineMs` + poll timeout (same as image)
- Offline image/audio hard-stop when local unavailable — no cloud loop
- Audio failures map `CloudFailure` → health kinds; CI runs `integration-edge-cases.py`

## 3.1.0-rc10
- **Five-star Q1:** per-studio session bags (pager tabs no longer wipe each other)
- OrtGraph safe session + output size caps; local packs mark in-use during generate
- Still-clip MediaCodec presentation timestamps; human handshake labels
- Prefer local Create/Edit/Code/Video when offline; honor cloud selection when online
- Help + product blurb updated for true-local; Clip studio naming

## CI / releases
- **Release APK only on `main`:** merges/pushes to main publish the rolling `latest` GitHub Release
- Feature-branch pushes no longer create preview releases (PR runs Android CI checks only)
- Publishing `latest` prunes any other leftover release tags

## 3.1.0-rc9
- **Pack device handshake:** Settings → Engines & packs and Model packs gain **Verify link** / **Verify all**
- Re-checks files + graphs on device and returns `HANDSHAKE_OK` / `HANDSHAKE_FAIL` with wired studios listed

## 3.1.0-rc8
- **True local for every studio:** Image Create/Edit, Video still-clip, Code (Gemma), Audio (system TTS) — not try-on only
- **Image Edit offline:** `vae_encoder` img2img via `local-sdturbo-v1` v3+
- **Video offline:** honest H.264 still-clip from on-device keyframe (`local-stillclip-v1`)
- **Code offline:** MediaPipe + published `local-gemma-v1` (~530 MB)
- Catalog / preflight / studio copy updated; airplane-safe generate when local packs ready

## 3.1.0-rc7
- **True local Image Create:** published `local-sdturbo-v1` (~994 MB tiny-SD ONNX FP16) to HF packs; catalog `runnable=true`
- Assemble tooling: `scripts/assemble-local-sdturbo-pack.py` (from public tiny-SD ONNX)
- Studio copy: download pack from Model packs for offline Create
- Continues rc6: Pixel try-on ORT R8 fix + cloud studio reliability

## 3.1.0-rc6
- **Try-on crash fix:** R8 keep `ai.onnxruntime.**` — Pixel SIGABRT was `NodeInfo.<init>` NoSuchMethodError during Lite generate
- **Cloud Image:** Prefer FLUX Schnell Space by default; mark SDXL Lightning unsupported (Space API 404); fix 402 credit copy (was mislabeled as token permissions); capability-aware Inference rejection hints
- **Cloud Audio:** Default Edge-TTS; budget 45s with budget-aware polls so Kokoro falls back instead of hanging ~90s
- Continues true-local work from rc5 (system TTS, SD-Turbo engine)

## 3.1.0-rc5
- **True local Audio:** Android system TTS offline (personas → device voices) + DSP knobs
- **True local Image engine:** `AndroidTxt2ImgEngine` ORT denoise loop wired (`SAMPLER_WIRED=true`); needs `local-sdturbo-v1` pack weights to run
- **Airplane-safe studios:** Image/Audio skip cloud API-key preflight when local engines are ready
- **Honesty:** system TTS reports `local-tts-system`; SD-Turbo picker shows green when pack graphs installed
- **Pack tooling:** `export_image_gen_pack.py` writes `pack.json` + optional tokenizer copy; `verify-local-sdturbo-pack.py`
- Catalog: `local-tts-system` Ready offline; SD-Turbo status “Engine ready · pack weights not on device”
- Plan: `docs/plans/true-local/PLAN.md`

## 3.1.0-rc4
- **Local model picker honesty:** Create Studio ON-DEVICE list uses `forStudioPicker` — Real-ESRGAN / BiRefNet / GFPGAN quality packs no longer appear as Image generators; SD-Turbo / local TTS / local video show scaffold · weights-not-published status
- **Audio mic + voice change:** Record short PCM/WAV on-device, apply local DSP knobs (record → transform → play); `RECORD_AUDIO` permission
- **Cloud audio hosts:** Edge-TTS → `innoai/Edge-TTS-Text-to-Speech` (`tts_interface`); Kokoro → Remsky ZeroGPU (`generate_speech_from_ui`); MMS-TTS demoted (HF Inference often rejects); default audio = Kokoro
- **Cloud video:** Wan2 fails faster (short poll) then falls back to LTX; rate-limit cooldown messaging
- **UX:** Fix double “Space Space” in offline 404 copy
- **Try-on crash hardening:** Soft-wrap ORT session create / UnsatisfiedLinkError; yield before heavy graphs; catch native Throwable on Lite/Pro generate path

## 3.1.0-rc3
- **Image edit timeouts:** Gradio poll GETs capped at ~12s (no more 60–75s stuck on “Space poll 1/N”)
- Honor the image deadline inside Space wake/poll loops; skip wake retries when budget is tight
- After Qwen (or another primary) burns the 120s window, grant a 45s grace pass for InstructPix2Pix fallback

## 3.1.0-rc2
- **Audio Studio:** new home tab — cloud TTS (MMS-TTS Inference, Kokoro Space, Edge/OpenVoice Space)
- **Voice personas:** Amina, Noor, Layla, Yasir, Omar, Sam, Rana, Kai (named varieties)
- **Local voice changer:** on-device DSP knobs — pitch, speed, formant, warmth, clarity (no pack required)
- **Local TTS scaffold:** `local-tts-v1` + `LocalAudioGenerator` (`TTS_RUNNER_WIRED=false` until weights)
- Honest Settings / model picker entries for audio

## 3.1.0-rc1
- **Big release R2 (true limits):** full ATR Auto classification for all garment categories; single-pass human parse on generate
- **Garment chips:** complete taxonomy (Abaya, Jilbab, Kaftan, Hijab, Niqab, Dupatta, Headscarf, Shalwar, Kurta, Lehenga, Dress, Upper, Trousers, Full coverage) + Auto
- **Real-input harness:** `scripts/test_atr_classify.py` + `scripts/fixtures/atr/*.json` (12 worn-photo shapes); Kotlin `AtrTaxonomyTest` mirrors fixtures
- **UI — Loom Ink:** cool mist + brass + teal-ink atelier; stronger brand hero; less card clutter on Packs intro
- **On-device Create Studio:** `Txt2ImgPipeline` scaffold (`SAMPLER_WIRED=false`); honest cloud-only Image/Video/Code until HF weights
- Plan: `docs/plans/big-release-r2/`

## 3.0.16
- Stable sideload keystore + soft network preflight (stop false offline blocks)

## 3.0.15
- Live gen console + ticking countdown; diagnostics share off main thread

## 3.0.14
- Garment pick no longer loads `human_parse.onnx`; connection-abort UX ≠ offline

## 3.0.13
- Offline ≠ Cooling down; Lite soft verify; trim-memory no longer clears ORT on UI_HIDDEN

## 3.0.12
- ORT CPU default; soft startup verify; Prefer NNAPI toggle (off)

## 3.0.11
- Abrupt-exit session watchdog; low-memory + logcat FATAL scrape

## 3.0.10
- **ZeroGPU UX:** account quota no longer shows misleading “Cooling down · 1m” — chip says **ZeroGPU empty · refills daily**
- After account ZeroGPU fail, skip other HF Spaces and try Inference fallbacks; error CTA becomes **Choose model**

## 3.0.9
- **Auto-troubleshooting:** uncaught crashes append to `diagnostics/crash_log.txt` (never auto-cleared) with classified `likelyCause`
- Continuous `app_trace.log` breadcrumbs (screen route) + rotating size cap
- Diagnostics: last-crash card, **Share troubleshooting bundle**, manual clear only for crash/trace

## 3.0.8
- Diagnostics export includes **logcat snippet** (warnings+) + app version in the JSON bundle
- Plan **COMPLETION.md** scorecard for Claude expansion + v3 follow-up (~95% in-repo done)

## 3.0.7
- **M4 LocalImageEngine:** `AndroidLocalImageGenerator` validates installed `local-sdturbo-v1` graphs (rejects scaffold placeholders); Create Studio stays on cloud until real weights + sampler
- **cycle4:** `DiffusionSteps` LCM clamp (4–8) extracted + unit-tested; export scaffold sets `lcmDistilled`
- **M5:** `scripts/catalog-matrix.py` + `verify-all-models.sh` fold local `runnable` flags into the report

## 3.0.6
- **C4 SettingsScreen split:** widgets + general/cloud/engines/appearance section files; orchestrator ~380 lines (was ~1,180)
- Durable-storage **primary CTA** moved off Appearance — pack download (`rememberPackDownloadStarter`) + Packs screen own enable flow; Settings shows status/tip only
- **Honesty polish:** `PackAwareLocalImageGenerator.isReady` false until runner wired; `pro-v2-int8` catalog `runnable=false` until HF; on-device picker “Coming soon” for unpublished packs
- Hostname sanitize in cloud failure hints; QNN comment honesty; `visual-verify.sh --dry-run`; accesslint routes expanded; release notes Android 15+

## 3.0.5
- **Live model health UI:** picker, Usage, Settings, and preflight show cooldown / verified labels from `ModelHealthTracker` (not static Ready)
- Health records success/failure for **code + video** as well as image
- **Blank-frame reject:** Android luminance MAD check after download; image size floor raised to 2 KB
- Scaffold `LocalImageGenerator` + pack-aware wiring in Create Studio (still `runnable = false` until weights)
- Unit tests: `ModelHealthTrackerTest`, validator 2 KB floor

## 3.0.4
- **Quality pack integration:** Real-ESRGAN runner feeds FP16 `input` + `denoise_strength` (was silent no-op via single float32 OrtModel)
- BiRefNet matte applies **sigmoid** on logits before resize (was min–max normalize)
- Integrity verify smoke-runs Real-ESRGAN; catalog sizes corrected (~224 MB / ~5 MB)
- `realesrgan-v1` minRam gate lowered to 2 GB in export metadata; integration script smokes both quality packs
- **Local model crash hardening:** pack in-use refcount; block uninstall/update while generating; invalidate ORT session cache before pack file replace; rethrow cancel; soft-fail quality OOM; harden OrtModel output bounds; BackdropCompositor shares session cache
- **Stable release plan:** `docs/plans/stable-release/` — R0 (this cut) vs R1 perfect (offline Create Studio, pro-v2-int8 HF, live health UI)
- Pro unavailable copy prefers **pro-v1** (matches HF manifest); docs clarify **minSdk 35 / Android 15+**

## 3.0.3
- Published **birefnet-v1** (~224 MB) and **realesrgan-v1** (~5 MB) to `Iamzakirzr/vestra-packs` manifest
- Download from **Settings → Model packs**; matte refine + upscale activate when installed
- `scripts/build-and-publish-quality-packs.py` for future quality-pack releases

## 3.0.2
- **Generation stability M2–M6 (remaining):** global image deadline (120s) with remaining-time stage text; Gradio wakeRetries=1 + budget-derived maxPolls
- **M3:** `GradioSchemaClient` live `/info` payloads; removed guessing 1-arg Space fallbacks; HF discovery only for known Inference routes
- **M5:** `visual-verify.sh --compare`, `compare-screenshots.py`, `verify-all-models.sh`, `e2e-matrix.sh`
- **M6:** `EpochClock` replaces `System.currentTimeMillis` in commonMain; `DiagnosticsHook` per-run handles (no concurrent clobber); stop silent Space→Inference rewrite on token save
- Catalog: `local-sdturbo-v1` reserved; BiRefNet/Real-ESRGAN marked downloadable when packs ship; `ml/export_image_gen_pack.py` scaffold

## 3.0.1
- **Generation stability (Claude plan M1/M2):** `CloudFailure` typed errors; image fallback chain correctly advances models (fixes root-cause `continue` bug); per-candidate preflight inside loop; `ModelHealthTracker` with exponential cooldown; stronger `CloudOutputValidator` (1 KB min + dimension check); video no longer hard-requires HF Space; 402 skips remaining Inference candidates
- Removed duplicate `deepseek-r1-free-or` catalog entry (migration to `openrouter-free`)
- Unit tests: `CloudFailureTest`, updated `GenerativeCloudServiceTest` fixtures

## 3.0.0
- Image edit fallback no longer hits broken InstructPix2Pix HF Inference (nscale HTTP 400)
- Qwen Image Edit → InstructPix2Pix Space chain; migrate stale inference edit selection
- DNS / offline errors map to friendly "No internet" instead of raw host resolution text
- FLUX Space failures suggest HF Inference fallback when token is configured
- Usage ledger failures prefix selected model when fallback chain exhausts
- Google Gemma 3 local LLM documented as feasible via LiteRT-LM (catalog placeholder)

## 2.9.14
- Quality plan: `QualityRating` maps catalog scores to 1–5★ (5★ = READY + score ≥ 90)
- Cloud downloads validated (reject empty/corrupt images and videos; retry fallback chain)
- News chat uses the same LLM fallback chain as Code studio (Groq → OpenRouter → HF)
- Bypass filter assist on by default for Image/Video (fewer false safety blocks)
- Lite try-on applies BiRefNet matte refinement when `birefnet-v1` pack is installed
- Human parse uses declared 512×512 input; model picker shows star rating + sorts by quality
- Saving HF token migrates image gen to FLUX Inference when Space defaults were selected

## 2.9.3
- Model fallback chains for video, cloud try-on, and code (tries the next free model when one is busy or missing a key)
- LTX-Video payload aligned to live Space schema (null image fields, 704×512, 2s / CFG 1)
- InstructPix2Pix uses 8 steps to fit free ZeroGPU seconds
- OpenRouter free models: read `reasoning` when `content` is null
- Model picker lists Ready models first

## 2.9.2
- Fixed the biggest cause of failed cloud generation: once a Hugging Face
  account's daily ZeroGPU allowance is spent, HF rejects every Space call that
  carries the token instantly with an empty `event: error` / `data: null`, even
  though the same request still runs anonymously. Space calls now retry without
  the token, so image generation keeps working after the allowance runs out
- Explain empty Gradio errors as a likely spent ZeroGPU allowance rather than an
  unexplained failure
- Point Qwen Image Edit at a distilled mirror of the Space: the official one
  rejects every REST call outright, and 8 steps instead of 50 fits the free
  allowance
- Show the bundled Lite pack as installed as soon as it finishes seeding,
  instead of only after the next app launch

## 2.9.1
- Fixed image generation and editing against live Hugging Face Spaces: image
  arguments are now sent as Gradio `FileData` objects, so Qwen Image Edit and
  InstructPix2Pix no longer fail validation with an empty `event: error` /
  `data: null` response
- Support Spaces on Gradio 4 (`/call`) as well as Gradio 5 (`/gradio_api/call`)
- Read the result image from anywhere in a Space's output, fixing
  InstructPix2Pix (image is the 4th output) and OOTDiffusion (gallery)
- Retry Spaces that are waking or restarting, then fall back to another free
  Space when the selected one is out of ZeroGPU quota
- Report out-of-quota and rate-limited Spaces in plain language instead of raw
  Gradio errors
- Only Hugging Face Spaces can serve try-on, image and video; a stored HF
  Inference model is migrated to a curated Space and the correction is saved
- Default try-on is now OOTDiffusion (verified end-to-end); IDM-VTON, CatVTON
  and SDXL Lightning are marked degraded after live failures
- Settings names the Lite pack as the reason Pro try-on is unavailable

## 2.9.0
- Home: “What would you like to do” action list first; Core Try-on centered below
- Image / Video / Code studios: searchable in-composer model picker (name search)
- Local Lite/Pro always selectable; selecting a pack sets the matching engine tier
- HF: clearer Gradio empty-error messages; default image edit → Qwen; InstructPix2Pix marked degraded
- Stop listing warm HF Inference image models that cannot run via Spaces

## 2.8.0
- Looks gallery: tap opens look detail; delete confirmation; favorite a11y labels
- Video studio results ingest into Looks gallery
- In-app Privacy Policy screen (offline) + Settings About link
- Export local content reports from Settings → Storage & privacy
- Help: search semantics, email support CTA, privacy/report FAQ topics
- Cloud usage empty state → Open Image studio
- Try-on result: favorite + open gallery
- Atelier home respects reduced motion
- Deep-link visual verification (`lookbook://screen/*`, `scripts/visual-verify.sh`)

## 2.7.7
- Saffron FilterChips (no Material purple selected state)
- About + Privacy moved to top of Settings

## 2.7.3–2.7.6
- Cancel / Back recovery for try-on and cloud studios
- Gallery empty CTAs, Report/Share on cloud results
- Model chip → Settings; preflight Open Settings
- Composer/home a11y; deep-link screencap tooling
