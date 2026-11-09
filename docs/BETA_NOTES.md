# BETA_NOTES.md

**مواقيتي بلس / Mawaqiti Plus** — version `1.0.0-beta2` (versionCode 2)

| Item | Value |
|---|---|
| Package | `com.mawaqiti.plus` |
| APK | `C:\rea_ws\mawaqiti_app\dist\MawaqitiPlus-1.0.0-beta2.apk` |
| Size | 21,367,745 bytes |
| SHA-256 | `2904C000E647115FFE89B41EB6CCF4AC3B1763FB2BAC62577973A1AD80C795AE` |
| Build | debug (`assembleDebug`), not Play-store signed |
| Minimum Android | 7.0 (API 24), target API 36 |
| Permissions | none requested (see note below) |

> `aapt2` lists `com.mawaqiti.plus.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
> That is a **signature-level permission defined by the app itself** and added
> automatically by AndroidX Core; it is not a permission prompt and grants no
> access to location, network, storage or any user data. The app still declares
> no location/Internet permission.

---

## beta2 — what changed

This iteration is a presentation-layer overhaul. `TableEngine`, the embedded
tables, `cities.json`, and every calculation path are **untouched** and still
produce the beta1 results bit for bit.

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
  filled green.
- Search field with magnifier and placeholder `ابحث عن مدينتك…` /
  `Search your city…`. Filtering is live, case-insensitive, ignores Arabic
  diacritics/tatweel and alef/ya variants, and matches **both** Arabic and
  English names.
- القدس carries an **افتراضي** tag. The selected row is bold green with a check.
- Tapping a row selects it and closes the sheet. Changing country clears the
  search. The keyboard opens only when the field is tapped.
- No manual latitude/longitude entry exists anywhere. Selection persists in
  DataStore.
- **Jordan** is present as required but shows an honest empty state: the
  extracted APK contains no verified Jordanian city, so no fabricated
  `minDiff` is shown.

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
  about box with the official-timetable wording and `1.0.0-beta2`.

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

### What to test in beta2

1. **Theme:** open the app at different times (or with the build flag below) and
   confirm the four palettes, the 500 ms cross-fade, and that the night crescent
   and stars never touch any text or control.
2. **City picker:** search `قدس`, `Jeru`, `نابلس`, `Nabl`; switch countries and
   confirm the filter resets; confirm القدس has the **افتراضي** tag and the
   selected row is bold green with a check.
3. **Clock format:** switch 24h ↔ 12h and confirm every time changes instantly,
   including the settings preview; check midnight/noon on a device clock set
   accordingly.
4. **Icon:** confirm the new icon on the home screen, in the app switcher, and
   as a themed icon on Android 13+.
5. **Regression:** repeat the beta1 checks below (city times, language, summer
   time, qibla). Nothing in the engine changed.

### Visual smoke pass (no device attached)

No Android device or emulator was available in this workspace, so on-device
screenshots could not be captured. Two substitutes were produced:

- `report/sky_phases_preview.png` — a four-panel design-token mock rendering the
  exact gradient stops, top haze, glass fills, text and accent colours of DAWN,
  DAY, GOLDEN and NIGHT, including the night decor band.
- `SkyPaletteTest` — programmatic WCAG AA checks over the rendered backgrounds.

The phase override is wired as a build flag and is **empty in shipped builds**:

```powershell
.\gradlew.bat assembleDebug -Pmawaqiti.skyPhase=dawn   # or day|golden|night
```

All four variants were built successfully, and the final APK was rebuilt
without the flag (`SKY_PHASE_OVERRIDE = ""`).

---

## beta1 baseline (engine and behaviour)

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

1. Copy `MawaqitiPlus-1.0.0-beta2.apk` to the phone.
2. Open it and allow installation from the current file manager / browser when
   Android asks.
3. Or install over USB:

   ```powershell
   adb install -r "C:\rea_ws\mawaqiti_app\dist\MawaqitiPlus-1.0.0-beta2.apk"
   ```

4. First launch opens in Arabic on **القدس – فلسطين**. No permission prompt is
   shown because the app requests no permissions.

## What to test (unchanged engine behaviour)

### 1. Minute-for-minute match with the original app

Open the original **مواقيتي** app and this build side by side on the same date.
Check all six times (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) for:

- القدس (default),
- غزه, حيفا, الخليل, نابلس, رام الله, بيت لحم, بئر السبع,
- مكة المكرمة and المدينة المنورة (these use their own `makkah` / `madinah`
  profiles),
- at least one synthetic offset such as `قبل القدس بـ 3 د`.

The beta is expected to match the original exactly, every minute.

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
| Unit tests (engine + beta2 UI/formatter/palette/search) | **39 / 39 passed** |
| Solar engine vs Adhan 1.2.1 (20 cities x 24 dates, MWL) | **2879 / 2879 strictly compared events within ±1 min, worst 0.99 min** |
| Solar MWL vs official tables (90 cities x 8 dates) | mean absolute deviation **3.24 min**, max 21 min (informative only) |
| WCAG AA (all palettes, rendered backgrounds + glass cards) | normal text **≥ 4.5:1**, accent/large text **≥ 3.0:1** |
| Phase override builds | dawn / day / golden / night all `BUILD SUCCESSFUL` |

Reports: `TABLE_FIDELITY.md`, `CALIBRATION.md`, `ENGINE_CROSSCHECK.md`,
`CITY_COVERAGE.md`, `sky_phases_preview.png`, `icon_preview.png`.

## Known limitations

- **Tables repeat every year.** There is no year input, exactly like the
  original app. The table is indexed by day-of-year only.
- **No notifications, adhan, Hijri date, or widget.**
- **No GPS.** The qibla bearing uses the selected city's coordinates from
  `cities.json`; the app requests no location permission.
- **No Jordanian cities.** The extracted APK list contains none, and inventing
  `minDiff` values would break bit-exactness. The beta2 sheet still shows the
  الأردن segment, but it presents an honest empty state rather than fabricated
  times.
- **Ten synthetic entries** (`قبل/بعد القدس بـ N د`) are shown because they are
  part of the original list. Their `minDiff` values are inconsistent in the
  original and were preserved verbatim, not corrected.
- **No on-device visual pass.** No device/emulator was attached; verification
  relied on the generated phase preview and automated contrast tests.
- **No real backdrop blur below API 31.** Glass cards use translucent fill,
  border and sheen instead of live blur.
- **Debug build.** It is installable and reproducible, but it is not signed for
  distribution on Google Play.
- **Fully offline.** No network permission, no ads, no analytics, no Firebase or
  Play-services dependencies.

## Privacy

The app declares no user-facing permissions and contains no networking code.
Everything (tables, city list, qibla maths, fonts) is bundled in the APK.

## Feedback

Please report the city, the date, the device model, and the two times (original
vs beta) for any mismatch. A single off-by-one minute is a bug in this beta.
