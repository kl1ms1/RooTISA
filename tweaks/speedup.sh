#!/usr/bin/env bash
# RooTISA speed-up for weak Android phones (XS11/MTK6580). No root. Reversible.
# Run with the phone connected and USB debugging on.  Undo: restore.sh
set -e
ADB="${ADB:-adb}"

echo "==> device:"; "$ADB" devices

echo "==> faster animations (0.5 = snappier; set to 0 for instant)"
SCALE="${SCALE:-0.5}"
"$ADB" shell settings put global window_animation_scale "$SCALE"
"$ADB" shell settings put global transition_animation_scale "$SCALE"
"$ADB" shell settings put global animator_duration_scale "$SCALE"

echo "==> trim background apps a bit (standard dev setting, reversible)"
# 3 = "at most 4 processes"; empty/undo restores default.
"$ADB" shell settings put global always_finish_activities 0 || true

echo "==> optional: screen density (commented out by default)"
echo "    # ${ADB} shell wm density 120   # меньше число = крупнее; 'wm density reset' вернёт"

echo
echo "Готово. Перезапусти телефон, если что-то выглядит иначе."
echo "Откат: bash restore.sh"
