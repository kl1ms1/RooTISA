# RooTISA Launcher

Крошечный домашний лаунчер (home screen) для слабых Android-телефонов —
сделан под **XS11 / MTK6580, экран 240×432, ~512 МБ–1 ГБ RAM, Android 8.1**.

Это **не прошивка** и не root: Android остаётся на месте. Лаунчер ставится как
домашний экран по умолчанию, и при включении/нажатии «домой» телефон сразу
открывает эту оболочку вместо стоковой.

## Что внутри
- Чистый Android framework API, **без AndroidX** → APK ~20 КБ.
- Тёмный градиент + «стеклянные» (frosted) панели без реального блюра — дёшево для GPU.
- Часы + дата, сетка приложений в 3 колонки, подогнанная под 240×432.
- Тап — запуск приложения; долгий тап — системное «О приложении» (удалить/очистить).
- Иконки кэшируются, список обновляется только при смене числа приложений — минимум нагрузки.

## Структура
```
launcher/
  AndroidManifest.xml          HOME + DEFAULT + LAUNCHER intent-filters
  src/com/rootisa/launcher/    MainActivity.java, AppItem.java
  res/                         strings, styles (Holo.NoActionBar), vector icon
  build.sh                     сборка APK без Gradle
```

## Сборка (без Android Studio / Gradle)
Нужны: `aapt`, `ecj`, `zipalign`, `apksigner` (ставятся из apt), а также
`android.jar` (API 27) и `dx.jar` в `~/android-tools/`.

```bash
cd launcher
./build.sh
# -> build/RooTISA.apk
```

Тулчейн конвейера: `aapt` (ресурсы+манифест) → `ecj -1.6` (Java 6 байткод) →
`dx` (classes.dex) → `aapt add` → `zipalign` → `apksigner` (v1+v2, min-sdk 16).
Java 6 — потому что легаси-дексер `dx 1.7` (единственный доступный без Google Maven)
не читает байткод новее Java 6.

## Установка на телефон
```bash
adb install -r build/RooTISA.apk
```
Или скинуть APK на телефон и открыть (включив «установка из неизвестных источников»).
Затем нажать «домой» и выбрать **RooTISA** → «Всегда».

Чтобы вернуть стоковый лаунчер: Настройки → Приложения → RooTISA → «Открывать по
умолчанию» → Очистить, либо просто удалить APK.
