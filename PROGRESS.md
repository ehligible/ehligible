# Progress

Checkpoint for resuming work (e.g., after a usage limit). Update at the end of every step.

## How to resume
1. Read this file, then `docs/SPEC.md`.
2. `git status` / `git log --oneline -10` to see where things stand.
3. Continue with the first unchecked item below.

## Milestones
- [x] M0 Repo, docs, GitHub — GitHub organization `ehligible` (moved there 2026-10-30 so no public address shows
      the owner's username): app repo https://github.com/ehligible/ehligible, website repo
      `ehligible/ehligible.github.io` served at https://ehligible.github.io/ (privacy policy: `/privacy.html`).
      App ID `ca.ehligible` (iOS bundle ID the same). Release secrets (upload keystore) not set yet:
      they wait for the Play account (`docs/publishing/google-play.md`).
- [x] M1 Gradle KMP/CMP skeleton + Android CI
- [x] M2 Rules engine (TDD) — IRCC worked examples, mutation-checked
- [x] M3 Presence model, inference, trips, export, backup codec, DataStore repository, border index
- [x] M4 Shared Compose UI (EN/FR) + fun touches (maple-leaf progress, confetti, one-liners, fun facts)
- [x] M5 Android detection (foreground fix + network country + 4-hourly worker) + notifications
- [x] M6 Android Glance widget (small / wide / large), pin-from-app button, midnight refresh
- [x] M7 Play release prep: signed v1.0 AAB verified on the emulator, EN/FR listing + graphics + screenshots,
      privacy/support site, `docs/publishing/google-play.md`, tag-triggered release workflow
- [x] M8 Background location (v1.1) behind `ehligible.backgroundLocation` — verified on the emulator: the fix
      is delivered while the app is closed and recorded; shipping steps in `docs/publishing/background-location.md`
- [x] M9 iOS app + WidgetKit — verified in CI (macOS 26.6, Xcode 26.6): Kotlin tests on an iPhone simulator,
      Swift widget tests, app build, launch (onboarding) and the seeded home screen, same numbers as Android.
      The widget's look in every size, state and language is rendered by a test. All of it is in the
      `ios-screenshots` artifact of the `iOS` workflow (`widget/widget-en.png`, `widget-fr.png`).
- [ ] M10 Real-device test on the owner's Android phone (needs the owner)

## Current step
All build work is done; what's left needs the owner (below).
- The signed v1.0 release is ready for the first Play upload: `C:\DevProjects\ehligible-release.0.0\`
  (`.aab` + `.apk`, built by CI from commit 4612559, signed locally with `tools/release/sign.sh`). The release
  APK was tested on the emulator on 2026-10-30: onboarding, permissions, data after a restart, the widget,
  WorkManager jobs; no crashes. Rebuild it if the app changes before the upload.
- Local R8 release builds run this PC out of memory (killed twice): use the CI artifact and `sign.sh` instead.
- Don't push while a CI run should finish: workflows cancel in-progress runs on the same branch.
- Dependabot moves Material 3 only together with Compose Multiplatform (ignore rule in `dependabot.yml`).

## Usage limits (owner asked to resume right after a reset)
- Built-in `autoContinueAtUsageLimit` is on (~/.claude/settings.json); it resumes after a reset if this window
  stays open and the laptop stays awake.
- The status line (`~/.claude/usage/statusline.py`) writes `~/.claude/usage/limits.json` (5h/7d used % and
  `resets_at`) and appends reset times to `limits.log`; a StopFailure/Notification hook logs limit hits and
  auto-resume events to `limit-events.log`.
- Auto-resume cron: removed on 2026-10-30 when the build work was done (owner's request). While unattended work
  remains, schedule one one-shot CronCreate ~2 min after `five_hour.resets_at` as a backup, and delete it when the
  work is done. Don't use frequent recurring crons: one firing mid-wait likely cancelled the auto-wait.

## Conventions
- Branch per milestone (`feat/...`), logical commits, merge `--no-ff` into main; run
  `./gradlew spotlessApply check :androidApp:assembleDebug` before merging (and with
  `-Pehligible.backgroundLocation=true` when touching detection).
- Two repos: the app here, and the website in `C:\DevProjects\ehligible.github.io` (plain HTML; pushing to `main`
  republishes it). Keep `https://ehligible.github.io/privacy.html` stable: the app and the Play listing link to it.
- Don't `cd` into subfolders in Bash (it moves the session's working directory); use absolute paths.
- Emulator helpers: `$LOCALAPPDATA/Temp/ehligible-ref/adbui.sh` (AVD `ehligible_pixel8`; debug app
  `ca.ehligible.debug`).
- When the owner's input or help is needed, send a phone notification (PushNotification).

## Waiting on the owner
1. Back up the upload key (`C:\Users\mirha\.ehligible\`) somewhere safe.
2. Create the Google Play Console personal account (identity verification takes days), then the app.
3. Add the three signing secrets to `ehligible/ehligible` so tagged releases build signed bundles.
4. Closed test with ~15 testers for 14 days (`docs/publishing/tester-invite.md`), then apply for production.
5. Connect the Android phone with USB debugging (M10).
6. When v1.1 ships: upload the declaration video to YouTube as Unlisted (script in `background-location.md`).
7. Apple Developer account and a custom domain: later, if the owner decides.

## Notes
- Toolchain: JDK 17 (`C:\Users\mirha\dev\jdk17`), Android SDK `C:\Users\mirha\dev\android-sdk` (platform 36,
  build-tools 36, emulator + `system-images;android-36;google_apis_playstore;x86_64`, WHPX works).
- Library versions checked 2026-10-29: Kotlin 2.4.21, AGP 9.5.0, CMP 1.12.2, Gradle 9.8.2, Glance 1.2.0,
  WorkManager 2.12.0, DataStore 1.3.0, kotlinx-datetime 0.8.0, serialization 1.12.0, coroutines 1.11.0.
- Upload keystore lives outside the repo: `~/.ehligible/upload-keystore.jks` + `keystore.properties`.
- Emulator quirks: its network country is "us" (use airplane mode for screenshots); it has no Wi-Fi/cell
  positioning, so balanced-power background updates only arrive while another app (Maps) uses GPS.
- Kotlin package names are still `io.github.mirhamedjz.ehligible` (internal, not shown to users).
