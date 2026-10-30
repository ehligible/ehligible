# Publishing Eh-ligible on Google Play

Everything here is a step only the owner can do (accounts, payments, identity, consoles). The code, signing key,
store texts and graphics are ready in the repo. Expect **about 3 weeks** from the first upload to the public
release: the first review, then the 14-day closed test, then the production review.

## 0. Before you start (one time)

1. **GitHub repo** (if not done yet), from this folder:
   ```
   gh repo create MirHamed-JZ/ehligible --public --source . --remote origin --push --description "Eh-ligible: a home-screen countdown to Canadian citizenship. Not affiliated with IRCC."
   ```
2. **Website for the privacy policy:** on GitHub, open the repo → Settings → Pages → *Build and deployment* →
   Source: **GitHub Actions**. The `Website` workflow then publishes `site/` to
   `https://mirhamed-jz.github.io/ehligible/` (privacy policy: `.../privacy.html`). Check that both pages load.
3. **Back up the upload key.** `C:\Users\mirha\.ehligible\` holds `upload-keystore.jks` and
   `keystore.properties` (its password). Copy both to a password manager or another safe place. If they're lost,
   Google can reset the upload key (Play App Signing), but it takes days.
4. **Signing secrets for automated releases** (GitHub → Settings → Secrets and variables → Actions):

   | Secret | Value |
   |---|---|
   | `UPLOAD_KEYSTORE_BASE64` | `base64 -w0 ~/.ehligible/upload-keystore.jks` |
   | `UPLOAD_KEYSTORE_PASSWORD` | `storePassword` from `keystore.properties` |
   | `UPLOAD_KEY_PASSWORD` | `keyPassword` from `keystore.properties` |

## 1. Create the developer account

1. Go to <https://play.google.com/console/signup> and choose a **personal** account.
2. Pay the one-time **US$25** fee, verify your identity (government ID) and contact details.
3. In the **Play Console app** on your Android phone, verify the device (Android 10 or newer).
4. Developer name: **Eh-ligible**. Contact email: `jafarzadeh.mirhamed@gmail.com`.
   Your legal name, country and this email are shown on the store page (your address and phone are not, since the
   app is free).
5. When asked (from November 2026) about **trader status** under the EU Digital Services Act: you are **not a
   trader** (no money is involved).

Identity verification can take a few days, so start early.

## 2. Create the app

Play Console → **Create app**:

| Field | Answer |
|---|---|
| App name | `Eh-ligible: Citizen Countdown` |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free |
| Declarations | Accept the policies and US export laws |

## 3. Store listing

**Grow → Store presence → Main store listing.** Copy from the repo:

| Item | File |
|---|---|
| Short description | `androidApp/src/main/play/listings/en-US/short-description.txt` |
| Full description | `androidApp/src/main/play/listings/en-US/full-description.txt` |
| App icon (512×512) | `androidApp/src/main/play/listings/en-US/graphics/icon/icon.png` |
| Feature graphic (1024×500) | `androidApp/src/main/play/listings/en-US/graphics/feature-graphic/feature.png` |
| Phone screenshots | `androidApp/src/main/play/listings/en-US/graphics/phone-screenshots/1.png` … `5.png` |

Then **Add translation → French (Canada) – fr-CA** with the files under `listings/fr-CA/`.

**Store settings:** category **Productivity**, contact email `jafarzadeh.mirhamed@gmail.com`, website
`https://mirhamed-jz.github.io/ehligible/`.

## 4. App content (Policy → App content)

| Section | Answer |
|---|---|
| Privacy policy | `https://mirhamed-jz.github.io/ehligible/privacy.html` |
| Ads | No, the app has no ads |
| App access | All functionality is available without special access (no login) |
| Content rating | Category *Utility, Productivity, Communication, or Other*; answer **No** to every content question (no violence, sexual content, gambling, user-to-user communication or location sharing with other users) |
| Target audience | **18 and over** |
| News app | No |
| Government apps | **No** (independent app, not made for or by a government) |
| Financial features | None |
| Health apps | No |
| Advertising ID | No, the app doesn't use it |
| Data safety | See below |

**Data safety:** answer **No** to "Does your app collect or share any of the required user data types?".
Everything the app processes (approximate location, mobile network country, trips) stays on the device; Google's
definition of *collection* is data transmitted off the device by the app, which never happens. The form then asks
nothing more. The privacy policy explains device backups.

**Permissions:** v1.0 uses approximate location only while the app is open, so there is no background-location
declaration yet. That comes with v1.1 (see `background-location.md`).

## 5. Closed test (starts the 14-day clock)

1. **Testing → Closed testing → Create track** (or use *Alpha*).
2. **Testers:** create an email list (e.g., "Eh-ligible testers") with the Gmail addresses of **at least 12
   people (aim for 15)** who have Android phones. Send them `docs/publishing/tester-invite.md`.
3. **Countries:** Canada (add others if testers live elsewhere).
4. **Create release:**
   - Accept **Play App Signing** (Google keeps the app-signing key; you keep the upload key).
   - Upload the bundle. The **first upload must be manual**: take `androidApp-release.aab` from the
     `ehligible-release` artifact of the *Android release* workflow (push the tag `v1.0.0`), or build it locally with
     `./gradlew :androidApp:bundleRelease` (output: `androidApp/build/outputs/bundle/release/`).
   - Release notes: `distribution/whatsnew/whatsnew-en-US` and `whatsnew-fr-CA`.
5. **Review and roll out.** After Google's review, copy the **opt-in link** from the track's *Testers* tab and send
   it to your testers. Each tester opens it, accepts, installs, and **stays opted in for 14 days in a row**.

## 6. Production

After 14 days with at least 12 opted-in testers, Play Console shows **Apply for production**. Answer the questions
about your test (what testers did, what you fixed) and wait for the review (usually up to 7 days). Then create a
production release with the same bundle, choose countries (Canada, or worldwide) and roll out.

## 7. Automatic uploads (optional, after the first manual upload)

1. Google Cloud console → create a project → enable the **Google Play Android Developer API**.
2. Create a **service account** and a JSON key for it.
3. Play Console → **Users and permissions → Invite new user** → the service account's email → grant
   *Release apps to testing tracks* (and *Release to production* if you want) for Eh-ligible.
4. Add the JSON as the GitHub secret `PLAY_SERVICE_ACCOUNT_JSON`.
5. From then on, pushing a tag like `v1.0.1` tests, builds, signs and uploads to the closed-testing track.
   Version codes come from the workflow run number, so every upload is higher than the last.
