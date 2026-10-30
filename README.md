# Eh-ligible 🍁

**Your countdown to Canadian citizenship, right on your home screen.**

Eh-ligible counts the days until a permanent resident can apply for Canadian citizenship
(1,095 days of physical presence in the 5 years before applying). It quietly checks — approximately and
entirely on your phone — whether you're in Canada each day, so international trips are handled
automatically.

- 📱 Home-screen widget with a filling maple leaf and the days left
- ✈️ Automatic trip detection (approximate location, processed offline; coordinates are never stored)
- 🧮 Official rules: departure/return days count, half-day credit for time before PR, rolling 5-year window
- 🗓️ Trip journal with export in the format IRCC's application asks for, plus "what-if" planned trips
- ✅ "Ready to apply?" checklist, milestones and confetti
- 🇨🇦 English and French · free · no ads · no tracking · no account

> **Not affiliated with Immigration, Refugees and Citizenship Canada (IRCC) or the Government of Canada.**
> Eh-ligible gives an estimate. Always confirm with IRCC's official tools before applying.

## Project layout

| Path | What |
|---|---|
| `core/` | Pure Kotlin Multiplatform logic: rules engine, presence model, trips, export |
| `composeApp/` | Shared Compose Multiplatform UI and app state |
| `androidApp/` | Android app: Glance widget, WorkManager checks, location |
| `iosApp/` | iOS app (XcodeGen project), WidgetKit extension, CoreLocation |
| `docs/` | [Spec](docs/SPEC.md), [glossary](docs/GLOSSARY.md), [research](docs/research/), publishing guide |
| `tools/` | Build-time scripts (border data, store graphics, screenshot data) |

The website (support page and privacy policy) is its own repo,
[ehligible/ehligible.github.io](https://github.com/ehligible/ehligible.github.io), served at
<https://ehligible.github.io/>.

## Building

Requirements: JDK 17+, Android SDK (platform 36).

```sh
./gradlew check                    # unit tests, lint, formatting (also compiles the iOS Kotlin code)
./gradlew :androidApp:installDebug # install the debug build on a device/emulator
```

The iOS app needs macOS with Xcode and [XcodeGen](https://github.com/yonaskolb/XcodeGen):

```sh
xcodegen generate --spec iosApp/project.yml   # writes iosApp/Ehligible.xcodeproj
open iosApp/Ehligible.xcodeproj               # the build runs Gradle for the Kotlin framework
```

Without a Mac, the `iOS` GitHub Actions workflow builds it, runs the Kotlin and widget tests on a simulator,
launches the app and uploads screenshots.

## License

[Apache License 2.0](LICENSE). The name "Eh-ligible" and its logo are not licensed for reuse.
Border data: [Natural Earth](https://www.naturalearthdata.com/) (public domain).
