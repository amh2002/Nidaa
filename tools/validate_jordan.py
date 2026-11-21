"""
Compares the generated Jordan table against the AlAdhan method-23 reference
calendar (the ministry preset) and prints the full diff table.

Usage:  python tools/validate_jordan.py <reference.json> <city-label> [--full]
"""

import importlib.util
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location(
    "gen", os.path.join(HERE, "generate_jordan_table.py")
)
gen = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gen)

EVENTS = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]
ALADHAN_KEYS = ["Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"]


def load_reference(path):
    data = json.load(open(path, encoding="utf-8"))["data"]
    rows = {}
    for month in data.values():
        for entry in month:
            g = entry["date"]["gregorian"]
            key = (int(g["year"]), int(g["month"]["number"]), int(g["day"]))
            timings = {}
            for event, api_key in zip(EVENTS, ALADHAN_KEYS):
                raw = entry["timings"][api_key].split(" ")[0]
                hh, mm = raw.split(":")
                timings[event] = int(hh) * 60 + int(mm)
            rows[key] = timings
    return rows


def chosen_dates(reference):
    """>=8 dates across the year + 2 near DST dates + spot checks."""
    wanted = [
        (2026, 1, 15), (2026, 2, 20), (2026, 3, 20), (2026, 12, 21),
        (2026, 4, 10), (2026, 6, 21), (2026, 7, 15), (2026, 8, 20),
        (2026, 9, 10), (2026, 10, 25), (2026, 3, 27),
    ]
    # DST-adjacent dates: 2026-03-27 (last Friday of March) and 2026-10-30.
    wanted.append((2026, 10, 30))
    return [d for d in wanted if d in reference]


def main():
    path = sys.argv[1]
    label = sys.argv[2] if len(sys.argv) > 2 else "Amman"
    full = "--full" in sys.argv
    reference = load_reference(path)

    dates = sorted(reference) if full else chosen_dates(reference)
    worst = 0
    failures = []
    print("%-12s %-9s %-7s %-7s %-5s" % ("date", "prayer", "official", "computed", "diff"))
    print("-" * 46)
    for (y, m, d) in dates:
        computed = gen.day_minutes(y, m, d)
        official = reference[(y, m, d)]
        for event in EVENTS:
            diff = computed[event] - official[event]
            worst = max(worst, abs(diff))
            flag = "" if abs(diff) <= 1 else "  <== FAIL"
            if abs(diff) > 1:
                failures.append(((y, m, d), event, official[event], computed[event], diff))
            print("%04d-%02d-%02d  %-9s %02d:%02d   %02d:%02d    %+3d%s" % (
                y, m, d, event,
                official[event] // 60, official[event] % 60,
                computed[event] // 60, computed[event] % 60,
                diff, flag,
            ))
        print()
    print("%s worst absolute difference: %d minute(s)" % (label, worst))
    print("rows compared:", len(dates) * len(EVENTS))
    if failures:
        print("FAILURES:", len(failures))
        return 1
    print("RESULT: PASS (all within +/-1 minute)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
