# Rename audit — «نداء / Nidaa» display names (beta 7.9)

Scope: every user-facing string, label and document. The old brand is
«مواقيتي بلس / Mawaqiti Plus» (also written "Mawaqiti", "mawaqiti").
Case-insensitive search over the whole repository (`.kt`, `.xml`, `.md`,
`.kts`, `.txt`, `.json`, `.properties`, excluding `.git`, `build`, `.gradle`).

## Result

| Surface | Occurrences of the old brand | Action |
|---|---|---|
| `res/values*/strings.xml` (all strings, incl. `app_name`, widget text, channel names) | **0** | none needed; `BrandIdentityTest` already asserts this |
| `AndroidManifest.xml` labels | **0** | all labels resolve to `@string/app_name` = `نداء - Nidaa` |
| About card (`settings_about_*`) | **0** | brand line is `نداء · Nidaa` |
| Widget labels / descriptions / channel names | **0** | `widget_description`, `notify_channel_*` are neutral |
| README | **0** | title is `# Nidaa — نداء · أوقات الصلاة (Beta)` |
| Test display names | **0** | no test method or suite name carries the old brand |
| `docs/BETA_NOTES.md` | 15 lines, all inside the archived beta3/beta4 sections | **kept** — see below |

## Leftovers that stay (deliberately)

1. **Historical release notes (beta3/beta4 sections).** These describe builds
   that literally shipped as `MawaqitiPlus-1.0.0-beta4.apk` and the rename table
   that records *from* the old name *to* Nidaa. Rewriting them would falsify the
   archive and break the APK filenames those sections reference. A banner at the
   top of the file now marks them as historical.
2. **Kotlin package / namespace `com.mawaqiti.plus`.** Explicitly out of scope:
   the installed id is `com.nidaa.prayer`, the namespace stays for engine and
   test parity (see beta5 notes). It never appears in the UI.
3. **Internal identifiers**: `MawaqitiWidgetProvider`, `MawaqitiTheme`,
   `MawaqitiViewModel`, `widget_mawaqiti_*.xml`, `Theme.Mawaqiti`,
   `mawaqiti_widget_info.xml`, DataStore name `mawaqiti_settings`, preference
   keys, `mawaqiti://` alarm data-URI scheme, Gradle properties
   `mawaqiti.skyPhase` / `mawaqiti.reportDir`. None is user-visible; renaming
   the widget provider class or its data URIs would orphan placed widgets and
   drop pending alarms.

## Verification

`BrandIdentityTest` already fails the build if any `R.string` value contains
the old brand. This audit adds the manifest/label/document review on top.
