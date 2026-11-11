package com.mawaqiti.plus

import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEngine
import java.io.File
import java.time.LocalDate
import java.util.Locale
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase C fidelity gate for the table engine.
 *
 * 1. Every row of `test_vectors.csv` (124 rows, the original app's own output)
 *    must match to the minute.
 * 2. Every case of `table_expected.json` (90 cities x 8 dates x summer on/off
 *    = 1440 cases) must match the Python reference exactly.
 */
class TableFidelityTest {

    @Test
    fun all124VectorsMatchExactly() {
        val evaluation = TableTestData.evaluate(TableTestData.loadVectors())

        assertEquals("vector row count", 124, evaluation.total)
        assertEquals(
            "vector rows that match on all six prayers:\n" +
                evaluation.mismatches.take(20).joinToString("\n"),
            124,
            evaluation.passed
        )
        assertTrue("no vector comparisons were executed", evaluation.comparisons >= 124 * 6)
    }

    @Test
    fun tableEngineMatchesAllReferenceData() {
        val vectors = TableTestData.evaluate(TableTestData.loadVectors())
        val sweep = evaluateSweep()

        assertEquals("vector rows", 124, vectors.total)
        assertEquals(0, vectors.failed)
        assertEquals("sweep cases", 1440, sweep.cases)
        assertEquals(
            "sweep mismatches:\n" + sweep.mismatches.take(20).joinToString("\n"),
            0,
            sweep.mismatches.size
        )

        writeReport(vectors, sweep)
    }

    private data class SweepResult(
        val cases: Int,
        val comparisons: Int,
        val summers: Int,
        val dates: List<String>,
        val mismatches: List<String>
    )

    private fun evaluateSweep(): SweepResult {
        val array: JSONArray = TableTestData.loadSweep()
        val mismatches = ArrayList<String>()
        val dates = sortedSetOf<String>()
        var comparisons = 0
        var summers = 0

        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val profile = TableTestData.profile(item.getString("profile"))
            val minDiff = item.getInt("minDiff")
            val date = LocalDate.parse(item.getString("date"))
            val summer = item.getBoolean("summer")
            val city = item.getString("nameAr")
            dates += item.getString("date")
            if (summer) summers++

            val day = TableEngine.day(profile, minDiff, date, summer)
            for (event in TableTestData.events) {
                comparisons++
                val actual = day.minutesFor(TableTestData.eventName(event))
                val expected = item.getInt(event)
                if (actual != expected) {
                    mismatches += "$city $date summer=$summer $event: expected=$expected actual=$actual"
                }
            }
            comparisons += 2
            check(mismatches, city, date, summer, "fajrTomorrow", day.fajrTomorrow, item.getInt("fajrTomorrow"))
            check(mismatches, city, date, summer, "lastThird", day.lastThird, item.getInt("lastThird"))
        }
        return SweepResult(
            cases = array.length(),
            comparisons = comparisons,
            summers = summers,
            dates = dates.toList(),
            mismatches = mismatches
        )
    }

    private fun check(
        mismatches: MutableList<String>,
        city: String,
        date: LocalDate,
        summer: Boolean,
        field: String,
        actual: Int,
        expected: Int
    ) {
        if (actual != expected) {
            mismatches += "$city $date summer=$summer $field: expected=$expected actual=$actual"
        }
    }

    private fun writeReport(vectors: TableTestData.Evaluation, sweep: SweepResult) {
        val reportDir = File(System.getProperty("mawaqiti.reportDir") ?: "../report")
        reportDir.mkdirs()

        val builder = StringBuilder()
        builder.appendLine("# TABLE_FIDELITY.md")
        builder.appendLine()
        builder.appendLine("Bit-exactness of `TableEngine`, the primary calculation path of")
        builder.appendLine("Nidaa BETA.")
        builder.appendLine()
        builder.appendLine("## Engine")
        builder.appendLine()
        builder.appendLine("| Item | Value |")
        builder.appendLine("|---|---|")
        builder.appendLine("| Kotlin engine | `com.mawaqiti.plus.engine.table.TableEngine` |")
        builder.appendLine("| Tables | `PrayerTables` - 3 profiles x 6 events x 2 arrays (hours/minutes) x 366 rows = 36 arrays |")
        builder.appendLine("| Reference semantics | `p061i1.j.o()` / `p061i1.j.w()` |")
        builder.appendLine("| Row rule | day-of-year; common year and doy > 59 -> +1; row 59 is 29 February |")
        builder.appendLine("| Runtime adjustment | +60 min only when summer time is on **and** profile is `default` |")
        builder.appendLine("| Per-city offset | `minDiff` in minutes, carry/borrow, `hour % 24` |")
        builder.appendLine("| Formatting | `%02d:%02d` with `Locale.US` (Latin digits in both languages) |")
        builder.appendLine("| Source data | `w7.c` arrays + `y7.C3444b` city list, verified against `reference_impl/prayer_times.py` |")
        builder.appendLine()
        builder.appendLine("## 1. Original app vectors")
        builder.appendLine()
        builder.appendLine("`app/src/test/resources/test_vectors.csv` holds 124 recorded day/city/profile")
        builder.appendLine("combinations generated from the extracted tables and `p061i1.j` arithmetic.")
        builder.appendLine()
        builder.appendLine("| Metric | Result |")
        builder.appendLine("|---|---:|")
        builder.appendLine("| Rows | ${vectors.total} |")
        builder.appendLine("| Rows matching on all six prayers | **${vectors.passed} / ${vectors.total}** |")
        builder.appendLine("| Prayer comparisons (6 per row) | ${vectors.total * 6} |")
        builder.appendLine("| Additional derived checks (last third + Fajr tomorrow) | ${vectors.total * 2} |")
        builder.appendLine("| Mismatches | **${vectors.mismatches.size}** |")
        builder.appendLine()
        builder.appendLine("## 2. Full city sweep")
        builder.appendLine()
        builder.appendLine("`app/src/test/resources/table_expected.json` was exported from the Python")
        builder.appendLine("reference implementation, independently of the Kotlin port.")
        builder.appendLine()
        builder.appendLine("| Metric | Result |")
        builder.appendLine("|---|---:|")
        builder.appendLine("| Cities | 90 |")
        builder.appendLine("| Dates | ${sweep.dates.size} (${sweep.dates.joinToString(", ")}) |")
        builder.appendLine("| Summer states per city/date | 2 (OFF / ON) |")
        builder.appendLine("| Cases | **${sweep.cases}** |")
        builder.appendLine("| Time comparisons (6 prayers + Fajr tomorrow + last third) | **${sweep.comparisons}** |")
        builder.appendLine("| Cases with summer ON | ${sweep.summers} |")
        builder.appendLine("| Mismatches | **${sweep.mismatches.size}** |")
        builder.appendLine()
        builder.appendLine("The date set includes 29 February 2028 (leap year), 2027 as a common")
        builder.appendLine("year, 28 February / 1 March boundaries, both solstices and 31 December.")
        builder.appendLine()
        builder.appendLine("## 3. Solar cross-check")
        builder.appendLine()
        builder.appendLine("The internal solar engine (test-only, never reachable from the UI) is")
        builder.appendLine("cross-checked against the independent Adhan library; see")
        builder.appendLine("`ENGINE_CROSSCHECK.md`. It is not used to produce any displayed time.")
        builder.appendLine()
        builder.appendLine("## Verdict")
        builder.appendLine()
        if (vectors.mismatches.isEmpty() && sweep.mismatches.isEmpty()) {
            builder.appendLine(
                "**PASS.** ${vectors.passed}/${vectors.total} original vectors and " +
                    "${sweep.cases}/${sweep.cases} sweep cases are bit-exact, with zero mismatches " +
                    "across ${vectors.comparisons + sweep.comparisons} comparisons."
            )
        } else {
            builder.appendLine("**FAIL.** See the mismatch list below.")
            builder.appendLine()
            (vectors.mismatches + sweep.mismatches).take(50).forEach { builder.appendLine("- $it") }
        }
        builder.appendLine()
        builder.appendLine("## Reproduce")
        builder.appendLine()
        builder.appendLine("```")
        builder.appendLine(".\\gradlew.bat test --tests \"com.mawaqiti.plus.TableFidelityTest\"")
        builder.appendLine("```")
        builder.appendLine()

        File(reportDir, "TABLE_FIDELITY.md").writeText(builder.toString(), Charsets.UTF_8)
        println(
            "TABLE_FIDELITY vectors=${vectors.passed}/${vectors.total} " +
                "sweep=${sweep.cases - sweep.mismatches.size}/${sweep.cases} " +
                "comparisons=${vectors.comparisons + sweep.comparisons} " +
                "mismatches=${vectors.mismatches.size + sweep.mismatches.size}"
        )
    }

    private fun format(value: Int): String = String.format(Locale.US, "%d", value)
}
