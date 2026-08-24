package com.zakir.vestra.ui

/**
 * Stable element identifiers for UI automation (Appium/UiAutomator, Espresso, and Compose UI
 * tests) across the generation flow — prompt input, model selection, generate/stop, live
 * progress, and each result type. Centralized here so tags stay unique and typo-free instead of
 * scattered as magic strings at each call site.
 *
 * Compose's `Modifier.testTag` is only visible to Compose UI tests by default; UiAutomator (and
 * therefore Appium's UiAutomator2 driver) only sees a tag once the app opts in via
 * `testTagsAsResourceId = true` (set once, at the root — see `MainActivity.kt`). Without that
 * flag every tag below exists in the semantics tree but is invisible to Appium.
 */
object TestTags {
    // Studio composer (PromptComposer.kt) — shared across Image/Video/Audio/Code.
    const val PROMPT_INPUT = "composer_prompt_input"
    const val MODEL_CHIP = "composer_model_chip"
    const val ASSIST_CHIP = "composer_assist_chip"
    const val SEND_BUTTON = "composer_send_button"
    const val ADD_REFERENCE_BUTTON = "composer_add_reference"

    // Home tab navigation (HomeScreen.kt) — one per HomeTab.routeKey.
    fun homeTab(routeKey: String): String = "home_tab_$routeKey"

    // Generation result / live response (ResultPane.kt, LiveGenConsole.kt).
    const val LIVE_CONSOLE = "result_live_console"
    const val RESULT_IMAGE_READY = "result_image_ready"
    const val RESULT_VIDEO_READY = "result_video_ready"
    const val RESULT_AUDIO_READY = "result_audio_ready"
    const val RESULT_CODE_STREAMING = "result_code_streaming"
    const val RESULT_CODE_READY = "result_code_ready"
    const val RESULT_TRANSCRIBE_READY = "result_transcribe_ready"
    const val RESULT_FAILED = "result_failed"
    const val RESULT_RETRY_BUTTON = "result_retry_button"
    const val RESULT_CANCEL_BUTTON = "result_cancel_button"

    // Model packs (PacksScreen.kt) — per-pack tags parameterized by pack id.
    fun packInstallButton(packId: String): String = "pack_install_$packId"
    fun packHandshakeButton(packId: String): String = "pack_handshake_$packId"

    // Model picker sheet (ModelPickerSheet.kt) — per-model-id row, cloud and on-device.
    fun modelPickerRow(modelId: String): String = "model_picker_row_$modelId"
}
