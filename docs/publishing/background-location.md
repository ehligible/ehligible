# v1.1: background location on Google Play

v1.0 only checks location while the app is open. v1.1 adds approximate location **in the background**, so
trips are noticed even on days you don't open the app. Google reviews this separately (on every track,
closed testing included), so it ships as its own update once v1.0 is in the closed test.

## What changes in the app

| | v1.0 | v1.1 |
|---|---|---|
| Permissions | `ACCESS_COARSE_LOCATION` | adds `ACCESS_BACKGROUND_LOCATION` (still approximate only, never `ACCESS_FINE_LOCATION`) |
| Onboarding | location step | adds "Notice trips even when the app is closed?" (prominent disclosure, then Android's "Allow all the time" page; "No thanks" skips it) |
| Settings | location on/off | adds "Allow in the background", behind the same disclosure |
| Checks while closed | mobile network country every ~4 h | plus approximate location delivered by Android every ~3 h (at most hourly) |

How it works on Android: apps in the background can't get a location on demand (Google Play services delays
those requests by several minutes). So v1.1 registers standing balanced-power (Wi-Fi/cell) location updates
with the fused location provider (`BackgroundLocation`), delivered to `LocationUpdatesReceiver`, which records
a check and drops the coordinates. Checks made with the app open still add a brief GPS-capable fix.

Everything is behind one build switch, so v1.0 builds never contain the permission.

## Shipping v1.1

1. In `gradle.properties` set `ehligible.versionName=1.1.0` and `ehligible.backgroundLocation=true`.
2. Replace the release notes in `distribution/whatsnew/` and `androidApp/src/main/play/release-notes/`
   with the texts below.
3. Update the store listing description (below). Google requires the background feature to be "prominently
   documented and promoted" in it.
4. Record the video (below), upload it to YouTube as **Unlisted**.
5. Fill in the declaration (below), then tag `v1.1.0` and push. The release workflow uploads the bundle to
   the closed-testing track as usual.

Check before tagging: `./gradlew check :androidApp:bundleRelease`, then confirm the merged manifest lists
`ACCESS_BACKGROUND_LOCATION` (`androidApp/build/intermediates/merged_manifest/release/.../AndroidManifest.xml`).

## Play Console: location permissions declaration

**App content → Sensitive app permissions → Location permissions → Start declaration**

- **Does your app access location in the background?** Yes.
- **Feature that needs background location** (one feature only; more is grounds for rejection):

  > Automatic daily presence log. Eh-ligible counts the days a permanent resident spends in Canada toward the
  > Canadian citizenship physical presence requirement (1,095 days in the 5 years before applying). A few
  > times a day, the app checks on the device which country the user is in, using approximate location, so
  > that days abroad are recorded even when the user doesn't open the app. Coordinates are never stored or
  > sent: only "in Canada" or the foreign country's code is saved for that day.

- **Why the feature needs it** (core functionality):

  > Counting days of physical presence is the app's main purpose. People rarely open the app every day,
  > least of all while travelling, which is exactly when the count changes. Without background access, every
  > day the app isn't opened is unknown, and the user has to reconstruct their travel history by hand, which
  > is what the app exists to avoid.

- **Video instructions:** the YouTube link (≤ 30 seconds, see below).
- **Prominent disclosure:** shown in onboarding before the system prompt, and again in Settings before
  asking. Text (English): "Eh-ligible collects your approximate location in the background, even when the app
  is closed or not in use, to log which country you're in each day for your citizenship count. The check
  happens on your phone a few times a day; your location is never stored or sent anywhere." The French text
  is in `composeApp/src/commonMain/composeResources/values-fr/strings.xml` (`onb_background_body`).

**Data safety:** unchanged. Location is processed on the device only and never transmitted, so it is not
"collected" in Google's sense; the answer stays **No**.

## The video (≤ 30 s)

Record on the emulator (`adb shell screenrecord --time-limit 30 /sdcard/bg.mp4`) or a phone, with a fresh
install of the v1.1 build:

1. **0–8 s** Onboarding reaches "Notice trips even when the app is closed?". Pause on the disclosure, then tap
   **Allow in the background**.
2. **8–14 s** Android's location page: tap **Allow all the time**, return to the app, finish onboarding.
3. **14–22 s** Go to the home screen (app closed, widget visible). On the emulator, set a location abroad
   (`adb emu geo fix -73.0 44.9`, just across the US border) and let a location arrive. Opening another app
   that uses GPS (Maps) makes Android deliver one right away.
4. **22–30 s** The notification "Looks like you're travelling ✈️" appears while Eh-ligible is closed (turn
   notifications on during onboarding); open the app to show the status "Abroad: United States".

## Store listing changes

Replace the "TRIPS, NOTICED AUTOMATICALLY" paragraph.

English:

> ✈️ TRIPS, NOTICED AUTOMATICALLY
> If you allow it, Eh-ligible checks which country you're in a few times a day, even when the app is closed,
> using your approximate location and your mobile network. That's how it notices your trips without you
> having to open it. The check happens on your phone: your location is never stored or sent anywhere. If it
> can't tell where you were on a day, it asks you instead of guessing.

French:

> ✈️ DES VOYAGES DÉTECTÉS AUTOMATIQUEMENT
> Si vous l'autorisez, Eh-ligible vérifie dans quel pays vous êtes quelques fois par jour, même lorsque
> l'application est fermée, grâce à votre position approximative et à votre réseau mobile. C'est ainsi
> qu'elle remarque vos voyages sans que vous ayez à l'ouvrir. La vérification se fait sur votre téléphone :
> votre position n'est jamais conservée ni envoyée. Si elle ne peut pas savoir où vous étiez un jour donné,
> elle vous le demande au lieu de deviner.

## Release notes

English: `Trips are now noticed even when the app is closed, if you allow location in the background. You can turn it on in Settings.`

French: `Les voyages sont maintenant détectés même lorsque l'application est fermée, si vous autorisez la position en arrière-plan. Vous pouvez l'activer dans les Paramètres.`
