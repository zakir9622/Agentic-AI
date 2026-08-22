#!/usr/bin/env bash
# Verify debug Lite pack assets exist for CI / fresh clones.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PACK="$ROOT/composeApp/src/debug/assets/packs/lite-v1"
required=(garment_seg.onnx human_parse.onnx)
missing=0
for f in "${required[@]}"; do
  if [[ ! -f "$PACK/$f" ]]; then
    echo "MISSING: $PACK/$f"
    missing=$((missing + 1))
  fi
done
if [[ $missing -gt 0 ]]; then
  echo "Run: python3 ml/export_lite_pack.py (or copy from CI cache)"
  exit 1
fi
echo "OK: lite-v1 debug assets present ($(du -sh "$PACK" | cut -f1))"
exit 0
