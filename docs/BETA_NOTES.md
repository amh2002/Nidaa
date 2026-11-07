# BETA_NOTES.md — مواقيتي / Mawaqiti 1.0.0-beta1

Clean-room replacement for the original table-lookup Mawaqiti app. The new app
computes prayer times from the sun's position instead of copying fixed tables.

## Build artifact

| Item | Value |
|---|---|
| APK | `dist/MawaqitiPlus-1.0.0-beta1.apk` |
| Package | `com.mawaqiti.plus` |
| versionCode / versionName | `1` / `1.0.0-beta1` |
| minSdk / compileSdk / targetSdk | 24 / 36 / 36 |
| Size | 19,922,025 bytes (19.0 MB) |
| SHA-256 | `4833E24898D4038D07B1525827B5FA65D8C97D0631CABD0859AAA519F3F13433` |
| Build | `gradlew clean test assembleDebug` — BUILD SUCCESSFUL |

Install on a device with USB debugging enabled:

```
adb install -r dist\MawaqitiPlus-1.0.0-beta1.apk
```

## What this beta is

- Real solar engine: Julian date → solar longitude → declination and equation of
  time → Dhuhr transit → hour angles for Fajr/Isha, sunrise/sunset (−0.833°) and
  Asr (shadow factor 1 or 2). Event-time declination is solved iteratively, so
  sunrise/sunset/asr stay within seconds of a Meeus-based reference.
- Methods: MWL (18°/17°, default), ISNA (15°/15°), Egypt (19.5°/17.5°),
  Umm al-Qura (18.5°, Isha = Maghrib + 90 min), Karachi (18°/18°), and Custom
  angles.
- High-latitude rules: middle of the night (default) and angle-based.
- Times are rounded **up** to the next whole minute and converted with
  `java.time` in the device zone, so DST transitions are automatic.
- Extra values: sunrise, Imsak (Fajr − 10 min), solar midnight, and the start of
  the last third of the night (Fajr tomorrow − night/3, the original convention).
- Arabic-first RTL interface with an English translation, Material 3, light and
  dark themes.
- Three tabs: **الصلاة** (today's times, next-prayer countdown), **القبلة**
  (qibla compass) and **الإعدادات** (method, madhhab, high-latitude rule,
  location).

### Qibla

The compass uses `TYPE_ROTATION_VECTOR` and the great-circle bearing to the
Kaaba (21.422487° N, 39.826206° E). The dial rotates with the device heading,
shows the bearing and distance, and tells the user which way to turn. Sensor
accuracy is shown implicitly through the live reading; a device without a
rotation-vector sensor gets an explanatory card instead of a fake reading.

### Location

On first launch an Arabic rationale explains why fine location is requested.
Granting it gets a fix through Android's `LocationManager` (no Google Play
Services) and labels the position with the nearest bundled city when one is
within 150 km. Denying it, or having no fix, falls back to the default Al-Quds
coordinates; manual latitude/longitude and the 63-city search are always
available in Settings.

### Offline and privacy

The manifest requests only `ACCESS_FINE_LOCATION` and
`ACCESS_COARSE_LOCATION`. There is **no** INTERNET permission, no analytics, no
ads, and no Play Services dependency. All calculations happen on the device.
Settings are stored locally with DataStore.

## Verification

### Independent engine cross-check — `report/ENGINE_CROSSCHECK.md`

The production engine was compared with Adhan 1.2.1 (test scope only, never
packaged) at 20 cities × 24 dates × 6 events = **2,880 comparisons**, MWL
parameters, Shafi'i Asr, middle-of-the-night rule.

- **2,879 / 2,879 strictly compared events within ±1 minute (100%).**
- One additional case — London, 2026-07-15, Isha — is a grazing-twilight
  singularity: the sun's midnight altitude is −17.00° ± 0.01°, so the hour
  angle is numerically ill-conditioned. The engine resolves the angle-based
  time (23:50); Adhan's solver cannot bootstrap from its 00:00 UT declination
  and falls back to the night-middle bound (00:07). The case is reported
  separately in the cross-check and is not hidden.
- Maximum deviation among the 2,879 strict events: **1 minute**, which is the
  expected result of "round up" (this app) versus "round to nearest" (Adhan).

### Calibration against the original app — `report/CALIBRATION.md`

All 124 original vectors were evaluated with MWL, Shafi'i Asr and the
middle-of-the-night rule. This is a measurement, not a pass/fail oracle; no
parameter was tuned to the old tables. Vectors are normalised to standard time
(the engine runs at `round(longitude/15)` and +60 min is removed only for
`summer_time=yes` **default-profile** rows, matching the original app's
behaviour).

Natural-profile subset (the configuration the real app would use, 39 rows per
event, signed engine − vector in minutes):

| Event | Mean | Min | Max |
|---|---:|---:|---:|
| Fajr | +0.90 | −3 | +5 |
| Sunrise | +2.46 | 0 | +7 |
| Dhuhr | +0.87 | −1 | +4 |
| Asr | +0.97 | −2 | +5 |
| Maghrib | −3.97 | −9 | +1 |
| Isha | −6.44 | −20 | 0 |

The Maghrib/Isha bias is systematic: the old tables use their own fixed
twilight convention, while the new engine uses −0.833° for sunset and 17° for
Isha. The report includes per-city/per-event tables, the synthetic 55°N stress
case, the Umm al-Qura comparison, and a tuning-viability verdict: 79.6% of
city/event groups vary by ≤3 minutes across the year, so a constant offset
table would absorb the bias for most cases, but the remaining groups would
need seasonal (monthly) offsets. Such offsets are deliberately **not**
implemented in this beta; the engine stays purely astronomical.

## Known limitations (beta)

1. **No notifications or adhan sound.** This beta shows times and a countdown;
   alarms/notifications are the next feature.
2. **Qibla needs a magnetometer.** Accuracy depends on the device and on
   calibration; metal objects and magnetic cases distort the reading. The
   numeric bearing is always exact even when the compass is noisy.
3. **No Hijri calendar** and no home-screen widget yet.
4. **City list is hand-seeded.** The original APK's city classes contain no
   coordinates, so `assets/cities.json` is a curated list of 63 major cities.
   GPS and manual coordinates cover everywhere else.
5. **Grazing twilight.** As described above, at latitudes/dates where the sun
   barely reaches the Fajr/Isha angle, the angle-based time is inherently
   sensitive; the engine keeps the astronomical solution and the case is
   documented rather than special-cased.
6. **Original table offsets are not reproduced.** The old app applies a
   per-city minute correction from its tables; the new app intentionally does
   not copy it (see the calibration verdict).
7. **Extreme polar cases.** The engine needs a defined sunrise/sunset; at
   latitudes where the sun does not rise or set it reports a calculation error
   instead of inventing times.

## Reproduce

```
git clone <repository-url>
cd mawaqiti_app
./gradlew clean test assembleDebug
```

Requirements: JDK 17, Android SDK with platform 36 and build-tools 36, and the
Gradle wrapper bundled in the project. Reports are written to the repository's
`docs/` folder (override with `-Pmawaqiti.reportDir=<path>` if needed).
