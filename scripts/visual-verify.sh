#!/usr/bin/env bash
# Deep-link visual verification — warm process + lookbook://screen/{route}.
# Usage: scripts/visual-verify.sh [serial] [outdir]
# Tip: adb shell cmd package compile -m speed -f com.zakir.vestra
set -euo pipefail
SERIAL="${1:-emulator-5554}"
OUT="${2:-/opt/cursor/artifacts/screenshots/visual-verify}"
PKG=com.zakir.vestra
POST_NAV_SLEEP="${POST_NAV_SLEEP:-3.5}"
mkdir -p "$OUT"

shot() {
  local name="$1"
  sleep "$POST_NAV_SLEEP"
  adb -s "$SERIAL" exec-out screencap -p > "$OUT/${name}.png"
  echo "✓ $name ($(wc -c < "$OUT/${name}.png") bytes)"
}

open_route() {
  local route="$1"
  adb -s "$SERIAL" shell am start -a android.intent.action.VIEW \
    -d "lookbook://screen/${route}" -n "$PKG/.MainActivity" >/dev/null || true
}

adb -s "$SERIAL" shell settings put global window_animation_scale 0
adb -s "$SERIAL" shell settings put global transition_animation_scale 0
adb -s "$SERIAL" shell settings put global animator_duration_scale 0

adb -s "$SERIAL" shell am force-stop "$PKG"
open_route studio
for i in $(seq 1 40); do
  adb -s "$SERIAL" exec-out screencap -p > "$OUT/_probe.png"
  bytes=$(wc -c < "$OUT/_probe.png")
  if [ "$bytes" -gt 200000 ]; then
    echo "warm after probe $i ($bytes bytes)"
    break
  fi
  sleep 2
done
cp "$OUT/_probe.png" "$OUT/01-atelier.png"
echo "✓ 01-atelier ($(wc -c < "$OUT/01-atelier.png") bytes)"

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
  open_route "${pair%%:*}"
  shot "${pair##*:}"
done

open_route settings
sleep 2
for _ in 1 2 3 4; do
  adb -s "$SERIAL" shell input swipe 540 2000 540 250 350
  sleep 0.35
done
sleep 1
adb -s "$SERIAL" exec-out screencap -p > "$OUT/08-settings-about.png"
echo "✓ 08-settings-about ($(wc -c < "$OUT/08-settings-about.png") bytes)"

rm -f "$OUT/_probe.png"
echo "Artifacts in $OUT"
ls -lh "$OUT"
