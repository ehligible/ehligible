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
- [ ] M4 Shared Compose UI (EN/FR) + fun touches
- [ ] M5 Android detection + notifications
- [ ] M6 Android Glance widget
- [ ] M7 Play release prep + v1.0 closed-test build (no background location)
- [ ] M8 Background location update (v1.1)
- [ ] M9 iOS app + WidgetKit, verified in CI simulators
- [ ] M10 Real-device test on the owner's Android phone (needs the owner)

## Current step
M4 — shared Compose UI (theme, onboarding, home, journal, review, checklist, settings), EN + FR.

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
  `./gradlew spotlessApply check :androidApp:assembleDebug` before merging.
- Don't `cd` into subfolders in Bash (it moves the session's working directory); use absolute paths.

## Waiting on the owner
- Create the Google Play Console personal account (identity verification takes days).
- Line up ~15 Android testers.
- Connect the Android phone with USB debugging when asked (M10).
- Apple Developer account: decision deferred by the owner.

## Notes
- Toolchain: JDK 17 (`C:\Users\mirha\dev\jdk17`), Android SDK `C:\Users\mirha\dev\android-sdk` (platform 36,
  build-tools 36, emulator + `system-images;android-36;google_apis_playstore;x86_64`, WHPX works).
- Library versions checked 2026-10-29: Kotlin 2.4.21, AGP 9.5.0, CMP 1.12.2, Gradle 9.8.2, Glance 1.2.0,
  WorkManager 2.12.0, DataStore 1.3.0, kotlinx-datetime 0.8.0, serialization 1.12.0, coroutines 1.11.0.
