# Appium test suite — The Lookbook

Real, executable Appium (UiAutomator2) tests against `com.zakir.vestra`, written against the
stable `testTag`s catalogued in `composeApp/src/main/kotlin/com/zakir/vestra/ui/TestTags.kt`.

**Honesty note, not a formality:** these tests have never been run — including the newest file.
Writing tests you cannot execute is not the same as verifying them; treat every test here as a
first draft that needs a real run on a real device before it's trusted.

The blocker has been re-checked, not assumed. An Android SDK *is* installed now
(`ANDROID_HOME=/root/android-sdk`, with `platform-tools/adb`), so the earlier "no `adb`" note is
out of date. What is still missing is anything for `adb` to talk to: **`/dev/kvm` does not exist
and `/proc/cpuinfo` reports no `vmx`/`svm` flags**, so an emulator cannot start in that
environment at any speed, and no physical device is attached. The suite is one `appium` install
and one connected device away from running; it is not one command away.

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
