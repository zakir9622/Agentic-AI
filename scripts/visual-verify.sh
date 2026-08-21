#!/usr/bin/env bash
# Deep-link visual verification — no UI Automator taps.
# Usage: scripts/visual-verify.sh [serial] [outdir]
set -euo pipefail
SERIAL="${1:-emulator-5554}"
OUT="${2:-/opt/cursor/artifacts/screenshots/visual-verify}"
PKG=com.zakir.vestra
SETTLE="${SETTLE:-14}"
mkdir -p "$OUT"

shot() {
  local name="$1"
  adb -s "$SERIAL" exec-out screencap -p > "$OUT/${name}.png"
  local bytes
  bytes=$(wc -c < "$OUT/${name}.png")
  echo "✓ $name ($bytes bytes)"
}

open_route() {
  local route="$1"
  adb -s "$SERIAL" shell am force-stop "$PKG"
  # Warm ART a bit less painfully: start then wait
  adb -s "$SERIAL" shell am start -a android.intent.action.VIEW \
    -d "lookbook://screen/${route}" -n "$PKG/.MainActivity" >/dev/null || true
  sleep "$SETTLE"
}

adb -s "$SERIAL" shell settings put global window_animation_scale 0
adb -s "$SERIAL" shell settings put global transition_animation_scale 0
adb -s "$SERIAL" shell settings put global animator_duration_scale 0

# One warm launch so subsequent screenshots are richer
open_route studio
shot "01-atelier"

for pair in \
  "create:02-image-studio" \
  "video:03-video-studio" \
  "code:04-code-studio" \
  "wardrobe:05-looks-gallery" \
  "help:06-help-faq" \
  "settings:07-settings" \
  "usage:09-cloud-usage" \
  "garment:10-garment-capture"
do
  route="${pair%%:*}"
  name="${pair##*:}"
  open_route "$route"
  shot "$name"
done

# About section: already on settings from last? re-open settings then scroll
open_route settings
adb -s "$SERIAL" shell input swipe 540 2000 540 200 450
sleep 0.5
adb -s "$SERIAL" shell input swipe 540 2000 540 200 450
sleep 1
shot "08-settings-about"

echo "Artifacts in $OUT"
ls -lh "$OUT"
