# CALIBRATION.md

Solar engine (MWL) versus the official timetable that Nidaa
displays. **Informative only:** the table path is the product truth.

## Method

| Item | Value |
|---|---|
| Table engine | `TableEngine` (same tables as the original app) |
| Solar engine | `PrayerEngine`, internal/test-only |
| Solar parameters | Muslim World League, Fajr 18 deg, Isha 17 deg |
| Asr | Shafi'i (shadow factor 1) |
| High latitudes | Middle of the night |
| Summer time | OFF on both sides (standard time) |
| Zones | Palestine UTC+2, Saudi Arabia UTC+3 |
| Cities | 90 (88 PS, 2 SA) |
| Dates | 8 (2027-01-01, 2027-02-28, 2027-03-01, 2027-06-21, 2027-12-31, 2028-02-29, 2028-06-21, 2028-12-31) |
| Comparisons | 4320 |

Deviation = solar - official table, in minutes; positive means the
astronomical result is later. Midnight wrap is handled with the shortest
signed difference. The official table includes each city's `minDiff` plus
the Palestinian Authority's own seasonal adjustments, which a pure MWL
calculation cannot reproduce exactly.

## Overall

| n | Mean (min) | Min (min) | Max (min) | Mean abs (min) | SD | Max abs (min) |
|---:|---:|---:|---:|---:|---:|---:|
| 4320 | -0.75 | -21 | 9 | 3.04 | 3.82 | 21 |

## Per event

| Event | n | Mean (min) | Min | Max | Mean abs | SD |
|---|---:|---:|---:|---:|---:|---:|
| fajr | 720 | +0.76 | -6 | 7 | 1.92 | 2.23 |
| sunrise | 720 | +3.26 | -3 | 9 | 3.32 | 1.91 |
| dhuhr | 720 | +0.85 | -5 | 7 | 1.15 | 1.32 |
| asr | 720 | +0.84 | -5 | 7 | 1.61 | 1.90 |
| maghrib | 720 | -5.84 | -11 | 0 | 5.84 | 2.09 |
| isha | 720 | -4.35 | -21 | 2 | 4.38 | 2.64 |

## Per country

| Country | n | Mean (min) | Mean abs (min) | Max abs (min) |
|---|---:|---:|---:|---:|
| PS | 4224 | -0.72 | 3.04 | 11 |
| SA | 96 | -2.05 | 3.05 | 21 |

## Cities with the largest mean absolute deviation

| City | Country | minDiff | Mean abs (min) | Mean (min) |
|---|---|---:|---:|---:|
| 5 min after Al-Quds | PS | 3 | 5.48 | -5.48 |
| 5 min before Al-Quds | PS | -7 | 4.73 | +4.52 |
| 4 min after Al-Quds | PS | 2 | 4.48 | -4.48 |
| 4 min before Al-Quds | PS | -6 | 4.15 | +3.52 |
| 3 min before Al-Quds | PS | -5 | 3.73 | +2.52 |
| 3 min after Al-Quds | PS | 1 | 3.65 | -3.48 |
| Bir al-Maksur | PS | 0 | 3.58 | -2.67 |
| 2 min before Al-Quds | PS | -4 | 3.40 | +1.52 |
| Sakhnin | PS | -1 | 3.27 | -1.85 |
| Al-Bayada | PS | -2 | 3.25 | -1.00 |
| Acre | PS | -1 | 3.25 | -1.00 |
| Tamra | PS | -3 | 3.23 | +0.44 |
| Kafr Yasif | PS | -2 | 3.21 | -0.42 |
| Makkah | SA | 0 | 3.17 | -2.25 |
| Al-Ba'na | PS | -2 | 3.17 | -0.79 |

## Largest individual deviations

| City | Date | Event | Deviation (min) |
|---|---|---|---:|
| Makkah | 2027-03-01 | isha | -21.00 |
| Makkah | 2027-02-28 | isha | -20.00 |
| Makkah | 2028-02-29 | isha | -20.00 |
| Madinah | 2027-02-28 | isha | -19.00 |
| Madinah | 2027-03-01 | isha | -19.00 |
| Madinah | 2028-02-29 | isha | -18.00 |
| Makkah | 2027-12-31 | isha | -16.00 |
| Makkah | 2027-01-01 | isha | -15.00 |
| Makkah | 2028-12-31 | isha | -15.00 |
| Madinah | 2027-01-01 | isha | -14.00 |
| Madinah | 2027-12-31 | isha | -14.00 |
| Madinah | 2028-12-31 | isha | -14.00 |
| Bir al-Maksur | 2027-12-31 | maghrib | -11.00 |
| 5 min after Al-Quds | 2027-03-01 | maghrib | -11.00 |
| 5 min after Al-Quds | 2027-06-21 | maghrib | -11.00 |

## Why table mode is the default

Across 4320 comparisons the solar MWL model differs from the official timetable by 3.04 minutes on average and up to 21 minutes. That gap is systematic (local sighting-based adjustments, city `minDiff`, and the Palestinian seasonal policy), not random. A table lookup is therefore the only way to guarantee minute-for-minute agreement with the original app, so it is the primary and only user-facing calculation path.

The solar engine stays in the codebase purely for the independent
cross-check in `ENGINE_CROSSCHECK.md`; it is not reachable from the UI.

