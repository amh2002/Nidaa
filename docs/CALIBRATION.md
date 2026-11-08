# CALIBRATION.md

Solar engine (MWL) versus the official timetable that Mawaqiti Plus
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
| 4320 | -0.75 | -21 | 9 | 3.24 | 4.04 | 21 |

## Per event

| Event | n | Mean (min) | Min | Max | Mean abs | SD |
|---|---:|---:|---:|---:|---:|---:|
| fajr | 720 | +0.76 | -9 | 7 | 2.25 | 2.67 |
| sunrise | 720 | +3.26 | -4 | 9 | 3.40 | 2.20 |
| dhuhr | 720 | +0.85 | -5 | 7 | 1.54 | 1.84 |
| asr | 720 | +0.84 | -7 | 7 | 2.01 | 2.39 |
| maghrib | 720 | -5.84 | -14 | 0 | 5.84 | 2.53 |
| isha | 720 | -4.35 | -21 | 2 | 4.39 | 2.86 |

## Per country

| Country | n | Mean (min) | Mean abs (min) | Max abs (min) |
|---|---:|---:|---:|---:|
| PS | 4224 | -0.72 | 3.24 | 14 |
| SA | 96 | -2.05 | 3.05 | 21 |

## Cities with the largest mean absolute deviation

| City | Country | minDiff | Mean abs (min) | Mean (min) |
|---|---|---:|---:|---:|
| 3 min after Al-Quds | PS | 3 | 5.48 | -5.48 |
| Acre | PS | 3 | 5.21 | -5.00 |
| 3 min before Al-Quds | PS | -7 | 4.73 | +4.52 |
| 2 min after Al-Quds | PS | 2 | 4.48 | -4.48 |
| Rafah | PS | -2 | 4.19 | +3.40 |
| 2 min before Al-Quds | PS | -6 | 4.15 | +3.52 |
| Beit She'an | PS | 0 | 3.98 | -3.69 |
| Dura | PS | 2 | 3.81 | -3.69 |
| Khan Yunis | PS | -1 | 3.77 | +2.19 |
| Sakhnin | PS | 0 | 3.77 | -2.85 |
| Gaza | PS | -2 | 3.77 | +2.52 |
| 1 min before Al-Quds | PS | -5 | 3.73 | +2.52 |
| Yatta | PS | -4 | 3.69 | +2.10 |
| Jaffa | PS | -3 | 3.67 | +2.38 |
| Nazareth | PS | 0 | 3.65 | -2.94 |

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
| Acre | 2027-12-31 | maghrib | -14.00 |
| Acre | 2027-01-01 | maghrib | -13.00 |
| Acre | 2028-12-31 | maghrib | -13.00 |

## Why table mode is the default

Across 4320 comparisons the solar MWL model differs from the official timetable by 3.24 minutes on average and up to 21 minutes. That gap is systematic (local sighting-based adjustments, city `minDiff`, and the Palestinian seasonal policy), not random. A table lookup is therefore the only way to guarantee minute-for-minute agreement with the original app, so it is the primary and only user-facing calculation path.

The solar engine stays in the codebase purely for the independent
cross-check in `ENGINE_CROSSCHECK.md`; it is not reachable from the UI.

