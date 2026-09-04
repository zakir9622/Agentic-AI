# Appium test suite — The Lookbook

Real, executable Appium (UiAutomator2) tests against `com.zakir.vestra`, written against the
stable `testTag`s catalogued in `composeApp/src/main/kotlin/com/zakir/vestra/ui/TestTags.kt`.

## These tests have now been run — and most of them fail on stale locators

The long-standing "never executed" note above this section was based on a wrong conclusion, and
the correction is worth stating plainly. `/dev/kvm` really is absent and `/proc/cpuinfo` really
does report no `vmx`/`svm` flags — but that rules out *hardware-accelerated* emulation, not
emulation. QEMU's TCG interpreter needs neither, ships with the SDK, and runs Android fine, just
slowly. The suite executed end-to-end for the first time under it: **46 tests collected, with
real passes and real failures.**

The failures were overwhelmingly this suite's fault, not the app's, and they have now been
repaired. **The locators are fixed; the count in the first write-up of this (28) was wrong.**
That number came from a script that compared test literals against `const val` declarations only,
so every tag built by a helper — `followUpChip(0)`, `chatMessageBubble(1, "assistant")`,
`wardrobeGalleryItem(id)`, `composerTool(mode)` and a dozen more — was counted as missing when it
was not. The real figure was **15**, and two of those (`home_hero_card`,
`home_capabilities_section`) were never broken at all: they appear in `assert not tag_exists(...)`
checks whose entire purpose is that the control is gone.

What was genuinely broken, and what replaced it:

| Was | Now |
|---|---|
| `home_tab_{image,video,code,audio}` | `select_tool(driver, mode)` — `+` sheet -> `composer_tool_<mode>` |
| `bottom_bar_{home,chat}` | one unified screen; `select_tool(driver, "chat")` clears the tool |
| `bottom_bar_settings` / `bottom_bar_library` | `open_settings(driver)` / `open_library(driver)` |
| `prompt_input` / `send_button` | `composer_prompt_input` / `composer_send_button` |
| `model_picker_sheet` | now exists — the sheet's rows were tagged but the sheet was not |

Four files each had their own `_goto_tab` helper clicking a dead tag, which is why they failed on
the first click rather than on anything they meant to assert. Navigation now lives once in
`conftest.py`, so the next redesign breaks one place instead of four.

Running the suite also exposed the reverse drift: **seven tags existed in the UI as bare string
literals**, written inline at the call site and never recorded here — `full_screen_image`,
`close_full_screen_button`, the three `viewer_*` buttons, `wardrobe_search` and
`litert_status_indicator`. This file claims to be the single catalogue of test tags; it was not.
They are constants now, and every `testTag(...)` in `composeApp/ui/` resolves through
`TestTags.kt`.

## Running it on a machine without KVM

The four things that make the difference, in order — omit the third and the app is ANR-killed at
startup every single time:

```bash
export ANDROID_HOME=/root/android-sdk
sdkmanager "emulator" "platform-tools" "system-images;android-35;aosp_atd;x86_64"
avdmanager create avd -n lookbook -k "system-images;android-35;aosp_atd;x86_64" -d pixel_5

# 1. TCG software emulation — no KVM required. Budget ~5-8 min to boot.
$ANDROID_HOME/emulator/emulator -avd lookbook -no-accel -no-window -no-audio \
    -no-boot-anim -no-snapshot -gpu swiftshader_indirect -memory 3072 -cores 4 &

# 2. AOT-compile the app: interpreted class verification costs 180-500ms *per method* here.
adb install -r -g composeApp/build/outputs/apk/sideload/debug/composeApp-sideload-debug.apk
adb shell cmd package compile -m speed -f com.zakir.vestra

# 3. Scale Android's own watchdogs. The process-start timeout is 10s; a cold start of this app
#    measures ~97s under TCG, so without this every launch ends in
#    "ANR ... failed to complete startup". The emulator rejects this via -prop (qemu.* only),
#    so set it at runtime — a ro. property can be written once while unset — then restart the
#    framework, which re-reads it. `stop`/`start` is a framework restart, not a reboot, and the
#    property survives it.
adb shell setprop ro.hw_timeout_multiplier 20
adb shell stop && adb shell start

# 4. Pre-install the UiAutomator2 server; installing it per session overruns adb's timeout.
adb install -r -g /root/.appium/node_modules/appium-uiautomator2-driver/node_modules/\
appium-uiautomator2-server/apks/appium-uiautomator2-server-v10.6.2.apk

appium --address 127.0.0.1 --port 4723 &
APPIUM_DEVICE_NAME=emulator-5554 python3 -m pytest test_glass_ui.py -v
```

`conftest.py` raises Appium's timeouts to match (see `APPIUM_*_TIMEOUT_MS` there). Those are
ceilings rather than waits, so they cost a real device nothing and are left on by default.

The margins are thin, not generous. A five-minute `uiautomator2ServerLaunchTimeout` was enough
for one session and then failed on the next — *on the same emulator, with the server already
installed* — as "the instrumentation process cannot be initialized within 300000ms". It is 15
minutes now. If a session dies during startup here, raise the ceiling before suspecting the app:
under TCG, slow and broken look identical from the outside.

**Do not use an ATD system image.** `aosp_atd` boots fastest and was the first choice here, and
under it the app held focus, ran, and drew *nothing* — a screenshot came back as a single colour
across all 2,527,200 pixels. ATD images are stripped for headless automation and are not a
surface an app renders on. Use `system-images;android-35;default;x86_64`.

That failure is worth understanding even if you never use ATD, because of how it presented: the
suite reported **22 failed, 6 passed** — and five of those six passes were
`assert not tag_exists(...)` checks. "The modality chip row is gone" is satisfied exactly as well
by a blank screen as by correct UI. A blank app does not fail a negative assertion; it flatters
it. `conftest.py` now checks for a positive anchor (`prompt_composer`) before every test and
stops the run when the app is in front but has drawn nothing, because a run that reports passes
while the app is blank is worse than one that reports nothing — the passes get quoted.

**What this setup can and cannot tell you.** It exercises real Android: real view hierarchy, real
touch dispatch, real lifecycle. It is far too slow to gate CI on — one test file took 39 minutes,
and there are six — and its timing is nothing like a phone's, so a *timing*-dependent failure here
is not evidence of a bug on real hardware. Treat it as a one-off "does this actually work"
check. For pre-release confidence on real devices, Firebase Test Lab or a Play pre-launch report
against the APK that CI already builds remains the better instrument.

## What's covered

| File | What it actually asserts |
|---|---|
| `test_prompt_isolation.py` | Prompts stay isolated per studio tab (Image/Video/Code/Audio); direct regression test for a real bug found and fixed this session — tapping a News headline used to overwrite whatever prompt was typed in the currently-active studio tab. **Stale as of the unified-screen redesign** — it still references `bottom_bar_chat`/`bottom_bar_home`, which no longer exist; needs a rewrite against the modality chips on the unified main screen. |
| `test_generation_flows.py` | Local image generation, local code generation (streaming → ready), local chat reach a genuine terminal state — a real result or a legible failure, never a hang or a raw stack trace. Also references `bottom_bar_chat` and needs the same update. |
| `test_image_edit.py` | The image-to-image/edit entry point (attach a reference photo on the Image tab) actually works: attach → thumbnail appears → clear → thumbnail disappears → generation with a reference reaches a terminal state. |
| `test_glass_ui.py` | **Current with the one-chatbox redesign.** Structure of the single composer: one attach affordance and no second one, the prompt field's bounds *not intersecting* the attach button's (the exact defect the previous build shipped), no modality chip row, the `+` sheet carrying every source and every generator, the active-tool chip appearing and clearing, the top-bar model selector naming a model rather than a blocked-reason sentence, an assistant turn's copy/speak/share actions, a fenced reply rendering as a code block, and all nine Settings hub rows with no setting control on the hub itself. Structural only — whether the glass *looks* right is what `RedesignScreenshotTest` renders and a human reviews. |

**Removed:** `test_bottom_bar.py` — the bottom dock it tested (Home/Library/Settings tabs) no longer
exists; the app now opens directly into one unified screen with Library/Settings reached via two
top-right icons instead (`unified_library_button`/`unified_settings_button`). `test_wardrobe.py`
and `test_model_packs.py` also reference now-removed `bottom_bar_*` tags and need the same pass —
none of this has been re-verified on a device yet.

**Stale as of the one-chatbox redesign.** Three tag families moved or disappeared:

- `modality_chip_<mode>` — **gone.** The five-chip row was replaced by the `+` sheet;
  switching generator is now `composer_attach_button` → `composer_tool_<mode>`.
- `composer_model_chip` — **gone.** Model selection moved to `top_model_selector` in the top bar.
  (That tag existed, was removed in the professional-UI pass, and is back in its original place.)
- `composer_add_reference` — **gone**, and its absence is now asserted: it was the second attach
  affordance that overlapped the placeholder. Attachment is `composer_attach_button` →
  `composer_source_{photos,camera,files}`.

`test_image_edit.py` drives the attach flow and needs rewriting against those two source tags
rather than the removed chip. `test_prompt_isolation.py` and `test_generation_flows.py` still
reference `bottom_bar_*` on top of that.

Settings is a hub of nine rows now, so anything reaching a setting must tap through it:
`settings_row_models` (then `models_provider_row_<PLATFORM>`), `settings_row_api_keys`,
`settings_row_default_models`, `settings_row_safety`, `settings_row_notifications`,
`settings_row_appearance`, `settings_row_api_monitor`, `settings_row_storage`,
`settings_row_about`. The flat scroll those tests assumed is gone, and so is the half-migrated
version of it that kept API keys and safety inline.

Other tags worth covering: `prompt_composer`, `composer_context_row`, `composer_active_tool`,
`composer_mic_button`, `new_chat_button`, `message_action_<action>_<index>`,
`provider_refresh_models` and `provider_model_row_<id>`.

## What a run on this emulator can and cannot prove

Measured on the AOSP emulator this harness was brought up on, so nobody has to re-derive it
from a failing test:

| Capability | Result | Why |
|---|---|---|
| Local image / video / code | legible refusal | no model packs on device (`files/packs/` empty) |
| Cloud, any capability | legible refusal | emulator has **no network** — only `lo` and `dummy0`, no default route |
| Audio | legible refusal | the image ships **no TTS engine**: no tts packages, `TTS_SERVICE` resolves to no services, `tts_default_synth` is null |

All four refusals name the specific model and the action to take, e.g.

    Pick a cloud model in the model picker, or add a free API key in Settings,
    to use FLUX.1 Schnell — nothing is sent to the network until you do.

No stack traces, no raw exceptions. That is the app behaving correctly, and it is the *only*
thing this environment can establish. `test_generation_flows.py` still reports these as
failures by design — the tests exist to verify generation *succeeds*, and they say so in their
failure text rather than passing on a refusal.

So: five red tests here is the expected, correct result, and it is not evidence of a defect.
To actually verify generation you need a device with model packs installed and real network —
Firebase Test Lab or a physical phone. Do not read a green run here as "generation works",
because there is no configuration of this emulator in which it could.
