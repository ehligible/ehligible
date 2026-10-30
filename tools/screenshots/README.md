# Store screenshots

How the Play Store screenshots are made (emulator + `adb`):

1. Build and install the **debug** app (`./gradlew :androidApp:installDebug`); debug builds let `run-as` seed data.
2. Turn on airplane mode so the emulator's fake "US" mobile network doesn't record checks, and clean the status bar
   with System UI demo mode (`clock 0930`, full battery and signal, no notifications).
3. Seed the fictional demo person: `python tools/screenshots/demo_seed.py demo.json`, push it to the device and copy it
   to the app's `files/ehligible.json` with `run-as`.
4. Set the app language with `adb shell cmd locale set-app-locales ca.ehligible.debug --locales en-CA`
   (or `fr-CA`), then capture each screen with `adb exec-out screencap -p`.
5. Frame each capture with a caption:
   `python tools/design/store_graphics.py frame raw.png androidApp/src/main/play/listings/<lang>/graphics/phone-screenshots/<n>.png "<caption>"`.

The framed images are 1080×1920 (Play requires a ratio of at most 2:1).
