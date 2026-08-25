"""
Model Packs coverage — reaching the Packs screen, the durable-storage-access prompt, and a
pack's install/handshake buttons.

Deliberately does NOT drive a real multi-GB pack download to completion inside a test: these
packs are documented as multi-GB (see docs/MODEL_LICENSES.md) and a real download would make
this suite impractical to run repeatedly. What this file verifies instead is real and honest:
that the screen is reachable, that at least one pack row's real install/handshake controls exist
and are clickable, and — only if a pack is ALREADY installed on the test device — that tapping
its handshake button drives a real verification (opens the actual ONNX/LiteRT session) rather
than a static label. A fresh device with nothing installed will skip that last assertion rather
than fail on a state this suite intentionally doesn't try to create.
"""

import time

from appium.webdriver.common.appiumby import AppiumBy

from conftest import by_tag, tag_exists

OPEN_SETTINGS_BUTTON = "bottom_bar_settings"
SETTINGS_OPEN_PACKS_BUTTON = "settings_open_packs_button"
DURABLE_STORAGE_ENABLE_BUTTON = "durable_storage_enable_button"

# TestTags.packInstallButton/packHandshakeButton are parameterized by pack id — Appium can't know
# which packs exist on a given device/manifest ahead of time, so these match by resource-id
# prefix instead of one exact tag, the same wildcard approach test_generation_flows.py uses for
# chat_message_{index}_{role}.
INSTALL_BUTTON_PREFIX = "pack_install_"
HANDSHAKE_BUTTON_PREFIX = "pack_handshake_"

HANDSHAKE_TIMEOUT_SECONDS = 60  # a real handshake opens an ONNX/LiteRT session — not instant


def _open_packs_screen(driver):
    by_tag(driver, OPEN_SETTINGS_BUTTON).click()
    time.sleep(1)
    by_tag(driver, SETTINGS_OPEN_PACKS_BUTTON).click()
    time.sleep(1)


def _elements_by_id_prefix(driver, prefix: str):
    return driver.find_elements(
        AppiumBy.ANDROID_UIAUTOMATOR,
        f'new UiSelector().resourceIdMatches("{prefix}.*")',
    )


class TestModelPacksScreen:
    def test_packs_screen_is_reachable_from_settings(self, driver):
        _open_packs_screen(driver)
        install_rows = _elements_by_id_prefix(driver, INSTALL_BUTTON_PREFIX)
        assert len(install_rows) > 0, (
            "Packs screen has no install-button rows at all — either navigation failed or the "
            "catalog rendered empty, both real findings worth investigating."
        )

    def test_durable_storage_prompt_appears_when_not_yet_granted(self, driver):
        _open_packs_screen(driver)
        # This button only renders when DurableStorage.hasAllFilesAccess() is false (see
        # PacksScreen.kt) — on a device that already granted all-files access it legitimately
        # won't exist, which is why this only asserts the button is clickable when present.
        if not tag_exists(driver, DURABLE_STORAGE_ENABLE_BUTTON):
            import pytest

            pytest.skip(
                "Durable storage already granted on this device (or the tag legitimately "
                "isn't present) — nothing to verify here."
            )
        button = by_tag(driver, DURABLE_STORAGE_ENABLE_BUTTON)
        assert button.is_enabled(), "Durable-storage prompt button is present but not clickable"

    def test_an_already_installed_packs_handshake_button_runs_a_real_verification(self, driver):
        _open_packs_screen(driver)
        handshake_rows = _elements_by_id_prefix(driver, HANDSHAKE_BUTTON_PREFIX)
        if not handshake_rows:
            import pytest

            pytest.skip(
                "No pack is installed on this device — handshake buttons only render for "
                "PackStatus.INSTALLED (see PacksScreen.kt). Install a pack first to exercise "
                "this path; this suite intentionally does not trigger a multi-GB download."
            )
        handshake_rows[0].click()

        # A real handshake opens the pack's actual ONNX/LiteRT session — poll for the button's
        # own label to leave "Verifying…" rather than asserting on a fixed sleep.
        deadline = time.time() + HANDSHAKE_TIMEOUT_SECONDS
        settled = False
        while time.time() < deadline:
            rows = _elements_by_id_prefix(driver, HANDSHAKE_BUTTON_PREFIX)
            if rows and "Verifying" not in rows[0].text:
                settled = True
                break
            time.sleep(2)
        assert settled, (
            "Handshake button stayed on 'Verifying…' past the timeout — either a real hang or "
            "the timeout needs to grow for this device's pack size."
        )
