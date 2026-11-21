"""
Generates the JORDAN prayer tables (one per city, 366 leap-aligned rows x 6
events) that the app ships as data behind `TableProfile.JORDAN_*`.

Why data and not a new algorithm
---------------------------------
The app's runtime engine (`TableEngine`) is a bit-exact port of the original
APK: it holds integer HOURS/MINUTES arrays and only applies the per-city minute
offset. Beta23 therefore adds Jordan as *new data* plus a *convention profile*
(the parameters below), and never touches the engine's arithmetic.

Convention (وزارة الأوقاف والشؤون والمقدسات الإسلامية، الأردن)
-------------------------------------------------------------
  Fajr     18.0 deg below the horizon
  Isha     18.0 deg below the horizon
  Maghrib  sunset + 5 minutes
  Asr      standard (Shafi'i, shadow factor 1)
  Sunrise  -0.833 deg (refraction + solar semidiameter)

Sources:
  * AlAdhan calculation method 23 "Ministry of Awqaf, Islamic Affairs and Holy
    Places, Jordan" -> Fajr 18, Isha 18, Maghrib +5 min, school STANDARD
    https://api.aladhan.com/v1/methods (id 23)
  * https://github.com/prayer-timetable/adhan_dart -> jordan(): 18/18, +5 Maghrib
  * assabile.com/prayer-times/methods -> Jordan: Fajr 18.2, Isha 18

Timezone
--------
Asia/Amman has been permanently UTC+03:00 since 2022-10-28, so a table in local
clock minutes is DST-safe. `JordanValidationTest` asserts the zone offset is
+03:00 for every day of the table's reference year, so a future rule change
fails the build instead of silently shifting every time by an hour.

One table per city
------------------
Jordan spans 2.6 degrees of latitude (Aqaba 29.53 N to Irbid 32.56 N). At the
18-degree Fajr angle that is worth more than ten minutes, so a single shared
table plus one constant minute offset cannot stay inside a +/-1 minute gate.
Each city therefore gets its own table generated from its own coordinates.

Output
------
  app/src/main/java/com/mawaqiti/plus/engine/table/JordanTables.kt
"""

import datetime
import math
import os
import sys

ZONE_OFFSET_HOURS = 3.0

FAJR_ANGLE = 18.0
ISHA_ANGLE = 18.0
MAGHRIB_OFFSET_MIN = 5
SUNRISE_ALTITUDE = -0.833
DEG = math.pi / 180.0

EVENTS = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]

# The twelve Jordanian cities, matching app/src/main/assets/cities.json.
CITIES = [
    ("Amman", "عمّان", 31.9539, 35.9106),
    ("Zarqa", "الزرقاء", 32.0728, 36.0880),
    ("Irbid", "إربد", 32.5556, 35.8500),
    ("Aqaba", "العقبة", 29.5321, 35.0063),
    ("Mafraq", "المفرق", 32.3429, 36.2080),
    ("Salt", "السلط", 32.0392, 35.7272),
    ("Madaba", "مأدبا", 31.7159, 35.7936),
    ("Karak", "الكرك", 31.1853, 35.7048),
    ("Tafilah", "الطفيلة", 30.8356, 35.6042),
    ("Maan", "معان", 30.1962, 35.7340),
    ("Jerash", "جرش", 32.2807, 35.8993),
    ("Ajloun", "عجلون", 32.3325, 35.7517),
]


def julian_day(y, m, d):
    if m <= 2:
        y -= 1
        m += 12
    a = math.floor(y / 100.0)
    b = 2.0 - a + math.floor(a / 4.0)
    return math.floor(365.25 * (y + 4716)) + math.floor(30.6001 * (m + 1)) + d + b - 1524.5


def fix_angle(a):
    a = a % 360.0
    return a + 360.0 if a < 0 else a


def fix_hour(h):
    h = h % 24.0
    return h + 24.0 if h < 0 else h


def sun_position(jd):
    d = jd - 2451545.0
    g = fix_angle(357.529 + 0.98560028 * d)
    q = fix_angle(280.459 + 0.98564736 * d)
    l = fix_angle(q + 1.915 * math.sin(g * DEG) + 0.020 * math.sin(2 * g * DEG))
    e = 23.439 - 0.00000036 * d
    ra = fix_hour(math.atan2(math.cos(e * DEG) * math.sin(l * DEG), math.cos(l * DEG)) / DEG / 15.0)
    dec = math.asin(math.sin(e * DEG) * math.sin(l * DEG)) / DEG
    eqt = q / 15.0 - ra
    return dec, eqt


def hour_angle(altitude, lat, dec):
    cos_h = (math.sin(altitude * DEG) - math.sin(lat * DEG) * math.sin(dec * DEG)) / (
        math.cos(lat * DEG) * math.cos(dec * DEG)
    )
    if cos_h != cos_h or cos_h > 1.0 or cos_h < -1.0:
        return None
    return math.acos(cos_h) / DEG / 15.0


def solar_day(y, m, d, lat, lon):
    jd = julian_day(y, m, d)

    def sun_at(hours):
        return sun_position(jd + hours / 24.0)

    transit = 12.0 - lon / 15.0 - sun_at(12.0)[1]
    for _ in range(3):
        transit = 12.0 - lon / 15.0 - sun_at(transit)[1]

    def event(altitude, after):
        angle = hour_angle(altitude, lat, sun_at(transit)[0])
        if angle is None:
            return None
        for _ in range(3):
            t = transit + angle if after else transit - angle
            angle = hour_angle(altitude, lat, sun_at(t)[0])
            if angle is None:
                return None
        return transit + angle if after else transit - angle

    sunrise = event(SUNRISE_ALTITUDE, False)
    sunset = event(SUNRISE_ALTITUDE, True)
    # Standard Asr: shadow = 1x object + noon shadow, declination at transit.
    asr_angle = math.degrees(
        math.atan(1.0 / (1.0 + math.tan(abs(lat - sun_at(transit)[0]) * DEG)))
    )
    asr = event(asr_angle, True)
    fajr = event(-FAJR_ANGLE, False)
    isha = event(-ISHA_ANGLE, True)
    return {
        "fajr": fajr,
        "sunrise": sunrise,
        "dhuhr": transit,
        "asr": asr,
        "sunset": sunset,
        "isha": isha,
    }


def to_minutes(ut_hours):
    """UT hours -> local minute of day, rounded to the nearest minute."""
    if ut_hours is None:
        return None
    local = ut_hours + ZONE_OFFSET_HOURS
    return int(math.floor(local * 60.0 + 0.5)) % 1440


def day_minutes(y, m, d, lat, lon):
    s = solar_day(y, m, d, lat, lon)
    sunset_min = to_minutes(s["sunset"])
    return {
        "fajr": to_minutes(s["fajr"]),
        "sunrise": to_minutes(s["sunrise"]),
        "dhuhr": to_minutes(s["dhuhr"]),
        "asr": to_minutes(s["asr"]),
        "maghrib": (sunset_min + MAGHRIB_OFFSET_MIN) % 1440,
        "isha": to_minutes(s["isha"]),
    }


def is_leap(year):
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def table_rows(year):
    """The 366 dates that fill the app's leap-aligned rows for [year].

    `TableEngine.rowFor` is `doy - 1` in a leap year and, for dates after
    28 February in a common year, `doy`. Row 59 (29 February) is therefore
    unreachable in a common year; it is filled from 28 February so a leap year
    still has a sensible value there.
    """
    start = datetime.date(year, 1, 1)
    rows = [None] * 366
    days = 366 if is_leap(year) else 365
    for index in range(days):
        day = start + datetime.timedelta(days=index)
        doy = day.timetuple().tm_yday
        row = doy - 1
        if not is_leap(year) and doy > 59:
            row = doy
        rows[row] = day
    for index, value in enumerate(rows):
        if value is None:
            rows[index] = rows[index - 1]
    return rows


def build_table(year, lat, lon):
    rows = table_rows(year)
    table = {e: [] for e in EVENTS}
    for date in rows:
        minutes = day_minutes(date.year, date.month, date.day, lat, lon)
        for e in EVENTS:
            table[e].append(minutes[e])
    return table


def emit_event_arrays(lines, table, field):
    lines.append("        val %s: Array<IntArray> = arrayOf(" % field)
    for event in EVENTS:
        lines.append("            // %s" % event)
        lines.append("            intArrayOf(")
        if field == "HOURS":
            values = [v // 60 for v in table[event]]
        else:
            values = [v % 60 for v in table[event]]
        for j in range(0, len(values), 24):
            lines.append("                " + ", ".join(str(v) for v in values[j:j + 24]) + ",")
        lines.append("            ),")
    lines.append("        )")


def emit_kotlin(tables, path, year):
    lines = []
    lines.append("package com.mawaqiti.plus.engine.table")
    lines.append("")
    lines.append("/**")
    lines.append(" * GENERATED FILE - do not edit by hand.")
    lines.append(" * Source: tools/generate_jordan_table.py (see its docstring for the")
    lines.append(" * full convention and the primary sources).")
    lines.append(" *")
    lines.append(" * Jordan convention profile, the parameters every table encodes:")
    lines.append(" *   Fajr 18.0 deg, Isha 18.0 deg, Maghrib = sunset + 5 min,")
    lines.append(" *   Asr standard (Shafi'i), Sunrise -0.833 deg.")
    lines.append(" *")
    lines.append(" * One table per city, generated from that city's own coordinates:")
    lines.append(" * Jordan spans 2.6 degrees of latitude, which is worth more than ten")
    lines.append(" * minutes at Fajr between Amman and Aqaba, so a single shared table")
    lines.append(" * plus a constant minute offset cannot be accurate.")
    lines.append(" *")
    lines.append(" * 366 rows indexed by the app's leap-year-aligned day row (0..365,")
    lines.append(" * 29 February at index 59). Event order: fajr, sunrise, dhuhr, asr,")
    lines.append(" * maghrib, isha. Values are local clock time (Asia/Amman = UTC+3).")
    lines.append(" * Geometry reference year: %d (regenerate with tools/generate_jordan_table.py)." % year)
    lines.append(" */")
    lines.append("internal object JordanTables {")
    for key, name_ar, lat, lon in CITIES:
        table = tables[key]
        lines.append("")
        lines.append("    /** %s (%s), %.4f N %.4f E. */" % (name_ar, key, lat, lon))
        lines.append("    internal object %s {" % key)
        emit_event_arrays(lines, table, "HOURS")
        emit_event_arrays(lines, table, "MINUTES")
        lines.append("    }")
    lines.append("}")
    lines.append("")
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(lines))


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    year = int(sys.argv[1]) if len(sys.argv) > 1 else 2026
    tables = {key: build_table(year, lat, lon) for key, _, lat, lon in CITIES}
    out_kt = os.path.join(
        root, "app", "src", "main", "java", "com", "mawaqiti", "plus",
        "engine", "table", "JordanTables.kt"
    )
    emit_kotlin(tables, out_kt, year)
    print("wrote", len(tables), "tables x", len(tables["Amman"]["fajr"]), "rows")
    for key in ("Amman", "Aqaba"):
        print(key, {e: "%02d:%02d" % (tables[key][e][0] // 60, tables[key][e][0] % 60)
                    for e in EVENTS})


if __name__ == "__main__":
    main()
