# Nidaa — نداء · أوقات الصلاة (Beta)

تطبيق مواقيت صلاة يعمل دون اتصال بالإنترنت، مبني بـ Kotlin وJetpack Compose،
ويعتمد جدول المواقيت الرسمي المعتمد لفلسطين والأردن المضمَّن داخل التطبيق
لجميع المدن المدعومة، مع تقويم هجري حسابي (أم القرى).

An offline prayer-time app for Android (Kotlin + Jetpack Compose) built on the
bundled official Palestinian timetable tables, with a computational Umm al-Qura
Hijri date.

---

## Download · التحميل

**https://github.com/amh2002/Nidaa/releases/tag/v1.0.0-beta7**

`Nidaa-1.0.0-beta7.apk` · versionCode 7 · Android 7.0+ (API 24)

The APK is a debug-signed beta, not a Play Store release.

> **beta5 changes the application id to `com.nidaa.prayer`.** Uninstall beta4
> before installing beta5; Android treats them as two different apps.

### Install · التثبيت

1. Copy the APK to the phone and open it.
2. Allow installation from the file manager / browser when Android asks.
3. Launch the app. It opens in Arabic on **القدس – فلسطين**.

Or over USB:

```powershell
adb install -r Nidaa-1.0.0-beta5.apk
```

---

## What this beta does

- **Table engine first.** Every displayed time is a lookup in the original
  app's tables (`w7.c`), with the original arithmetic (`p061i1.j.o()` /
  `p061i1.j.w()`): leap-aligned day row, per-city `minDiff`, carry/borrow,
  `hour % 24`, and `%02d:%02d`.
- **Living Sky auto theme.** Dawn / Day / Golden / Night palettes follow the
  table with a 500 ms cross-fade; glass cards, gold accents and a
  clearance-safe night crescent band are part of the design system.
- **Hero + grid main screen.** Next prayer with countdown and progress, the
  other five prayers in a two-column glass grid, dashed last-third card, and
  القبلة / الإعدادات footer buttons.
- **Searchable city picker.** Country segmented control (فلسطين / الأردن /
  السعودية), live case- and diacritic-insensitive search over Arabic and
  English names, افتراضي tag on القدس, one-tap selection.
- **12 h / 24 h clock setting.** Display-only, default 24 h, Arabic `ص/م` and
  English `a.m./p.m.`; stored values and the engine stay 24 h.
- **Qibla compass** from `TYPE_ROTATION_VECTOR`, using the selected city's
  coordinates (the app does not request location permission).
- **Arabic / English in-app switch** via `AppCompatDelegate.setApplicationLocales`
  with `locales_config.xml` and `autoStoreLocales`. RTL follows the locale.
  Times stay in Latin digits in both languages.
- **Manual summer time** (+1 h), default OFF, visible only for Palestine and
  applied only to the default table profile.
- **Notifications per prayer** with Adhan / silent / vibrate modes, an optional
  custom adhan sound, exact alarms, and a "last third of the night" reminder.
- **Home-screen widget**, fixed 4×2, with its own language (`widget.lang`,
  independent of the app language), Arabic right-to-left prayer order and an
  English mirror.
- **«نداء / Nidaa» identity.** Launcher label `نداء - Nidaa`, short Arabic
  in-app wordmark «نداء», and a new adaptive icon: a gold crescent with three
  echo arcs — the call travelling under the Living Sky.
- **New adaptive launcher icon** (night gradient #060A20→#1D2C62, gold #FFD98A
  crescent and three concentric call arcs, three stars, themed monochrome
  layer, round and square legacy mipmaps for API 24–25).
- **Fully offline.** No internet permission, no ads, no analytics, no Firebase
  or Play-services.

## City coverage

| Group | Entries |
|---|---:|
| فلسطين | 88 |
| السعودية (مكة المكرمة، المدينة المنورة) | 2 |
| Jordanian cities | 0 |

The verified embedded city list contains **no Jordanian city**. The beta does
not invent `minDiff` values, so the country segment **الأردن** is present but
shows an honest empty state. Full tables, including the ten synthetic
`قبل/بعد القدس` entries and the list of uncovered localities, are in
[`docs/CITY_COVERAGE.md`](docs/CITY_COVERAGE.md).

## Verification

| Check | Result |
|---|---|
| Reference vectors (124 rows × 6 prayers) | **124 / 124 exact, 0 mismatches** |
| 90 cities × 8 dates × summer OFF/ON | **1440 / 1440 exact, 0 mismatches** |
| Table comparisons incl. last third / Fajr tomorrow | **12,512, 0 mismatches** |
| Unit tests (engine + UI, Hijri, formatter, palette, search, brand, widget) | **94 / 94 passed** |
| Palette contrast (WCAG AA) | text **≥ 4.5:1**, accent **≥ 3.0:1** |
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
  engine/table/TableEngine.kt       primary, table-driven engine
  engine/table/PrayerTables.kt      generated 36 arrays × 366 rows
  engine/PrayerEngine.kt            internal solar engine (test-only)
  engine/Qibla.kt                   great-circle bearing/distance
  format/TimeFormatter.kt           24 h / 12 h display formatter
  format/HijriDateFormatter.kt      Umm al-Qura Hijri date, AR/EN
  data/City.kt, CityRepository.kt   90-city model + asset loader
  data/SettingsRepository.kt        DataStore: city, language, clock, summer time
  ui/theme/Sky.kt                   phases, palettes, WCAG colour math
  ui/theme/SkyBackground.kt         gradients, haze, night decor band
  ui/theme/Type.kt, Theme.kt        Tajawal + Amiri, animated Living Sky theme
  ui/components/Glass.kt            glass card + dashed outline
  ui/CitySearch.kt                  diacritic-insensitive city search
  ui/screens/                       Prayer, Qibla, Settings, CityPickerSheet
  notify/                           scheduler, channels, text, widget updater
  ui/theme/                         Living Sky, glass, adaptive icon palette
app/src/main/assets/cities.json     90 verified entries
app/src/main/assets/licenses/       Tajawal + Amiri OFL licences
app/src/main/res/layout/            widget_mawaqiti_ar.xml, widget_mawaqiti_en.xml
app/src/main/res/drawable/          ic_launcher_background/foreground/monochrome
app/src/test/                       vector test, 1440-case sweep, calibration,
                                     formatter, phase, palette, city-search,
                                     brand-identity and widget-layout tests
```

## Known limitations

- Tables repeat every year (no year input); the table is indexed by day-of-year.
- The Hijri line is computational (Umm al-Qura) and may differ by ±1 day from
  local Palestinian moon sighting.
- No GPS; the qibla bearing uses the selected city's coordinates.
- No verified Jordanian cities yet (the segment shows an empty state), while the
  About claim names both Palestine and Jordan as the official-table scope.
- No real backdrop blur below API 31 (glass = fill + border + sheen).
- No on-device visual pass was possible in this environment (no device and no
  emulator installed); see `report/screens_beta5/` and
  `report/icons/beta5/` for generated previews, plus the Robolectric layout,
  brand and widget tests that guard the same rules.
- Debug-signed APK.

## Privacy

No network permission and no network code, no ads, no analytics. Everything is
bundled in the APK. Notifications use POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM,
RECEIVE_BOOT_COMPLETED, VIBRATE, WAKE_LOCK and a media-playback foreground
service for the adhan; nothing is collected or transmitted.

## Disclaimer

This is an independent clean-room implementation made for personal use. It is
not affiliated with any other app or publisher.
