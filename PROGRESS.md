# Progress

Checkpoint for resuming work (e.g., after a usage limit). Update at the end of every step.

## How to resume
1. Read this file, then `docs/SPEC.md`.
2. `git status` / `git log --oneline -10` to see where things stand.
3. Continue with the first unchecked item below.

## Milestones
- [ ] M0 Repo, docs, GitHub
- [ ] M1 Gradle KMP/CMP skeleton + Android CI
- [ ] M2 Rules engine (TDD)
- [ ] M3 Presence model, inference, trips, export, backup
- [ ] M4 Shared Compose UI (EN/FR) + fun touches
- [ ] M5 Android detection + notifications
- [ ] M6 Android Glance widget
- [ ] M7 Play release prep + v1.0 closed-test build (no background location)
- [ ] M8 Background location update (v1.1)
- [ ] M9 iOS app + WidgetKit, verified in CI simulators
- [ ] M10 Real-device test on the owner's Android phone (needs the owner)

## Current step
M0 — writing docs and creating the GitHub repo.

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
