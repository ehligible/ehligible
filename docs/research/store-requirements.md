# Store requirements for a background-location day counter

Researched 2026-10-29 from official Google/Apple/GitHub/Codemagic documentation. Drives the release plan in
`docs/SPEC.md` and the publishing guide.

## Google Play

### Accounts
- Fee: "US$25 one-time registration fee" (both account types).
  https://support.google.com/googleplay/android-developer/answer/6112435
- Personal accounts: legal name/address from the payments profile (or an official ID); must verify a
  "non-rooted physical Android mobile device that runs at least the Android 10" in the Play Console app.
  https://support.google.com/googleplay/android-developer/answer/10841920 ,
  https://support.google.com/googleplay/android-developer/answer/14316361
- Organization accounts need D-U-N-S; mandatory only for finance, health, VPN and government apps.
  https://support.google.com/googleplay/android-developer/answer/13634885
- Shown publicly (personal, not monetized): developer name, "your legal name, your country (as per your legal
  address), and developer email address"; full address only "If you decide to monetize".
  https://support.google.com/googleplay/android-developer/answer/13628312
- **Testing gate** (personal accounts created after 2023-11-13): closed test with "a minimum of 12 testers who
  have been opted in continuously for at least 14 days", then apply for production (review usually ≤ 7 days).
  https://support.google.com/googleplay/android-developer/answer/14151465

### Android developer verification
- Play identity verification counts; packages must be registered ("Google Play automatically registers 99% of
  apps"); global enforcement in 2027. Action: confirm the package shows as registered in Play Console.
  https://developer.android.com/developer-verification ,
  https://support.google.com/googleplay/android-developer/answer/16984799

### Background location (https://support.google.com/googleplay/android-developer/answer/9799150)
- Any background access on Android 10+ needs ACCESS_BACKGROUND_LOCATION, which triggers Play's declaration —
  **even for coarse location**.
- Must be "relevant to the core functionality", give a "significant benefit"; convenience is "minimal"; core =
  without it the app is "broken or rendered unusable"; must be "prominently documented and promoted in the
  app's description". https://support.google.com/googleplay/android-developer/answer/16558241
- Declaration form: one feature only ("multiple features will result in an app's rejection"); video ≤ 30 s
  showing disclosure → runtime prompt → feature working in background. All tracks are reviewed, including
  closed testing.
- Prominent disclosure (in-app, during normal use, before the prompt) must contain "location", one of
  "background" / "when the app is closed" / "always in use" / "when the app is not in use", and the feature.
  Template: "[This app] collects location data to enable [feature] … even when the app is closed or not in
  use." Provide a way to decline (https://support.google.com/googleplay/android-developer/answer/11150561).
- Android 11+: background must be granted separately via Settings ("Allow all the time").
  https://developer.android.com/about/versions/11/privacy/location
- Common rejections: vague or multiple features; feature not verifiable or would work in foreground; disclosure
  missing/late; privacy policy not referencing location data, a PDF, or not linked in app + listing.
- Precedent (live, updated Aug–Oct 2026): Immio, Country Days Count, Domicile365, Country Tracker.

### Other 2026 rules
- Target API: since 2026-08-31, "New apps and app updates must target Android 16 (API level 36)".
  https://developer.android.com/google/play/requirements/target-sdk
- Use WorkManager; no location foreground service (needs its own declaration); no exact alarms; request
  POST_NOTIFICATIONS only if sending notifications.
- ACCESS_FINE_LOCATION declaration becomes mandatory 2027-01-27; coarse-only apps are exempt → **check the
  merged manifest never contains FINE**. https://support.google.com/googleplay/android-developer/answer/17033915
- 16 KB page size required for apps targeting API 35+ (relevant only for native libs).
- Data safety: on-device-only processing "does not need to be disclosed", but the form must still be completed;
  privacy policy required. https://support.google.com/googleplay/android-developer/answer/10787469
- Government information apps need a non-affiliation statement + source links; answer "No" to the Government
  apps declaration. https://support.google.com/googleplay/android-developer/answer/9514050
- Trader-status declaration starting November 2026. https://support.google.com/googleplay/android-developer/answer/18100766

## Apple (deferred — user hasn't decided on an Apple account)

- Program: US$99/year; individual shows "Your name … as the seller name"; organizations need D-U-N-S.
- Guidelines (updated 2026-10-16): 5.1.5 location only when "directly relevant" with consent; 5.1.1(ii)
  purpose strings must "clearly and completely describe" the use; 5.1.1(iv) offer manual entry if location is
  declined; 2.5.4 background services only "for their intended purposes"; 5.2.1 no misleading representations;
  5.1.1(ix) regulated/sensitive apps "should be submitted by a legal entity" (precedent: individual sellers
  publish similar apps — Maple Tracker, Days in Canada, PR Tracker with an IRCC non-affiliation statement).
- "Always" authorization: purpose strings + in-app explanation; request once; significant-change/visits/region
  monitoring relaunch the app. Use `NSLocationDefaultAccuracyReduced`. Don't add the `location` background mode.
- App Privacy: "Data that is processed only on device is not 'collected'" → "No, we do not collect data".
- Privacy manifest: required-reason APIs (UserDefaults via App Group → CA92.1).
- Uploads need Xcode 26 / iOS 26 SDK (since 2026-04-28); iOS 17+ ≈ 93%+ of devices (June 2026).
- Set `ITSAppUsesNonExemptEncryption=NO`; answer DSA trader and new age-rating questions.

## Building iOS without a Mac
- GitHub Actions: standard runners are free in public repos; private repos get US$12/month credit (~190 macOS
  minutes). https://docs.github.com/en/billing/concepts/product-billing/github-actions
- Codemagic: 500 free macOS minutes/month. Xcode Cloud needs a Mac for initial setup.
- Signing: App Store Connect API key (cloud-managed certificates) is simplest for a solo developer.
- TestFlight needs an iPhone on iOS 16+ (none available yet).

## Gotchas for this app
1. Play's "significant benefit" test is the main risk → declare one feature (automatic daily presence logging
   for the citizenship requirement), lead the listing with it, cite precedent on appeal.
2. Reviews are sequential: declaration + 14-day test + production review ≥ 3 weeks → staged launch.
3. Keep FINE location out of the merged manifest.
4. Android disclosure offers "No thanks"; Apple's pre-prompt has one button and avoids the word "Allow".
5. Never look official: no "Official", no government emblems, no "IRCC" in the title; non-affiliation
   statement + source links in app and listing.
6. Reviewers are probably outside Canada → the app must visibly work (explain in review notes/video).
7. Privacy policy must reference location data; adding any analytics/crash SDK would break "no data collected".
