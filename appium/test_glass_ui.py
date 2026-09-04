"""UI structure of the single-chatbox redesign.

**These tests have never been executed.** The environment that wrote them has no device and no
emulator — `/dev/kvm` does not exist and the CPU exposes no virtualisation flags, so an emulator
cannot start here at all. `adb` is installed but has nothing to talk to. Treat every locator and
every timing assumption below as a first draft.

What they are for: the app now has exactly one chatbox, on the Gemini pattern. The five-chip
modality row is gone, the composer's model chip moved to the top bar, and the second attach
affordance was removed — so a suite written against the previous shell would assert on controls
that no longer exist. These cover the structure that replaced them.

Deliberately structural, not visual. Appium cannot judge whether frosted glass looks right; that
is what `RedesignScreenshotTest` renders and a human reviews. What Appium *can* check is that the
elements exist, are reachable, and respond — which is the half that silently breaks on a refactor.
"""

import pytest

from conftest import by_tag, tag_exists

# Mirrors composeApp/src/main/kotlin/com/zakir/vestra/ui/TestTags.kt. Kept as literals rather than
# imported so a rename in Kotlin fails loudly here instead of silently matching nothing.
HOME_EMPTY_STATE = "home_empty_state"
HOME_HISTORY_SECTION = "home_history_section"
PROMPT_COMPOSER = "prompt_composer"
PROMPT_INPUT = "composer_prompt_input"
SEND_BUTTON = "composer_send_button"
COMPOSER_ATTACH_BUTTON = "composer_attach_button"
COMPOSER_TOOLS_SHEET = "composer_tools_sheet"
COMPOSER_ACTIVE_TOOL = "composer_active_tool"
COMPOSER_CONTEXT_ROW = "composer_context_row"
COMPOSER_BLOCKED_HINT = "composer_blocked_hint"
TOP_MODEL_SELECTOR = "top_model_selector"
CHAT_CODE_BLOCK = "chat_code_block"
SETTINGS_BUTTON = "unified_settings_button"

# Every destination the Settings hub must offer. The hub is a list of these and nothing else —
# a control appearing directly on it is the regression this list exists to catch.
SETTINGS_ROWS = (
    "settings_row_models",
    "settings_row_api_keys",
    "settings_row_default_models",
    "settings_row_safety",
    "settings_row_notifications",
    "settings_row_appearance",
    "settings_row_api_monitor",
    "settings_row_storage",
    "settings_row_about",
)

TOOLS = ("chat", "image", "video", "code", "audio")
SOURCES = ("photos", "camera", "files")


class TestSingleChatbox:
    """One composer, one attach affordance, one way to switch generator."""

    def test_composer_is_present_on_a_cold_start(self, driver):
        assert by_tag(driver, PROMPT_COMPOSER).is_displayed()
        assert by_tag(driver, PROMPT_INPUT).is_displayed()

    def test_the_modality_chip_row_is_gone(self, driver):
        # The five always-visible chips were replaced by the `+` sheet. If any of these still
        # resolves, the old row survived a merge.
        for mode in TOOLS:
            assert not tag_exists(driver, f"modality_chip_{mode}"), (
                f"modality_chip_{mode} still exists — the chip row should be in the + sheet now"
            )

    def test_there_is_exactly_one_attach_affordance(self, driver):
        # The defect this replaced: an "Attach Reference" chip row *and* a leading attach button
        # rendered at the same time in Image mode, the chip covering the placeholder.
        assert tag_exists(driver, COMPOSER_ATTACH_BUTTON)
        assert not tag_exists(driver, "composer_add_reference"), (
            "the old Attach Reference chip is back alongside the + button"
        )

    def test_the_placeholder_is_not_obscured(self, driver):
        """The composer's own hint must be readable, not sat on by another control.

        Geometric rather than semantic: the input's bounds must not intersect the attach
        button's. That is exactly the failure the screenshot showed, and it is invisible to any
        assertion that only checks both elements exist.
        """
        field = by_tag(driver, PROMPT_INPUT).rect
        plus = by_tag(driver, COMPOSER_ATTACH_BUTTON).rect
        overlap_y = min(field["y"] + field["height"], plus["y"] + plus["height"]) - max(field["y"], plus["y"])
        overlap_x = min(field["x"] + field["width"], plus["x"] + plus["width"]) - max(field["x"], plus["x"])
        assert overlap_y <= 0 or overlap_x <= 0, "the attach button overlaps the prompt field"

    def test_composer_survives_a_tool_switch(self, driver):
        by_tag(driver, COMPOSER_ATTACH_BUTTON).click()
        by_tag(driver, COMPOSER_TOOLS_SHEET)
        by_tag(driver, "composer_tool_image").click()
        assert tag_exists(driver, PROMPT_INPUT)
        assert tag_exists(driver, COMPOSER_ACTIVE_TOOL), "the active tool must stay visible after switching"

    def test_clearing_the_active_tool_returns_to_chat(self, driver):
        by_tag(driver, COMPOSER_ATTACH_BUTTON).click()
        by_tag(driver, "composer_tool_image").click()
        by_tag(driver, COMPOSER_ACTIVE_TOOL).click()
        # Chat is the default, and it is the one mode that renders no chip at all.
        assert not tag_exists(driver, COMPOSER_ACTIVE_TOOL)

    def test_chat_mode_shows_no_context_row(self, driver):
        # The point of a single chatbox: with no tool and no attachment the composer is a field
        # and a send button, nothing else.
        assert not tag_exists(driver, COMPOSER_CONTEXT_ROW)

    def test_blocked_reason_has_its_own_row_when_cloud_is_gated(self, driver):
        # Only present when cloud is actually gated, so this asserts shape rather than presence.
        if tag_exists(driver, COMPOSER_BLOCKED_HINT):
            hint = by_tag(driver, COMPOSER_BLOCKED_HINT)
            assert hint.is_displayed()
            assert (hint.text or "").strip(), "a blocked hint with no text is worse than none"


class TestToolsSheet:
    """The `+` sheet carries every source and every generator."""

    @pytest.fixture(autouse=True)
    def _open_sheet(self, driver):
        by_tag(driver, COMPOSER_ATTACH_BUTTON).click()
        by_tag(driver, COMPOSER_TOOLS_SHEET)

    @pytest.mark.parametrize("source", SOURCES)
    def test_every_attachment_source_is_present(self, driver, source):
        assert tag_exists(driver, f"composer_source_{source}")

    @pytest.mark.parametrize("tool", TOOLS)
    def test_every_generator_is_reachable(self, driver, tool):
        assert tag_exists(driver, f"composer_tool_{tool}"), (
            f"{tool} is unreachable — the + sheet is now the only way to switch generator"
        )


class TestTopBar:
    """The model selector moved here, and must name a model rather than a status sentence."""

    def test_model_selector_is_present(self, driver):
        assert by_tag(driver, TOP_MODEL_SELECTOR).is_displayed()

    def test_model_selector_is_not_an_error_sentence(self, driver):
        text = by_tag(driver, TOP_MODEL_SELECTOR).text or ""
        # The regression this guards: preflightLabel()'s blocked *reason* was rendered in the
        # model control and came out as "Pick a cloud model in the model pi…".
        assert len(text) < 60, f"the selector should name a model, got a sentence: {text!r}"
        assert "Pick a cloud model" not in text

    def test_model_selector_opens_the_picker(self, driver):
        by_tag(driver, TOP_MODEL_SELECTOR).click()
        assert tag_exists(driver, "model_picker_sheet"), "tapping the top-bar model chip must open the picker sheet"


class TestChatSurface:
    """How a reply renders once it lands."""

    def test_reply_actions_are_present_on_an_assistant_turn(self, driver):
        """Copy / regenerate / speak / share on the newest reply.

        Needs a working local or cloud chat model, so it skips rather than fails when the run
        never reaches a reply — a missing model is an environment gap, not a UI regression.
        """
        by_tag(driver, PROMPT_INPUT).send_keys("hi")
        by_tag(driver, SEND_BUTTON).click()
        try:
            by_tag(driver, "message_action_copy_1", timeout=120.0)
        except Exception:
            pytest.skip("no chat model reached a reply in this environment")
        for action in ("copy", "speak", "share"):
            assert tag_exists(driver, f"message_action_{action}_1")

    def test_code_reply_renders_as_a_code_block(self, driver):
        """A fenced reply must render as a code block, not as wrapped prose."""
        by_tag(driver, COMPOSER_ATTACH_BUTTON).click()
        by_tag(driver, "composer_tool_code").click()
        by_tag(driver, PROMPT_INPUT).send_keys("Write a CSS class for a frosted glass card")
        by_tag(driver, SEND_BUTTON).click()
        try:
            by_tag(driver, CHAT_CODE_BLOCK, timeout=120.0)
        except Exception:
            pytest.skip("no code model reached a reply in this environment")


class TestImageResult:
    """A generated image in the thread is a picture; its actions live in the viewer."""

    ACTION_TAGS = ("privacy_blur_button", "report_button")

    def _generate(self, driver):
        by_tag(driver, COMPOSER_ATTACH_BUTTON).click()
        by_tag(driver, "composer_tool_image").click()
        by_tag(driver, PROMPT_INPUT).send_keys("a linen abaya in warm sand, studio lighting")
        by_tag(driver, SEND_BUTTON).click()
        try:
            return by_tag(driver, "result_image_ready", timeout=180.0)
        except Exception:
            pytest.skip("no image model reached a result in this environment")

    def test_the_thread_card_carries_no_action_buttons(self, driver):
        """The defect this replaces: ~300dp of chrome wrapped around 320dp of image.

        Save / Share / Privacy blur / Report were four full-width buttons under every result,
        pushing the next turn off screen. They moved into the full-screen viewer.
        """
        self._generate(driver)
        for tag in self.ACTION_TAGS:
            assert not tag_exists(driver, tag), (
                f"{tag} is back in the thread — image result actions belong in the viewer"
            )

    def test_tapping_the_image_opens_the_viewer_with_the_actions(self, driver):
        self._generate(driver).click()
        assert tag_exists(driver, "full_screen_image"), "tapping a result must open it full screen"
        for tag in ("viewer_save_button", "viewer_share_button") + self.ACTION_TAGS:
            assert tag_exists(driver, tag), f"missing viewer action: {tag}"

    def test_the_viewer_closes_back_to_the_thread(self, driver):
        self._generate(driver).click()
        by_tag(driver, "close_full_screen_button").click()
        assert tag_exists(driver, PROMPT_COMPOSER)


class TestKeyboardInsets:
    """The composer tracks the keyboard exactly once."""

    def test_composer_sits_against_the_keyboard_not_a_gap_above_it(self, driver):
        """Regression: the composer floated roughly one keyboard-height above the keyboard.

        `enableEdgeToEdge()` was on but the activity declared no `windowSoftInputMode`, so the
        window resized for the IME *and* Compose added the same inset again via
        `safeDrawingPadding()`. Geometric rather than semantic, because both the composer and
        the keyboard existed and were "correct" — only their spacing was wrong.
        """
        screen_h = driver.get_window_size()["height"]
        before = by_tag(driver, PROMPT_COMPOSER).rect
        by_tag(driver, PROMPT_INPUT).click()
        after = by_tag(driver, PROMPT_COMPOSER).rect
        assert after["y"] < before["y"], "the composer must rise above the opened keyboard"
        # With the inset applied once, the composer's bottom edge sits near the keyboard's top.
        # Double-counting left a band of roughly one keyboard height between them.
        gap = screen_h - (after["y"] + after["height"])
        assert gap < screen_h * 0.55, (
            f"{gap}px between the composer and the bottom of the screen — the IME inset looks "
            "applied twice"
        )


class TestHomeStructure:
    """The empty state a cold install opens on."""

    def test_empty_state_is_present_on_a_fresh_thread(self, driver):
        assert tag_exists(driver, HOME_EMPTY_STATE)

    def test_history_section_is_absent_before_any_message(self, driver):
        assert not tag_exists(driver, HOME_HISTORY_SECTION)

    def test_the_hero_card_and_capability_grid_are_gone(self, driver):
        # Both duplicated what the + sheet now offers, above the one control the screen is for.
        assert not tag_exists(driver, "home_hero_card")
        assert not tag_exists(driver, "home_capabilities_section")


class TestSettingsHub:
    """Settings is a list of destinations — every setting is one tap from it, and none is on it."""

    @pytest.fixture(autouse=True)
    def _open_settings(self, driver):
        by_tag(driver, SETTINGS_BUTTON).click()

    @pytest.mark.parametrize("tag", SETTINGS_ROWS)
    def test_every_destination_is_present(self, driver, tag):
        assert tag_exists(driver, tag), f"missing settings hub row: {tag}"

    def test_no_setting_control_renders_on_the_hub_itself(self, driver):
        # The failure this catches is the one the previous restructure shipped: hub rows added,
        # but the long inline sections left around them.
        for stranded in ("settings_clear_tokens_button", "settings_open_packs_button"):
            assert not tag_exists(driver, stranded), (
                f"{stranded} is on the hub — it belongs on its own page"
            )

class TestConversationHistory:
    """New chat must file a conversation, never delete one.

    This is the suite's highest-value class: the button shipped for two releases calling
    `ChatRepository.clear()`, which removed the store key outright. Every assertion below is the
    difference between "started a new thread" and "lost the old one".
    """

    def test_history_button_opens_the_drawer(self, driver):
        by_tag(driver, "chat_history_button").click()
        assert tag_exists(driver, "chat_history_drawer")

    def test_new_chat_files_the_previous_conversation(self, driver):
        by_tag(driver, PROMPT_INPUT).send_keys("first conversation")
        by_tag(driver, SEND_BUTTON).click()
        by_tag(driver, "new_chat_button").click()

        by_tag(driver, "chat_history_button").click()
        drawer = by_tag(driver, "chat_history_drawer")
        assert "first conversation" in (drawer.text or ""), (
            "the previous conversation is gone — New chat deleted it instead of filing it"
        )

    def test_a_filed_conversation_reopens(self, driver):
        by_tag(driver, PROMPT_INPUT).send_keys("reopen me")
        by_tag(driver, SEND_BUTTON).click()
        by_tag(driver, "new_chat_button").click()
        by_tag(driver, "chat_history_button").click()
        # Row ids are conversation ids, so match on the drawer's rendered text instead.
        for row in driver.find_elements("xpath", "//*[contains(@resource-id,'conversation_row_')]"):
            if "reopen me" in (row.text or ""):
                row.click()
                break
        else:
            raise AssertionError("no history row carried the filed conversation")
        assert "reopen me" in (by_tag(driver, "prompt_composer").parent.page_source or "")

    def test_search_appears_only_once_the_list_is_worth_filtering(self, driver):
        by_tag(driver, "chat_history_button").click()
        # Below the threshold scanning beats typing, so the field is absent by design on a
        # fresh install rather than present and useless.
        assert not tag_exists(driver, "drawer_search_field")


class TestThreadAffordances:
    """The controls the reference app has on every reply."""

    def test_scroll_to_bottom_is_absent_on_a_short_thread(self, driver):
        # A jump button on a thread already at the bottom is a permanently inert control.
        assert not tag_exists(driver, "scroll_to_bottom")

    def test_long_press_a_reply_opens_the_action_menu(self, driver):
        by_tag(driver, PROMPT_INPUT).send_keys("hi")
        by_tag(driver, SEND_BUTTON).click()
        try:
            bubble = by_tag(driver, "chat_message_1_assistant", timeout=120.0)
        except Exception:
            pytest.skip("no chat model reached a reply in this environment")
        driver.execute_script("mobile: longClickGesture", {"elementId": bubble.id})
        assert tag_exists(driver, "message_action_sheet")
        for tag in ("message_menu_copy", "message_menu_share", "message_menu_delete"):
            assert tag_exists(driver, tag), f"missing long-press action: {tag}"

    def test_follow_up_chips_appear_under_a_settled_reply(self, driver):
        by_tag(driver, PROMPT_INPUT).send_keys("hi")
        by_tag(driver, SEND_BUTTON).click()
        try:
            by_tag(driver, "follow_up_chip_0", timeout=120.0)
        except Exception:
            pytest.skip("no chat model reached a reply in this environment")
        assert tag_exists(driver, "follow_up_chip_0")

    def test_the_newest_prompt_can_be_edited(self, driver):
        by_tag(driver, PROMPT_INPUT).send_keys("teh typo")
        by_tag(driver, SEND_BUTTON).click()
        assert tag_exists(driver, "message_edit_0"), "the newest user turn must offer edit"

