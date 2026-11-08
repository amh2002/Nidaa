# BETA_NOTES.md

**مواقيتي بلس / Mawaqiti Plus** — version `1.0.0-beta1` (versionCode 1)

| Item | Value |
|---|---|
| Package | `com.mawaqiti.plus` |
| APK | `C:\rea_ws\mawaqiti_app\dist\MawaqitiPlus-1.0.0-beta1.apk` |
| Size | 21,044,764 bytes |
| SHA-256 | `4F10E6BA5A6C587E2D4598BD0A40BEB23570875C9E24899A38B32FC07338DDAB` |
| Build | debug (`assembleDebug`), not Play-store signed |
| Minimum Android | 7.0 (API 24), target API 36 |
| Permissions | none (no location, no internet) |

## What this beta is

This build replaces the earlier solar-only prototype with a **bit-exact port of
the original app's table engine**. Every displayed time comes from the 36
integer arrays extracted from `w7.c`, adjusted exactly like `p061i1.j.o()` /
`p061i1.j.w()`:

- leap-aligned day row (row 59 = 29 February),
- optional manual summer time (+60 min, `default` profile only),
- per-city `minDiff` with carry/borrow,
- `hour % 24` and `%02d:%02d` formatting,
- last third of the night from today's Isha and tomorrow's Fajr.

The solar engine still exists in the codebase but is **test-only** and cannot be
reached from the UI.

## Install

1. Copy `MawaqitiPlus-1.0.0-beta1.apk` to the phone.
2. Open it and allow installation from the current file manager / browser when
   Android asks.
3. Or install over USB:

   ```powershell
   adb install -r "C:\rea_ws\mawaqiti_app\dist\MawaqitiPlus-1.0.0-beta1.apk"
   ```

4. First launch opens in Arabic on **القدس – فلسطين**. No permission prompt is
   shown because the app requests no permissions.

## What to test

### 1. Minute-for-minute match with the original app

Open the original **مواقيتي** app and this build side by side on the same date.
Check all six times (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) for:

- القدس (default),
- غزه, حيفا, الخليل, نابلس, رام الله, بيت لحم, بئر السبع,
- مكة المكرمة and المدينة المنورة (these use their own `makkah` / `madinah`
  profiles),
- at least one synthetic offset such as `قبل القدس بـ 3 د`.

The beta is expected to match the original exactly, every minute.

### 2. Zero-typing city cascade

- Tap the city chip in the main-screen header.
- Step 1 shows **فلسطين / السعودية** cards.
- Step 2 shows a read-only city dropdown for that country, sorted by the active
  language. No keyboard should ever appear.
- Confirm; every time on the main screen updates immediately.
- Repeat from **الإعدادات → المدينة**; it opens the same dialog.
- The choice survives an app restart.

### 3. Language switch

- **الإعدادات → اللغة → English**: the whole UI flips to English and LTR; city
  labels switch to their English transliterations.
- Switch back to **العربية**: UI returns to Arabic/RTL.
- Times must stay in Latin digits (`04:46`, not `٠٤:٤٦`) in both languages.
- The choice survives an app restart.

### 4. Summer time (Palestine only)

- Select a Palestinian city. **الإعدادات → التوقيت الصيفي** is visible,
  default **OFF**.
- Turning it ON adds exactly one hour to all six times and to the last third.
- The switch disappears when a Saudi city is selected, and the +1 hour is never
  applied to Makkah/Madinah tables (same as the original).
- Compare against the original app with its summer-time option in the same
  state.

### 5. Qibla

- Select القدس; the bearing should read about **157°** and the distance about
  **1238 km**.
- The dial uses `TYPE_ROTATION_VECTOR`; rotate the phone and check that the
  needle stays fixed to the Kaaba while the dial rotates.
- If the phone has no rotation-vector sensor, the screen shows a clear message
  instead of crashing.

## Verification already performed

| Check | Result |
|---|---|
| Original app vectors (124 rows x 6 prayers) | **124 / 124 rows exact, 0 mismatches** |
| Full sweep: 90 cities x 8 dates x summer OFF/ON | **1440 / 1440 cases, 0 mismatches** |
| Total table comparisons (including last third / Fajr tomorrow) | **12,512, 0 mismatches** |
| Solar engine vs Adhan 1.2.1 (20 cities x 24 dates, MWL) | **2879 / 2879 strictly compared events within ±1 min, worst 0.99 min** |
| Solar MWL vs official tables (90 cities x 8 dates) | mean absolute deviation **3.24 min**, max 21 min (informative only) |

Reports: `TABLE_FIDELITY.md`, `CALIBRATION.md`, `ENGINE_CROSSCHECK.md`,
`CITY_COVERAGE.md`.

## Known limitations (by design in this beta)

- **Tables repeat every year.** There is no year input, exactly like the
  original app. The table is indexed by day-of-year only.
- **No notifications, adhan, Hijri date, or widget.**
- **No GPS.** The qibla bearing uses the selected city's coordinates from
  `cities.json`; the app requests no location permission.
- **No Jordanian cities.** The extracted APK list contains none, and inventing
  `minDiff` values would break bit-exactness. The country step therefore offers
  فلسطين and السعودية only.
- **Ten synthetic entries** (`قبل/بعد القدس بـ N د`) are shown because they are
  part of the original list. Their `minDiff` values are inconsistent in the
  original and were preserved verbatim, not corrected.
- **Debug build.** It is installable and reproducible, but it is not signed for
  distribution on Google Play.
- **Fully offline.** No network permission, no ads, no analytics, no Firebase or
  Play-services dependencies.

## Privacy

The app declares **no permissions** and contains no networking code. Everything
(tables, city list, qibla maths) is bundled in the APK.

## Feedback

Please report the city, the date, the device model, and the two times (original
vs beta) for any mismatch. A single off-by-one minute is a bug in this beta.
