@echo off
REM RooTISA speed-up for weak Android phones (XS11/MTK6580). No root. Reversible.
REM Phone connected + USB debugging on. Undo: run the three "put ... 1" lines.
setlocal
if "%ADB%"=="" set ADB=adb

echo ==^> device:
%ADB% devices

echo ==^> faster animations (0.5 = snappier; use 0 for instant)
if "%SCALE%"=="" set SCALE=0.5
%ADB% shell settings put global window_animation_scale %SCALE%
%ADB% shell settings put global transition_animation_scale %SCALE%
%ADB% shell settings put global animator_duration_scale %SCALE%

echo ==^> trim background behavior (reversible)
%ADB% shell settings put global always_finish_activities 0

echo.
echo Gotovo. Perezapusti telefon pri neobhodimosti.
echo Otkat: postav' znacheniya animacij obratno v 1.
pause
