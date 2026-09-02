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

    // Chat (now the unified main screen's default composer mode, UnifiedMainScreen.kt) —
    // on-device by default (cloud models are hidden from the picker until the global cloud
    // toggle is on), so Appium can drive the whole "type a prompt, get a local reply" loop
    // without ever touching a network call.
    const val CHAT_REFRESH_BUTTON = "chat_news_refresh"
    fun chatHeadlineCard(index: Int): String = "chat_headline_$index"
    fun chatMessageBubble(index: Int, role: String): String = "chat_message_${index}_$role"
    const val CHAT_TYPING_INDICATOR = "chat_typing_indicator"
    // "Remembering N things" header pill (ChatComponents.kt) — A4.2/A5, Part B.1's memory count.
    const val CHAT_MEMORY_PILL = "chat_memory_pill"
    const val CHAT_EMPTY_STATE = "chat_empty_state"
    fun chatStarterPrompt(index: Int): String = "chat_starter_prompt_$index"
    const val QUICK_PROMPT_CAROUSEL = "quick_prompt_carousel"
    fun quickPromptChip(index: Int): String = "quick_prompt_chip_$index"

    // Interrupted-job banner (InterruptedJobsBanner.kt) — a local run still RUNNING/QUEUED from
    // a previous app process, surfaced on Home rather than silently lost.
    const val INTERRUPTED_JOBS_BANNER = "interrupted_jobs_banner"
    fun interruptedJobDismiss(jobId: String): String = "interrupted_job_dismiss_$jobId"

    // Wardrobe look-detail dialog's version-history row (WardrobeScreen.kt).
    fun wardrobeHistoryRow(entryId: String): String = "wardrobe_history_row_$entryId"

    // Wardrobe gallery grid (WardrobeScreen.kt) — per-entry tap target and row actions.
    const val WARDROBE_FILTER_ALL = "wardrobe_filter_all"
    const val WARDROBE_FILTER_FAVORITES = "wardrobe_filter_favorites"
    // Media-type filter row (A4.4) — independent of the favorites filter above; the two
    // compose (e.g. "Favorites" + "Videos" shows only favorited video looks).
    const val WARDROBE_FILTER_TYPE_ALL = "wardrobe_filter_type_all"
    const val WARDROBE_FILTER_TYPE_IMAGES = "wardrobe_filter_type_images"
    const val WARDROBE_FILTER_TYPE_VIDEOS = "wardrobe_filter_type_videos"
    fun wardrobeGalleryItem(entryId: String): String = "wardrobe_gallery_item_$entryId"
    fun wardrobeFavoriteButton(entryId: String): String = "wardrobe_favorite_$entryId"
    fun wardrobeDeleteButton(entryId: String): String = "wardrobe_delete_$entryId"
    const val WARDROBE_DELETE_CONFIRM = "wardrobe_delete_confirm"
    const val WARDROBE_DELETE_CANCEL = "wardrobe_delete_cancel"

    // Unified main screen (UnifiedMainScreen.kt) — top-right icons, no bottom dock any more.
    const val UNIFIED_LIBRARY_BUTTON = "unified_library_button"
    const val UNIFIED_SETTINGS_BUTTON = "unified_settings_button"
    const val TOP_MODEL_SELECTOR = "top_model_selector"
    fun modalityChip(id: String): String = "modality_chip_$id"

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

    // Safety-preset confirm-before-generate dialog (UnifiedMainScreen.kt) — shown only for
    // presets with SafetyPreset.confirm = true (Blur identities, Redact details).
    const val SAFETY_PRESET_CONFIRM_GENERATE = "safety_preset_confirm_generate"
    const val SAFETY_PRESET_CONFIRM_CANCEL = "safety_preset_confirm_cancel"

    // Live context-budget indicator above the chat composer (ChatComponents.kt) — Part B.2.
    const val CONTEXT_BUDGET_BAR = "context_budget_bar"

    const val ANALYZE_REFERENCE_SWITCH = "analyze_reference_switch"
    const val MATURE_FASHION_ASSIST_SWITCH = "mature_fashion_assist_switch"
    const val PROMPT_CLARITY_ASSIST_SWITCH = "prompt_clarity_assist_switch"

    // "What the assistant remembers" panel (SettingsMemorySection.kt) — Part B.1.
    const val MEMORY_ENABLED_SWITCH = "memory_enabled_switch"
    const val MEMORY_EMPTY_STATE = "memory_empty_state"
    const val MEMORY_CLEAR_ALL_BUTTON = "memory_clear_all_button"
    fun memoryFactRow(id: String): String = "memory_fact_row_$id"
    fun memoryFactRemove(id: String): String = "memory_fact_remove_$id"

    // Changelog screen (ChangelogScreen.kt) — Part A4.10.
    fun changelogRelease(version: String): String = "changelog_release_${version.replace(' ', '_')}"

    // Top-center glass snackbar (GlassSnackbar.kt) — A7, replaces bottom-anchored Toast.
    const val GLASS_SNACKBAR = "glass_snackbar"

    // API Usage and Token Monitoring Dashboard Card (ApiUsageDashboardCard.kt)
    const val API_USAGE_DASHBOARD_CARD = "api_usage_dashboard_card"
    const val API_USAGE_TOGGLE_BUTTON = "api_usage_toggle_button"
    const val API_USAGE_EXPAND_BUTTON = "api_usage_expand_button"
    const val API_USAGE_CLEAR_HISTORY_BUTTON = "api_usage_clear_history_button"
    const val API_USAGE_SETTINGS_BUTTON = "api_usage_settings_button"
    fun apiUsageServiceCard(serviceKey: String): String = "api_usage_service_$serviceKey"

    // Composer blocked-reason hint (PromptComposer.kt) — the slot that used to be crammed
    // into the model chip, where it rendered as "Pick a cloud model in the model pi…".
    const val COMPOSER_BLOCKED_HINT = "composer_blocked_hint"

    // Home empty state (UnifiedMainScreen.kt) — replaced the pinned usage dashboard.
    const val HOME_EMPTY_STATE = "home_empty_state"
    fun homeSuggestion(index: Int): String = "home_suggestion_$index"

    // Settings hub navigation rows (SettingsScreen.kt).
    const val SETTINGS_ROW_MODELS = "settings_row_models"
    const val SETTINGS_ROW_DEFAULT_MODELS = "settings_row_default_models"
    const val SETTINGS_ROW_NOTIFICATIONS = "settings_row_notifications"
    const val SETTINGS_ROW_API_MONITOR = "settings_row_api_monitor"

    // Models screen (ModelsScreen.kt) and per-provider detail (ProviderModelsScreen.kt).
    const val MODELS_ON_DEVICE_SECTION = "models_on_device_section"
    const val MODELS_CLOUD_SECTION = "models_cloud_section"
    fun modelsProviderRow(platform: String): String = "models_provider_row_$platform"
    const val PROVIDER_TOKEN_FIELD = "provider_token_field"
    const val PROVIDER_TOKEN_SAVE = "provider_token_save"
    const val PROVIDER_REFRESH_MODELS = "provider_refresh_models"
    const val PROVIDER_MODEL_LIST = "provider_model_list"
    fun providerModelRow(id: String): String = "provider_model_row_$id"

    // Default-model-per-modality screen (DefaultModelsScreen.kt).
    fun defaultModelRow(capability: String): String = "default_model_row_$capability"

    // Glass UI kit (GlassUiKit.kt) and the home thread. The greeting header, hero card and
    // capability tiles that lived here were removed with the one-chatbox redesign: they offered
    // the same generators the composer's + sheet does, from a second place.
    const val HOME_HISTORY_SECTION = "home_history_section"
    fun homeHistoryRow(index: Int): String = "home_history_row_$index"
    const val CHAT_CODE_BLOCK = "chat_code_block"
    const val COMPOSER_ATTACH_BUTTON = "composer_attach_button"

    // The single chatbox (PromptComposer.kt) and its `+` sheet (ComposerToolsSheet.kt). The
    // modality chip row these replaced is gone; `modalityChip` above is retained only because
    // older Appium specs still reference it and must fail loudly rather than match nothing.
    const val PROMPT_COMPOSER = "prompt_composer"
    const val COMPOSER_CONTEXT_ROW = "composer_context_row"
    const val COMPOSER_ACTIVE_TOOL = "composer_active_tool"
    const val COMPOSER_CLEAR_BUTTON = "composer_clear_button"
    const val COMPOSER_MIC_BUTTON = "composer_mic_button"
    const val COMPOSER_TOOLS_SHEET = "composer_tools_sheet"
    const val NEW_CHAT_BUTTON = "new_chat_button"
    fun composerSource(id: String): String = "composer_source_$id"
    fun composerTool(id: String): String = "composer_tool_$id"


    // Assistant reply actions (ChatComponents.kt): copy, regenerate, speak, share.
    fun messageAction(action: String, index: Int): String = "message_action_${action}_$index"

    // Further settings sub-pages, so the hub is a list of destinations rather than a long scroll.
    const val SETTINGS_ROW_API_KEYS = "settings_row_api_keys"
    const val SETTINGS_ROW_SAFETY = "settings_row_safety"
    const val SETTINGS_ROW_APPEARANCE = "settings_row_appearance"
    const val SETTINGS_ROW_STORAGE = "settings_row_storage"
    const val SETTINGS_ROW_MEMORY = "settings_row_memory"
    const val SETTINGS_ROW_ABOUT = "settings_row_about"

    // Notifications screen (NotificationsScreen.kt).
    const val NOTIFICATIONS_PERMISSION_CARD = "notifications_permission_card"
    const val NOTIFY_GENERATION_COMPLETE_SWITCH = "notify_generation_complete_switch"
    const val NOTIFY_GENERATION_FAILED_SWITCH = "notify_generation_failed_switch"
    const val NOTIFY_PACK_DOWNLOAD_SWITCH = "notify_pack_download_switch"
    const val NOTIFICATIONS_SYSTEM_SETTINGS_BUTTON = "notifications_system_settings_button"
}
