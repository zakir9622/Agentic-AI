"""
Pytest fixtures for driving The Lookbook (com.zakir.vestra) via Appium's UiAutomator2 driver.

Locating elements: MainActivity.kt sets `testTagsAsResourceId = true` at the composable root,
which makes every `Modifier.testTag("some_tag")` in composeApp/.../ui/TestTags.kt visible to
UiAutomator as that view's resource-id — the *raw* tag string, not prefixed with the app's
package (that's how Compose's testTagsAsResourceId semantics property is documented to behave).
`by_tag()` below wraps that lookup; if your Appium/UiAutomator2 version resolves resource-ids
differently, adjust the AppiumBy.ANDROID_UIAUTOMATOR selector there — this is the one thing this
suite could not verify without a real device/emulator in the session that authored it.

Run: see appium/README.md for prerequisites and exact commands. Not executed by CI or by any
Claude session — no device or Appium server is available in that environment.
"""

import os
import time

import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy

APP_PACKAGE = "com.zakir.vestra"
APP_ACTIVITY = ".MainActivity"

APPIUM_SERVER_URL = os.environ.get("APPIUM_SERVER_URL", "http://127.0.0.1:4723")
DEVICE_NAME = os.environ.get("APPIUM_DEVICE_NAME", "Android")
# Path to a built sideload debug/release APK. If unset, the fixture assumes the app is already
# installed on the target device/emulator and only launches it (noReset keeps its state).
APP_PATH = os.environ.get("APPIUM_APP_PATH")
NO_RESET = os.environ.get("APPIUM_NO_RESET", "true").lower() != "false"


# Emulators without KVM (QEMU TCG software emulation) run roughly 10-20x slower than a real
# device: installing the UiAutomator2 server alone overran the driver's stock 20s adb timeout,
# and a cold app start measured 97s. These are ceilings, not waits — a fast device still
# finishes in the same time it always did — so they are safe to keep on by default and can be
# lowered per-run through the environment.
def _timeout(name: str, default: int) -> int:
    return int(os.environ.get(name, default))


ADB_EXEC_TIMEOUT_MS = _timeout("APPIUM_ADB_EXEC_TIMEOUT_MS", 600_000)
UIA2_INSTALL_TIMEOUT_MS = _timeout("APPIUM_UIA2_INSTALL_TIMEOUT_MS", 600_000)
# 300_000 was not enough: a later session failed with "the instrumentation process cannot be
# initialized within 300000ms" on the same emulator that had already run the suite once, so
# the margin here is genuinely thin rather than generous.
UIA2_LAUNCH_TIMEOUT_MS = _timeout("APPIUM_UIA2_LAUNCH_TIMEOUT_MS", 900_000)
APP_WAIT_TIMEOUT_MS = _timeout("APPIUM_APP_WAIT_TIMEOUT_MS", 300_000)
ANDROID_INSTALL_TIMEOUT_MS = _timeout("APPIUM_ANDROID_INSTALL_TIMEOUT_MS", 900_000)


@pytest.fixture(scope="session")
def driver():
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.device_name = DEVICE_NAME
    options.app_package = APP_PACKAGE
    options.app_activity = APP_ACTIVITY
    options.no_reset = NO_RESET
    options.new_command_timeout = 300
    options.adb_exec_timeout = ADB_EXEC_TIMEOUT_MS
    options.uiautomator2_server_install_timeout = UIA2_INSTALL_TIMEOUT_MS
    options.uiautomator2_server_launch_timeout = UIA2_LAUNCH_TIMEOUT_MS
    options.android_install_timeout = ANDROID_INSTALL_TIMEOUT_MS
    options.app_wait_activity = "*"
    options.app_wait_duration = APP_WAIT_TIMEOUT_MS
    # Reuse an already-installed server rather than reinstalling it per session: on a software
    # emulator that install is minutes, and it is identical every time.
    options.skip_server_installation = os.environ.get("APPIUM_SKIP_SERVER_INSTALL", "true").lower() != "false"
    if APP_PATH:
        options.app = APP_PATH

    drv = webdriver.Remote(APPIUM_SERVER_URL, options=options)
    yield drv
    drv.quit()


@pytest.fixture(autouse=True)
def _reset_to_home(driver):
    """Return to a known state, and refuse to run a test against the wrong app.

    This used to press BACK unconditionally after every test. On the app's root screen BACK
    *leaves the app*, so the first test to finish on the root dropped the suite onto the
    launcher and every test after it ran against a blank screen.

    That did not merely cause failures — it manufactured passes. Every
    `assert not tag_exists(...)` check in this suite is satisfied by an empty screen, so
    assertions like "the modality chip row is gone" reported success while proving nothing.
    A negative assertion with no positive anchor cannot tell "removed from the app" from
    "no app". The pre-test guard below is the anchor: if the app under test is not in the
    foreground, the test fails saying so, instead of passing for the wrong reason.
    """
    _require_app_foreground(driver)
    _dismiss_onboarding(driver)
    _require_app_rendering(driver)
    yield
    try:
        # Dismiss a sheet or dialog if one is open, but never navigate out of the app: if
        # BACK would exit, re-activate instead.
        driver.press_keycode(4)
        if driver.current_package != APP_PACKAGE:
            driver.activate_app(APP_PACKAGE)
    except Exception:
        pass


def _require_app_foreground(driver):
    """Bring the app under test to the front, and fail loudly if it will not come."""
    try:
        if driver.current_package == APP_PACKAGE:
            return
        driver.activate_app(APP_PACKAGE)
    except Exception as exc:  # pragma: no cover - environment dependent
        raise AssertionError(f"could not foreground {APP_PACKAGE}: {exc}") from exc
    deadline = time.time() + float(os.environ.get("APPIUM_FOREGROUND_TIMEOUT_S", "120"))
    while time.time() < deadline:
        if driver.current_package == APP_PACKAGE:
            return
        time.sleep(2)
    raise AssertionError(
        f"{APP_PACKAGE} is not in the foreground (current: {driver.current_package!r}). "
        "Every negative assertion in this suite would pass vacuously against another app, "
        "so the run is stopped here rather than reporting misleading successes."
    )


RENDER_ANCHOR = os.environ.get("APPIUM_RENDER_ANCHOR", "prompt_composer")


def _require_app_rendering(driver):
    """Fail when the app is in front but has drawn nothing.

    The foreground guard above was necessary and not sufficient. On a software-emulated device
    the app held focus, its process was alive, and it rendered a completely black screen — one
    distinct colour across every pixel. Under that, twenty-two tests failed and *six passed*, of
    which five were `assert not tag_exists(...)` checks: "the chip row is gone" is satisfied
    just as well by nothing being drawn at all.

    A run that reports passes while the app is blank is worse than a run that reports nothing,
    because the passes get quoted. One positive anchor closes it: if the composer cannot be
    found on the app's own main screen, no assertion in this suite means anything, and the test
    says so instead of scoring it.

    Set APPIUM_RENDER_ANCHOR to a different tag for a suite that starts somewhere else.
    """
    if tag_exists(driver, RENDER_ANCHOR):
        return
    raise AssertionError(
        f"{APP_PACKAGE} is in the foreground but '{RENDER_ANCHOR}' was not found, so the app "
        "has most likely drawn nothing. Every negative assertion in this suite would pass "
        "against a blank screen, so the run is stopped rather than reporting those as successes. "
        "On an emulator, check that the system image actually renders (ATD images are stripped "
        "for headless automation) before suspecting the app."
    )


ONBOARDING_SCREEN = "onboarding_screen"
ONBOARDING_SKIP = "onboarding_skip"
ONBOARDING_GET_STARTED = "onboarding_get_started"
ONBOARDING_CONTINUE = "onboarding_continue"


def _dismiss_onboarding(driver):
    """Clear the first-run gate if it is showing.

    On a fresh install the app opens on onboarding, not the composer. The suite had no idea
    this screen existed — and neither did `TestTags.kt`, which carried no tags for it — so a
    first-run device produced a full sheet of misleading results: positive assertions failed
    because the composer was not there, and negative ones passed because nothing was.

    Skip is preferred over walking the pages: this suite is not onboarding's test, and the
    fewer taps between install and a testable state, the fewer ways a run can go wrong.
    """
    for _ in range(len(_ONBOARDING_MAX_PAGES)):
        if not tag_exists(driver, ONBOARDING_SCREEN):
            return
        for tag in (ONBOARDING_SKIP, ONBOARDING_GET_STARTED, ONBOARDING_CONTINUE):
            if tag_exists(driver, tag):
                by_tag(driver, tag).click()
                break
        else:
            raise AssertionError(
                "onboarding is showing but none of its buttons could be found — the app cannot "
                "be driven past its first-run gate, so no result from this run is meaningful."
            )
        time.sleep(1)
    if tag_exists(driver, ONBOARDING_SCREEN):
        raise AssertionError("onboarding did not complete after several attempts")


_ONBOARDING_MAX_PAGES = range(8)


def by_tag(driver, tag: str, timeout: float = float(os.environ.get("APPIUM_FIND_TIMEOUT_S", "45"))):
    """Find one element by its Compose testTag (exposed as resource-id via testTagsAsResourceId)."""
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC

    locator = (AppiumBy.ID, tag)
    return WebDriverWait(driver, timeout).until(EC.presence_of_element_located(locator))


def all_by_tag(driver, tag: str):
    return driver.find_elements(AppiumBy.ID, tag)


def tag_exists(driver, tag: str) -> bool:
    return len(all_by_tag(driver, tag)) > 0


# ── Navigation ────────────────────────────────────────────────────────────────────────────
#
# The app used to have a bottom dock (Home/Chat/Library/Settings) and a row of studio tabs
# (`home_tab_image` and friends). Both are gone: there is one unified screen, the generator is
# chosen from the `+` sheet, and Library/Settings are two icons in the top bar. Four test files
# each had their own `_goto_tab` helper clicking tags that no longer exist, which is why they
# failed at the first click rather than on anything they meant to assert.
#
# One helper each, here, so the next redesign breaks one place instead of four.

TOOLS_SHEET_TRIGGER = "composer_attach_button"
TOOLS_SHEET = "composer_tools_sheet"
ACTIVE_TOOL_CHIP = "composer_active_tool"


def select_tool(driver, mode: str):
    """Switch the composer to a generator: `+` -> sheet -> `composer_tool_<mode>`.

    Replaces the old `home_tab_<mode>` click. "chat" is the default mode and is reached by
    clearing the active tool rather than by picking it, so it is routed to `clear_tool`.
    """
    if mode == "chat":
        clear_tool(driver)
        return
    by_tag(driver, TOOLS_SHEET_TRIGGER).click()
    by_tag(driver, TOOLS_SHEET)
    by_tag(driver, f"composer_tool_{mode}").click()


def clear_tool(driver):
    """Return the composer to plain chat by dismissing the active-tool chip, if one is shown."""
    if tag_exists(driver, ACTIVE_TOOL_CHIP):
        by_tag(driver, ACTIVE_TOOL_CHIP).click()


def attach_from(driver, source: str):
    """Open the `+` sheet and pick an attachment source (photos / camera / files)."""
    by_tag(driver, TOOLS_SHEET_TRIGGER).click()
    by_tag(driver, TOOLS_SHEET)
    by_tag(driver, f"composer_source_{source}").click()


def open_settings(driver):
    """Settings is a top-bar icon now, not a dock tab."""
    by_tag(driver, "unified_settings_button").click()


def open_library(driver):
    """Library (Wardrobe) is a top-bar icon now, not a dock tab."""
    by_tag(driver, "unified_library_button").click()


def back(driver):
    """Hardware back — how you leave a sub-screen now that there is no dock to tap."""
    driver.press_keycode(4)
