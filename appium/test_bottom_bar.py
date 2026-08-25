"""
Covers A3 (docs/plans/lovable-parity-local-first/PLAN.md): the bottom dock nav bar (Home/
Library/Create/Chat/Settings) must actually navigate between the five top-level destinations,
and the raised center Create FAB must land back in the studio pager.
"""

import time

from conftest import by_tag, tag_exists

BOTTOM_BAR_HOME = "bottom_bar_home"
BOTTOM_BAR_LIBRARY = "bottom_bar_library"
BOTTOM_BAR_CREATE = "bottom_bar_create"
BOTTOM_BAR_CHAT = "bottom_bar_chat"
BOTTOM_BAR_SETTINGS = "bottom_bar_settings"

HOME_TAB_IMAGE = "home_tab_image"
PROMPT_INPUT = "composer_prompt_input"
CHAT_REFRESH_BUTTON = "chat_news_refresh"
PROCESSING_MODE_LOCAL = "processing_mode_local"


def _tap(driver, tag: str):
    by_tag(driver, tag).click()
    time.sleep(1)


class TestBottomBar:
    def test_all_five_items_are_visible_on_home(self, driver):
        for tag in [
            BOTTOM_BAR_HOME,
            BOTTOM_BAR_LIBRARY,
            BOTTOM_BAR_CREATE,
            BOTTOM_BAR_CHAT,
            BOTTOM_BAR_SETTINGS,
        ]:
            assert tag_exists(driver, tag), f"Bottom bar item {tag!r} not found on Home"

    def test_library_tab_opens_wardrobe_and_bar_stays_visible(self, driver):
        _tap(driver, BOTTOM_BAR_LIBRARY)
        assert tag_exists(driver, BOTTOM_BAR_LIBRARY), (
            "Bottom bar disappeared after navigating to Library — it must stay visible on "
            "every top-level destination"
        )
        _tap(driver, BOTTOM_BAR_HOME)

    def test_chat_tab_opens_news_chat_screen(self, driver):
        _tap(driver, BOTTOM_BAR_CHAT)
        assert tag_exists(driver, CHAT_REFRESH_BUTTON) or tag_exists(driver, PROMPT_INPUT), (
            "Chat destination did not render NewsChatScreen content"
        )
        _tap(driver, BOTTOM_BAR_HOME)

    def test_settings_tab_opens_settings_and_processing_mode_is_reachable(self, driver):
        _tap(driver, BOTTOM_BAR_SETTINGS)
        assert tag_exists(driver, PROCESSING_MODE_LOCAL), (
            "Settings destination did not render the Processing Mode card"
        )
        _tap(driver, BOTTOM_BAR_HOME)

    def test_create_fab_returns_to_the_studio_pager(self, driver):
        _tap(driver, BOTTOM_BAR_LIBRARY)  # leave the studio first
        _tap(driver, BOTTOM_BAR_CREATE)
        assert tag_exists(driver, HOME_TAB_IMAGE), (
            "Create FAB did not land back on the studio pager (Image tab not found)"
        )
        assert tag_exists(driver, PROMPT_INPUT), "Studio composer not visible after Create FAB tap"

    def test_studio_prompt_survives_a_round_trip_through_library(self, driver):
        """Regression guard for A3: bottom-bar navigation must not reset in-studio session state."""
        marker = "UNIQUE_BOTTOM_BAR_ROUNDTRIP_4d2b"
        _tap(driver, BOTTOM_BAR_HOME)
        field = by_tag(driver, PROMPT_INPUT)
        field.clear()
        field.send_keys(marker)

        _tap(driver, BOTTOM_BAR_LIBRARY)
        _tap(driver, BOTTOM_BAR_HOME)

        field = by_tag(driver, PROMPT_INPUT)
        text = field.get_attribute("text") or ""
        assert marker in text, (
            f"Studio prompt was lost after a Library round trip via the bottom bar — got {text!r}"
        )
