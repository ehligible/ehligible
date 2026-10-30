# Publishing on the App Store (when the owner decides)

The iOS app is built, tested and launched on a simulator by the `iOS` GitHub Actions workflow on every push.
Publishing it waits for one decision by the owner: joining the Apple Developer Program. Nothing below needs a
Mac. Testing on a real iPhone needs testers with iPhones (TestFlight), since the owner has none.

## What it takes

- **Apple Developer Program**, individual: US$99 per year. Apple verifies identity; for individuals the
  developer name shown on the App Store is the legal name.
- Signing and uploads run on GitHub's macOS runners with an **App Store Connect API key** (no Mac, no
  certificates to manage by hand: Xcode's cloud-managed signing creates them).

## One-time setup

1. Enroll at <https://developer.apple.com/programs/enroll/> (individual).
2. **Certificates, Identifiers & Profiles → Identifiers:**
   - App Group `group.io.github.mirhamedjz.ehligible`.
   - App ID `io.github.mirhamedjz.ehligible` with the App Groups capability (that group).
   - App ID `io.github.mirhamedjz.ehligible.widget` with the same App Group.
3. **App Store Connect → Apps → New App:** platform iOS, name `Eh-ligible: Citizen Countdown`, primary language
   English (Canada), bundle ID `io.github.mirhamedjz.ehligible`, SKU `ehligible`. Add French (Canada).
4. **Users and Access → Integrations → App Store Connect API:** create a key with the *App Manager* role.
   Save the `.p8` file, the key ID and the issuer ID as repository secrets (`ASC_KEY_P8`, `ASC_KEY_ID`,
   `ASC_ISSUER_ID`). Never commit the `.p8` (it is in `.gitignore`).
5. Put the team ID (Membership page) in `iosApp/project.yml` → `DEVELOPMENT_TEAM`.
6. Add a release workflow (Claude can write it then): `xcodebuild archive` with `-allowProvisioningUpdates`
   and the API key (`-authenticationKeyPath/-ID/-IssuerID`), then `xcodebuild -exportArchive` with an export
   options plist using `method: app-store-connect` and `destination: upload`, which uploads to TestFlight.

## App Store Connect answers

| Field | Answer |
|---|---|
| App Privacy | **Data Not Collected** (everything stays on the device; `PrivacyInfo.xcprivacy` agrees) |
| Age rating | No objectionable content → 4+ |
| Category | Utilities (secondary: Lifestyle) |
| Price | Free, all territories (or Canada only) |
| Encryption | None beyond Apple's (`ITSAppUsesNonExemptEncryption` = NO is in Info.plist) |
| Privacy policy URL | `https://mirhamed-jz.github.io/ehligible/privacy.html` |
| Support URL | `https://mirhamed-jz.github.io/ehligible/` |

**Subtitle** (30 characters): `Countdown to citizenship` / `Décompte vers la citoyenneté`.

**Keywords** (100 characters): `citizenship,canada,permanent resident,PR,countdown,days,presence,widget,immigration,travel`
/ `citoyenneté,canada,résident permanent,RP,décompte,jours,présence,widget,immigration,voyage`.

**Description:** reuse `androidApp/src/main/play/listings/<lang>/full-description.txt`, mentioning the
"Always" location option in the trips paragraph (see `background-location.md` for that wording).

**Screenshots:** 6.9-inch iPhone (1320 × 2868). The CI simulator can produce them: add a Pro Max simulator to
the `iOS` workflow's screenshot step and seed `tools/screenshots/demo_seed.py` data, as it already does.

## Review notes (paste into "App Review Information")

> Eh-ligible counts the days a permanent resident spends in Canada toward the citizenship physical presence
> requirement. To try it, enter any permanent residence date (e.g., January 1, 2025) during onboarding.
> Location is approximate only (NSLocationDefaultAccuracyReduced) and is checked on the device; nothing is
> sent anywhere and there is no account. "Always" access is optional: with it, the significant-change location
> service lets the app notice trips abroad on days the app isn't opened, which is the app's core feature.
