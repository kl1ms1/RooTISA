#!/usr/bin/env bash
# Revert RooTISA speed-up tweaks to Android defaults. No root.
set -e
ADB="${ADB:-adb}"

echo "==> restoring default animations"
"$ADB" shell settings put global window_animation_scale 1
"$ADB" shell settings put global transition_animation_scale 1
"$ADB" shell settings put global animator_duration_scale 1

echo "==> restoring activity behavior"
"$ADB" shell settings put global always_finish_activities 0

echo "==> restoring screen density (if you changed it)"
"$ADB" shell wm density reset || true

echo "Готово. Дефолты возвращены."
