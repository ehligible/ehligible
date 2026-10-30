# Progress

Checkpoint for resuming work (e.g., after a usage limit). Update at the end of every step.

## How to resume
1. Read this file, then `docs/SPEC.md`.
2. `git status` / `git log --oneline -10` to see where things stand.
3. Continue with the first unchecked item below.

## Milestones
- [~] M0 Repo, docs, GitHub — local repo done; **public GitHub repo blocked**: the owner must run
      `gh repo create MirHamed-JZ/ehligible --public --source . --remote origin --push` (auto-mode denied it for Claude)
- [x] M1 Gradle KMP/CMP skeleton + Android CI (workflow written; runs once the repo exists)
- [x] M2 Rules engine (TDD) — IRCC worked examples, mutation-checked
- [x] M3 Presence model, inference, trips, export, backup codec, DataStore repository, border index
- [x] M4 Shared Compose UI (EN/FR) + fun touches (maple-leaf progress, confetti, one-liners, fun facts)
- [x] M5 Android detection (foreground fix + network country + 4-hourly worker) + notifications
- [x] M6 Android Glance widget (small / wide / large), pin-from-app button, midnight refresh
- [x] M7 Play release prep: signed v1.0 AAB verified on the emulator, EN/FR listing + graphics + screenshots,
      privacy/support site, `docs/publishing/google-play.md`, tag-triggered release workflow
- [x] M8 Background location (v1.1) behind `ehligible.backgroundLocation` — verified on the emulator: the fix
      is delivered while the app is closed and recorded; shipping steps in `docs/publishing/background-location.md`
- [ ] M9 iOS app + WidgetKit, verified in CI simulators
- [ ] M10 Real-device test on the owner's Android phone (needs the owner)

## Current step
M9 — iOS app: XcodeGen project (`iosApp/`), SwiftUI WidgetKit widget fed by an App Group snapshot,
CoreLocation (significant-location changes + visits, reduced accuracy), privacy manifest, Info.plist usage
strings (EN/FR), and a macOS GitHub Actions workflow that builds and runs the simulator tests/screenshots.

Notes for M9:
- `onb_background_body` ends with Android's wording ("Allow all the time"); iOS needs its own ending
  ("Change to Always Allow").
- The Kotlin iOS actuals already exist (`composeApp/src/iosMain`), including `requestAlwaysAuthorization()`;
  `./gradlew check` compiles them on Windows.

## Usage limits (owner asked to resume right after a reset)
- Built-in `autoContinueAtUsageLimit` is on (~/.claude/settings.json); it resumes after a reset if this window
  stays open and the laptop stays awake.
- The status line (`~/.claude/usage/statusline.py`) writes `~/.claude/usage/limits.json` (5h/7d used % and
  `resets_at`) and appends reset times to `limits.log`; a StopFailure/Notification hook logs limit hits and
  auto-resume events to `limit-events.log`.
- Keep exactly one one-shot CronCreate scheduled ~2 min after `five_hour.resets_at` (a backup that never fires
  during the wait). Don't use frequent recurring crons: one firing mid-wait likely cancelled the auto-wait.

## Conventions
- Branch per milestone (`feat/...`), logical commits, merge `--no-ff` into main; run
  `./gradlew spotlessApply check :androidApp:assembleDebug` before merging (and with
  `-Pehligible.backgroundLocation=true` when touching detection).
- Don't `cd` into subfolders in Bash (it moves the session's working directory); use absolute paths.
- Emulator helpers: `$LOCALAPPDATA/Temp/ehligible-ref/adbui.sh` (AVD `ehligible_pixel8`).

## Waiting on the owner
- Create the public GitHub repo (command above), then the Pages site and release secrets can be set up.
- Create the Google Play Console personal account (identity verification takes days).
- Line up ~15 Android testers (`docs/publishing/tester-invite.md`).
- Connect the Android phone with USB debugging when asked (M10).
- When v1.1 ships: upload the declaration video to YouTube as Unlisted (script in `background-location.md`).
- Apple Developer account: decision deferred by the owner.

## Notes
- Toolchain: JDK 17 (`C:\Users\mirha\dev\jdk17`), Android SDK `C:\Users\mirha\dev\android-sdk` (platform 36,
  build-tools 36, emulator + `system-images;android-36;google_apis_playstore;x86_64`, WHPX works).
- Library versions checked 2026-10-29: Kotlin 2.4.21, AGP 9.5.0, CMP 1.12.2, Gradle 9.8.2, Glance 1.2.0,
  WorkManager 2.12.0, DataStore 1.3.0, kotlinx-datetime 0.8.0, serialization 1.12.0, coroutines 1.11.0.
- Upload keystore lives outside the repo: `~/.ehligible/upload-keystore.jks` + `keystore.properties`.
- Emulator quirks: its network country is "us" (use airplane mode for screenshots); it has no Wi-Fi/cell
  positioning, so balanced-power background updates only arrive while another app (Maps) uses GPS.
