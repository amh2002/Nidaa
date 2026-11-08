# مواقيتي بلس · Mawaqiti Plus (Beta)

تطبيق مواقيت صلاة يعمل دون اتصال بالإنترنت، مبني بـ Kotlin وJetpack Compose،
ويطابق تطبيق **مواقيتي** الأصلي (`org.roki.tech.newbildqibla`) **دقيقة بدقيقة**
لجميع المدن المدعومة، لأنه يستخدم جداول المواقيت نفسها المستخرجة من التطبيق
الأصلي.

An offline prayer-time app for Android (Kotlin + Jetpack Compose) that is
**bit-exact with the original مواقيتي app** for every supported city, using the
same extracted 36 × 366 integer tables.

---

## Download · التحميل

**https://github.com/amh2002/mawaqiti-plus/releases/tag/v1.0.0-beta1**

`MawaqitiPlus-1.0.0-beta1.apk` · versionCode 1 · Android 7.0+ (API 24)

The APK is a debug-signed beta, not a Play Store release. It requests **no
permissions** and has no network access.

### Install · التثبيت

1. Copy the APK to the phone and open it.
2. Allow installation from the file manager / browser when Android asks.
3. Launch the app. It opens in Arabic on **القدس – فلسطين**.

Or over USB:

```powershell
adb install -r MawaqitiPlus-1.0.0-beta1.apk
```

---

## What this beta does

- **Table engine first.** Every displayed time is a lookup in the original
  app's tables (`w7.c`), with the original arithmetic (`p061i1.j.o()` /
  `p061i1.j.w()`): leap-aligned day row, per-city `minDiff`, carry/borrow,
  `hour % 24`, and `%02d:%02d`.
- **Six times + last third + live countdown** on the main screen.
- **Zero-typing city cascade.** Country card → read-only city dropdown.
  Default القدس – فلسطين. No search box, no free text, no manual coordinates.
- **Qibla compass** from `TYPE_ROTATION_VECTOR`, using the selected city's
  coordinates (the app does not request location permission).
- **Arabic / English in-app switch** via `AppCompatDelegate.setApplicationLocales`
  with `locales_config.xml` and `autoStoreLocales`. RTL follows the locale.
  Times stay in Latin digits in both languages.
- **Manual summer time** (+1 h), default OFF, visible only for Palestine and
  applied only to the default table profile — exactly like the original.
- **Fully offline.** No internet permission, no ads, no analytics, no Firebase
  or Play-services.

## City coverage

| Group | Entries |
|---|---:|
| فلسطين | 88 |
| السعودية (مكة المكرمة، المدينة المنورة) | 2 |
| Jordanian cities | 0 |

The original APK's embedded list contains **no Jordanian city**. The beta does
not invent `minDiff` values, so the country step offers فلسطين and السعودية
only. Full tables, including the ten synthetic `قبل/بعد القدس` entries and the
list of uncovered localities, are in [`docs/CITY_COVERAGE.md`](docs/CITY_COVERAGE.md).

## Verification

| Check | Result |
|---|---|
| Original app vectors (124 rows × 6 prayers) | **124 / 124 exact, 0 mismatches** |
| 90 cities × 8 dates × summer OFF/ON | **1440 / 1440 exact, 0 mismatches** |
| Table comparisons incl. last third / Fajr tomorrow | **12,512, 0 mismatches** |
| Solar engine vs Adhan 1.2.1 (test-only cross-check) | **2879 / 2879 within ±1 min**, worst 0.99 min |
| Solar MWL vs official tables (informative) | mean abs **3.24 min**, max 21 min |

Details:

- [`docs/TABLE_FIDELITY.md`](docs/TABLE_FIDELITY.md)
- [`docs/CALIBRATION.md`](docs/CALIBRATION.md)
- [`docs/ENGINE_CROSSCHECK.md`](docs/ENGINE_CROSSCHECK.md)

The solar engine remains in the codebase for tests and calibration only. It is
not reachable from the UI.

## Build and test

Prerequisites: JDK 17, Android SDK with API 36, Gradle wrapper (included).

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot'
$env:ANDROID_HOME='C:\android-sdk'
.\gradlew.bat test assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

To regenerate the reports into `docs/` (default) or another folder:

```powershell
.\gradlew.bat test -Pmawaqiti.reportDir=C:\rea_ws\report
```

## Project layout

```
app/src/main/java/com/mawaqiti/plus/
  engine/table/TableEngine.kt       primary, bit-exact table engine
  engine/table/PrayerTables.kt      generated 36 arrays × 366 rows
  engine/PrayerEngine.kt            internal solar engine (test-only)
  engine/Qibla.kt                   great-circle bearing/distance
  data/City.kt, CityRepository.kt   90-city model + asset loader
  data/SettingsRepository.kt        DataStore: city, language, summer time
  ui/screens/                       Prayer, Qibla, Settings, CitySelectionDialog
app/src/main/assets/cities.json     90 verified entries
app/src/test/                       vector test, 1440-case sweep, calibration
```

## Known limitations

- Tables repeat every year (no year input), exactly like the original app.
- No notifications, adhan, Hijri date, or widget in this beta.
- No GPS; the qibla bearing uses the selected city's coordinates.
- No Jordanian cities until a verified city list is available.
- Debug-signed APK.

## Privacy

No permissions, no network code, no ads, no analytics. Everything is bundled in
the APK.

## Disclaimer

This is an independent clean-room port made for personal use and verification
against the original application. It is not affiliated with the original
publisher.
