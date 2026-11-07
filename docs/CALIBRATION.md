# CALIBRATION.md

Comparison of the new solar engine with the original Mawaqiti app's
124 recorded timetables. **This is a measurement, not a pass/fail test,**
and no engine parameter was tuned to match the vectors.

## Method

- Engine: `PrayerEngine`, Muslim World League (18°/17°), Shafi'i Asr,
  middle-of-the-night high-latitude rule, ceil to the next minute.
- Coordinates and dates come from `test_vectors.csv`; the original app's
  per-city `minDiff` is **inside the vector values**, not applied again.
- The vectors are wall-clock values with a manual `summer_time` flag. To
  isolate the solar model from DST policy, both sides are compared in
  standard time: the engine runs at the longitude-implied fixed offset
  (`round(longitude / 15)`) and 60 minutes are removed from vectors that are
  both flagged `summer_time=yes` **and** use the `default` profile. The
  original app never shifts its Makkah/Madinah tables.
- Deviation = engine − vector, in minutes (positive = engine is later).
- The `HighLatitude-55N` rows are a synthetic stress case: the original app
  still serves the Al-Quds table there, so those rows are reported but
  excluded from the tuning-viability verdict.

## Overall — MWL vs all 124 vectors

| Event | n | Mean (min) | Min (min) | Max (min) | Mean abs (min) | SD |
|---|---:|---:|---:|---:|---:|---:|
| fajr | 124 | -20.44 | -238 | 78 | 31.12 | 45.29 |
| sunrise | 124 | -10.87 | -127 | 119 | 27.55 | 37.30 |
| dhuhr | 124 | -14.47 | -43 | 42 | 23.11 | 26.02 |
| asr | 124 | -18.30 | -109 | 83 | 29.59 | 35.42 |
| maghrib | 124 | -19.39 | -115 | 119 | 31.97 | 38.64 |
| isha | 124 | -18.88 | -76 | 225 | 38.25 | 53.58 |
| **all** | 744 | -17.06 | -238 | 225 | 30.26 | 40.43 |


### Per city (all rows)

#### القدس

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 13 | -25.08 | -75 | 1 | 76 |
| sunrise | 13 | -20.38 | -63 | 4 | 67 |
| dhuhr | 13 | -24.38 | -41 | 1 | 42 |
| asr | 13 | -26.08 | -60 | 2 | 62 |
| maghrib | 13 | -29.23 | -61 | -5 | 56 |
| isha | 13 | -33.15 | -69 | -3 | 66 |


#### مكة المكرمة

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 13 | +14.92 | -2 | 78 | 80 |
| sunrise | 13 | +10.92 | -7 | 67 | 74 |
| dhuhr | 13 | +12.46 | -1 | 41 | 42 |
| asr | 13 | +14.23 | -1 | 60 | 61 |
| maghrib | 13 | +12.31 | -8 | 56 | 64 |
| isha | 13 | -0.54 | -20 | 50 | 70 |


#### المدينة المنورة

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 13 | +15.23 | -6 | 70 | 76 |
| sunrise | 13 | +13.00 | -6 | 61 | 67 |
| dhuhr | 13 | +13.15 | 0 | 42 | 42 |
| asr | 13 | +13.08 | -5 | 56 | 61 |
| maghrib | 13 | +11.54 | -5 | 51 | 56 |
| isha | 13 | +1.31 | -19 | 46 | 65 |


#### حيفا

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 12 | -27.50 | -79 | 3 | 82 |
| sunrise | 12 | -21.42 | -65 | 7 | 72 |
| dhuhr | 12 | -25.50 | -40 | 2 | 42 |
| asr | 12 | -27.83 | -61 | 5 | 66 |
| maghrib | 12 | -30.92 | -63 | -2 | 61 |
| isha | 12 | -34.25 | -70 | 0 | 70 |


#### غزه

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 12 | -24.25 | -71 | 5 | 76 |
| sunrise | 12 | -19.92 | -60 | 7 | 67 |
| dhuhr | 12 | -23.50 | -38 | 4 | 42 |
| asr | 12 | -25.08 | -56 | 5 | 61 |
| maghrib | 12 | -28.17 | -58 | -1 | 57 |
| isha | 12 | -33.00 | -66 | 0 | 66 |


#### نابلس

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 12 | -30.00 | -79 | -1 | 78 |
| sunrise | 12 | -24.67 | -67 | 3 | 70 |
| dhuhr | 12 | -28.50 | -43 | -1 | 42 |
| asr | 12 | -30.58 | -63 | 0 | 63 |
| maghrib | 12 | -33.67 | -65 | -7 | 58 |
| isha | 12 | -37.75 | -72 | -5 | 67 |


#### رام الله

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 12 | -29.50 | -78 | -1 | 77 |
| sunrise | 12 | -24.67 | -66 | 2 | 68 |
| dhuhr | 12 | -28.50 | -43 | -1 | 42 |
| asr | 12 | -30.08 | -62 | 0 | 62 |
| maghrib | 12 | -33.17 | -64 | -7 | 57 |
| isha | 12 | -37.75 | -71 | -5 | 66 |


#### الخليل

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 12 | -29.00 | -76 | 0 | 76 |
| sunrise | 12 | -24.17 | -64 | 3 | 67 |
| dhuhr | 12 | -27.50 | -42 | 0 | 42 |
| asr | 12 | -29.58 | -61 | 1 | 62 |
| maghrib | 12 | -32.42 | -62 | -6 | 56 |
| isha | 12 | -37.50 | -71 | -4 | 67 |


#### بئر السبع

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 12 | -25.50 | -71 | 5 | 76 |
| sunrise | 12 | -21.17 | -60 | 7 | 67 |
| dhuhr | 12 | -24.50 | -39 | 3 | 42 |
| asr | 12 | -26.33 | -57 | 4 | 61 |
| maghrib | 12 | -28.92 | -58 | -2 | 56 |
| isha | 12 | -34.25 | -67 | -1 | 66 |


#### HighLatitude-55N

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 13 | -47.08 | -238 | 67 | 305 |
| sunrise | 13 | +18.31 | -127 | 119 | 246 |
| dhuhr | 13 | +6.62 | -10 | 32 | 42 |
| asr | 13 | -19.31 | -109 | 83 | 192 |
| maghrib | 13 | -6.69 | -115 | 119 | 234 |
| isha | 13 | +50.31 | -76 | 225 | 301 |


## Natural-profile subset (the configuration the real app would use)

Only rows whose original profile matches the city are included:
default for ordinary cities, `makkah` for مكة المكرمة, `madinah` for
المدينة المنورة. This removes the synthetic forced-profile combinations.
The synthetic `HighLatitude-55N` rows are reported separately below.

| Event | n | Mean (min) | Min (min) | Max (min) | Mean abs (min) | SD |
|---|---:|---:|---:|---:|---:|---:|
| fajr | 39 | +0.90 | -3 | 5 | 1.97 | 2.10 |
| sunrise | 39 | +2.46 | 0 | 7 | 2.46 | 2.26 |
| dhuhr | 39 | +0.87 | -1 | 4 | 1.28 | 1.60 |
| asr | 39 | +0.97 | -2 | 5 | 1.44 | 1.91 |
| maghrib | 39 | -3.97 | -9 | 1 | 4.03 | 2.96 |
| isha | 39 | -6.44 | -20 | 0 | 6.44 | 5.12 |
| **all** | 234 | -0.87 | -20 | 7 | 2.94 | 4.32 |


### Per city (natural profiles)

#### القدس

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 5 | +0.80 | 0 | 1 | 1 |
| sunrise | 5 | +3.60 | 3 | 4 | 1 |
| dhuhr | 5 | +1.00 | 1 | 1 | 0 |
| asr | 5 | +1.20 | 1 | 2 | 1 |
| maghrib | 5 | -5.40 | -6 | -5 | 1 |
| isha | 5 | -4.00 | -5 | -3 | 2 |


#### مكة المكرمة

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 5 | +2.00 | 2 | 2 | 0 |
| sunrise | 5 | +0.00 | 0 | 0 | 0 |
| dhuhr | 5 | +0.00 | 0 | 0 | 0 |
| asr | 5 | -0.40 | -1 | 0 | 1 |
| maghrib | 5 | +0.20 | 0 | 1 | 1 |
| isha | 5 | -14.60 | -20 | -10 | 10 |


#### المدينة المنورة

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 5 | +2.40 | 1 | 3 | 2 |
| sunrise | 5 | +0.00 | 0 | 0 | 0 |
| dhuhr | 5 | +0.20 | 0 | 1 | 1 |
| asr | 5 | +0.40 | 0 | 1 | 1 |
| maghrib | 5 | -0.40 | -1 | 0 | 1 |
| isha | 5 | -12.40 | -18 | -7 | 11 |


#### حيفا

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | +0.50 | -3 | 3 | 6 |
| sunrise | 4 | +4.50 | 2 | 7 | 5 |
| dhuhr | 4 | +2.00 | 2 | 2 | 0 |
| asr | 4 | +1.75 | 0 | 5 | 5 |
| maghrib | 4 | -5.00 | -7 | -2 | 5 |
| isha | 4 | -2.50 | -5 | 0 | 5 |


#### غزه

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | +3.75 | 3 | 5 | 2 |
| sunrise | 4 | +6.00 | 5 | 7 | 2 |
| dhuhr | 4 | +4.00 | 4 | 4 | 0 |
| asr | 4 | +4.50 | 4 | 5 | 1 |
| maghrib | 4 | -2.25 | -4 | -1 | 3 |
| isha | 4 | -1.25 | -4 | 0 | 4 |


#### نابلس

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -2.00 | -3 | -1 | 2 |
| sunrise | 4 | +1.25 | 0 | 3 | 3 |
| dhuhr | 4 | -1.00 | -1 | -1 | 0 |
| asr | 4 | -1.00 | -2 | 0 | 2 |
| maghrib | 4 | -7.75 | -9 | -7 | 2 |
| isha | 4 | -6.00 | -7 | -5 | 2 |


#### رام الله

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -1.50 | -2 | -1 | 1 |
| sunrise | 4 | +1.25 | 1 | 2 | 1 |
| dhuhr | 4 | -1.00 | -1 | -1 | 0 |
| asr | 4 | -0.50 | -1 | 0 | 1 |
| maghrib | 4 | -7.25 | -8 | -7 | 1 |
| isha | 4 | -6.00 | -7 | -5 | 2 |


#### الخليل

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -1.00 | -2 | 0 | 2 |
| sunrise | 4 | +1.75 | 1 | 3 | 2 |
| dhuhr | 4 | +0.00 | 0 | 0 | 0 |
| asr | 4 | +0.00 | -1 | 1 | 2 |
| maghrib | 4 | -6.50 | -8 | -6 | 2 |
| isha | 4 | -5.75 | -8 | -4 | 4 |


#### بئر السبع

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | +2.50 | 1 | 5 | 4 |
| sunrise | 4 | +4.75 | 4 | 7 | 3 |
| dhuhr | 4 | +3.00 | 3 | 3 | 0 |
| asr | 4 | +3.25 | 2 | 4 | 2 |
| maghrib | 4 | -3.00 | -5 | -2 | 3 |
| isha | 4 | -2.50 | -6 | -1 | 5 |


## Synthetic 55°N stress case (excluded from the verdict)

These rows place Al-Quds-style tables at latitude 55°N. The original app
has no latitude input at all, so its values stay at the Al-Quds timetable
while the solar engine correctly shortens the summer nights. The large
deviations are the expected evidence that a real solar engine is needed.

| Event | n | Mean (min) | Min (min) | Max (min) | Mean abs (min) | SD |
|---|---:|---:|---:|---:|---:|---:|
| fajr | 5 | -40.00 | -162 | 67 | 89.60 | 100.96 |
| sunrise | 5 | +28.60 | -60 | 119 | 76.60 | 74.95 |
| dhuhr | 5 | +32.00 | 32 | 32 | 32.00 | 0.00 |
| asr | 5 | +18.00 | -48 | 83 | 48.40 | 55.22 |
| maghrib | 5 | +30.80 | -59 | 119 | 65.60 | 74.64 |
| isha | 5 | +99.00 | -11 | 225 | 103.40 | 104.29 |
| **all** | 30 | +28.07 | -162 | 225 | 69.27 | 86.70 |


## Umm al-Qura method vs the 41 makkah-profile vectors

The engine runs Fajr 18.5° with Isha = Maghrib + 90 minutes.

| Event | n | Mean (min) | Min (min) | Max (min) | Mean abs (min) | SD |
|---|---:|---:|---:|---:|---:|---:|
| fajr | 41 | -35.39 | -238 | 39 | 38.17 | 42.54 |
| sunrise | 41 | -22.05 | -127 | 98 | 31.51 | 35.61 |
| dhuhr | 41 | -28.02 | -42 | 1 | 28.17 | 17.60 |
| asr | 41 | -34.66 | -109 | 62 | 37.73 | 29.94 |
| maghrib | 41 | -33.22 | -115 | 109 | 38.93 | 35.24 |
| isha | 41 | -33.22 | -115 | 109 | 38.93 | 35.24 |
| **all** | 246 | -31.09 | -238 | 109 | 35.57 | 33.91 |


### Per city (Umm al-Qura)

#### القدس

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -44.25 | -78 | -26 | 52 |
| sunrise | 4 | -34.25 | -63 | -17 | 46 |
| dhuhr | 4 | -40.00 | -40 | -40 | 0 |
| asr | 4 | -44.25 | -60 | -20 | 40 |
| maghrib | 4 | -44.25 | -61 | -16 | 45 |
| isha | 4 | -44.25 | -61 | -16 | 45 |


#### مكة المكرمة

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 5 | +0.00 | 0 | 0 | 0 |
| sunrise | 5 | +0.00 | 0 | 0 | 0 |
| dhuhr | 5 | +0.00 | 0 | 0 | 0 |
| asr | 5 | -0.40 | -1 | 0 | 1 |
| maghrib | 5 | +0.20 | 0 | 1 | 1 |
| isha | 5 | +0.20 | 0 | 1 | 1 |


#### المدينة المنورة

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | +0.00 | -9 | 5 | 14 |
| sunrise | 4 | +2.25 | -6 | 7 | 13 |
| dhuhr | 4 | +0.75 | 0 | 1 | 1 |
| asr | 4 | -1.75 | -5 | 1 | 6 |
| maghrib | 4 | -0.50 | -5 | 7 | 12 |
| isha | 4 | -0.50 | -5 | 7 | 12 |


#### حيفا

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -43.75 | -82 | -23 | 59 |
| sunrise | 4 | -33.25 | -65 | -14 | 51 |
| dhuhr | 4 | -39.00 | -39 | -39 | 0 |
| asr | 4 | -43.75 | -61 | -16 | 45 |
| maghrib | 4 | -44.00 | -63 | -12 | 51 |
| isha | 4 | -44.00 | -63 | -12 | 51 |


#### غزه

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -41.00 | -74 | -23 | 51 |
| sunrise | 4 | -31.75 | -60 | -15 | 45 |
| dhuhr | 4 | -37.00 | -37 | -37 | 0 |
| asr | 4 | -41.00 | -56 | -17 | 39 |
| maghrib | 4 | -41.25 | -58 | -14 | 44 |
| isha | 4 | -41.25 | -58 | -14 | 44 |


#### نابلس

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -46.50 | -83 | -27 | 56 |
| sunrise | 4 | -36.50 | -67 | -18 | 49 |
| dhuhr | 4 | -42.00 | -42 | -42 | 0 |
| asr | 4 | -46.50 | -63 | -21 | 42 |
| maghrib | 4 | -46.75 | -65 | -17 | 48 |
| isha | 4 | -46.75 | -65 | -17 | 48 |


#### رام الله

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -46.00 | -81 | -27 | 54 |
| sunrise | 4 | -36.50 | -66 | -19 | 47 |
| dhuhr | 4 | -42.00 | -42 | -42 | 0 |
| asr | 4 | -46.00 | -62 | -21 | 41 |
| maghrib | 4 | -46.25 | -64 | -17 | 47 |
| isha | 4 | -46.25 | -64 | -17 | 47 |


#### الخليل

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -45.25 | -79 | -27 | 52 |
| sunrise | 4 | -36.00 | -64 | -19 | 45 |
| dhuhr | 4 | -41.00 | -41 | -41 | 0 |
| asr | 4 | -45.50 | -61 | -22 | 39 |
| maghrib | 4 | -45.50 | -62 | -18 | 44 |
| isha | 4 | -45.50 | -62 | -18 | 44 |


#### بئر السبع

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -42.00 | -74 | -25 | 49 |
| sunrise | 4 | -33.00 | -60 | -17 | 43 |
| dhuhr | 4 | -38.00 | -38 | -38 | 0 |
| asr | 4 | -42.25 | -57 | -19 | 38 |
| maghrib | 4 | -42.00 | -58 | -15 | 43 |
| isha | 4 | -42.00 | -58 | -15 | 43 |


#### HighLatitude-55N

| Event | n | Mean (min) | Min (min) | Max (min) | Spread |
|---|---:|---:|---:|---:|---:|
| fajr | 4 | -54.00 | -238 | 39 | 277 |
| sunrise | 4 | +13.00 | -127 | 98 | 225 |
| dhuhr | 4 | -9.00 | -9 | -9 | 0 |
| asr | 4 | -43.75 | -109 | 62 | 171 |
| maghrib | 4 | -30.25 | -115 | 109 | 224 |
| isha | 4 | -30.25 | -115 | 109 | 224 |


## Verdict: are tuning offsets justified?

A fixed per-city/per-event offset is only useful if the deviation is
consistent across dates. Using the natural-profile subset, excluding the
synthetic 55°N rows:

| Metric | Value |
|---|---:|
| City/event groups with n ≥ 3 | 54 |
| Groups with spread ≤ 3 min | 43 (79.63%) |
| Groups with spread ≤ 5 min | 51 |

Mean deviation by city (all events, natural profiles):

| City | Mean deviation (min) |
|---|---:|
| نابلس | -2.75 |
| رام الله | -2.50 |
| مكة المكرمة | -2.13 |
| الخليل | -1.92 |
| المدينة المنورة | -1.63 |
| القدس | -0.47 |
| حيفا | +0.21 |
| بئر السبع | +1.33 |
| غزه | +2.46 |

**Conclusion.** 
Most city/event groups (79.63%) vary by 3 minutes or less across the year, so a small constant offset per city and event would absorb the systematic difference for the majority of cases. The remaining groups would need seasonal (monthly) offsets. This beta does **not** implement such offsets; the engine stays purely astronomical.

Largest mean deviation: **غزه** (+2.46 min); smallest: **نابلس** (-2.75 min).

Note: the synthetic `HighLatitude-55N` rows are excluded here because the original app has no latitude input and serves the Al-Quds table at 55°N; their large deviations are expected and are the clearest demonstration of why a real solar engine is needed.


## DST note

The original app has no timezone database: it stores fixed wall-clock
tables and adds a manual +60 minutes only for the default profile. The new
app converts real instants with `java.time` and the device zone, so late-March
and late-October transitions are automatic. That intentional difference is
removed from the numbers above by comparing in standard time.

