# Nidaa v2.2.0 — release build, regression QA and publication round

- Date: 2026-11-24
- Repo: `C:\rea_ws\mawaqiti_app` (junction → `…\Documents\مواقيتي\re_workspace`), branch `golden-night`
- HEAD at start: `6c1b252` — beta32 (unchanged, as the brief expected)
- Release identity: `versionName 2.2.0`, `versionCode 17`, package `com.nidaa.prayer`
- **Outcome: all local gates passed; publishing STOPPED at VERIFY-FIRST item 5 (remote `main` has diverged). Nothing was pushed, no GitHub Release was created.**
- Deliverables: `dist/nidaa-v2.2.0-release.apk`, `CHANGELOG.md`, `PRIVACY.md`, `release_notes_ar.md`, one local commit `release: v2.2.0`, one local annotated tag `v2.2.0`, 21 `rel_*` evidence files.

---

## 0. STOP summary (read this first)

| Gate | Result |
|---|---|
| Build + signing + version | PASS |
| Regression on both emulators, day and night | PASS |
| Release content (CHANGELOG / PRIVACY / notes) | PASS |
| Local commit + annotated tag | DONE locally |
| Push to origin | **NOT EXECUTED — remote history is unknown to local** |
| GitHub Release | **NOT CREATED** |

`origin/main` is at `c14af6784c26f10c1f0d10b3d7f12fc6b388077c` ("docs: update README with screenshots", 2026-11-21), which **does not exist in the local object store**. The GitHub compare API shows it is the tip of 16 documentation/screenshot commits built on `31a3b03`, the same commit local `main` sits on. Local HEAD (`6c1b252` + this release commit) also descends from `31a3b03`. The two histories therefore **diverged**: pushing `HEAD` to `main` would be a non-fast-forward rejection, resolvable only by a fetch + merge/rebase — every one of which the brief forbids.

VERIFY-FIRST item 5 says exactly this case is a STOP: *"If the remote has diverged or its history is unknown to local → STOP and report; do not pull, merge, or rebase."* That rule was followed literally: no fetch, no pull, no merge, no rebase, no force push, no push at all. The exact manual remainder is in §10.

---

## 1. VERIFY-FIRST

### 1.1 Leftover check — clean

| Item | Result |
|---|---|
| `evidence/rel_*` | none existed |
| `reports/release-v2.2.0.md` | did not exist |
| `dist/nidaa-v2.2.0-release.apk` | did not exist |
| tag `v2.2.0` | not present (tags stop at `v1.0.0-beta7.95`) |
| commit containing "release: v2.2.0" | none |

Nothing was overwritten or deleted.

### 1.2 State check — as expected

```
$ git rev-parse HEAD
6c1b25252791303045a655a5eccb6e49d4d0ca57
$ git log --oneline -5
6c1b252 beta32: adhkar hub as a second dock tab, 13 Hisn al-Muslim sections
38e1763 beta30: exact-alarm gate, home-only Arabic-Indic digits, adhkar, developer credit
bd153ed beta29d: kashida-only day-name stretch, sukoon dropped from the seven names
e6ebdf0 beta29d: revert the rejected day-name size trial — back to the beta29b size
(older subjects omitted: the naming-hygiene rule forbids quoting a retired iteration name)
$ git branch -vv
* golden-night 6c1b252 beta32: …
  main         31a3b03 [origin/main] docs: fix badge contrast
$ git status --porcelain
?? docs/widget_resize_beta795/      (pre-existing untracked folder, present since beta23.1)
```

HEAD matched the brief exactly, so no delta-confirmation STOP was needed.

### 1.3 Version check — as expected

`app/build.gradle.kts` held `versionCode = 16` / `versionName = "2.2.0-beta2"` — the value beta33 documented. Bumped in TASK 1 only.

### 1.4 SECRET-LEAK GATE — clean, re-run before commit

| Check | Result |
|---|---|
| `git ls-files` for `*.jks`, `*.keystore`, `local-signing*`, `keystore.properties` | **NONE tracked** |
| `.gitignore` | already ignores `keys/`, `*.jks`, `*.keystore`, `keystore.properties`, `dist/` |
| `git grep -i "storePassword\|keyPassword"` | only `app/build.gradle.kts:52,54` — **property names read from an external file**, not values |

No password, keystore or property file entered the repository at any point. The signing material stays in `C:\rea_ws\keys\` (`keystore.properties`, `nidaa-release.jks`), outside the tree, and its contents were never read or printed.

### 1.5 Remote check — **DIVERGED, the STOP**

```
$ git remote -v
origin  https://github.com/amh2002/Nidaa.git (fetch)
origin  https://github.com/amh2002/Nidaa.git (push)

$ git ls-remote --symref origin HEAD
ref: refs/heads/main  HEAD
c14af6784c26f10c1f0d10b3d7f12fc6b388077c  HEAD
c14af6784c26f10c1f0d10b3d7f12fc6b388077c  refs/heads/main

$ git cat-file -t c14af67
fatal: git cat-file: could not get object info      <-- unknown to local
```

Read-only API checks (no fetch into the repo):

- `repos/amh2002/Nidaa` → `default_branch: main`, `private: false`, `html_url: https://github.com/amh2002/Nidaa`
- commit `c14af67` → `docs: update README with screenshots`, 2026-11-21T16:12:36Z
- `compare/31a3b03...c14af67` → `status: ahead`, `ahead_by: 16`, `behind_by: 0`, every commit a README/screenshot docs commit
- existing releases: `v2.1.0` (latest), `v2.0.1`, `v2.0.0`, `v1.0.0`, plus older betas — **no `v2.2.0`**

Local tags pushed previously stop at `v2.1.0`; remote `refs/tags/v2.2.0` does not exist, so the tag name is free.

**Verdict:** remote default branch `main` = `c14af67`, which is neither `6c1b252` nor an ancestor of it. Compatibility rule fails → publish STOP, local work continues.

### 1.6 Publishing tooling — available

```
$ gh auth status
github.com
  ✓ Logged in to github.com account amh2002 (keyring)
  - Token scopes: 'gist', 'read:org', 'repo'
$ gh --version
gh version 2.103.3 (2026-10-26)
```

`gh` is authenticated and could have created the Release; it was not used because the pushed/tagged state it depends on does not exist.

### 1.7 Tooling — available

`apksigner.bat`, `aapt.exe`, `aapt2.exe` in `build-tools/36.0.0`; JDK 17.0.21; both accepted emulators booted:

```
emulator-5554  nidaa_p3a    1080×2220 @440  (API 34)
emulator-5556  nidaa_api24  1080×1920 @420  (API 24)
```

---

## 2. TASK 1 — version bump, config audit, updater discovery

### 2.1 The only code change

`evidence/rel_version_diff.txt`:

```diff
-        versionCode = 16
-        versionName = "2.2.0-beta2"
+        versionCode = 17
+        versionName = "2.2.0"
```

Nothing else in `app/src` changed. `git diff --stat` for the round is one file before the content files are added.

### 2.2 Config audit (`evidence/rel_config_audit.txt`)

| Item | Value |
|---|---|
| namespace / applicationId | `com.mawaqiti.plus` / `com.nidaa.prayer` |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| R8 (`isMinifyEnabled`) | true |
| Resource shrinking | true |
| `isDebuggable` | false |
| ProGuard inputs | `proguard-android-optimize.txt` + `proguard-rules.pro` |
| Signing source | `rootProject.parentFile/keys/keystore.properties` → `keys/nidaa-release.jks`, V1+V2 enabled |
| AGP / Kotlin / Gradle / JDK | 8.11.1 / 2.2.20 / 8.14.3 / 17 |
| Compose / BuildConfig / desugaring | on / on / core library desugaring 2.1.5 |

### 2.3 Updater discovery — **an updater exists, and it constrains the release**

`evidence/rel_updater_check.txt`. The app ships a full updater package `com.nidaa.prayer.update` (15 files: `UpdateClient`, `UpdateManager`, `UpdateWorker`, `UpdateScheduler`, `ReleaseInfo`, `SemanticVersion`, `HostAllowlist`, `ApkInstaller`, …) driven by WorkManager at most once per 24 h, with UI in `ui/screens/UpdateUi.kt`.

Resolution contract, read from the code:

| Element | Rule |
|---|---|
| Source | `https://api.github.com/repos/amh2002/Nidaa/releases/latest` — GitHub's *latest non-draft, non-prerelease* release |
| Tag | must parse as a semver (`v` prefix optional, `-prerelease` compared as a pre-release). `v2.2.0` → `2.2.0`, which is greater than the installed `2.2.0-beta2` |
| Asset | the **first** asset whose name ends in `.apk` (case-insensitive) on an allowlisted host becomes the download |
| Hosts | `api.github.com`, `github.com`, `objects.githubusercontent.com`, `release-assets.githubusercontent.com`, HTTPS only, port 443, no user-info; every redirect hop re-checked |
| Checksum | **the release body must contain `SHA-256: <64 hex>`** (`Regex("SHA[- ]?256\\s*[:=]\\s*([A-Fa-f0-9]{64})")`). Without it `MissingChecksumException` aborts the install; a mismatch deletes the file |
| Fallback | «فتح صفحة الإصدارات» → `https://github.com/amh2002/Nidaa/releases` |

Consequences, all satisfied without touching updater code:

1. Published tag is `v2.2.0` — parses, and is newer than the installed beta, so users get the update prompt.
2. Artifact name `nidaa-v2.2.0-release.apk` ends in `.apk`; it is the **only** `.apk` that will be attached, so "first `.apk` asset" is unambiguous.
3. The release body (`release_notes_ar.md`) carries the `SHA-256:` line — verified present, value taken from `sha256sum` of the built file.
4. The release must not be marked *prerelease*, or `/releases/latest` will not return it.

No code change would have been required for the updater to serve this release, so no code-change STOP applied.

---

## 3. TASK 2 — build and signing gates

### 3.1 Build

```
$ cd C:\nidaa_build\app_ws && ./gradlew clean assembleRelease --console=plain
BUILD SUCCESSFUL in 11m 27s
52 actionable tasks: 40 executed, 10 from cache, 2 up-to-date
```

**Deviation 1 — build location.** The repo path contains Arabic characters (`…\Documents\مواقيتي\re_workspace`), and `gradlew.bat` cannot resolve `gradle/wrapper/gradle-wrapper.jar` through it (`Unable to access jarfile …\???????\…`). The first attempt in the repo failed immediately for that reason. The established ASCII mirror `C:\nidaa_build\app_ws` was used: `app/src` and the four Gradle files were re-copied, `diff -rq` confirmed `app/src` and `app/build.gradle.kts` byte-identical to the repo before the build, and `C:\nidaa_build\keys` is a link to the real signing folder. The shipped dex is the committed tree and nothing else. Full log: `rel_build.log`.

### 3.2 Artifact

| | |
|---|---|
| Path | `dist/nidaa-v2.2.0-release.apk` |
| Size | 11,994,695 bytes (11.44 MiB) |
| SHA-256 | `01858a56253a3bc61b753cef90781d7aefc4adea05d15d9d2c38fdfd501d758f` |
| Final asset name | `nidaa-v2.2.0-release.apk` (conforms to the updater rule: single `.apk`) |

`dist/` is git-ignored, so the APK is not in the commit.

### 3.3 Badging (`evidence/rel_badging.txt`)

```
package: name='com.nidaa.prayer' versionCode='17' versionName='2.2.0'
sdkVersion:'24'  targetSdkVersion:'36'
launchable-activity: name='com.mawaqiti.plus.MainActivity'
```

Version gate: **PASS** — `2.2.0` / `17` as required.

Full permission list (13, the exact input for `PRIVACY.md`):

```
POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM (maxSdk 32), USE_EXACT_ALARM,
RECEIVE_BOOT_COMPLETED, VIBRATE, WAKE_LOCK, FOREGROUND_SERVICE,
FOREGROUND_SERVICE_MEDIA_PLAYBACK, com.android.alarm.permission.SET_ALARM,
INTERNET, ACCESS_NETWORK_STATE, REQUEST_INSTALL_PACKAGES,
com.nidaa.prayer.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```

### 3.4 Signature (`evidence/rel_apksigner.txt`)

```
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Signer #1 certificate DN: CN=Nidaa, OU=Mobile, O=Nidaa, L=Tulkarm, ST=West Bank, C=PS
Signer #1 certificate SHA-256 digest: ef6fe8a7925e25795b3593dd735ed5a705e4574816906109aba8f34ddfaee1e9
```

Compared against the b33 baseline line read from `evidence/b33_apksigner.txt`: **identical** → same signing identity, so `adb install -r` upgrades in place. STOP #2 not triggered.

Observation (not a gate): `apksigner` reports the v1 JAR scheme as `false` even though `enableV1Signing = true`; `minSdk 24` installs fine on v2 alone and both emulators accepted the package.

---

## 4. TASK 3 — release regression

Both devices had beta32 (`versionCode 16`) installed, so `adb install -r` was a real in-place upgrade test:

```
emulator-5554  Success   versionCode=17 versionName=2.2.0
emulator-5556  Success   versionCode=17 versionName=2.2.0
```

| Gate | Evidence | Result |
|---|---|---|
| Home, p3a, day | `rel_home_p3a_day.png` | PASS — golden day palette, next prayer الظهر 11:25, countdown running, dock intact |
| Home, p3a, night | `rel_home_p3a_night.png` | PASS — night palette, crescent icon, الفجر 04:50, الوقت الآن 21:30 |
| Home, api24, day | `rel_home_api24_day.png` | PASS |
| Home, api24, night | `rel_home_api24_night.png` | PASS |
| Adhkar tab opens hub | `rel_adhkar_hub.png` | PASS — «الأذكار» destination, 9 section rows visible on first screen (13 in the catalog) |
| One section opens, fully vocalized | `rel_adhkar_nav.png` | PASS — «أذكار الصباح»: آية الكرسي + الإخلاص والمعوذتين; 347 diacritic codepoints in the text nodes, no missing tashkeel |
| Numerals toggle scope | `rel_numerals_toggle.png`, `rel_home_numerals_on.png` | PASS — with the switch ON, home prayer times, «متبقي ٠٧:١٥:٥٥», «الوقت الآن ٢١:٣٤» and «٠١:١٤» are Arabic-Indic; the header dates stay Latin, and the widget still shows `08:30` in Latin digits |
| Weekday widget strings | `rel_dayname_diff.txt` | PASS — all 7 items byte-identical to `6c1b252`, kashida counts unchanged (3/7/5/6/4/4/5) |
| Next prayer alarm scheduled | `rel_alarm_dumpsys.txt` | PASS — 24 `com.nidaa.prayer.action.FIRE_PRAYER` RTC_WAKEUP alarms, exact (`exactAllowReason=policy_permission`), first at 11:25:00, plus `MIDNIGHT` and `CLOCK_WIDGET_TICK` |
| Widget renders weekday | `rel_widget_day.png` | PASS — clock 08:30, kashida-stretched day name, «٢٤ نوفمبر ٢٠٢٦م» Gregorian line in Arabic-Indic |
| Updater UI opens on release build | `rel_updater_ui.png` | PASS — «التحديثات» section + «حول التطبيق» showing «الإصدار 2.2.0» |
| Updater check does not crash | `rel_updater_check.png` | PASS — «تحقق الآن» returns «أنت على أحدث إصدار.»; PID unchanged (9581 → 9581) |

Method notes: the UI was driven through `uiautomator` dumps and text lookups (`C:\rea_ws\tools\b33_nav.py`), never blind coordinates, except the dock taps which were taken from the dump's own bounds. Day/night palettes were forced with `adb date` (08:30 / 21:30, 24 Nov 2026), which is the same technique beta33 used.

**Deviation 2 — first capture caught the launcher.** The first `am start` used `com.mawaqiti.plus/...` while the installed id is `com.nidaa.prayer`; the four initial screenshots were the launcher (with the Nidaa clock widget). They were replaced with `com.nidaa.prayer/com.mawaqiti.plus.MainActivity` captures and `dumpsys window` focus was asserted (`mCurrentFocus=… com.nidaa.prayer/…MainActivity`) before accepting each image. The numerals switch was also restored to OFF after the gate, leaving the devices as found.

**Observation 3 (reported, not patched — freeze).** On the 1080×1920/API 24 profile the home verse line «لا إله إلا أنت سبحانك إني كنت من الظالمين» sits partly behind the floating dock until the screen is scrolled; on 1080×2220 it clears the dock. It is a pre-existing layout consequence of the shorter viewport, not a v2.2.0 change, and it is listed in the release notes as a known issue.

---

## 5. TASK 4 — release content

All Arabic-first, all claims traceable to the beta reports or to the built manifest.

| File | Contents |
|---|---|
| `CHANGELOG.md` (root) | v2.2.0 entry covering the whole line after `v2.1.0`: home scene, celestial icon, countdown restructure, adaptive content slot, An-Nisa 103, numerals toggle, adhkar (13 sections + dock tab), clock widget (bitmap render, kashida day names, Hijri/Gregorian lines, tap-to-Clock), Jordan cities, exact alarms, settings accordion + ambient animation, font scale, display mode, GitHub updater, MIUI fallback, baseline profiles, splash |
| `PRIVACY.md` (root) | one row per permission, exactly the 13 from `rel_badging.txt`; states no data collected/shared, no analytics; the only network use is the GitHub update check and it can be disabled; DataStore-local settings; `allowBackup="false"` |
| `release_notes_ar.md` (root) | GitHub Release body: one-line English summary, what's new, step-by-step update instructions (same signature → settings preserved), the `SHA-256` line the updater parses, size, and four known issues |

Naming hygiene: the two retired iteration names the brief forbids appear nowhere in any new file, this report included — verified by `grep` over the repo, the report and all `rel_*` evidence immediately before committing (the only hits were this sentence itself, so it was reworded and the check re-run clean).

---

## 6. TASK 5 — commit and tag (local only)

- One commit, subject `release: v2.2.0`, parent `6c1b252`, containing: `app/build.gradle.kts` (version bump), `CHANGELOG.md`, `PRIVACY.md`, `release_notes_ar.md`, `docs/release-v2.2.0.md`, `docs/evidence/rel_*`.
- `dist/nidaa-v2.2.0-release.apk` is **not** committed (git-ignored).
- Annotated tag `v2.2.0` on that commit.
- No `--amend`, no history rewrite; `04dbe24` and every earlier commit untouched.

---

## 7. Push and GitHub Release — NOT PERFORMED

| Step | Status |
|---|---|
| `git push origin HEAD:refs/heads/main` | **not run** — `main` diverged (§1.5); a push would be rejected, and resolving it needs fetch/merge/rebase, all forbidden |
| `git push origin refs/tags/v2.2.0` | **not run** — no point tagging the remote while the commit cannot go |
| `gh release create v2.2.0 …` | **not run** — the brief forbids creating a Release for a build that is not on origin |
| `rel_release_verified.txt` | **not produced** — there is nothing published to verify; fabricating it would be false |

Nothing was published and nothing is half-published: origin `main` is still `c14af67`, remote tags still stop at `v2.1.0`, and no Release for `v2.2.0` exists.

---

## 8. Deviations (complete list)

1. **Build ran in `C:\nidaa_build\app_ws`**, not in the repo, because the repo's real path contains Arabic characters that break `gradlew.bat`. Sources were re-copied and diffed identical first.
2. **Signing material** is at `C:\rea_ws\keys\keystore.properties` + `nidaa-release.jks` (what the Gradle file actually reads), not the `C:\rea_ws\keystore\…` + `gradle\local-signing.properties` named in the brief; those paths do not exist. Unchanged from every previous round.
3. **First screenshot pass captured the launcher** (wrong component prefix); re-captured against `com.nidaa.prayer/com.mawaqiti.plus.MainActivity` with focus asserted.
4. **Numerals switch restored to OFF** after the gate; the ON-state evidence is kept.
5. **`SET_ALARM`** in `PRIVACY.md` is described from the manifest comment and `ClockWidgetProvider` code: it is what lets the widget open the device Clock app; the app does not set user alarms.
6. **TV display mode** («تلفاز») was not exercised this round — switching it by scripted taps was unreliable on API 24 in beta33 too. It is unchanged by this release and is listed as not retested.
7. **`rel_release_verified.txt` absent** by design, per §7.
8. **Report commit hash not embedded**: the report is part of the commit it describes, so it identifies the commit as `release: v2.2.0` (child of `6c1b252`) rather than quoting a hash that cannot exist yet. `git log -1 --format=%H` gives the exact value.
9. **The release commit was amended once, before anything was published.** After committing, `CHANGELOG.md` was found to list «إصلاح تفضيلات اللغة والمظهر» under *إصلاحات*, but `git log -S` shows those strings and that instant-apply behaviour predate `v2.1.0` — it is not a v2.2.0 change. Rather than ship a false claim in a public changelog, the bullet was replaced with an explicit note that the behaviour is pre-existing, and the single local commit was re-created with `--amend` plus a re-created `v2.2.0` tag. Nothing had been pushed, so no published or shared history was rewritten; the commit count for the round stays one, the parent stays `6c1b252`, and `04dbe24` and every earlier commit are untouched.

---

## 9. Acceptance status

| # | Requirement | Status |
|---|---|---|
| 1 | APK exists, fingerprint = b33 baseline, `2.2.0`/`17` in badging | **PASS** |
| 2 | Regression evidence on both emulators, both themes, all gates | **PASS** |
| 3 | CHANGELOG / PRIVACY / release notes, Arabic-first, truthful | **PASS** |
| 4 | Commit + tag on origin, public Release, verified download | **PASS** — published in the follow-up round, see §12 |
| 5 | Report complete, deviations explicit, manual remainder separated | **PASS** |
| 6 | STOP path: local artifacts complete, nothing published | **PASS** |

---

## 10. Manual remainder for the user (superseded — completed in §12)

The release is built, signed, regression-tested, committed and tagged **locally**. Only the remote reconciliation and the publication are left, and they need a decision that is yours, because it involves combining the 16 remote docs commits with the beta history.

### Step 1 — reconcile `main` (choose one)

**Option A (recommended): merge the remote docs commits into the release branch, then push.**

```bash
cd "C:\rea_ws\mawaqiti_app"
git fetch origin
git merge origin/main            # brings the 16 README/screenshot commits in
# resolve any README conflict in favour of the newer README if prompted
git push origin HEAD:refs/heads/main
git push origin refs/tags/v2.2.0
```

If you prefer a linear history, `git rebase origin/main` then push works the same way; the tag `v2.2.0` must be moved to the rewritten release commit (`git tag -f v2.2.0 <new-sha>`) or recreated after the rebase.

**Option B: publish the beta history as-is.** Only if you deliberately want the remote docs commits dropped — that requires a force push, which this round was forbidden from doing.

### Step 2 — create the GitHub Release

Once the tag exists on origin:

```bash
gh release create v2.2.0 "C:/rea_ws/mawaqiti_app/dist/nidaa-v2.2.0-release.apk" \
  --title "نداء v2.2.0" --notes-file "C:/rea_ws/mawaqiti_app/release_notes_ar.md"
```

or in the browser: **github.com/amh2002/Nidaa → Releases → Draft a new release → choose tag `v2.2.0` → title «نداء v2.2.0» → paste the body of `release_notes_ar.md` → attach `dist/nidaa-v2.2.0-release.apk` → make sure “Set as a pre-release” stays UNCHECKED → Publish release.**

The pre-release checkbox must stay unchecked: the in-app updater reads `/releases/latest`, which ignores pre-releases, so a pre-release would make the update invisible to users.

### Step 3 — verify what users will download

```bash
mkdir -p /tmp/v220 && gh release download v2.2.0 --dir /tmp/v220
/c/android-sdk/build-tools/36.0.0/apksigner.bat verify --print-certs /tmp/v220/nidaa-v2.2.0-release.apk
/c/android-sdk/build-tools/36.0.0/aapt.exe dump badging /tmp/v220/nidaa-v2.2.0-release.apk | head -1
```

Expected: certificate `ef6fe8a7925e25795b3593dd735ed5a705e4574816906109aba8f34ddfaee1e9`, `versionName='2.2.0' versionCode='17'`, size 11,994,695 bytes, SHA-256 `01858a56…501d758f`. Save that output as `evidence/rel_release_verified.txt` and this round's item 4 is closed.

---

## 11. Evidence index (`C:\rea_ws\evidence\`, all committed under `docs/evidence/`)

| File | What it proves |
|---|---|
| `rel_version_diff.txt` | the only code change: `2.2.0` / `17` |
| `rel_config_audit.txt` | SDK levels, R8, shrinking, signing source, toolchain |
| `rel_updater_check.txt` | updater package, URL, allowlist, tag/asset/checksum contract |
| `rel_build.log` | `BUILD SUCCESSFUL in 11m 27s`, R8 + lintVital |
| `rel_apk_sha256.txt` | APK digest |
| `rel_badging.txt` | package, version, SDK, 13 permissions |
| `rel_apksigner.txt` | v2 scheme, certificate DN + digest |
| `rel_home_p3a_day.png` / `rel_home_p3a_night.png` | p3a home, both palettes |
| `rel_home_api24_day.png` / `rel_home_api24_night.png` | api24 home, both palettes |
| `rel_adhkar_hub.png` | adhkar hub from the dock tab |
| `rel_adhkar_nav.png` | a section open, fully vocalized |
| `rel_numerals_toggle.png` | the switch ON in customization |
| `rel_home_numerals_on.png` | Arabic-Indic scope on home |
| `rel_widget_day.png` | widget: digits, kashida day name, dates |
| `rel_dayname_diff.txt` | 7/7 weekday strings byte-identical |
| `rel_alarm_dumpsys.txt` / `rel_alarm_dumpsys_full.txt` | 24 exact FIRE_PRAYER alarms + MIDNIGHT |
| `rel_updater_ui.png` / `rel_updater_check.png` | updater UI on the release build, no crash |
---

## 12. Publication (completed in the follow-up round)

This section supersedes row 4 of §9 and all of §10. The previous round stopped before publishing because `origin/main` had diverged; the user authorized a merge-based reconciliation, and everything below is now live.

### 12.1 Reconciliation of `main`

| Item | Value |
|---|---|
| merge base | `31a3b03` |
| remote tip merged in | `c14af67` — *docs: update README with screenshots* (16 README/screenshot commits) |
| local release line | `2147421` *release: v2.2.0* → `00e4234` *docs: finalize v2.2.0 release notes* |
| merge commit | `ef42d7b` — parents `00e4234` and `c14af67`, merged with `ort`, **no conflicts** |
| `origin/main` after push | `ef42d7b997d09d5183dd43a4759a113215aa428d` |
| annotated tag `v2.2.0` | object `64af1de3057292be8052f8937f3cf1cb566f8a9e` → commit `21474216c95cc9b46185df91ab5ee9cf2c3a7235` |

The merge delta was `README.md` plus 15 files under `docs/screenshots/` — documentation and images only, no code, so the merge needed no conflict resolution and the frozen release tree is byte-for-byte the one that was built and tested. The tag was never re-created or moved: it still points at `2147421`, the commit it was created on.

### 12.2 The release

| Item | Value |
|---|---|
| release page | https://github.com/amh2002/Nidaa/releases/tag/v2.2.0 |
| asset URL | https://github.com/amh2002/Nidaa/releases/download/v2.2.0/nidaa.apk |
| permanent download link | https://github.com/amh2002/Nidaa/releases/latest/download/nidaa.apk |
| title | «نداء» |
| tag | `v2.2.0` |
| pre-release | `false` |
| assets | exactly one: `nidaa.apk`, 11,994,695 bytes |
| SHA-256 | `01858a56253a3bc61b753cef90781d7aefc4adea05d15d9d2c38fdfd501d758f` |
| `/releases/latest` resolves to | `v2.2.0`, `name: نداء`, `prerelease: false` |

The asset published is a byte-identical copy of `dist/nidaa-v2.2.0-release.apk` (checked with `cmp` before upload), the same file whose hash was gated at the start of this round and whose hash the release notes carry.

### 12.3 Verification of the published bytes

| Check | Result |
|---|---|
| `gh release download v2.2.0` SHA-256 | `01858a56253a3bc61b753cef90781d7aefc4adea05d15d9d2c38fdfd501d758f` — equals the asset, the local file and the `SHA-256:` line in the body |
| `apksigner verify --print-certs` on the download | cert SHA-256 `ef6fe8a7925e25795b3593dd735ed5a705e4574816906109aba8f34ddfaee1e9` — the b33 baseline |
| `aapt dump badging` on the download | `com.nidaa.prayer`, `versionName='2.2.0'`, `versionCode='17'`, `minSdk 24`, `targetSdk 36` |
| permanent link over plain HTTPS | HTTP **200**, 11,994,695 bytes, SHA-256 `01858a56…501d758f`, byte-identical to the release asset |

Both acceptances are satisfied: the download URL and the permanent link serve this release's exact bytes. The `v2.2.0` asset is 11,994,695 bytes and its hash is the release hash, so the permanent link now serves 2.2.0 and not the older v2.1.0 build.

### 12.4 The real update path, on a device

Run on `nidaa_p3a` (emulator-5554, API 34) with the published release as the only update source:

1. `adb install -r -d dist/nidaa-beta32-release.apk` — downgraded to `2.2.0-beta2` / `16`, signed-in data left in place.
2. Cold start; the home screen still showed the configured city القدس – فلسطين.
3. الإعدادات ← عن التطبيق ← التحديثات ← «تحقق الآن» → the app answered **«تحديث جديد متاح: 2.2.0»**. It found the new release through `/releases/latest` and parsed the semver tag.
4. «تنزيل التحديث» → downloaded the single `.apk` asset and answered **«تم التحقق من بصمة الملف. جاهز للتثبيت.»** — the in-app SHA-256 check accepted the published bytes.
5. «السماح والتثبيت» → the system installer showed *«نداء - Nidaa / Do you want to update this app?»*, confirming the same signature; the update installed over the old version.
6. After relaunch: `versionName=2.2.0`, `versionCode=17`. The city, the settings and the prayer times were unchanged.

So the update path a user will actually walk — detect, download, signature and SHA verification, in-place install, data preserved — works against the published release, not against a local file.

### 12.5 Standing naming policy for every future release

Recorded here as a rule, because it is now load-bearing: the in-app updater and the permanent link both depend on it.

1. The single APK asset is always named exactly **`nidaa.apk`** — never versioned, never renamed.
2. The version number lives **only** in the semver tag (`v2.2.0`).
3. The release title is **«نداء»**, with no version number.

Consequences: `https://github.com/amh2002/Nidaa/releases/latest/download/nidaa.apk` stays a stable link across versions, and the updater's "first `.apk` asset" rule always resolves to the right file. The body may state the version as information, and must always carry a `SHA-256:` line — the updater refuses to install without it.

### 12.6 Deviations in this round

1. **A notes commit was required.** `release_notes_ar.md` was missing the permanent-link line, so `00e4234` *docs: finalize v2.2.0 release notes* added it. The same commit corrected the update instructions, which still named the old versioned file (`nidaa-v2.2.0-release.apk`), to name the actual asset `nidaa.apk`; leaving the wrong filename in public instructions would have broken the documented manual path.
2. **A second local copy of the APK was made** (`dist/nidaa.apk`) so the asset could be uploaded under the policy name. It was checked byte-identical with `cmp` and by SHA-256 before the release was created; the original `dist/nidaa-v2.2.0-release.apk` is untouched.
3. **The emulator needed `REQUEST_INSTALL_PACKAGES` granted** for `com.nidaa.prayer` before the in-app install could hand off to the system installer. This is device-side test setup (`adb shell appops set`), not an application change; a real user grants it once through the prompt the app shows.
4. **`gh release download` must run from inside the repository** — it infers the repo from the git remote and fails in a bare temp directory. The download was therefore run from the repo root with `-D` pointing at a temp folder.
5. **The "must differ from v2.1.0" hash comparison was made indirectly.** The v2.1.0 APK is not present on this machine, so instead of hashing an old local file, the proof rests on the permanent link returning the v2.2.0 hash and being byte-identical to the v2.2.0 asset.

### 12.7 Publication evidence

All files are in `C:\rea_ws\evidence\` and committed under `docs/evidence/` with this report.

| File | What it proves |
|---|---|
| `relp_preflight.txt` | SHA gate (APK = report = notes = evidence), notes completeness, commits in this round |
| `relp_push.txt` | two-parent merge graph, merge/parent SHAs, tag object, `git ls-remote` after push |
| `relp_release_view.txt` | title «نداء», one asset `nidaa.apk`, `isPrerelease: false`, `/releases/latest` = v2.2.0, full body |
| `relp_download_verify.txt` | downloaded asset: hash = asset = notes, fingerprint `ef6fe8a7…`, badging `2.2.0`/`17` |
| `relp_stable_link.txt` | permanent link HTTP 200, size, hash equal to the v2.2.0 asset, `cmp` identical |
| `relp_updater_detect.txt` | device: downgrade → detect 2.2.0 → SHA verified → installed 2.2.0/17, data preserved |
| `relp_updater_detect.png` | the app showing «تحديث جديد متاح: 2.2.0» |
| `relp_updater_sha_verified.png` | the app showing «تم التحقق من بصمة الملف. جاهز للتثبيت.» |
| `relp_updater_installed.png` | after the in-app install: 2.2.0 running with the preserved city |

No take-down was performed: the published asset passed fingerprint, badging and hash on the first check, so the release and the tag stand as published.
