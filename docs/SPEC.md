# Eh-ligible — product spec and decisions

Agreed with the product owner on 2026-10-29 (grilling rounds Q1–Q21). Rules come from
[research/citizenship-rules.md](research/citizenship-rules.md); store constraints from
[research/store-requirements.md](research/store-requirements.md). Domain terms: [GLOSSARY.md](GLOSSARY.md).

## Product

| Decision | Choice |
|---|---|
| Name | **Eh-ligible** — store title "Eh-ligible: Citizenship Countdown" |
| App ID | `io.github.mirhamedjz.ehligible` (debug builds: `.debug` suffix) |
| Platforms | Android first (store release); iOS built + verified in CI simulators, parked until the Apple account decision |
| Business model | Free. No ads, no analytics, no crash SDK, no account, no server |
| Languages | English + French |
| Users | One person per phone (detection follows the phone) |
| Look | "Playful but clean": red/white maple palette, dark mode, confetti milestones, rotating one-liners, fun fact of the day, trip mode |
| Privacy | All data on device. Location → country happens offline; coordinates are never stored or sent. Backup = export/import file + Android Auto Backup |
| Not affiliated | Never look official: no government emblems, "IRCC" never in the title; non-affiliation statement + links to official sources in app and listing |

## Rules engine (see research doc for sources)

- Eligible on signing date **A** when `2·PR + min(NPR, 730) ≥ 2,190` over window `[A − 5 years, A − 1]`
  (if A is Feb 29 the window starts Mar 1). Displayed as `PR + min(NPR/2, 365)` floored, out of 1,095.
- A day is present unless it lies strictly between a departure and a return. Same-day / next-day trips cost 0
  days but are still listed.
- Pre-PR credit: present days as temporary resident (visitor/student/worker/TRP) or protected person (from the
  approval date), within the window; excluding no-status periods and refugee-claim waiting periods. Maintained
  status counts (info note: exclude anything you're unsure about). The PR landing day is a PR day.
- **Countdown target** = first date A ≥ today that is eligible, assuming presence on every future day except
  planned trips (day-by-day search with prefix sums).
- **Safer apply date** = first date A ≥ today with `credited ≥ 1,095 + margin` (margin default 7, range
  0–60). The widget counts down to the exact date by default; a setting switches it to the safer date.
- Checklist: valid PR status; tax returns for ≥ 3 of the calendar years overlapping the window excluding the
  application year (the app lists them); language proof (CLB/NCLC 4) + test if 18–54 on A (optional birth
  year); adult fee $653 (updates every March 31); no prohibitions; links to official pages.
- Out of scope (v1): minors, Crown servants/CAF abroad, sentence periods, PR residency obligation, Google
  Timeline import. The app points those users to IRCC's calculator.

## Presence detection

**Checks** happen several times a day; each produces one result:

| Location result | Network country (Android) | Check result |
|---|---|---|
| inside Canada, not near a foreign border | any | `IN` |
| outside Canada, not near Canada | any | `OUT(country)` |
| near the Canada/foreign border (within the fix's accuracy, min 2.5 km) | Canadian network or none | `BORDER` |
| near the border | foreign network | `BORDER_FOREIGN` |
| no location (no permission / no fix) | Canadian network | `IN` |
| no location | foreign network | `OUT(country)` |
| no location | none | no result |

Points at sea within ~25 km of Canada (and closer to Canada than to foreign land) count as Canada; points far
from any land give no result.

**Day verdict** (local calendar date where the phone was at the time of the check), in precedence order:
1. User verdict for that day (from Review or trip edits).
2. Strictly inside a manual trip → absent; a manual trip's departure/return day → present.
3. Tracking period (install date … today), from that day's checks:
   any `IN` → present · only `BORDER` → present (near-border) · `OUT`/`BORDER_FOREIGN` without `IN` → absent
   if all are `OUT`, otherwise *needs review* · no checks → gap inference.
4. Future days → present unless inside a planned trip.
5. Before tracking started → present unless inside a manual trip.

**Gap inference**: a run of days without checks bounded by present days on both sides (or ending today after a
present day) → *assumed present*. Any other gap → *needs review*. Needs-review days count as absent until the
user confirms. A review badge shows in the app and on the widget.

**Trips view**: maximal runs of absent/needs-review days become trips (departure = last present day before,
return = first present day after; ongoing if none). Present days that also had foreign checks with present
neighbours become same-day / next-day trips. Destination = countries seen; reason = user annotation.

**Schedule**
- Android v1.0: check on app open / widget tap (approximate foreground location if granted, with a brief
  GPS-capable fix that works abroad without data) + mobile network country every ~4 h via WorkManager + on
  boot / time-zone change / app update. No background location permission.
- Android v1.1 (`ehligible.backgroundLocation=true`): adds `ACCESS_BACKGROUND_LOCATION` (coarse only), asked
  after a prominent disclosure (onboarding step, or Settings). Background apps can't get fixes on demand, so
  the fused provider delivers standing balanced-power updates (~3 h, at most hourly) to a receiver that records
  a check; the periodic worker also uses a cached fix ≤ 30 min old. Details: `docs/publishing/background-location.md`.
  Never request `ACCESS_FINE_LOCATION`.
- iOS: check on app open (one fix at reduced accuracy; `NSLocationDefaultAccuracyReduced`), and with "Always"
  the significant-location-change service, which wakes or relaunches the app when the phone moves. iOS has no
  public API for the network country, so location decides alone.

## Screens

- **Onboarding**: welcome → PR date → first status date (optional; exclusions) → past trips or "I haven't
  left" → location disclosure (prominent, with "Not now") → notifications (opt-in) → celebration.
- **Home**: big countdown, maple-leaf progress, "Apply on" + safer date, status chip (In Canada / Abroad —
  countdown paused / Unknown), review banner, one-liner, fun fact.
- **Journal**: past + detected + planned trips; add/edit/delete; export (text/CSV in IRCC's columns:
  departure, return, destination, reason, days absent).
- **Review**: confirm days that need review.
- **Checklist**, **Settings** (margin, widget target, notifications, backup export/import, profile edits,
  about/disclaimer/privacy/licences incl. Natural Earth).

## Widgets

- Android (Glance, resizable): small = leaf fill + days to go; wide adds apply date, progress x/1,095, status
  chip, review badge. Tap opens the app. Refreshes on every recompute and just after midnight.
- iOS (WidgetKit): small + medium home-screen, lock-screen circular + rectangular + inline. A widget can't
  run the rules, so the app writes a 14-day snapshot (`core/model/WidgetSnapshot.kt`) to the App Group
  container on every change; each day is projected like the home screen and carries its own status.

## Notifications (opt-in)
Milestones (25/50/75 %, 100/30/7/1 days left), "You're eligible today!", "Looks like you're travelling —
countdown paused", "A few days need your review".

## Engineering

- Kotlin Multiplatform + Compose Multiplatform. Modules: `core` (pure logic, no UI), `composeApp` (shared UI +
  state, Android KMP library plugin), `androidApp` (application, Glance widget, WorkManager, location),
  `iosApp` (XcodeGen project, SwiftUI widget, CoreLocation).
- Android 8.0+ (minSdk 26), targetSdk 36. iOS 17+.
- Storage: one versioned JSON document (kotlinx.serialization) in DataStore (okio), atomic writes.
- Tests: rules engine test-first incl. IRCC worked examples; presence inference + trips; UI smoke tests.
- Tooling: Gradle version catalog, Spotless + ktlint, Android Lint, GitHub Actions (Android on Linux, iOS on
  macOS with simulator tests + screenshots), conventional commits, PRs per milestone.
- Repo: public `MirHamed-JZ/ehligible`, Apache-2.0; privacy policy + support page on GitHub Pages.

## Release plan (Google Play)

1. Personal account (owner pays US$25), developer name "Eh-ligible"; legal name, country, and
   `jafarzadeh.mirhamed@gmail.com` are public.
2. Closed test v1.0 **without** background location → starts the 14-day / 12-tester clock (~15 testers).
3. Update v1.1 adds background location + Permissions declaration (one feature: automatic daily presence
   logging for the citizenship physical-presence requirement) + ≤ 30 s video, reviewed during the test.
4. Apply for production after 14 days. Data safety: no data collected. Target API 36.
