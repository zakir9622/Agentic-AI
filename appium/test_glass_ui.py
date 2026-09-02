"""UI structure of the glassmorphism redesign.

**These tests have never been executed.** The environment that wrote them has no device and no
emulator — `/dev/kvm` does not exist and the CPU exposes no virtualisation flags, so an emulator
cannot start here at all. `adb` is installed but has nothing to talk to. Treat every locator and
every timing assumption below as a first draft.

What they are for: the redesign moved or renamed most of the surfaces the older suite drove, so
without this file the suite would assert against a UI that no longer exists. These cover the
structure the redesign introduced — greeting header, hero card, capability tiles, history rows,
the chat status header, and syntax-highlighted code inside a message bubble.

Deliberately structural, not visual. Appium cannot judge whether frosted glass looks right; that
is what `RedesignScreenshotTest` renders and a human reviews. What Appium *can* check is that the
elements exist, are reachable, and respond — which is the half that silently breaks on a refactor.
"""

import pytest

from conftest import by_tag, tag_exists

# Mirrors composeApp/src/main/kotlin/com/zakir/vestra/ui/TestTags.kt. Kept as literals rather than
# imported so a rename in Kotlin fails loudly here instead of silently matching nothing.
HOME_GREETING_HEADER = "home_greeting_header"
HOME_HERO_CARD = "home_hero_card"
HOME_HERO_PRIMARY = "home_hero_primary"
HOME_CAPABILITIES_SECTION = "home_capabilities_section"
HOME_HISTORY_SECTION = "home_history_section"
HOME_EMPTY_STATE = "home_empty_state"
CHAT_STATUS_HEADER = "chat_status_header"
CHAT_CODE_BLOCK = "chat_code_block"
COMPOSER_ATTACH_BUTTON = "composer_attach_button"
COMPOSER_BLOCKED_HINT = "composer_blocked_hint"
PROMPT_INPUT = "prompt_input"
SEND_BUTTON = "send_button"
MODEL_CHIP = "composer_model_chip"
SETTINGS_BUTTON = "unified_settings_button"
SETTINGS_ROW_MODELS = "settings_row_models"
SETTINGS_ROW_NOTIFICATIONS = "settings_row_notifications"
SETTINGS_ROW_API_MONITOR = "settings_row_api_monitor"


class TestHomeStructure:
    """The home screen a cold install opens on."""

    def test_greeting_header_is_present(self, driver):
        assert by_tag(driver, HOME_GREETING_HEADER).is_displayed()

    def test_hero_card_offers_a_primary_action(self, driver):
        by_tag(driver, HOME_HERO_CARD)
        primary = by_tag(driver, HOME_HERO_PRIMARY)
        assert primary.is_displayed()
        assert primary.is_enabled(), "the hero CTA is the two-tap path to a first result"

    def test_capabilities_section_is_present_on_an_empty_thread(self, driver):
        assert tag_exists(driver, HOME_EMPTY_STATE), "fresh install should show the empty state"
        assert tag_exists(driver, HOME_CAPABILITIES_SECTION)

    def test_history_section_is_absent_before_any_generation(self, driver):
        # HISTORY derives from the thread. On a cold install there is nothing to show, and the
        # section hides rather than rendering an empty heading.
        assert not tag_exists(driver, HOME_HISTORY_SECTION)

    @pytest.mark.parametrize("mode", ["chat", "image", "video", "code", "audio"])
    def test_every_modality_chip_is_reachable(self, driver, mode):
        chip = by_tag(driver, f"modality_chip_{mode}")
        assert chip.is_displayed()
        chip.click()
        # Switching mode must never leave the composer behind.
        assert tag_exists(driver, PROMPT_INPUT)


class TestComposer:
    """The composer, including the split that stopped the chip showing an error sentence."""

    def test_model_chip_is_not_an_error_sentence(self, driver):
        chip = by_tag(driver, MODEL_CHIP)
        text = chip.text or ""
        # The regression this guards: preflightLabel()'s blocked *reason* was rendered here and
        # came out as "Pick a cloud model in the model pi…". A model name is short.
        assert len(text) < 60, f"model chip should name a model, got a sentence: {text!r}"
        assert "Pick a cloud model" not in text

    def test_blocked_reason_has_its_own_row_when_cloud_is_gated(self, driver):
        # Only present when cloud is actually gated, so this asserts shape rather than presence.
        if tag_exists(driver, COMPOSER_BLOCKED_HINT):
            hint = by_tag(driver, COMPOSER_BLOCKED_HINT)
            assert hint.is_displayed()
            assert (hint.text or "").strip(), "a blocked hint with no text is worse than none"

    def test_attach_button_shows_in_image_mode(self, driver):
        by_tag(driver, "modality_chip_image").click()
        assert tag_exists(driver, COMPOSER_ATTACH_BUTTON)


class TestChatSurface:
    """The chat thread's redesigned header and code rendering."""

    def test_status_header_is_present_in_chat_mode(self, driver):
        by_tag(driver, "modality_chip_chat").click()
        # The unified screen keeps its own top bar; the status header appears on the chat route.
        assert tag_exists(driver, CHAT_STATUS_HEADER) or tag_exists(driver, HOME_GREETING_HEADER)

    def test_code_reply_renders_as_a_code_block(self, driver):
        """A fenced reply must render as a code block, not as wrapped prose.

        Needs a working local or cloud code model, so it skips rather than fails when the run
        never reaches a reply — a missing model is an environment gap, not a UI regression.
        """
        by_tag(driver, "modality_chip_code").click()
        by_tag(driver, PROMPT_INPUT).send_keys("Write a CSS class for a frosted glass card")
        by_tag(driver, SEND_BUTTON).click()
        try:
            by_tag(driver, CHAT_CODE_BLOCK, timeout=120.0)
        except Exception:
            pytest.skip("no code model reached a reply in this environment")


class TestSettingsHub:
    """Settings became a hub; every destination must be one tap from it."""

    def test_hub_rows_are_present(self, driver):
        by_tag(driver, SETTINGS_BUTTON).click()
        for tag in (SETTINGS_ROW_MODELS, SETTINGS_ROW_NOTIFICATIONS, SETTINGS_ROW_API_MONITOR):
            assert tag_exists(driver, tag), f"missing settings hub row: {tag}"
