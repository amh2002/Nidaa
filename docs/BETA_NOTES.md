# BETA_NOTES.md

**مواقيتي بلس / Mawaqiti Plus** — version `1.0.0-beta3` (versionCode 3)

| Item | Value |
|---|---|
| Package | `com.mawaqiti.plus` |
| APK | `C:\rea_ws\mawaqiti_app\dist\MawaqitiPlus-1.0.0-beta3.apk` |
| Size | 21,384,701 bytes |
| SHA-256 | `8A13DF87EB4CC3770113B4DA4DDBA32A199E9D3ED8997D27EB63BDCB38E62424` |
| Build | debug (`assembleDebug`), not Play-store signed |
| Minimum Android | 7.0 (API 24), target API 36 |
| Permissions | none requested (see note below) |

> `aapt2` lists `com.mawaqiti.plus.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
> That is a **signature-level permission defined by the app itself** and added
> automatically by AndroidX Core; it is not a permission prompt and grants no
> access to location, network, storage or any user data. The app still declares
> no location/Internet permission.

---

## beta3 — what changed

This is a focused hotfix on top of beta2. `TableEngine`, the embedded tables,
`cities.json`, and every calculation path are **untouched**; the full fidelity
suite still reports 124/124 vectors, 1440/1440 sweep cases and 0 mismatches
across 12,512 comparisons.

### FIX 1 — Settings rows never overlap

**Root cause.** In beta2 each settings card was one plain `Row`: the title used
`Modifier.weight(1f)` while the control (two choice pills or a `Switch`) took
its natural width. On a narrow RTL phone, at `fontScale 1.3`, or with a long
localised label, the control consumed nearly the whole card width. The weighted
title was then measured into almost no space, wrapped to several lines, and
drew underneath / behind the control. `ChoicePill` also forced `maxLines = 1`,
so long labels were clipped instead of wrapping. The summer-time card had the
same pattern with a nested `Column(weight(1f))` + `Switch`.

**Fix.** A new adaptive row, `ui/components/SettingRow.kt`:

- `SubcomposeLayout` measures the control at its natural width first.
- `SettingRowMetrics.shouldStack(contentWidth, controlWidth, gap, minLabelWidth)`
  keeps the control trailing only while the label column is at least **132 dp**;
  otherwise the control moves to a **new line under the text**. The rule is a
  pure function and is unit-tested.
- Title is line 1 and the description is line 2 in a single `Column`, so they
  can never be separated or overlapped.
- Vertical padding is `12 dp` per row; height comes from content (`minHeight`),
  never a fixed height. There is no absolute positioning.
- `ChoicePill` no longer sets `maxLines = 1`.

**Verification.** Eight variants were rendered and checked programmatically:

| Variant | Result |
|---|---|
| AR × 24h × fontScale 1.0 | PASS |
| AR × 24h × fontScale 1.3 | PASS |
| AR × 12h × fontScale 1.0 | PASS |
| AR × 12h × fontScale 1.3 | PASS |
| EN × 24h × fontScale 1.0 | PASS |
| EN × 24h × fontScale 1.3 | PASS |
| EN × 12h × fontScale 1.0 | PASS |
| EN × 12h × fontScale 1.3 | PASS |

Longest label used: `البعينة (نجيدات) – فلسطين` /
`Al-Bu'eina (Nujeidat) – Palestine`. `overlap_report.txt` ends with
`RESULT: PASS - zero overlaps in all 8 variants`; `SettingRowMetricsTest`
adds 4 boundary tests (wide, narrow, large-control, exact-minimum).

Screenshots: `C:\rea_ws\report\settings_check\settings_{ar,en}_{12h,24h}_{1.0,1.3}.png`
and `C:\rea_ws\report\settings_check\overlap_report.txt`.

*No Android device or emulator exists in this workspace, so these are
design-accurate Compose-layout previews plus automated rectangle-intersection
checks, not on-device screenshots.*

### FIX 2 — city picker is always dark

The sheet no longer reads `SkyPalette`; it uses fixed `CitySheetPalette` tokens
(`ui/theme/CitySheet.kt`) in every Living Sky phase:

- gradient `#0B102A → #141B3F` (solid `#0E1540` fallback), top radius **26 dp**;
- grabber `rgba(255,255,255,.25)`, primary text `#EEF2FF`;
- dividers `rgba(255,255,255,.08)`;
- search field fill `.06`, border `1.5 dp` at `.22`, placeholder/magnifier `.45`;
- segmented track `.08`, active fill `rgba(255,255,255,.16)` with **bold
  `#FFD98A`**; selected city bold `#FFD98A` + check; **افتراضي** tag at `.5`;
- scrim `rgba(0,0,0,.55)`.

**Active segment choice.** White `.16` + bold gold was chosen over a solid
`#1E4D3B` fill because it measures a higher contrast ratio at both gradient
ends — **8.79:1** at the top and **7.49:1** at the bottom — while keeping the
specified track/active relationship and avoiding a second green in an otherwise
night-blue + gold palette.

While the sheet is visible the app asks the system for **light status/navigation
icons** (`WindowCompat.getInsetsController`, both flags `false`) and restores
the previous values when it closes. The IME's own surface is drawn by the
keyboard process; the app can only request the system-bar appearance.

Behaviour is unchanged: country segments (فلسطين / الأردن / السعودية), live
search, single-tap selection, default-city tag, Jordan empty state.

Previews: `report\city_sheet_day.png`, `report\city_sheet_night.png`,
`report\city_sheet_preview.png`. Contrast is enforced by
`CitySheetPaletteTest` (6 tests; primary ≥ 14.9:1, muted ≥ 7.8:1, active gold
≥ 7.49:1, hint ≥ 4.39:1 against the 3:1 graphical threshold).

### FIX 3 — Hijri date

A dimmer second date line now sits directly under the Gregorian date on the
main screen: `bodySmall`, 75 % opacity in the dark phases (muted token in the
light phases to keep WCAG AA), always **Latin digits**:

- Arabic: `19 جمادى الآخرة 1448هـ`
- English: `19 Jumada al-Akhirah 1448 AH`

Implementation is `format/HijriDateFormatter.kt`. The primary conversion is
`java.time.chrono.HijrahDate.from(LocalDate)` — the Umm al-Qura chronology,
available down to API 24 through core-library desugaring. If that conversion
ever throws, the code falls back to Android ICU
`IslamicCalendar.CalculationType.ISLAMIC_UMALQURA`. **No moon-sighting
arithmetic is hand-rolled**; both paths delegate to the platform chronology.
The input is the same local `LocalDate` the app already uses as "today", so the
line follows the device timezone and rolls over at local midnight. It is not a
settings row and adds no persistence.

`HijriDateFormatterTest` (7 tests) covers: a fixed 1448-06-19 render in both
locales; every month's 1st and last day (29 or 30) round-tripping through
Gregorian; all twelve names in both languages; the `هـ` / `AH` suffix; Latin
digits only; local-midnight rollover; and month-argument validation. Expected
Gregorian↔Hijri pairs are derived from the chrono library itself, not invented.

**Caveat.** The Umm al-Qura calendar is computational. Local Palestinian moon
sighting can differ by **±1 day**, so the Hijri line is informative, not a
religious ruling.

### FIX 4 — provenance wording removed

All user-facing claims that the app matches another app were removed. The APK
was audited after signing: the old phrases are absent from `resources.arsc` and
from all 12 dex files.

| File | Removed | Replacement |
|---|---|---|
| `app/src/main/res/values/strings.xml` | `official_note`: «…مطابقة للتطبيق الأصلي دقيقة بدقيقة.» | «المواقيت وفق الجدول الرسمي الفلسطيني.» |
| `app/src/main/res/values-en/strings.xml` | `official_note`: “…identical to the original app, minute for minute.” | “Times follow the official Palestinian timetable.” |
| `app/src/main/res/values/strings.xml` | `jordan_empty_body`: «…توقيتًا غير مطابق للتطبيق الأصلي.» | «…توقيتًا غير موثّق.» |
| `app/src/main/res/values-en/strings.xml` | `jordan_empty_body`: “…a time that differs from the original app.” | “…an unverified time.” |
| `app/src/main/res/values/strings.xml` | `settings_about_body` (whole paragraph) | removed; replaced by `settings_about_timetable` |
| `app/src/main/res/values-en/strings.xml` | `settings_about_body` (whole paragraph) | removed; replaced by `settings_about_timetable` |
| `README.md` | “bit-exact with the original app”, “minute-for-minute”, “exactly like the original app”, affiliation sentence | neutral timetable/Hijri description |

The About card now says only «الأوقات وفق التقويم الرسمي الفلسطيني» /
“Times follow the official Palestinian timetable”.

Remaining `original app` hits are **internal Kotlin comments** (engineering
provenance in `City.kt`, `AppSettings.kt`, `PrayerEngine.kt`, `TableEngine.kt`).
None is rendered, none is a promise to the user, and `TableEngine` was left
untouched per the beta3 constraint.

### FIX 5 — About order

`الإعدادات → حول التطبيق / About` is now, top to bottom:

1. app name + version (`مواقيتي بلس` / `Mawaqiti Plus`, `1.0.0-beta3`);
2. «الأوقات وفق التقويم الرسمي الفلسطيني» / “Times follow the official
   Palestinian timetable”;
3. «منشئ التطبيق: عبد الرحمن حسين» / “App creator: Abdulrahman Hussein”;
4. «يعمل دون اتصال بالإنترنت ولا يجمع أي بيانات» / “Works offline and collects
   no data”.

### beta3 verification

| Check | Result |
|---|---|
| Debug unit tests | **56 / 56 passed** |
| Release unit tests | **56 / 56 passed** |
| Reference vectors (124 rows × 6 prayers) | **124 / 124 exact, 0 mismatches** |
| Full sweep: 90 cities × 8 dates × summer OFF/ON | **1440 / 1440 cases, 0 mismatches** |
| Total table comparisons | **12,512, 0 mismatches** |
| Settings overlap matrix | **8 / 8 variants, zero overlaps** |
| City-sheet contrast | primary ≥ 14.9:1, muted ≥ 7.8:1, active gold ≥ 7.49:1 |
| APK audit (old phrases in resources/dex) | **absent** |
| APK version | `versionCode 3`, `versionName 1.0.0-beta3` |
| `SKY_PHASE_OVERRIDE` | `""` (auto phase in the shipped APK) |

### What to test in beta3

1. **Settings:** open Settings in Arabic and English, 24h and 12h, on the
   smallest screen you have, with system font size enlarged. No title,
   description or control may overlap; long city names must wrap.
2. **City picker:** open it during the day and at night; it must stay dark in
   both. Check the search field, segments, selected row and **افتراضي** tag.
3. **Hijri date:** compare the second date line with a known Umm al-Qura
   calendar; remember the ±1-day sighting caveat.
4. **Wording:** About must show app name + version, timetable line, creator,
   offline line — with no comparison to another app anywhere.
5. **Regression:** repeat the beta2 checks (Living Sky, language, 12h/24h,
   summer time, qibla). The engine is unchanged.

---

## beta2 — what changed

This iteration was a presentation-layer overhaul. `TableEngine`, the embedded
tables, `cities.json`, and every calculation path were **untouched** and still
produce the beta1 results exactly.

### 1. Living Sky auto theme

The whole app tints itself from the current table:

| Phase | Window | Look |
|---|---|---|
| **DAWN** | Fajr − 90 min → sunrise | deep indigo-violet `#1A1440 → #3A2C63`, warm amber glow at the bottom |
| **DAY** | sunrise → Asr | azure `#2F6EA6 → #5C9CC9 → #A8D0E8` |
| **GOLDEN** | Asr → Maghrib | day gradient warmed with `#F2B263` overlay and a horizon tint |
| **NIGHT** | Maghrib → dawn | `#060A20 → #0E1540 → #1D2C62`, gold crescent and faint stars |

- The four palettes cross-fade over **500 ms** at the phase boundary.
- Night decoration (crescent + star dots) is drawn only inside a fixed
  **60 dp band** between the header and the hero card. The band is layout
  space, so the crescent can never touch the wordmark, chips, hero, grid or
  buttons in any phase.
- Glass cards follow the spec: Day `rgba(255,255,255,.16)` fill / `.35` border,
  Night `.075` / `.16`, radius 17–22 dp, plus a top sheen. Real backdrop blur
  is unavailable below API 31 without a RenderEffect trade-off, so the glass is
  translucent fill + border + sheen (documented in `ui/components/Glass.kt`).
- Text contrast is enforced by unit tests: every normal-size text colour is
  ≥ 4.5:1 and every accent/large-text use is ≥ 3.0:1 against the *rendered*
  background (gradient + top haze) and against the composited glass cards.
  The two light phases paint a pale haze from the top that fades out by 42 % of
  the screen height; this is what keeps `#0D2438` text at WCAG AA over the Day
  gradient's darkest stop, without altering the authoritative gradient tokens.
- Bundled fonts: **Tajawal** Regular/Medium/Bold everywhere, **Amiri Bold** for
  the wordmark only. OFL licences are shipped in `assets/licenses/` because
  `res/font` rejects `.txt` files.

### 2. New city picker

- Opens as a bottom sheet from the header chip or **الإعدادات → المدينة**.
- Country segmented control: **فلسطين | الأردن | السعودية**, active segment
  filled.
- Search field with magnifier and placeholder `ابحث عن مدينتك…` /
  `Search your city…`. Filtering is live, case-insensitive, ignores Arabic
  diacritics/tatweel and alef/ya variants, and matches **both** Arabic and
  English names.
- القدس carries an **افتراضي** tag. The selected row is bold gold with a check.
- Tapping a row selects it and closes the sheet. Changing country clears the
  search. The keyboard opens only when the field is tapped.
- No manual latitude/longitude entry exists anywhere. Selection persists in
  DataStore.
- **Jordan** is present as required but shows an honest empty state: the
  verified list contains no Jordanian city, so no fabricated `minDiff` is shown.

### 3. Clock format setting

- **الإعدادات → نظام الساعة / Clock format**: `24 ساعة` (default) or `12 ساعة`.
- 12-hour rendering uses `h:mm` with `ص/م` in Arabic and `a.m./p.m.` in English;
  midnight is `12:00 ص` / `12:00 a.m.`, noon is `12:00 م` / `12:00 p.m.`.
- The choice persists and applies instantly to the hero, grid, last-third card
  and settings preview. The hero countdown stays elapsed-style `h:mm:ss`.
- This is display-only: engine values, DataStore values, table comparisons and
  all tests remain 24-hour. Digits are always Latin.
- Unit tests cover `00:00`, `00:59`, `12:00`, `12:59`, `23:59` in both locales,
  the spec examples (`5:09 ص`, `6:21 م`), wrap-around and Latin-digit checks.

### 4. Screens

- **Main:** safe header (Amiri wordmark, city chip, language pill), date line,
  hero card (next prayer label/name/time, `متبقي h:mm:ss` pill, previous→next
  progress bar), 2-column glass grid of the other five prayers, dashed-outline
  last-third card, footer buttons **القبلة** and **الإعدادات**.
- **Qibla:** same Living Sky background, glass dial card, rotation-vector
  azimuth, great-circle bearing to the Kaaba (21.422487 N, 39.826206 E),
  rotating accent needle, numeric degrees, Arabic/English hints.
- **Settings:** city, language, clock format, summer time (Palestine only),
  about box.

### 5. Launcher icon

- Adaptive icon: night-sky gradient background + faint amber corner glow; gold
  crescent (`#FFD98A`) cradling one pale four-point star; no text.
- All art lies inside the 66/108 safe zone (crescent bbox x 27.0–67.8,
  y 31.0–77.0; star bbox x 66.6–80.4, y 60.4–74.2 in the 108-unit canvas).
- Monochrome layer for Android 13+ themed icons.
- Round + square legacy PNGs at mdpi–xxxhdpi (48–192 px); `roundIcon` points to
  `@mipmap/ic_launcher_round`.
- A stale beta1 `mipmap-anydpi/ic_launcher.xml` (old green icon) was found and
  removed during APK verification; it had been shadowing the new PNGs on
  API 24–25.
- Preview: `C:\rea_ws\report\icon_preview.png` (512 px).

---

## beta1 baseline (engine and behaviour)

This build replaces the earlier solar-only prototype with a reimplementation of
the verified table path. Every displayed time comes from the 36 integer arrays
extracted from the reference APK (`w7.c`), adjusted the same way as
`p061i1.j.o()` / `p061i1.j.w()`:

- leap-aligned day row (row 59 = 29 February),
- optional manual summer time (+60 min, `default` profile only),
- per-city `minDiff` with carry/borrow,
- `hour % 24` and `%02d:%02d` formatting,
- last third of the night from today's Isha and tomorrow's Fajr.

The solar engine still exists in the codebase but is **test-only** and cannot be
reached from the UI.

## Install

1. Copy `MawaqitiPlus-1.0.0-beta3.apk` to the phone.
2. Open it and allow installation from the current file manager / browser when
   Android asks.
3. Or install over USB:

   ```powershell
   adb install -r "C:\rea_ws\mawaqiti_app\dist\MawaqitiPlus-1.0.0-beta3.apk"
   ```

4. First launch opens in Arabic on **القدس – فلسطين**. No permission prompt is
   shown because the app requests no permissions.

## What to test (engine behaviour)

### 1. Time agreement with the reference timetable

Open the reference timetable and this build side by side on the same date.
Check all six times (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) for:

- القدس (default),
- غزه, حيفا, الخليل, نابلس, رام الله, بيت لحم, بئر السبع,
- مكة المكرمة and المدينة المنورة (these use their own `makkah` / `madinah`
  profiles),
- at least one synthetic offset such as `قبل القدس بـ 3 د`.

### 2. City selection

- Tap the city chip or use **الإعدادات → المدينة**; both open the same sheet.
- Select a city and confirm every time updates immediately.
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
  applied to Makkah/Madinah tables.
- Compare with the reference timetable in the same summer-time state.

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
| Reference vectors (124 rows x 6 prayers) | **124 / 124 rows exact, 0 mismatches** |
| Full sweep: 90 cities x 8 dates x summer OFF/ON | **1440 / 1440 cases, 0 mismatches** |
| Total table comparisons (including last third / Fajr tomorrow) | **12,512, 0 mismatches** |
| Unit tests (engine + beta3 UI/Hijri/formatter/palette/search) | **56 / 56 passed** (debug and release) |
| Solar engine vs Adhan 1.2.1 (20 cities x 24 dates, MWL) | **2879 / 2879 strictly compared events within ±1 min, worst 0.99 min** |
| Solar MWL vs official tables (90 cities x 8 dates) | mean absolute deviation **3.24 min**, max 21 min (informative only) |
| WCAG AA (all palettes, rendered backgrounds + glass cards) | normal text **≥ 4.5:1**, accent/large text **≥ 3.0:1** |
| City sheet (always dark) | primary ≥ 14.9:1, muted ≥ 7.8:1, active gold ≥ 7.49:1 |
| Settings overlap matrix | **8 / 8 variants, zero overlaps** |
| Phase override builds | dawn / day / golden / night all `BUILD SUCCESSFUL` |

Reports: `TABLE_FIDELITY.md`, `CALIBRATION.md`, `ENGINE_CROSSCHECK.md`,
`CITY_COVERAGE.md`, `settings_check\overlap_report.txt`, `sky_phases_preview.png`,
`city_sheet_preview.png`, `icon_preview.png`.

## Known limitations

- **Hijri date is computational.** Umm al-Qura can differ from local moon
  sighting by ±1 day.
- **Tables repeat every year.** There is no year input; the table is indexed by
  day-of-year only.
- **No notifications, adhan, or widget.**
- **No GPS.** The qibla bearing uses the selected city's coordinates from
  `cities.json`; the app requests no location permission.
- **No Jordanian cities.** The verified list contains none, and inventing
  `minDiff` values would break fidelity. The sheet still shows the الأردن
  segment with an honest empty state rather than fabricated times.
- **Ten synthetic entries** (`قبل/بعد القدس بـ N د`) are shown because they are
  part of the verified list. Their `minDiff` values are inconsistent in the
  source and were preserved verbatim, not corrected.
- **No on-device visual pass.** No device/emulator was attached; verification
  relied on generated layout previews and automated intersection/contrast tests.
- **No real backdrop blur below API 31.** Glass cards use translucent fill,
  border and sheen instead of live blur.
- **IME colour is system-owned.** The sheet forces light system-bar icons, but
  the keyboard's own background is drawn by the IME process.
- **Debug build.** It is installable and reproducible, but it is not signed for
  distribution on Google Play.
- **Fully offline.** No network permission, no ads, no analytics, no Firebase or
  Play-services dependencies.

## Privacy

The app declares no user-facing permissions and contains no networking code.
Everything (tables, city list, qibla maths, fonts) is bundled in the APK.

## Feedback

Please report the city, the date, the device model, and the two times
(reference vs beta) for any mismatch. A single off-by-one minute is a bug in
this beta.
