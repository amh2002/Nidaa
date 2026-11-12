# TABLE_FIDELITY.md

Bit-exactness of `TableEngine`, the primary calculation path of
Nidaa BETA.

## Engine

| Item | Value |
|---|---|
| Kotlin engine | `com.mawaqiti.plus.engine.table.TableEngine` |
| Tables | `PrayerTables` - 3 profiles x 6 events x 2 arrays (hours/minutes) x 366 rows = 36 arrays |
| Reference semantics | `p061i1.j.o()` / `p061i1.j.w()` |
| Row rule | day-of-year; common year and doy > 59 -> +1; row 59 is 29 February |
| Runtime adjustment | +60 min only when summer time is on **and** profile is `default` |
| Per-city offset | `minDiff` in minutes, carry/borrow, `hour % 24` |
| Formatting | `%02d:%02d` with `Locale.US` (Latin digits in both languages) |
| Source data | `w7.c` arrays + `y7.C3444b` city list, verified against `reference_impl/prayer_times.py` |

## 1. Original app vectors

`app/src/test/resources/test_vectors.csv` holds 392 recorded day/city/profile
combinations generated from the extracted tables and `p061i1.j` arithmetic.

| Metric | Result |
|---|---:|
| Rows | 392 |
| Rows matching on all six prayers | **392 / 392** |
| Prayer comparisons (6 per row) | 2352 |
| Additional derived checks (last third + Fajr tomorrow) | 784 |
| Mismatches | **0** |

## 2. Full city sweep

`app/src/test/resources/table_expected.json` was exported from the Python
reference implementation, independently of the Kotlin port.

| Metric | Result |
|---|---:|
| Cities | 90 |
| Dates | 8 (2027-01-01, 2027-02-28, 2027-03-01, 2027-06-21, 2027-12-31, 2028-02-29, 2028-06-21, 2028-12-31) |
| Summer states per city/date | 2 (OFF / ON) |
| Cases | **1440** |
| Time comparisons (6 prayers + Fajr tomorrow + last third) | **11520** |
| Cases with summer ON | 720 |
| Mismatches | **0** |

The date set includes 29 February 2028 (leap year), 2027 as a common
year, 28 February / 1 March boundaries, both solstices and 31 December.

## 3. Solar cross-check

The internal solar engine (test-only, never reachable from the UI) is
cross-checked against the independent Adhan library; see
`ENGINE_CROSSCHECK.md`. It is not used to produce any displayed time.

## Verdict

**PASS.** 392/392 original vectors and 1440/1440 sweep cases are bit-exact, with zero mismatches across 14656 comparisons.

## Reproduce

```
.\gradlew.bat test --tests "com.mawaqiti.plus.TableFidelityTest"
```

