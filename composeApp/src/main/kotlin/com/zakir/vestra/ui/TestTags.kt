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
    /** The attached reference-image thumbnail — tap to clear it. Drives image-edit/img2img flows. */
    const val REFERENCE_IMAGE_THUMB = "composer_reference_thumb"

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

    // News & Chat window (NewsChatScreen.kt) — on-device by default (cloud models are hidden
    // from this screen's picker until the global cloud toggle is on), so Appium can drive the
    // whole "type a prompt, get a local reply" loop without ever touching a network call.
    const val CHAT_REFRESH_BUTTON = "chat_news_refresh"
    fun chatHeadlineCard(index: Int): String = "chat_headline_$index"
    fun chatMessageBubble(index: Int, role: String): String = "chat_message_${index}_$role"
    const val CHAT_TYPING_INDICATOR = "chat_typing_indicator"
    const val CHAT_EMPTY_STATE = "chat_empty_state"
    fun chatStarterPrompt(index: Int): String = "chat_starter_prompt_$index"
    const val QUICK_PROMPT_CAROUSEL = "quick_prompt_carousel"
    fun quickPromptChip(index: Int): String = "quick_prompt_chip_$index"

    // Audio Studio (AudioStudioPane.kt) — import an existing audio file from device storage.
    const val AUDIO_IMPORT_BUTTON = "audio_import_button"

    // Processing mode card (SettingsCloudSection.kt) — the on-device-only / cloud-allowed choice.
    const val PROCESSING_MODE_LOCAL = "processing_mode_local"
    const val PROCESSING_MODE_CLOUD = "processing_mode_cloud"

    // Interrupted-job banner (InterruptedJobsBanner.kt) — a local run still RUNNING/QUEUED from
    // a previous app process, surfaced on Home rather than silently lost.
    const val INTERRUPTED_JOBS_BANNER = "interrupted_jobs_banner"
    fun interruptedJobDismiss(jobId: String): String = "interrupted_job_dismiss_$jobId"

    // Wardrobe look-detail dialog's version-history row (WardrobeScreen.kt).
    fun wardrobeHistoryRow(entryId: String): String = "wardrobe_history_row_$entryId"

    // Wardrobe gallery grid (WardrobeScreen.kt) — per-entry tap target and row actions.
    const val WARDROBE_FILTER_ALL = "wardrobe_filter_all"
    const val WARDROBE_FILTER_FAVORITES = "wardrobe_filter_favorites"
    fun wardrobeGalleryItem(entryId: String): String = "wardrobe_gallery_item_$entryId"
    fun wardrobeFavoriteButton(entryId: String): String = "wardrobe_favorite_$entryId"
    fun wardrobeDeleteButton(entryId: String): String = "wardrobe_delete_$entryId"
    const val WARDROBE_DELETE_CONFIRM = "wardrobe_delete_confirm"
    const val WARDROBE_DELETE_CANCEL = "wardrobe_delete_cancel"

    // Bottom dock navigation (LookbookBottomBar.kt) — Home/Library/Create/Chat/Settings.
    const val BOTTOM_BAR = "bottom_bar"
    const val BOTTOM_BAR_HOME = "bottom_bar_home"
    const val BOTTOM_BAR_LIBRARY = "bottom_bar_library"
    const val BOTTOM_BAR_CREATE = "bottom_bar_create"
    const val BOTTOM_BAR_CHAT = "bottom_bar_chat"
    const val BOTTOM_BAR_SETTINGS = "bottom_bar_settings"

    // Privacy blur post-process (PrivacyBlurSheet.kt, RegionBlurOverlay.kt) — B7.
    const val PRIVACY_BLUR_BUTTON = "privacy_blur_button"
    const val PRIVACY_BLUR_TOGGLE = "privacy_blur_toggle"
    const val PRIVACY_BLUR_APPLY = "privacy_blur_apply"
    const val PRIVACY_BLUR_CANVAS = "privacy_blur_canvas"
    const val PRIVACY_BLUR_SAVE_ORIGINAL = "privacy_blur_save_original"

    // Report-content dialog (ResultPane.kt) — flag a result as inappropriate; stored locally only.
    const val REPORT_BUTTON = "report_button"
    fun reportReason(reason: String): String = "report_reason_$reason"
    const val REPORT_CANCEL_BUTTON = "report_cancel_button"

    // Durable-storage prompt (PacksScreen.kt) — requests all-files access so multi-GB packs
    // survive uninstall/reinstall in Documents/TheLookbook rather than app-private storage.
    const val DURABLE_STORAGE_ENABLE_BUTTON = "durable_storage_enable_button"

    // Settings screen (SettingsScreen.kt) — destructive/confirm actions.
    const val SETTINGS_CLEAR_TOKENS_BUTTON = "settings_clear_tokens_button"
    const val SETTINGS_CLEAR_TOKENS_CONFIRM = "settings_clear_tokens_confirm"
    const val SETTINGS_CLEAR_TOKENS_CANCEL = "settings_clear_tokens_cancel"

    // Settings → Engines "All packs" entry point (SettingsEnginesSection.kt) — the deterministic
    // route into PacksScreen for automation (Code studio's LiteRtStatusIndicator also opens it,
    // but only when a pack isn't installed).
    const val SETTINGS_OPEN_PACKS_BUTTON = "settings_open_packs_button"

    // Cloud connectivity test buttons (SettingsCloudSection.kt) — 3.1.2.
    fun connectivityTestButton(provider: String): String = "connectivity_test_$provider"

    // Prompt-level safety preset picker (SettingsSafetySection.kt) — Part B.3.
    fun safetyPreset(id: String): String = "safety_preset_$id"

    // Live context-budget indicator above the chat composer (ChatComponents.kt) — Part B.2.
    const val CONTEXT_BUDGET_BAR = "context_budget_bar"
}
