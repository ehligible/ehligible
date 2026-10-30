# Testing on a real phone

What to try on a real Android phone: for the owner's device test (M10) and for closed testers. Each item says what
should happen. Report anything else with a screenshot and the steps that led to it.

## Install

- **Owner (USB):** Developer options → USB debugging on, phone plugged in, then
  `./gradlew :androidApp:installDebug`. The test build is a separate app (`ca.ehligible.debug`) and doesn't touch
  the Play version.
- **Testers:** open the opt-in link from the invitation, accept, then install from the Play Store page it shows.

## First run (10 minutes)

1. **Welcome → PR date:** pick the date on your Confirmation of Permanent Residence. Future dates are refused.
2. **Status before PR:** answer for yourself; with "Yes", add the date your first temporary status began.
3. **Trips:** add one past trip (any dates after your PR date), or tick "I haven't left Canada".
4. **Location:** "Use approximate location" shows Android's prompt with *Approximate* preselected and no precise
   option. "No thanks" must still let you finish.
5. **Notifications:** "Yes, notify me" shows Android's prompt (Android 13+); "Not now" skips it.
6. **Done:** the maple leaf, days to go and the apply date appear, with confetti.

Cross-check with IRCC's own calculator (<https://eservices.cic.gc.ca/rescalc/resCalcStartNew.do?lang=en>): enter
the same PR date and trips, and use Eh-ligible's "Apply from" date as the application date. IRCC should show at
least 1,095 days. If it doesn't, send us your dates.

## Every day for a week

- **Status chip:** after opening the app with location on, it says *In Canada* (or the country you're in).
- **Widget:** Settings → *Add the widget to your home screen* (or long-press the home screen → Widgets →
  Eh-ligible). Try the small and the larger size. After midnight the number goes down by one without opening
  the app. Tapping the widget opens the app.
- **Battery:** Settings → Battery → app usage should show Eh-ligible near zero.

## Trips and the countdown

- **Add, edit, delete** a past trip in *Trips*: the days counted and the apply date update right away. The day
  you leave and the day you come back count as days in Canada; only full days away are subtracted.
- **Planned trip:** add one in the future: the apply date moves later by its full days away.
- **Export:** *Trips → Export*: share it to yourself (email or notes); the columns match the citizenship form.
- **Travel for real (if you do):** with location on, the app notices the trip on its own: the status changes to
  the country you're in and, if notifications are on, "Looks like you're travelling" appears. Days it couldn't
  check show up in *Review*; tell it where you were.

## Settings

- **Safety margin and countdown target:** switching to "Safer date" changes the big number and the date line.
- **Language:** Android Settings → System → Languages → App languages → Eh-ligible → Français: the app, its
  notifications and the widget should all switch to French (the widget at its next update).
- **Backup:** *Save a backup file*, then *Restore from a backup file*: your data comes back unchanged.
- **Links:** privacy policy, IRCC pages and source code open in the browser.

## What to send back

The phone model and Android version, what you did, what you expected, and what happened, ideally with a
screenshot. Email: jafarzadeh.mirhamed@gmail.com.
