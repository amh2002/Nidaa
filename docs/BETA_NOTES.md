# BETA_NOTES.md

**نداء | Nidaa** — latest release `1.0.0-beta5` (versionCode 5), 11 Nov 2026

| Item | Value |
|---|---|
| Package | `com.nidaa.prayer` |
| APK | `C:\rea_ws\mawaqiti_app\dist\Nidaa-1.0.0-beta5.apk` |
| Size | 21,903,686 bytes |
| SHA-256 | `B2216695D12EC28D7519B254CF71C0F5CB3989A5BF4D113A8914FA59436E1483` |
| Build | debug (`assembleDebug`), not Play-store signed |
| Minimum Android | 7.0 (API 24), target API 36 |
| Launcher label | `نداء - Nidaa` (identical in every locale) |
| Suggested store title | «نداء | Nidaa - أوقات الصلاة» |

---

## beta5 — rename, "the Call" icon, About restructure, widget fixes

> **Uninstall beta4 first.** beta5 changes the `applicationId` from
> `com.mawaqiti.plus` to `com.nidaa.prayer`. Android therefore sees a different
> app: `adb install -r` over beta4 fails with
> `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Uninstall beta4, then install beta5.
> Widgets and notification settings from beta4 do not migrate.

### Rename mapping

| Surface | beta4 | beta5 |
|---|---|---|
| Product name (Arabic) | مواقيتي بلس | **نداء** |
| Product name (Latin) | Mawaqiti Plus | **Nidaa** (never translated to "Call") |
| `applicationId` | `com.mawaqiti.plus` | **`com.nidaa.prayer`** |
| Kotlin namespace | `com.mawaqiti.plus` | **unchanged** `com.mawaqiti.plus` (engine, tables and tests stay byte-identical) |
| Launcher label (`app_name`, both locales) | مواقيتي بلس / Mawaqiti Plus | **`نداء - Nidaa`** (one non-localized value) |
| Application + widget receiver label | `@string/app_name` (localized) | `@string/app_name` (single value) |
| Home hero wordmark | `@string/app_name` | **`@string/brand_name` = نداء** |
| Widget row 1 wordmark | `@string/app_name` | **`@string/brand_name` = نداء** |
| About title | `@string/app_name` | **`نداء · Nidaa`** (`settings_about_brand`) |
| About timetable line | "Times follow the official Palestinian timetable" | **one line covering both countries**: «أوقات الصلاة وفق الجدول الرسمي المعتمد لفلسطين والأردن.» / "Prayer times per the official tables for Palestine and Jordan." |
| About credit | "App creator: Abdulrahman Hussein" between two claims | **own separated last line**: «تطوير: عبد الرحمن حسين — Abdulrahman Hussein» |
| Footer card | `settings_footer` duplicated the About claims | **deleted**; About is the last list item |
| Intent actions | `com.mawaqiti.plus.action.*` | **`com.nidaa.prayer.action.*`** |
| Gradle root project | `MawaqitiPlus` | **`Nidaa`** |
| APK name | `MawaqitiPlus-1.0.0-beta4.apk` | **`Nidaa-1.0.0-beta5.apk`** |
| README / docs | Mawaqiti Plus | Nidaa |

Why the namespace stays: renaming `com.mawaqiti.plus` across
`engine/table/*` and the fidelity tests would rewrite the guarded engine files
and destroy the "engine untouched" proof. Only the installed identity changes.
Consequence: `com.mawaqiti.plus` still appears in `package`/`import`
declarations and in fully-qualified class names inside generated reports; that
is the Kotlin package, not the app identity.

Bidi note: `app_name` is stored once as `نداء - Nidaa`. On an Arabic (RTL)
launcher the label is laid out right-to-left, so it reads «نداء - Nidaa» with
نداء first; on an English launcher it reads left-to-right. No translation is
involved.

### Icon — "the Call" (نداء الصلاة)

Replaces the old monogram entirely: **no text or letters inside the icon.**

* **Background** (`drawable/ic_launcher_background.xml`): vertical night
  gradient `#060A20 → #101838 → #1D2C62` plus a radial `#FFD98A` glow at ~18 %
  peak opacity centred on the crescent, so the gold sits in light.
* **Foreground** (`drawable/ic_launcher_foreground.xml`): a gold `#FFD98A`
  crescent in the upper-right, then **three thin concentric arcs** with rounded
  caps at 70 / 45 / 25 % opacity radiating from the crescent — the adhan
  travelling outwards — plus three tiny four-point stars at 40–60 % opacity.
  Every element is inside the 66 dp safe zone (radius 33 around 54,54), so
  circle, squircle and rounded-square masks never clip it.
* **Monochrome** (`drawable/ic_launcher_monochrome.xml`): identical geometry in
  one white ink for Android 13+ themed icons; the platform tints it.
* **Legacy PNGs** (API 24–25): `mipmap-{m,h,x,xx,xxx}dpi/ic_launcher.png` and
  `ic_launcher_round.png`, rendered from the same geometry by
  `C:\rea_ws\tools\nidaa_icons.py` with rounded-square and circular masks.
* Meaning: the prayer call rises from the night sky and keeps travelling —
  the three arcs are the sound, the crescent is the sky it is called under.

Mask previews: `report/icons/beta5/icon_mask_circle.png`,
`icon_mask_squircle.png`, `icon_mask_rounded.png`, `icon_monochrome.png`,
`icon_monochrome_themed.png`, `icon_safe_zone.png`.

### About restructure

beta4 rendered the timetable/offline/no-data claims twice: once inside the
About card (with the creator line wedged between them) and again in a separate
footer card. beta5 removes the footer card completely and makes the About card
the **last** list item, so each claim exists in exactly one place:

1. title `نداء · Nidaa`
2. `الإصدار 1.0.0-beta5` / `Version 1.0.0-beta5`
3. **one** description: «أوقات الصلاة وفق الجدول الرسمي المعتمد لفلسطين
   والأردن.» / "Prayer times per the official tables for Palestine and Jordan."
4. two non-interactive info chips: «يعمل دون إنترنت» / "Fully offline" and
   «لا يجمع أي بيانات» / "Collects no data"
5. divider, then the credit alone at the bottom:
   «تطوير: عبد الرحمن حسين — Abdulrahman Hussein»

No links, no licences, no store text. The footer card component stays in the
framework (unused) for any future legal footer, and `FooterCard` is no longer
rendered. `SettingsFrameworkTest` now asserts: timetable claim exactly once,
zero footer nodes, credit reachable by scrolling — for ar/en at fontScale 1.0
and 1.3.

### Widget fixes

**W1 — row 1 overlap.** The old layout put the wordmark and city in the same
weighted row without a guaranteed gap, producing "مواقيتي بلسطولكرم". Now row 1
is two separate views anchored to opposite edges with a fixed **12 dp**
`<Space>` (`@dimen/widget_row_gap`) between them; the city uses
`layout_weight=1`, `maxLines=1`, `ellipsize=end`, and both are vertically
centred. The guaranteed gap and truncation are asserted by `WidgetLayoutTest`
(Robolectric inflation) and rendered with the longest real city name,
«البعينة (نجيدات)» / "Al-Bu'eina (Nujeidat)".

**W2 — reading order.** Two completely mirrored layouts are shipped and chosen
at update time:

| | Arabic (`widget_mawaqiti_ar.xml`) | English (`widget_mawaqiti_en.xml`) |
|---|---|---|
| Row 1 | wordmark right, city left | wordmark left, city right |
| Row 2 | gold next-prayer pill right, date left | pill left, date right |
| Row A | right→left: الفجر، الشروق، الظهر | left→right: Fajr, Sunrise, Dhuhr |
| Row B | right→left: العصر، المغرب، العشاء | left→right: Asr, Maghrib, Isha |

Both layouts force `layoutDirection="ltr"` and mirror explicitly in XML, so
the result is identical on every launcher. Latin digits and the 12 h/24 h
setting are respected in both.

**W3 — widget language.** A new Settings card «الويدجت / Widget» contains one
`SegmentedPillRow` with exactly two choices: العربية / English. The value is
stored in DataStore as **`widget.lang`** (`ar`/`en`). `AppSettings.widgetLanguage`
is nullable: `null` means "never chosen", and `effectiveWidgetLanguage` then
follows the app language — exactly "default = the app's current language at
first render" without writing a value at first launch. Changing it calls
`WidgetUpdater.updateAsync` immediately (no waiting for the next prayer), it is
independent of the UI language, all widget strings resolve from the chosen
language's resources (prayer names, city, Gregorian, Hijri, «القادمة/Next»), and
the launcher label is unaffected. Guarded by `WidgetLanguageTest`.

**W4 — fixed size.** `res/xml/mawaqiti_widget_info.xml` now declares
`android:resizeMode="none"`, `targetCellWidth=4`, `targetCellHeight=2`
(API 31+), `minWidth=250dp`, `minHeight=110dp` for older APIs, and
`widgetCategory="home_screen"`. Launchers show no resize handles, so there are
no collapsed/expanded variants.

### beta5 verification

| Check | Result |
|---|---|
| Reference vectors | **124 / 124**, 0 mismatches |
| 90-city sweep (8 dates × summer OFF/ON) | **1440 / 1440**, 0 mismatches |
| Table comparisons (incl. last third, Fajr tomorrow) | **12,512**, 0 mismatches |
| Unit tests | **94 / 94 passed** (19 suites) |
| Engine/table diff | `git status` shows **zero** changes under `engine/` or `assets/cities.json` |
| Old brand in code/resources | **0** occurrences of «مواقيتي بلس», "Mawaqiti Plus", "MawaqitiPlus" |
| APK identity | `aapt2 dump badging`: `package: name='com.nidaa.prayer' versionCode='5' versionName='1.0.0-beta5'` |
| Label | `application-label:'نداء - Nidaa'` and identical for **every** locale |
| Widget XML in APK | `resizeMode=0x00000000` (none), `targetCellWidth=4`, `targetCellHeight=2`, `widgetCategory=1` |
| Spot check, القدس 2026-01-01 | engine `fajr=05:12 maghrib=16:52 isha=18:13` = CSV `T004` (05:12 / 16:52 / 18:13) |

> **Verification environment (unchanged).** There is still no Android device
> and no emulator binary in this workspace, so device-only evidence
> (launcher label, Arabic app-drawer search, real launcher masks, widget
> long-press, `dumpsys package`, `dumpsys alarm`) could not be captured. It is
> substituted with `aapt2` inspection of the shipped APK, Robolectric inflation
> and Compose tests, and generated previews in `report/screens_beta5/` and
> `report/icons/beta5/`:
>
> * `launcher_home_ar.png` — icon + label «نداء - Nidaa»
> * `launcher_search_ar.png` — Arabic search for «نداء» finds the app
> * `icon_masks.png`, `icon_mask_*.png`, `icon_monochrome*.png`, `icon_safe_zone.png`
> * `settings_{ar,en}_{1.0,1.3}_bottom.png` — About card, claims once, credit alone
> * `widget_{ar,en}_{12h,24h}.png` — both reading orders and clock formats
> * `widget_{ar,en}_24h_long_city.png` — longest city name, no touching
> * `widget_long_press.png` — selection outline only, no resize handles

---

## beta4 — version 1.0.0-beta4

**مواقيتي بلس / Mawaqiti Plus** — version `1.0.0-beta4` (versionCode 4)

| Item | Value |
|---|---|
| Package | `com.mawaqiti.plus` |
| APK | `C:\rea_ws\mawaqiti_app\dist\MawaqitiPlus-1.0.0-beta4.apk` |
| Size | 21,865,272 bytes |
| SHA-256 | `64A3AB660715B55B03EAB4C802046A4CE4AC72C7FA6761516724F4183086ECC7` |
| Build | debug (`assembleDebug`), not Play-store signed |
| Minimum Android | 7.0 (API 24), target API 36 |
| Permissions | `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` |

> `aapt2` also lists `com.mawaqiti.plus.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
> That is a **signature-level permission defined by the app itself** and added
> automatically by AndroidX Core; it is not a permission prompt and grants no
> access to location, network, storage or any user data. The app still declares
> no location/Internet permission.

---

## beta4 — settings framework, notifications, widget

_(beta4 detail sections follow; beta5 above supersedes the footer/About parts.)_

> **Verification environment.** This workspace has no Android device or
> emulator (`adb devices` is empty and `C:\android-sdk\emulator\emulator.exe`
> does not exist). On-device-only evidence (launcher widget, `dumpsys
> notification`, reboot `dumpsys alarm`, real notification tray screenshots)
> could not be captured. It is replaced with: (a) Robolectric Compose UI tests
> for the settings framework, (b) pure JVM tests for scheduling/routing/widget
> strings, and (c) generated Compose-accurate previews in
> `report\screens_beta4\` and `report\hero_check\`. Those previews are labelled
> as generated, not device screenshots.

### Settings framework rule (permanent)

The Settings screen is now **one scrollable list**. Every item is a
`SettingsCard`; rows use `SettingsToggleRow` / `SegmentedPillRow`; the page ends
with exactly one `FooterCard`. There are no Box-stacked texts, negative
offsets, absolute positioning or fixed row heights anywhere in Settings.
Height is always automatic, so a large font scale grows the card instead of
piling text on top of it. Any future settings item must follow this rule.

The regression guard is `SettingsFrameworkTest` (Robolectric + Compose):
Arabic and English at fontScale 1.0 and 1.3, asserting exactly one footer text
semantics node and that scrolling reaches the footer without clipping.

### Footer double-text root cause

* `app/src/main/java/com/mawaqiti/plus/ui/components/Glass.kt:62` renders
  `GlassCard` content inside a plain `Box`.
* The beta3 About card (`app/src/main/java/com/mawaqiti/plus/ui/screens/SettingsScreen.kt`,
  lines 195–234) passed several sibling `Text`/`Spacer` children into that
  `Box`. `Box` stacks children at the same top-start position, so the timetable,
  creator and offline strings were painted on top of one another.

Fix: `SettingsCard` wraps content in a `Column`, and the footer is a dedicated
`FooterCard` with exactly one `Text` (`settings_footer`), resolved per locale
from `values/strings.xml` / `values-en/strings.xml` and placed last with 32 dp
bottom space.

### Notifications

* Master switch (default ON) plus one independent row per event: Fajr,
  Sunrise, Dhuhr, Asr, Maghrib, Isha, Last third. Each row has its own switch,
  exactly three modes (Adhan / Silent / Vibrate) and a **Test** button that
  fires that event's notification in its own mode five seconds later.
* Defaults: Fajr/Dhuhr/Asr/Maghrib/Isha = ON + Adhan; Sunrise and Last third =
  OFF + Silent. “Apply to all” sets one mode for every enabled event.
* Everything is stored per event in Preferences DataStore. The alarm only
  carries the event identity; **enabled/mode are read again at fire time**, so
  changing a mode never misfires an already-scheduled alarm.
* `SchedulePlanner` builds the next 48 hours from `TableEngine` only (about 14
  occurrences), honours the manual summer-time toggle, uses fixed per-event
  request codes and unique `mawaqiti://event/<key>/<epoch>` data URIs.
  `AlarmManager.setExactAndAllowWhileIdle` is used when exact alarms are
  allowed; otherwise `setWindow(…, 15 min)` is used and Settings shows an
  “Exact alarms are off” row that deep-links to
  `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
* Rescheduling happens after each fired alarm, after every notification/city/
  summer setting change, at midnight, and on boot / `TIME_SET` /
  `TIMEZONE_CHANGED` / `DATE_CHANGED` / app update.
* Delivery channels (exactly three): `adhan` (channel sound off; the app plays
  the audio), `silent` (no sound, no vibration), `vibrate` (vibration only).
  Adhan audio is a foreground service
  (`foregroundServiceType="mediaPlayback"`, `USAGE_ALARM`), with an “إيقاف”
  action that stops playback.
* `POST_NOTIFICATIONS` is requested with an Arabic rationale on the first
  Settings visit (API 33+). Settings also shows a battery-optimization hint
  that opens the system list.
* Adhan sound: generated license-free 4.2 s chime
  (`res/raw/placeholder_adhan.wav`) by default; “Choose file” uses
  `ACTION_OPEN_DOCUMENT` with a persisted read permission.

### Widget

Implemented with **RemoteViews**, not Glance: no extra dependency, minSdk 24
friendly, and the 4x2 layout is simple enough to keep pixel-stable. Fixed night
palette (`#0B102A → #141B3F`), translucent glass rows, gold `#FFD98A`, 18 dp
corners. It shows city + wordmark, Gregorian short date + Hijri date, the next
prayer/time in gold, and all six times in a 3-column × 2-row grid. It follows
the 12h/24h and language settings and opens the app on tap. There is no
per-second countdown. It refreshes on boot, after settings changes, at every
prayer boundary, and every 30 minutes as a safety net.

### How to test on a real device

1. Install the beta4 APK, open **Settings**.
2. Allow notifications in the Arabic rationale dialog.
3. If the “Exact alarms are off” card appears, tap it and allow
   “Alarms & reminders”.
4. Tap the battery-optimization card and exclude Mawaqiti Plus from
   optimization (manufacturer battery savers can still delay alarms).
5. Use each event's **Test** button, or the debug-only **Test notifications**
   group (Adhan / Silent / Vibrate). Each fires after ~5 seconds.
6. Set e.g. Fajr = Adhan, Asr = Silent, Isha = Vibrate and test all three to
   confirm independent routing.
7. Long-press the launcher → Widgets → Mawaqiti Plus → drag the 4x2 widget.
   Change the city or the 12h/24h setting and confirm the widget updates.

### Known limitations

* Widget has no live per-second countdown (by design in beta4).
* Bundled adhan is a short placeholder chime until the user picks their own
  audio file.
* Exactness depends on the OS exact-alarm grant and on manufacturer battery
  optimization; the app exposes both system settings but cannot force them.
* No device/emulator was available, so launcher, tray and reboot evidence is
  substituted as described above.

### Verification

* Unit tests: 78/78 debug + 74/74 release, 0 failures (the four Compose
  framework tests run in debug where the test host manifest is present).
* Fidelity unchanged: 124/124 vectors, 1440/1440 sweep, 12,512 comparisons,
  0 mismatches.
* Engine/table/cities diff: empty (`git diff --stat -- …/engine …/cities.json`).
* New tests: `SchedulePlannerTest` (fixed now, summer ON/OFF, midnight wrap,
  skip logic), `NotificationRoutingTest` (mixed modes at once), `WidgetTextTest`
  (12h/24h × both locales), `HeroMetricsTest`, `SettingsFrameworkTest`
  (both locales × fontScale 1.0/1.3, single footer, scroll reachable).
* Previews: `report\screens_beta4\` (settings full/bottom + notifications
  close-ups + 4 widget variants) and `report\hero_check\`.

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
