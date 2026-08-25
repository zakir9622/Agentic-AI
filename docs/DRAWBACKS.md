# Honest drawbacks — The Lookbook

This is a plain list of real, current limitations — not a marketing page. Every item here is
either something verified directly against the code/models this session, or a known
architectural tradeoff the team should not pretend away. Update this file when a drawback is
actually fixed, not when it's merely reworded.

## Generation quality

- **On-device image generation uses small, distilled models (SD-Turbo / LCM-class, ~4 steps).**
  These trade quality for speed and offline capability. Output can look soft, low-detail, or
  under-conditioned compared to a full-step cloud diffusion model — this is an expected
  characteristic of the model class, not necessarily a bug. If a generation looks unusually
  flat or near-featureless, it is worth comparing against a fresh run before assuming a defect;
  we do not yet have an automated way to distinguish "expected 4-step softness" from a real
  regression (e.g. an execution-provider numerical difference) on a specific device.
- **NNAPI/hardware acceleration is a partial, per-graph offload, not all-or-nothing.** On real
  devices, only a subset of a model's graph nodes may be assigned to NNAPI, with the rest
  falling back to CPU inside the same inference session. This is normal ONNX Runtime behavior,
  but it means generation quality and speed can differ between devices, and even between runs
  on the same device if drivers change — we have not built tooling to detect or report which
  execution provider actually served a given generation.
- **No committed, on-device latency/quality benchmark exists yet.** Every "fast" or "good
  quality" claim in this repo's docs is either a desktop CPU measurement or an estimate, not a
  number captured from a real phone and checked into the repo with a date and device model.

## Reliability

- **Fixed this session: a prompt could leak from the News/Chat headline tap into whichever
  studio tab (Image/Video/Code/Audio) was active.** `HomeScreen.openNewsChat()` and
  `VestraNavHost`'s `onOpenNewsChat` callback both wrote the tapped headline's text into
  `GenerativeViewModel.prompt` — a single `StateFlow` every studio tab reads — even though
  `NewsChatScreen` already fills its own separate local chat-input state with that same text and
  never reads `GenerativeViewModel.prompt` at all. Both writes were dead code whose only real
  effect was overwriting whatever the user had typed in the currently-bound studio. Fixed by
  deleting both; per-tab prompt isolation itself (`GenerativeViewModel.bindStudio`/`StudioBag`)
  was already correctly implemented — this was a leak from *outside* that mechanism, not a flaw
  in it. Regression-covered in `appium/test_prompt_isolation.py` (unexecuted — see Testability).
- **GPU delegate initialization can fail on some devices for the local LiteRT-LM models**
  (confirmed via a real user device log: a GPU-backend engine-load failure with no fallback).
  This session added an automatic CPU fallback (`LiteRtLmEngine.initialize()`) so a failed GPU
  init no longer permanently blocks loading — but CPU is slower, and a device whose GPU
  delegate fails will silently run slower than one whose GPU delegate works, with only a debug
  log line noting which backend actually loaded.
- **Cloud generation depends on free-tier Hugging Face Spaces / Inference Providers**, which can
  be slow to wake, rate-limited, or occasionally serve schema/route errors outside this app's
  control. Typed failure classification and model-health-aware routing exist to route around a
  known-bad model, but a systemic HF outage still degrades cloud generation across the board.
- **Video and audio generation lag image and code generation in maturity** — they were built to
  the same typed-failure/health-tracking pattern, but have had less real-device exercise this
  session than the image path (which had two real device-reported bugs fixed and verified this
  cycle).
- **D2 closed in 3.1.0, with a real bug found and fixed.** `AudioDspVerificationTest`
  (`shared/androidUnitTest`, JVM-real, not stubbed) verifies the *shipped*
  `AndroidLocalVoiceChanger.transform()` pipeline against synthetic tone fixtures: ±12 semitones
  measures the expected ~2x/~0.5x frequency shift (5% tolerance), extreme knobs never exceed the
  16-bit PCM range, and default knobs preserve pitch and sample count. Writing the speed-accuracy
  tests surfaced a genuine, previously-unnoticed bug: `applyPitchAndSpeed()` divided `readStep` by
  `speed` instead of multiplying it, so the "Speed" knob's effect was inverted — a 2.00× setting
  played audio *slower* (longer), a 0.50× setting played it *faster* (shorter), opposite of what
  the UI's label promised. Fixed; the fix was verified against all 6 new tests plus a full re-run
  of the 293-test `shared` suite and the full `composeApp` suite (both green afterward).
- **D1 closed in 3.1.0, compiled but unexecuted on device.**
  `composeApp/src/androidTest/kotlin/com/zakir/vestra/LiteRtLmOutputQualityTest.kt` runs three
  representative prompts (Kotlin quicksort, a StateFlow-vs-Flow explanation, a Jetpack Compose
  counter button) against the real installed Gemma 4 pack via `LiteRtLmEngine`, checking for
  genuinely code-shaped output (non-empty, substantive, no leaked `<think>` blocks, and
  prompt-appropriate markers like `fun`/`pivot`/`@Composable`/`remember`). It compiles cleanly
  (`:composeApp:compileSideloadDebugAndroidTestKotlin`) and follows `LiteRtLmBenchmarkTest`'s
  graceful-skip pattern when no pack is installed — but like the rest of this app's `androidTest`
  suite, it has never actually run: no Android device or emulator exists in this environment (see
  the Testability section below). Treat it as a real, will-fail-on-bad-output test that is simply
  unexecuted, not as evidence of measured output quality.
- **New voice-studio DSP device I/O is unverified on real hardware** (B6, 3.1.0-rc26). The
  signal-processing math — `PitchDetector`, `PitchMatcher`, `LatencyCalibrator`'s cross-correlation
  core, `SimpleFft` — is real and unit-tested against synthetic sine/chirp signals, not stubbed.
  What's untested is the Android I/O around it: `AndroidMicRecorder`'s new per-buffer RMS
  amplitude stream, and `AndroidLatencyCalibrator`'s simultaneous `AudioTrack` playback +
  `AudioRecord` capture, both added this session with no device available to confirm real-world
  timing behavior — same honesty posture as the GPU-delegate CPU fallback below.
- **ML Kit's face detector (B7, 3.1.0-rc27) is not exercised against a real photo with a real
  face in this environment.** `FaceBlurProcessor.detectFaces()` wraps ML Kit's bundled on-device
  detector correctly per its documented API, and `BoxBlur` (the actual pixel-blur math applied to
  each detected region) is unit-tested against real `Bitmap`s — but whether the detector itself
  finds faces reliably, at what confidence, and how it behaves on partial/angled/multiple faces
  is unverified: no device, and ML Kit's native detection model doesn't run meaningfully under
  Robolectric. Treat detection accuracy as unverified until tested on a real device with real
  photos.

## Design/UX parity with the reference app (lookbookweb)

- **`docs/plans/lovable-parity-local-first/PLAN.md` is DONE as of 3.1.0 (stable)** — every item
  A0–A3, B1–B8, D1–D2 is shipped; see the plan's own "Implementation status" table for the full
  per-item ledger. Landed: per-modality accent color tokens propagated across every studio surface
  (composer, results, tab row, model picker, voice knobs — not just the header label), a derived
  radius+spacing token scale, a press-lift micro-interaction and a reduced-motion-gated 3D tilt
  on cards, a bottom-dock navigation pattern (Home/Library/Create/Chat/Settings) that preserves
  per-tab session isolation by construction, confirmation that cloud generation is off by default
  everywhere (it was already correct; only misleading "Cloud by default" copy in the studio
  subtitle was fixed to reflect that), a resumable-job "interrupted" banner for local generations
  killed mid-run, correlation-ID-first error messages for local failures, a storage-used rollup +
  device-requirement checklist in Model Packs, a single honestly-labeled Processing Mode card
  replacing the old cloud switch, a shared shimmer-loading component wired into the one real gap
  found, and version lineage for local generations (retries in the same studio tab chain as a
  discoverable history), a voice-studio DSP layer — real, unit-tested pitch-detection/
  latency-estimation/FFT algorithms wired into a live RMS level meter, grouped voice personas,
  a "Match voice" pitch-matching chip, and a "Calibrate mic latency" chip (see Reliability below
  for what's unverified on real hardware there) — and a privacy-blur post-process: fully offline
  ML Kit face detection + a real box-blur, plus a manual drag-to-draw region tool, wired into a
  "Privacy blur" button on every image result. Part D's real-model output-quality testing for code
  (D1) and audio (D2) is also closed — see the D1/D2 entries above. **Still explicitly
  unfinished within an otherwise-done plan:** a live spectrum-scope data source (`SpectrumScope`
  is built and smoke-tested, but nothing feeds it real playback FFT data yet — recorded above as
  part of B6's device-I/O caveat, not a blocking gap). "Matches lookbookweb's design" now holds
  for typography, navigation pattern, interaction/UX patterns, generation-lifecycle UX, and
  color-identity direction, with the caveats above the only remaining honesty notes.
- **3.1.1 UI port from `zakir9622/GoogleLookBookUI`.** That repo turned out to be an earlier
  snapshot of this same codebase (same package, same architecture, frozen around v3.1.0-rc23),
  not a separate product — most of it was already behind 3.1.0. A real file-level diff found five
  genuinely additive pieces (richer chat bubbles/typing indicator/empty state/headlines bar,
  quick-prompt carousel, audio file import, a real on-device model status chip, and a "Create"
  tool-picker sheet) and ported them; see `CHANGELOG.md`'s 3.1.1 entry for the full list and 18
  new tests. **Deliberately not ported:** the source repo's `ModelConfigScreen.kt` shows
  per-provider connectivity "ping" status, but the check is fake — `delay(600)` then a random
  65–115ms number presented as a real measurement, exactly the kind of fabricated-status UI this
  file exists to flag rather than import quietly.
- **Closed in 3.1.2: a real connectivity check now exists, verified by real screenshots and 13
  new tests.** `ProviderConnectivityChecker` (`shared/commonMain`) makes an actual `GET` request
  per provider against the same hosts this app's real generation code calls, and a "Test
  [Provider] key" button in Settings → Cloud → API Keys shows the real result — a genuinely
  measured latency on success, or the real HTTP status meaning on failure. 10 tests
  (`ProviderConnectivityCheckerTest`) exercise every real response branch (200/401/403/429/5xx/
  thrown exception) against a mock HTTP engine — no live network calls in the test suite, but
  every branch matches an actual HTTP outcome the checker can hit for real. `ScreenshotTest` was
  also extended with 10 new real pixel renders (Robolectric `GraphicsMode.NATIVE`) covering every
  piece of the 3.1.1 port plus this screen — confirmed correct by direct visual inspection, not
  claimed from reading the code. **One remaining honesty note:** `ConnectivityTestRowTest` could
  not reliably assert on the *async-completed* click-to-result UI state within this
  environment's Robolectric Compose harness (a coroutine-scheduling/idle-detection limitation,
  same class as the one documented for `PrivacyBlurFlowTest`) — it verifies the UI renders and a
  tap drives the real code path without crashing, while the actual network-logic correctness is
  covered by the 10 mock-engine tests instead.

## Testability

- **Appium/UiAutomator visibility was added this session, not something the app shipped with.**
  `testTagsAsResourceId` is now enabled at the app root and the core generation flow — prompt
  input, model chip, assist toggle, send/stop, home tabs, every `GenerativeState` result card,
  the live generation console, retry/cancel, model-pack install/handshake buttons, and the
  model picker's per-model rows — now carry stable `testTag`s (see
  `composeApp/src/main/kotlin/com/zakir/vestra/ui/TestTags.kt`). Coverage is not yet
  exhaustive: Settings screens, Wardrobe/gallery browsing, and some secondary dialogs
  (report-content sheet, durable-storage prompt) do not yet carry tags. Extend `TestTags.kt`
  and its call sites incrementally as automation needs grow, rather than tagging speculatively
  ahead of an actual test.
- **A real Appium test suite now exists (`appium/`) but has never been run.** No Android device,
  emulator, or Appium server exists in the environment that authored it (verified directly: no
  `adb`, no `ANDROID_HOME`, no Appium binary) — every test in it is a first draft that needs a
  real run on a real device before it's trusted, per `appium/README.md`'s own honesty note. It
  covers: prompt isolation across studio tabs, local image/code/chat generation reaching a real
  terminal state, the image-edit/img2img entry point, and the Processing Mode card. Not yet
  covered: video/audio generation end-to-end, Model Packs install/handshake, Wardrobe history
  navigation, and anything about generation *quality* rather than "a result exists."

## Platform

- **iOS is not supported.** `shared/build.gradle.kts` does not declare an iOS target, and
  several commonMain files still call JVM-only APIs (e.g. `System.currentTimeMillis()`
  directly rather than through a `Clock` abstraction), so commonMain would not compile for iOS
  without further work.
- **`minSdk` is high (Android 15)** relative to some of the app's own documentation, which in
  places still describes broader device support. Treat any "works on Android 8+" style claim
  elsewhere in the docs as aspirational until the `minSdk` and the docs are reconciled.

## What "verified" means in this repo

Several rounds of this project's own audit history found status claims ("done", "fixed") that
did not hold up under direct code inspection — a UI control that changed no request payload, a
health tracker whose display function was never called, a benchmark harness with no actual
numbers. Nothing in this file should be read as more certain than what a `git grep`, a real
device run, or a passing test that fails on the old code can back up. When a drawback here is
closed, replace it with a one-line "closed in <version>, verified by <evidence>" note rather
than deleting it silently.
