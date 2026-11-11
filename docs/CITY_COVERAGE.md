# CITY_COVERAGE.md

City coverage of Nidaa BETA, derived from the city list embedded in
the original APK (`y7.C3444b`) and exported to `app/src/main/assets/cities.json`.

## Summary

| Item | Value |
|---|---:|
| Entries in the verified list | 90 |
| Offered under فلسطين | 88 |
| Offered under السعودية | 2 |
| Synthetic Al-Quds offsets | 10 |
| Jordanian entries | 0 |

The picker is a two-step, zero-typing cascade:

1. **Country** — two large cards: فلسطين / السعودية.
2. **City** — a read-only `ExposedDropdownMenuBox` filtered to that country,
   sorted with the active locale's `Collator`. No search field, no free text,
   no manual coordinates.

The default on first launch is **القدس – فلسطين** (`minDiff = -2`), matching
the original app.

## فلسطين (88 entries)

Sorted by English name for readability; the app sorts by the active language.

| Order | nameAr | nameEn | minDiff | Profile | Note |
|---:|---|---|---:|---|---|
| 85 | بعد القدس بـ 1 د | 1 min after Al-Quds | +1 | default | synthetic offset |
| 80 | قبل القدس بـ 1 د | 1 min before Al-Quds | -5 | default | synthetic offset |
| 86 | بعد القدس بـ 2 د | 2 min after Al-Quds | +2 | default | synthetic offset |
| 81 | قبل القدس بـ 2 د | 2 min before Al-Quds | -6 | default | synthetic offset |
| 87 | بعد القدس بـ 3 د | 3 min after Al-Quds | +3 | default | synthetic offset |
| 82 | قبل القدس بـ 3 د | 3 min before Al-Quds | -7 | default | synthetic offset |
| 88 | بعد القدس بـ 4 د | 4 min after Al-Quds | -2 | default | synthetic offset |
| 83 | قبل القدس بـ 4 د | 4 min before Al-Quds | -1 | default | synthetic offset |
| 89 | بعد القدس بـ 5 د | 5 min after Al-Quds | -2 | default | synthetic offset |
| 84 | قبل القدس بـ 5 د | 5 min before Al-Quds | 0 | default | synthetic offset |
| 61 | عكا | Acre | +3 | default |  |
| 16 | العفولة | Afula | 0 | default |  |
| 7 | البعنة | Al-Ba'na | -2 | default |  |
| 9 | البياضة  | Al-Bayada | -2 | default |  |
| 8 | البعينة (نجيدات) | Al-Bu'eina (Nujeidat) | -1 | default |  |
| 72 | مخيم الفوار | Al-Fawwar Camp | -2 | default |  |
| 20 | المزرعة | Al-Mazra'a | 0 | default |  |
| 0 | القدس | Al-Quds | -2 | default |  |
| 15 | الظاهريه | Al-Zahiriya | -2 | default |  |
| 59 | عاره | Ar'ara | -1 | default |  |
| 60 | عرعرة النقب | Ar'arat an-Naqab | -2 | default |  |
| 11 | الرامة | Ar-Rama | -1 | default |  |
| 24 | باقة الغربية | Baqa al-Gharbiyye | -1 | default |  |
| 25 | بسمة | Basma | -2 | default |  |
| 26 | بسمة طبعون | Basmat Tab'un | -1 | default |  |
| 22 | بئر السبع | Beersheba | -2 | default |  |
| 29 | بيت عوا | Beit Awwa | -3 | default |  |
| 31 | بيسان | Beit She'an | 0 | default |  |
| 28 | بيت اولى | Beit Ula | -2 | default |  |
| 27 | بيت امر | Beit Ummar | -1 | default |  |
| 30 | بيت لحم | Bethlehem | 0 | default |  |
| 23 | بئر المكسور | Bir al-Maksur | -2 | default |  |
| 41 | دبورية | Daburiyya | -2 | default |  |
| 43 | دير الأسد | Deir al-Asad | -2 | default |  |
| 44 | دير البلح | Deir al-Balah | +2 | default |  |
| 42 | دورا | Dura | +2 | default |  |
| 62 | عين السهلة | Ein as-Sahla | -1 | default |  |
| 17 | الفريديس | Fureidis | 0 | default |  |
| 63 | غزه | Gaza | -2 | default |  |
| 39 | حيفا | Haifa | -2 | default |  |
| 10 | الخليل | Hebron | 0 | default |  |
| 38 | حورة | Hura | +2 | default |  |
| 5 | اذنا | Idhna | -2 | default |  |
| 4 | اكسال | Iksal | -3 | default |  |
| 35 | جديدة - المكر | Jadeidi-Makr | -2 | default |  |
| 78 | يافا | Jaffa | -3 | default |  |
| 34 | جت  | Jatt | -1 | default |  |
| 37 | جنين | Jenin | -1 | default |  |
| 6 | اريحا | Jericho | -2 | default |  |
| 36 | جسر الزرقاء | Jisr az-Zarqa | 0 | default |  |
| 69 | كفر مندا | Kafr Manda | -2 | default |  |
| 68 | كفر قرع | Kafr Qara | -2 | default |  |
| 67 | كفر قاسم | Kafr Qasim | -1 | default |  |
| 70 | كفر ياسيف | Kafr Yasif | 0 | default |  |
| 40 | خان يونس | Khan Yunis | -1 | default |  |
| 66 | كسيفة | Kuseife | -2 | default |  |
| 19 | اللقية | Laqiya | -2 | default |  |
| 18 | اللد | Lod | -2 | default |  |
| 71 | مجد الكروم | Majd al-Krum | -2 | default |  |
| 76 | منشية زبدة | Manshiyat Zabda | 0 | default |  |
| 75 | معاوية | Mu'awiya | -2 | default |  |
| 73 | مشيرفة | Musheirifa | -2 | default |  |
| 74 | مصمص | Musmus | -2 | default |  |
| 77 | نابلس | Nablus | 0 | default |  |
| 21 | الناصرة | Nazareth | 0 | default |  |
| 65 | قلنسوة | Qalansawe | -1 | default |  |
| 64 | قلقيلية | Qalqilya | 0 | default |  |
| 46 | رفح | Rafah | -2 | default |  |
| 47 | رهط | Rahat | -2 | default |  |
| 45 | رام الله | Ramallah | 0 | default |  |
| 12 | الرملة | Ramla | 0 | default |  |
| 53 | صفد | Safed | -3 | default |  |
| 50 | سخنين | Sakhnin | 0 | default |  |
| 49 | سالم | Salem | -2 | default |  |
| 54 | صندلة | Sandala | -2 | default |  |
| 52 | شقيب السلام | Shaqib al-Salam | -2 | default |  |
| 51 | شفا عمرو | Shfar'am | -3 | default |  |
| 56 | طلعة عارة | Tal'at 'Ara | -1 | default |  |
| 57 | طمرة | Tamra | -2 | default |  |
| 13 | الطيبة | Tayibe | -1 | default |  |
| 33 | تل السبع | Tel as-Sabi | -2 | default |  |
| 32 | تل ابيب | Tel Aviv | -2 | default |  |
| 55 | طبريا | Tiberias | -3 | default |  |
| 14 | الطيرة | Tira | -2 | default |  |
| 58 | طولكرم | Tulkarm | 0 | default |  |
| 3 | ام الفحم | Umm al-Fahm | -1 | default |  |
| 79 | يطا | Yatta | -4 | default |  |
| 48 | زلفة | Zalafa | -1 | default |  |

## السعودية (2 entries)

| Order | nameAr | nameEn | minDiff | Profile | Note |
|---:|---|---|---:|---|---|
| 2 | المدينة المنورة | Madinah | 0 | madinah |  |
| 1 | مكة المكرمة | Makkah | 0 | makkah |  |

### Why مكة المكرمة and المدينة المنورة appear under السعودية

They are the only two entries with their own tables (`makkah` and `madinah`).
They are not approximations: the engine uses their real profiles, and all
three profiles are covered by the 1440-case sweep.

## Synthetic Al-Quds offsets (10 entries)

These ten entries are synthetic offsets shipped by the original app. Their `minDiff` values are internally inconsistent in the original (for example, `قبل القدس بـ 4 د` is -1 while `قبل القدس بـ 5 د` is 0). They are reproduced byte-for-byte rather than corrected, because the goal is parity, not repair.

| Order | nameAr | nameEn | minDiff |
|---:|---|---|---:|
| 80 | قبل القدس بـ 1 د | 1 min before Al-Quds | -5 |
| 81 | قبل القدس بـ 2 د | 2 min before Al-Quds | -6 |
| 82 | قبل القدس بـ 3 د | 3 min before Al-Quds | -7 |
| 83 | قبل القدس بـ 4 د | 4 min before Al-Quds | -1 |
| 84 | قبل القدس بـ 5 د | 5 min before Al-Quds | 0 |
| 85 | بعد القدس بـ 1 د | 1 min after Al-Quds | +1 |
| 86 | بعد القدس بـ 2 د | 2 min after Al-Quds | +2 |
| 87 | بعد القدس بـ 3 د | 3 min after Al-Quds | +3 |
| 88 | بعد القدس بـ 4 د | 4 min after Al-Quds | -2 |
| 89 | بعد القدس بـ 5 د | 5 min after Al-Quds | -2 |

## Not covered in 1.0.0-beta1

### Palestinian localities absent from the extracted list

The following well-known localities have no entry in the verified list, so no
verified `minDiff` exists for them. They are **not** fabricated. Until a future
release ingests the original app's server list, the suggested treatment is to
select the nearest verified entry shown below.

| nameAr | nameEn | Suggested verified selection |
|---|---|---|
| سلفيت | Salfit | نابلس (Nablus) |
| طوباس | Tubas | نابلس (Nablus) / جنين (Jenin) |
| بيت جالا | Beit Jala | بيت لحم (Bethlehem) |
| بيت ساحور | Beit Sahour | بيت لحم (Bethlehem) |
| اليامون | Al-Yamun | جنين (Jenin) |
| يعبد | Ya'bad | جنين (Jenin) |
| السموع | As-Samu | الخليل (Hebron) |
| أبوديس | Abu Dis | القدس (Al-Quds) |
| العيزرية | Al-Eizariya | القدس (Al-Quds) |
| كفر عقب | Kafr 'Aqab | القدس (Al-Quds) |
| مخيم بلاطة | Balata Camp | نابلس (Nablus) |
| مخيم جباليا | Jabalia Camp | غزه (Gaza) |
| جباليا | Jabalia | غزه (Gaza) |
| بيت حانون | Beit Hanoun | غزه (Gaza) |
| بيت لاهيا | Beit Lahia | غزه (Gaza) |
| بني سهيلا | Bani Suheila | خان يونس (Khan Yunis) |
| عبسان الكبيرة | 'Abasan al-Kabira | خان يونس (Khan Yunis) |
| الشجاعية | Shuja'iyya | غزه (Gaza) |

### Jordanian cities

The extracted 90-entry fallback list contains **zero Jordanian cities**. The original app can refresh its city list from an authenticated server endpoint (`api/ref-data/cites`) that could not be retrieved during the RE. Adding JO rows here would require inventing `minDiff` values, which would break the bit-exactness guarantee, so the Jordan card is intentionally absent from 1.0.0-beta1. A future release should ingest the server list when it is available; until then, do not approximate Jordanian times.

| nameAr | nameEn | Suggested treatment |
|---|---|---|
| عمّان | Amman | Not offered; wait for verified server data. |
| إربد | Irbid | Not offered; wait for verified server data. |
| الزرقاء | Zarqa | Not offered; wait for verified server data. |
| العقبة | Aqaba | Not offered; wait for verified server data. |
| السلط | Salt | Not offered; wait for verified server data. |
| مادبا | Madaba | Not offered; wait for verified server data. |
| جرش | Jerash | Not offered; wait for verified server data. |
| عجلون | Ajloun | Not offered; wait for verified server data. |
| الكرك | Karak | Not offered; wait for verified server data. |
| معان | Ma'an | Not offered; wait for verified server data. |
| المفرق | Mafraq | Not offered; wait for verified server data. |
| الطفيلة | Tafilah | Not offered; wait for verified server data. |
| الرمثا | Ramtha | Not offered; wait for verified server data. |
| وادي السير | Wadi as-Sir | Not offered; wait for verified server data. |
| سحاب | Sahab | Not offered; wait for verified server data. |
| الرصيفة | Russeifa | Not offered; wait for verified server data. |

## Notes

* Times come from the table lookup only; latitude and longitude are used
  **exclusively** for the qibla bearing and distance.
* `minDiff` is the per-city minute offset applied exactly as the original app
  applies it (including carry/borrow and `% 24`).
* `profile` is derived from the exact Arabic name in the original; it is stored
  explicitly here so that display-name trimming can never change the table.
* The two names `البياضة ` and `جت ` ship with a trailing space in the original
  and are preserved verbatim in `cities.json`; the UI trims them for display.

