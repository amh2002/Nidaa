# نداء | Nidaa — أوقات الصلاة | Prayer times

**الإصدار 1.0.0 — Android 7.0+ (API 24)** · offline-first, بلا إعلانات، بلا تتبّع

تطبيق أوقات صلاة عربي (Kotlin + Jetpack Compose) مبني على جداول التوقيت
الفلسطيني الرسمية المضمّنة داخل التطبيق، مع تاريخ هجري حسابي (أم القرى).
يعمل كامل التطبيق دون إنترنت: الحساب والجداول والأذان والأذكار كلها في الجهاز.

An offline prayer-time app for Android (Kotlin + Jetpack Compose), built on the
bundled official Palestinian timetable tables, with a computational Umm al-Qura
Hijri date. Everything runs on the device.

---

## Download — التحميل

**https://github.com/amh2002/Nidaa/releases/latest**

`Nidaa-1.0.0.apk` — versionCode 11, Android 7.0+ (API 24), signed release build
with R8 shrinking.

```text
SHA-256: 7A49E73169FC27D3CB1E9C7D751586A659DAADA3934A3E696F18D70FF9D6F528
```

`sha256sum Nidaa-1.0.0.apk` (or `Get-FileHash -Algorithm SHA256`) must match
before you install. The same digest is printed in the release notes, and the
in-app updater refuses any APK whose digest does not match the release body.

## Install — التثبيت

1. Send the APK to the phone and open it (or `adb install -r Nidaa-1.0.0.apk`).
2. Android asks to allow installs from this source: enable
   **«السماح من هذا المصدر» / "Allow from this source"** for the file manager or
   browser you opened it from, then go back.
3. If Play Protect shows **"Unsafe app blocked"**: tap **"More details" →
   "Install anyway"**. This is expected for any sideloaded app that is not
   distributed through Play.
4. Launch **نداء - Nidaa**; it opens in Arabic on **طولكرم - فلسطين**.

**Huawei / Honor (EMUI):** if the system warns about the app or refuses to
finish the install, open **Settings → Security → More settings → Install apps
from external sources**, confirm the prompt, and (on newer EMUI) make sure
**Pure Mode** is off for this source. After installing, allow Nidaa to run in
the background: **Settings → Battery → App launch → Nidaa → Manage manually**
(enable Auto-launch, Secondary launch, Run in background) and, if a hint card
appears in Nidaa's Settings, tap it and choose **Unrestricted / غير مقيّد**.
Exact alarm delivery depends on this on EMUI.

**Upgrade note:** after updating, **remove and re-add the home-screen widgets
once** so the launcher picks up the new resize sizes and layout.

## Privacy — الخصوصية

Offline-first: **no ads, no analytics, no tracking, no data collection.**

The **only** network access is the optional app-update check and download from
GitHub Releases, and it can be turned off in **Settings → «التحديثات» → «التحقق
التلقائي»**. With it off the app makes no network request at all (WorkManager
work is cancelled; «تحقق الآن» stays available if you ask for it explicitly).
No telemetry, crash-reporting or advertising SDK is linked; nothing about your
prayers, city or device leaves the phone.

Permissions requested and why:

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | show the prayer notification (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | fire the adhan at the exact minute |
| `RECEIVE_BOOT_COMPLETED` | rebuild the 48-hour alarm window after reboot |
| `WAKE_LOCK` | keep the adhan playing while the screen is off |
| `VIBRATE` | vibrate mode |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | play the adhan as a media foreground service |
| `INTERNET`, `ACCESS_NETWORK_STATE` | app-update check only |
| `REQUEST_INSTALL_PACKAGES` | install a SHA-256-verified update you approve |

No storage, location, contacts, camera or microphone permission. `allowBackup`
is off — settings stay on the device. The app never reads your location; the
qibla bearing uses the coordinates of the city you selected.

## Features — المزايا

- **Table engine first.** Every displayed time is a lookup in the original
  official tables with the original arithmetic (leap-aligned day row, per-city
  `minDiff`, carry/borrow, `hour % 24`, `%02d:%02d`). 90 cities: 88 Palestinian,
  مكة المكرمة and المدينة المنورة.
- **Living Sky theme.** Dawn / Day / Golden / Night palettes follow the table
  with a cross-fade; glass cards and gold accents.
- **Main screen.** Next prayer with countdown, the other prayers in a glass
  grid, dashed last-third-of-the-night card, city picker with live search.
- **Per-prayer notifications.** Adhan (bundled voice), silent, or vibrate;
  exact alarms; last-third reminder; lock-screen visibility.
- **Two home-screen widgets**, both resizable: the six-prayer main widget
  (compact layout on small instances, full grid when enlarged) and the
  next-prayer widget (name, time, «بعد Xh Ym»).
- **Settings.** City, Arabic/English, 12 h/24 h, in-app font scale (0.8×–1.2×),
  widget language, manual summer time (+1 h), notification modes, updates.
- **In-app update check.** Optional, 24-hour WorkManager check against GitHub
  Releases; download with progress, SHA-256 verified before the installer opens.

## Build and test — البناء والاختبار

Prerequisites: JDK 17, Android SDK with API 36, Gradle wrapper (included).

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot'
$env:ANDROID_HOME='C:\android-sdk'
.\gradlew.bat test assembleDebug          # debug
.\gradlew.bat testReleaseUnitTest assembleRelease   # R8 + signed release
```

Release signing reads `..\keys\keystore.properties` (outside this repository,
never committed). Without it `assembleRelease` produces an unsigned APK. The
keystore must be backed up off-device: losing it means no future version can
install over an existing install.

Reports are written to `docs/` by default; override with
`-Pmawaqiti.reportDir=<path>`.

## Verification

| Check | Result |
|---|---|
| Reference vectors (`test_vectors.csv`) | **392 rows, 0 mismatches** |
| 90 cities × 4 dates × summer OFF/ON | **360/360 day rows, 2880/2880 times** |
| City parity with the live reference | **90/90 cities** |
| Unit tests, debug | **145 / 145 passed** |
| Unit tests, release (R8) | **137 / 137 passed** |
| Release signature | APK Signature Scheme v2, RSA 4096, CN=Nidaa |
| Release APK size | 2,925,152 bytes (R8 + resource shrinking) |

Details: [`docs/TABLE_FIDELITY.md`](docs/TABLE_FIDELITY.md),
[`docs/CALIBRATION.md`](docs/CALIBRATION.md),
[`docs/ENGINE_CROSSCHECK.md`](docs/ENGINE_CROSSCHECK.md).

## Project layout

```
app/src/main/java/com/mawaqiti/plus/
  engine/table/            primary table engine + 36 × 366 tables
  engine/                  internal solar engine (tests/calibration only)
  format/                  24 h / 12 h formatter, Umm al-Qura Hijri date
  data/                    90-city model, assets loader, DataStore settings
  ui/                      Living Sky theme, screens, settings components
  notify/                  scheduler, channels, adhan service, widget updater
app/src/main/java/com/nidaa/prayer/update/   the ONLY network package
app/src/main/assets/cities.json              90 verified cities
app/src/main/res/layout*/                    widget layouts (AR/EN, compact, API 26 twins)
app/src/test/                                vectors, parity, widgets, updater, audit
```

## Known limitations

- Tables repeat every year (indexed by day-of-year); no year input.
- The Hijri line is computational (Umm al-Qura) and may differ by ±1 day from
  local moon sighting.
- The Jordan segment shows an honest empty state: no verified Jordanian city
  table is bundled, and none is invented.
- No real backdrop blur below API 31 (glass = fill + border + sheen).
- Widget rendering and layout are guarded by Robolectric renders; the launcher's
  own resize handles are OS behaviour.

## License and credit

Timetable data and the original application logic are the work of
**مؤسسة مفتاح - Abdulrahman Hussein**; this is an independent clean-room
implementation, not affiliated with any other app or publisher. Fonts: Tajawal
and Amiri (SIL OFL), licences in `app/src/main/assets/licenses/`.
