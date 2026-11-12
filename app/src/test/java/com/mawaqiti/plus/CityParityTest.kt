package com.mawaqiti.plus

import com.mawaqiti.plus.engine.table.TableEngine
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.engine.table.TableProfile
import java.io.File
import java.time.LocalDate
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Beta 7 — per-city constants and parity gate.
 *
 * 1. [referenceConstantsMatchEntryByEntry] hardcodes the 90 live reference
 *    values (`GET /api/ref-data/cites` as consumed by
 *    org.roki.tech.newbildqibla v6.28) and asserts Nidaa's cities.json
 *    entry-by-entry.
 * 2. [ninetyCitiesFourDatesParity] runs the real [TableEngine] against the
 *    Python oracle (`reference_impl/prayer_times.py`) for all 90 cities on
 *    2024-02-29, 2025-02-28, 2025-03-01 and 2025-06-21: six prayers plus the
 *    last third of the night, final HH:MM strings identical.
 */
class CityParityTest {

    private val events = listOf(
        "fajr" to TableEvent.FAJR,
        "sunrise" to TableEvent.SUNRISE,
        "dhuhr" to TableEvent.DHUHR,
        "asr" to TableEvent.ASR,
        "maghrib" to TableEvent.MAGHRIB,
        "isha" to TableEvent.ISHA
    )

    @Test
    fun referenceConstantsMatchEntryByEntry() {
        val nidaa = loadCities().associateBy { it.name }
        assertEquals("city count", 90, nidaa.size)

        val missing = REFERENCE_MIN_DIFF.keys.filterNot { it in nidaa }
        val extra = nidaa.keys.filterNot { it in REFERENCE_MIN_DIFF }
        assertEquals("missing cities: $missing", emptyList<String>(), missing)
        assertEquals("unexpected cities: $extra", emptyList<String>(), extra)

        val mismatches = REFERENCE_MIN_DIFF.mapNotNull { (name, expected) ->
            val actual = nidaa.getValue(name).minDiff
            if (actual == expected) null else "$name: reference=$expected nidaa=$actual"
        }
        assertEquals("minDiff mismatches:\n" + mismatches.joinToString("\n"), 0, mismatches.size)

        // The three owner-reported cities plus the Jerusalem control.
        assertEquals(-1, nidaa.getValue("طولكرم").minDiff)
        assertEquals(-2, nidaa.getValue("نابلس").minDiff)
        assertEquals(-2, nidaa.getValue("رام الله").minDiff)
        assertEquals(-2, nidaa.getValue("القدس").minDiff)
    }

    @Test
    fun ninetyCitiesFourDatesParity() {
        val cities = loadCities().associateBy { it.name }
        val expected = loadParityExpectations()

        val mismatches = ArrayList<String>()
        var comparisons = 0
        val reportRows = ArrayList<String>()

        for (row in expected) {
            val city = cities[row.city] ?: run {
                mismatches += "city missing from cities.json: ${row.city}"
                continue
            }
            assertEquals(
                "profile for ${row.city}",
                TableProfile.valueOf(row.profile.uppercase()),
                TableProfile.valueOf(city.profile.uppercase())
            )
            assertEquals("minDiff for ${row.city}", row.minDiff, city.minDiff)

            val day = TableEngine.day(
                TableProfile.valueOf(row.profile.uppercase()),
                city.minDiff,
                LocalDate.parse(row.date),
                summerTime = false
            )

            val actual = LinkedHashMap<String, String>()
            for ((name, event) in events) {
                actual[name] = TableEngine.format(day.minutesFor(event))
            }
            actual["last_third_of_night"] = TableEngine.format(day.lastThird)
            actual["fajr_tomorrow"] = TableEngine.format(day.fajrTomorrow)

            val expectedRow = LinkedHashMap<String, String>().apply {
                putAll(row.times)
                put("last_third_of_night", row.lastThird)
                put("fajr_tomorrow", row.fajrTomorrow)
            }

            for ((name, want) in expectedRow) {
                comparisons++
                val got = actual.getValue(name)
                if (got != want) {
                    mismatches += "${row.city} ${row.date} $name: expected=$want actual=$got"
                }
            }

            reportRows += "%s | %s | %s | %s | %s | %s | %s | %s | %s | %s".format(
                row.city,
                row.date,
                actual.getValue("fajr"),
                actual.getValue("sunrise"),
                actual.getValue("dhuhr"),
                actual.getValue("asr"),
                actual.getValue("maghrib"),
                actual.getValue("isha"),
                actual.getValue("last_third_of_night"),
                actual.getValue("fajr_tomorrow")
            )
        }

        writeReport(expected.size, comparisons, mismatches, reportRows)

        assertTrue("no parity rows were evaluated", expected.isNotEmpty())
        assertEquals(
            "parity mismatches:\n" + mismatches.take(40).joinToString("\n"),
            0,
            mismatches.size
        )
        assertEquals("day rows", 360, expected.size)
        assertEquals("time strings", 2880, comparisons)
    }

    // ------------------------------------------------------------------ data

    private data class City(val name: String, val minDiff: Int, val profile: String)

    private data class Expectation(
        val city: String,
        val date: String,
        val profile: String,
        val minDiff: Int,
        val times: Map<String, String>,
        val lastThird: String,
        val fajrTomorrow: String
    )

    private fun loadCities(): List<City> {
        val candidates = listOf(
            File("src/main/assets/cities.json"),
            File("app/src/main/assets/cities.json"),
            File("../app/src/main/assets/cities.json")
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("cities.json not found from ${File(".").absolutePath}")
        val array = JSONArray(file.readText(Charsets.UTF_8))
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            City(
                name = item.getString("nameAr").trim(),
                minDiff = item.getInt("minDiff"),
                profile = item.getString("profile")
            )
        }
    }

    private fun loadParityExpectations(): List<Expectation> {
        val text = CityParityTest::class.java
            .getResourceAsStream("/city_parity_expected.csv")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("city_parity_expected.csv not found in test resources")

        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        val header = lines.first().removePrefix("\uFEFF").split(",")
        if (header.size < 13) error("unexpected parity header: $header")

        return lines.drop(1).map { line ->
            val fields = line.split(",")
            Expectation(
                city = fields[0],
                date = fields[2],
                profile = fields[3],
                minDiff = fields[4].toInt(),
                times = linkedMapOf(
                    "fajr" to fields[5],
                    "sunrise" to fields[6],
                    "dhuhr" to fields[7],
                    "asr" to fields[8],
                    "maghrib" to fields[9],
                    "isha" to fields[10]
                ),
                lastThird = fields[11],
                fajrTomorrow = fields[12]
            )
        }
    }

    private fun writeReport(
        rows: Int,
        comparisons: Int,
        mismatches: List<String>,
        reportRows: List<String>
    ) {
        val reportDir = File(System.getProperty("mawaqiti.reportDir") ?: "../report")
        reportDir.mkdirs()
        val builder = StringBuilder()
        builder.appendLine("city_parity_report.txt  (beta7)")
        builder.appendLine()
        builder.appendLine("Reference : org.roki.tech.newbildqibla v6.28")
        builder.appendLine("            GET https://mawagets.net/api/ref-data/cites (live min_diff)")
        builder.appendLine("Oracle    : reference_impl/prayer_times.py (w7.c tables + p061i1.j.o/w)")
        builder.appendLine("Engine    : com.mawaqiti.plus.engine.table.TableEngine (Kotlin)")
        builder.appendLine("Dates     : 2024-02-29, 2025-02-28, 2025-03-01, 2025-06-21")
        builder.appendLine("Fields    : fajr, sunrise, dhuhr, asr, maghrib, isha, last third, fajr tomorrow")
        builder.appendLine()
        builder.appendLine("Summary")
        builder.appendLine("-------")
        builder.appendLine("cities        : 90")
        builder.appendLine("day rows      : %d".format(rows))
        builder.appendLine("time strings  : %d".format(comparisons))
        builder.appendLine("mismatches    : %d".format(mismatches.size))
        builder.appendLine("result        : %s".format(if (mismatches.isEmpty()) "PASS" else "FAIL"))
        builder.appendLine()
        if (mismatches.isNotEmpty()) {
            builder.appendLine("Mismatches")
            builder.appendLine("----------")
            mismatches.forEach { builder.appendLine(it) }
            builder.appendLine()
        }
        builder.appendLine("Jerusalem / reported cities (control + owner cases)")
        builder.appendLine("--------------------------------------------------")
        reportRows.filter {
            it.startsWith("القدس ") || it.startsWith("طولكرم ") ||
                it.startsWith("نابلس ") || it.startsWith("رام الله ")
        }.forEach { builder.appendLine(it) }
        builder.appendLine()
        builder.appendLine("All rows (city | date | fajr | sunrise | dhuhr | asr | maghrib | isha | lastThird | fajrTomorrow)")
        builder.appendLine("-".repeat(118))
        reportRows.forEach { builder.appendLine(it) }
        File(reportDir, "city_parity_report.txt").writeText(builder.toString(), Charsets.UTF_8)
    }

    private companion object {
        /**
         * The 90 live reference constants, verbatim from
         * report/reference_cities_api.json (seq_no order). The 90-entry array
         * embedded in the reference APK (y7/C3444b.java) is its stale offline
         * fallback and differs on 67 cities; betas 1-6 used that by mistake.
         */
        val REFERENCE_MIN_DIFF: Map<String, Int> = linkedMapOf(
            "القدس" to -2,
            "مكة المكرمة" to 0,
            "المدينة المنورة" to 0,
            "ام الفحم" to -2,
            "اكسال" to -2,
            "اذنا" to -1,
            "اريحا" to -3,
            "البعنة" to -2,
            "البعينة (نجيدات)" to -2,
            "البياضة" to -2,
            "الخليل" to -1,
            "الرامة" to -2,
            "الرملة" to 0,
            "الطيبة" to -1,
            "الطيرة" to 0,
            "الظاهريه" to -1,
            "العفولة" to -2,
            "الفريديس" to -2,
            "اللد" to 0,
            "اللقية" to 0,
            "المزرعة" to -2,
            "الناصرة" to -2,
            "بئر السبع" to 0,
            "بئر المكسور" to 0,
            "باقة الغربية" to -2,
            "بسمة" to -2,
            "بسمة طبعون" to -1,
            "بيت امر" to -2,
            "بيت اولى" to -1,
            "بيت عوا" to -1,
            "بيت لحم" to -2,
            "بيسان" to -3,
            "تل ابيب" to 0,
            "تل السبع" to 0,
            "جت" to -2,
            "جديدة - المكر" to -2,
            "جسر الزرقاء" to -1,
            "جنين" to -2,
            "حورة" to 0,
            "حيفا" to -1,
            "خان يونس" to 2,
            "دبورية" to -2,
            "دورا" to -1,
            "دير الأسد" to -2,
            "دير البلح" to 2,
            "رام الله" to -2,
            "رفح" to 2,
            "رهط" to 0,
            "زلفة" to -2,
            "سالم" to -2,
            "سخنين" to -1,
            "شفا عمرو" to -2,
            "شقيب السلام" to 0,
            "صفد" to -3,
            "صندلة" to -2,
            "طبريا" to -3,
            "طلعة عارة" to -2,
            "طمرة" to -3,
            "طولكرم" to -1,
            "عاره" to -2,
            "عرعرة النقب" to 0,
            "عكا" to -1,
            "عين السهلة" to -2,
            "غزه" to 3,
            "قلقيلية" to -1,
            "قلنسوة" to -2,
            "كسيفة" to 0,
            "كفر قاسم" to -1,
            "كفر قرع" to -2,
            "كفر مندا" to -1,
            "كفر ياسيف" to -2,
            "مجد الكروم" to -2,
            "مخيم الفوار" to 0,
            "مشيرفة" to -2,
            "مصمص" to -2,
            "معاوية" to -2,
            "منشية زبدة" to -2,
            "نابلس" to -2,
            "يافا" to 0,
            "يطا" to 0,
            "قبل القدس بـ 1 د" to -3,
            "قبل القدس بـ 2 د" to -4,
            "قبل القدس بـ 3 د" to -5,
            "قبل القدس بـ 4 د" to -6,
            "قبل القدس بـ 5 د" to -7,
            "بعد القدس بـ 1 د" to -1,
            "بعد القدس بـ 2 د" to 0,
            "بعد القدس بـ 3 د" to 1,
            "بعد القدس بـ 4 د" to 2,
            "بعد القدس بـ 5 د" to 3
        )
    }
}
