package com.mawaqiti.plus

import com.mawaqiti.plus.engine.table.TableEngine
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.engine.table.TableProfile
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta23 - the Jordan gate.
 *
 * The twelve Jordanian tables in
 * [com.mawaqiti.plus.engine.table.JordanTables] were generated from the
 * ministry convention profile (Fajr 18, Isha 18, Maghrib = sunset + 5 min, Asr
 * standard). This test is the gate the task demands: it runs the real
 * [TableEngine] against the ministry preset's published times and fails if any
 * of the five prayers is more than ONE minute away.
 *
 * Coverage:
 *  - Amman: 12 dates spread across the whole year, including both old Jordanian
 *    DST transition dates (last Friday of March, last Friday of October).
 *  - Irbid and Aqaba: 2 dates each (winter + summer).
 *  - All 12 cities are present in cities.json with verified coordinates and
 *    their own profile.
 *  - Asia/Amman is UTC+03:00 for every day of the reference year (the table is
 *    in local clock minutes, so a future DST rule change must fail here rather
 *    than silently shifting every time by an hour).
 */
class JordanValidationTest {

    private val toleranceMinutes = 1

    @Test
    fun `amman and spot checks match the official times within one minute`() {
        val rows = loadReference()
        assertTrue("no reference rows", rows.isNotEmpty())

        val byCity = rows.groupBy { it.city }
        assertTrue("Amman rows missing", byCity["Amman"]?.isNotEmpty() == true)
        assertTrue("Irbid rows missing", byCity["Irbid"]?.isNotEmpty() == true)
        assertTrue("Aqaba rows missing", byCity["Aqaba"]?.isNotEmpty() == true)

        val table = StringBuilder()
        table.appendLine("beta23 Jordan gate - computed vs official (ministry preset)")
        table.appendLine("tolerance: +/-$toleranceMinutes minute")
        table.appendLine()
        table.appendLine(
            "%-7s %-11s %-9s %-9s %-9s %-5s %s".format(
                "city", "date", "prayer", "official", "computed", "diff", "result"
            )
        )
        table.appendLine("-".repeat(64))

        val failures = ArrayList<String>()
        var worst = 0
        var compared = 0

        for (row in rows) {
            val day = TableEngine.day(
                profile = row.profile,
                minDiff = 0,
                date = row.date,
                summerTime = false
            )
            val computed = day.minutesFor(row.event)
            val diff = computed - row.official
            worst = maxOf(worst, kotlin.math.abs(diff))
            compared++
            val ok = kotlin.math.abs(diff) <= toleranceMinutes
            if (!ok) {
                failures += "${row.city} ${row.date} ${row.event}: official=${hhmm(row.official)} " +
                    "computed=${hhmm(computed)} diff=$diff"
            }
            table.appendLine(
                "%-7s %-11s %-9s %-9s %-9s %+4d %s".format(
                    row.city, row.date, row.event, hhmm(row.official),
                    hhmm(computed), diff, if (ok) "PASS" else "FAIL"
                )
            )
        }

        table.appendLine()
        table.appendLine("rows compared : $compared")
        table.appendLine("worst |diff|  : $worst minute(s)")
        table.appendLine("result        : ${if (failures.isEmpty()) "PASS" else "FAIL"}")
        val report = table.toString()
        println(report)
        writeReport(report)

        assertEquals(
            "Jordan gate failures (tolerance $toleranceMinutes min):\n" +
                failures.joinToString("\n"),
            0,
            failures.size
        )
        assertTrue("gate compared too few rows: $compared", compared >= 60)
    }

    @Test
    fun `all twelve jordan cities are present with coordinates and their own table`() {
        val array = JSONArray(citiesFile().readText(Charsets.UTF_8))
        val jordan = (0 until array.length())
            .map { array.getJSONObject(it) }
            .filter { it.getString("country") == "JO" }

        assertEquals("Jordanian city count", 12, jordan.size)

        val expected = setOf(
            "عمّان", "الزرقاء", "إربد", "العقبة", "المفرق", "السلط",
            "مأدبا", "الكرك", "الطفيلة", "معان", "جرش", "عجلون"
        )
        assertEquals("Jordanian city names", expected, jordan.map { it.getString("nameAr") }.toSet())

        for (city in jordan) {
            val lat = city.getDouble("lat")
            val lon = city.getDouble("lon")
            assertTrue("latitude out of Jordan for ${city.getString("nameAr")}: $lat",
                lat in 29.0..33.5)
            assertTrue("longitude out of Jordan for ${city.getString("nameAr")}: $lon",
                lon in 34.8..37.0)

            val profile = TableProfile.valueOf(city.getString("profile").uppercase())
            assertTrue(
                "profile ${profile.name} is not a Jordanian table",
                profile.name.startsWith("JORDAN_")
            )
            // A profile must resolve to a real table: the engine would silently
            // fall back to the Palestinian default on a missing entry.
            val day = TableEngine.day(profile, 0, LocalDate.of(2026, 6, 21), summerTime = false)
            assertTrue("no Fajr for ${city.getString("nameAr")}", day.fajr in 0..1439)
            assertTrue("no Isha for ${city.getString("nameAr")}", day.isha in 0..1439)
        }
    }

    @Test
    fun `asia amman stays at utc plus three every day of the reference year`() {
        val zone = ZoneId.of("Asia/Amman")
        val plusThree = ZoneOffset.ofHours(3)
        val date = LocalDate.of(2026, 1, 1)
        val offsets = (0 until 366).map { day ->
            // Noon of each day: the offset that actually applies to the table.
            zone.rules.getOffset(date.plusDays(day.toLong()).atTime(12, 0).atZone(zone).toInstant())
        }.toSet()
        assertEquals(
            "Asia/Amman must not switch offset while the tables are in local time",
            setOf(plusThree),
            offsets
        )
    }

    // ------------------------------------------------------------------ data

    private data class Row(
        val city: String,
        val date: LocalDate,
        val event: TableEvent,
        val official: Int
    ) {
        val profile: TableProfile
            get() = when (city) {
                "Amman" -> TableProfile.JORDAN_AMMAN
                "Irbid" -> TableProfile.JORDAN_IRBID
                "Aqaba" -> TableProfile.JORDAN_AQABA
                else -> error("unmapped reference city: $city")
            }
    }

    private fun loadReference(): List<Row> {
        val text = JordanValidationTest::class.java
            .getResourceAsStream("/jordan_reference.csv")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("jordan_reference.csv not found in test resources")

        return text.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("city,") }
            .map { line ->
                val fields = line.split(",")
                Row(
                    city = fields[0],
                    date = LocalDate.parse(fields[1]),
                    event = TableEvent.valueOf(fields[2]),
                    official = fields[3].toInt()
                )
            }
            .toList()
    }

    private fun citiesFile(): File {
        val candidates = listOf(
            File("src/main/assets/cities.json"),
            File("app/src/main/assets/cities.json"),
            File("../app/src/main/assets/cities.json")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("cities.json not found from ${File(".").absolutePath}")
    }

    private fun writeReport(body: String) {
        val dir = File(System.getProperty("mawaqiti.reportDir") ?: "../report")
        dir.mkdirs()
        File(dir, "jordan_validation_report.txt").writeText(body, Charsets.UTF_8)
    }

    private fun hhmm(minutes: Int): String =
        String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
}
