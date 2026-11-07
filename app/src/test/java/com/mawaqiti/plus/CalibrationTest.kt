package com.mawaqiti.plus

import com.mawaqiti.plus.engine.AsrMadhab
import com.mawaqiti.plus.engine.CalculationMethod
import com.mawaqiti.plus.engine.CalculationSettings
import com.mawaqiti.plus.engine.HighLatitudeRule
import com.mawaqiti.plus.engine.PrayerDay
import com.mawaqiti.plus.engine.PrayerEngine
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Calibration against the original app's 124 vectors.
 *
 * These vectors are NOT a pass/fail oracle: the original is a table lookup and
 * the new app is a solar calculator. The test measures the difference and
 * writes report/CALIBRATION.md. It never changes engine parameters to fit.
 */
class CalibrationTest {

    private val events = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

    private data class VectorRow(
        val id: String,
        val city: String,
        val latitude: Double,
        val longitude: Double,
        val date: LocalDate,
        val profile: String,
        val method: String,
        val minDiff: Int,
        val summer: Boolean,
        val expected: Map<String, Int>
    )

    private data class Deviation(
        val row: VectorRow,
        val event: String,
        val minutes: Int
    )

    private data class Stats(
        val n: Int,
        val mean: Double,
        val min: Int,
        val max: Int,
        val meanAbs: Double,
        val sd: Double
    ) {
        val spread: Int get() = max - min
    }

    @Test
    fun calibrateAgainstOriginalVectors() {
        val text = javaClass.getResourceAsStream("/test_vectors.csv")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("test_vectors.csv not found in test resources")

        val rows = parseRows(text)
        assertEquals("vector row count", 124, rows.size)
        assertTrue("all rows must have six expected times", rows.all { it.expected.size == 6 })

        val mwl = CalculationSettings(
            method = CalculationMethod.MWL,
            asrMadhab = AsrMadhab.SHAFI,
            highLatitudeRule = HighLatitudeRule.NIGHT_MIDDLE
        )

        val all = ArrayList<Deviation>()
        val natural = ArrayList<Deviation>()
        val synthetic = ArrayList<Deviation>()
        val ummAlQura = ArrayList<Deviation>()

        for (row in rows) {
            val day = calculate(row, mwl)
            for (event in events) {
                val deviation = Deviation(row, event, diff(day.eventMinutes(event), row.expected.getValue(event)))
                all += deviation
                if (isNaturalProfile(row)) {
                    if (row.city == SYNTHETIC_CITY) synthetic += deviation else natural += deviation
                }
            }

            if (row.method == "UmmAlQura") {
                val uqSettings = CalculationSettings(
                    method = CalculationMethod.UMM_AL_QURA,
                    asrMadhab = AsrMadhab.SHAFI,
                    highLatitudeRule = HighLatitudeRule.NIGHT_MIDDLE
                )
                val uqDay = calculate(row, uqSettings)
                for (event in events) {
                    ummAlQura += Deviation(
                        row,
                        event,
                        diff(uqDay.eventMinutes(event), row.expected.getValue(event))
                    )
                }
            }
        }

        writeReport(all, natural, synthetic, ummAlQura)

        println("CALIBRATION rows=${rows.size} deviations=${all.size} " +
            "natural=${natural.size} synthetic=${synthetic.size} ummAlQura=${ummAlQura.size}")
    }

    private fun calculate(row: VectorRow, settings: CalculationSettings): PrayerDay {
        val offsetHours = Math.round(row.longitude / 15.0).toInt()
        return PrayerEngine.calculate(
            date = row.date,
            latitude = row.latitude,
            longitude = row.longitude,
            zone = ZoneOffset.ofHours(offsetHours),
            settings = settings
        )
    }

    private fun PrayerDay.eventMinutes(event: String): Int = when (event) {
        "fajr" -> fajr.minuteOfDay
        "sunrise" -> sunrise.minuteOfDay
        "dhuhr" -> dhuhr.minuteOfDay
        "asr" -> asr.minuteOfDay
        "maghrib" -> maghrib.minuteOfDay
        else -> isha.minuteOfDay
    }

    private fun isNaturalProfile(row: VectorRow): Boolean = when (row.city) {
        "مكة المكرمة" -> row.profile == "makkah"
        "المدينة المنورة" -> row.profile == "madinah"
        else -> row.profile == "default"
    }

    private companion object {
        const val SYNTHETIC_CITY = "HighLatitude-55N"
    }

    /** Signed shortest difference in minutes, so midnight wrap does not distort statistics. */
    private fun diff(actual: Int, expected: Int): Int {
        var value = actual - expected
        if (value > 720) value -= 1440
        if (value < -720) value += 1440
        return value
    }

    // ------------------------------------------------------------------
    // Report
    // ------------------------------------------------------------------

    private fun writeReport(
        all: List<Deviation>,
        natural: List<Deviation>,
        synthetic: List<Deviation>,
        ummAlQura: List<Deviation>
    ) {
        val reportDir = File(System.getProperty("mawaqiti.reportDir") ?: "../report")
        reportDir.mkdirs()

        val builder = StringBuilder()
        builder.appendLine("# CALIBRATION.md")
        builder.appendLine()
        builder.appendLine("Comparison of the new solar engine with the original Mawaqiti app's")
        builder.appendLine("124 recorded timetables. **This is a measurement, not a pass/fail test,**")
        builder.appendLine("and no engine parameter was tuned to match the vectors.")
        builder.appendLine()
        builder.appendLine("## Method")
        builder.appendLine()
        builder.appendLine("- Engine: `PrayerEngine`, Muslim World League (18°/17°), Shafi'i Asr,")
        builder.appendLine("  middle-of-the-night high-latitude rule, ceil to the next minute.")
        builder.appendLine("- Coordinates and dates come from `test_vectors.csv`; the original app's")
        builder.appendLine("  per-city `minDiff` is **inside the vector values**, not applied again.")
        builder.appendLine("- The vectors are wall-clock values with a manual `summer_time` flag. To")
        builder.appendLine("  isolate the solar model from DST policy, both sides are compared in")
        builder.appendLine("  standard time: the engine runs at the longitude-implied fixed offset")
        builder.appendLine("  (`round(longitude / 15)`) and 60 minutes are removed from vectors that are")
        builder.appendLine("  both flagged `summer_time=yes` **and** use the `default` profile. The")
        builder.appendLine("  original app never shifts its Makkah/Madinah tables.")
        builder.appendLine("- Deviation = engine − vector, in minutes (positive = engine is later).")
        builder.appendLine("- The `HighLatitude-55N` rows are a synthetic stress case: the original app")
        builder.appendLine("  still serves the Al-Quds table there, so those rows are reported but")
        builder.appendLine("  excluded from the tuning-viability verdict.")
        builder.appendLine()

        builder.appendLine("## Overall — MWL vs all 124 vectors")
        builder.appendLine()
        builder.appendLine(overallTable(all))
        builder.appendLine()

        builder.appendLine("### Per city (all rows)")
        builder.appendLine()
        for ((city, deviations) in all.groupBy { it.row.city }) {
            builder.appendLine("#### $city")
            builder.appendLine()
            builder.appendLine(perEventTable(deviations))
            builder.appendLine()
        }

        builder.appendLine("## Natural-profile subset (the configuration the real app would use)")
        builder.appendLine()
        builder.appendLine("Only rows whose original profile matches the city are included:")
        builder.appendLine("default for ordinary cities, `makkah` for مكة المكرمة, `madinah` for")
        builder.appendLine("المدينة المنورة. This removes the synthetic forced-profile combinations.")
        builder.appendLine("The synthetic `HighLatitude-55N` rows are reported separately below.")
        builder.appendLine()
        builder.appendLine(overallTable(natural))
        builder.appendLine()
        builder.appendLine("### Per city (natural profiles)")
        builder.appendLine()
        for ((city, deviations) in natural.groupBy { it.row.city }) {
            builder.appendLine("#### $city")
            builder.appendLine()
            builder.appendLine(perEventTable(deviations))
            builder.appendLine()
        }

        builder.appendLine("## Synthetic 55°N stress case (excluded from the verdict)")
        builder.appendLine()
        builder.appendLine("These rows place Al-Quds-style tables at latitude 55°N. The original app")
        builder.appendLine("has no latitude input at all, so its values stay at the Al-Quds timetable")
        builder.appendLine("while the solar engine correctly shortens the summer nights. The large")
        builder.appendLine("deviations are the expected evidence that a real solar engine is needed.")
        builder.appendLine()
        builder.appendLine(overallTable(synthetic))
        builder.appendLine()

        builder.appendLine("## Umm al-Qura method vs the 41 makkah-profile vectors")
        builder.appendLine()
        builder.appendLine("The engine runs Fajr 18.5° with Isha = Maghrib + 90 minutes.")
        builder.appendLine()
        builder.appendLine(overallTable(ummAlQura))
        builder.appendLine()
        builder.appendLine("### Per city (Umm al-Qura)")
        builder.appendLine()
        for ((city, deviations) in ummAlQura.groupBy { it.row.city }) {
            builder.appendLine("#### $city")
            builder.appendLine()
            builder.appendLine(perEventTable(deviations))
            builder.appendLine()
        }

        builder.appendLine(verdict(natural))
        builder.appendLine()
        builder.appendLine("## DST note")
        builder.appendLine()
        builder.appendLine("The original app has no timezone database: it stores fixed wall-clock")
        builder.appendLine("tables and adds a manual +60 minutes only for the default profile. The new")
        builder.appendLine("app converts real instants with `java.time` and the device zone, so late-March")
        builder.appendLine("and late-October transitions are automatic. That intentional difference is")
        builder.appendLine("removed from the numbers above by comparing in standard time.")
        builder.appendLine()

        File(reportDir, "CALIBRATION.md").writeText(builder.toString(), Charsets.UTF_8)

        println(
            "CALIBRATION natural overall: " +
                natural.groupBy { it.event }.entries.joinToString("; ") { (event, list) ->
                    val stats = stats(list.map { it.minutes })
                    "$event mean=${format(stats.mean)} min=${stats.min} max=${stats.max}"
                }
        )
    }

    private fun overallTable(deviations: List<Deviation>): String {
        val out = StringBuilder()
        out.appendLine("| Event | n | Mean (min) | Min (min) | Max (min) | Mean abs (min) | SD |")
        out.appendLine("|---|---:|---:|---:|---:|---:|---:|")
        for (event in events) {
            val stats = stats(deviations.filter { it.event == event }.map { it.minutes })
            out.appendLine(
                "| $event | ${stats.n} | ${signed(stats.mean)} | ${stats.min} | ${stats.max} | " +
                    "${format(stats.meanAbs)} | ${format(stats.sd)} |"
            )
        }
        val total = stats(deviations.map { it.minutes })
        out.appendLine(
            "| **all** | ${total.n} | ${signed(total.mean)} | ${total.min} | ${total.max} | " +
                "${format(total.meanAbs)} | ${format(total.sd)} |"
        )
        return out.toString()
    }

    private fun perEventTable(deviations: List<Deviation>): String {
        val out = StringBuilder()
        out.appendLine("| Event | n | Mean (min) | Min (min) | Max (min) | Spread |")
        out.appendLine("|---|---:|---:|---:|---:|---:|")
        for (event in events) {
            val stats = stats(deviations.filter { it.event == event }.map { it.minutes })
            if (stats.n == 0) continue
            out.appendLine(
                "| $event | ${stats.n} | ${signed(stats.mean)} | ${stats.min} | ${stats.max} | " +
                    "${stats.spread} |"
            )
        }
        return out.toString()
    }

    private fun verdict(natural: List<Deviation>): String {
        val tuning = natural
            .filter { it.row.city != "HighLatitude-55N" }
            .groupBy { it.row.city to it.event }
            .filterValues { it.size >= 3 }
            .mapValues { stats(it.value.map { deviation -> deviation.minutes }) }

        val total = tuning.size
        val stable = tuning.values.count { it.spread <= 3 }
        val fairlyStable = tuning.values.count { it.spread <= 5 }
        val ratio = if (total == 0) 0.0 else stable.toDouble() / total

        val cityMeans = natural
            .filter { it.row.city != "HighLatitude-55N" }
            .groupBy { it.row.city }
            .mapValues { stats(it.value.map { deviation -> deviation.minutes }).mean }
            .toList()
            .sortedBy { it.second }

        val out = StringBuilder()
        out.appendLine("## Verdict: are tuning offsets justified?")
        out.appendLine()
        out.appendLine("A fixed per-city/per-event offset is only useful if the deviation is")
        out.appendLine("consistent across dates. Using the natural-profile subset, excluding the")
        out.appendLine("synthetic 55°N rows:")
        out.appendLine()
        out.appendLine("| Metric | Value |")
        out.appendLine("|---|---:|")
        out.appendLine("| City/event groups with n ≥ 3 | $total |")
        out.appendLine("| Groups with spread ≤ 3 min | $stable (${format(100.0 * ratio)}%) |")
        out.appendLine("| Groups with spread ≤ 5 min | $fairlyStable |")
        out.appendLine()
        out.appendLine("Mean deviation by city (all events, natural profiles):")
        out.appendLine()
        out.appendLine("| City | Mean deviation (min) |")
        out.appendLine("|---|---:|")
        for ((city, mean) in cityMeans) {
            out.appendLine("| $city | ${signed(mean)} |")
        }
        out.appendLine()

        out.appendLine("**Conclusion.** ")
        when {
            ratio >= 0.75 -> out.appendLine(
                "Most city/event groups (" + format(100.0 * ratio) + "%) vary by 3 minutes or " +
                    "less across the year, so a small constant offset per city and event would " +
                    "absorb the systematic difference for the majority of cases. The remaining " +
                    "groups would need seasonal (monthly) offsets. This beta does **not** " +
                    "implement such offsets; the engine stays purely astronomical."
            )

            ratio >= 0.50 -> out.appendLine(
                "The evidence is mixed: " + format(100.0 * ratio) + "% of the city/event groups " +
                    "are stable within 3 minutes, but the rest vary too much across the year for " +
                    "a single constant offset. A tuning feature would need seasonal (monthly) " +
                    "offsets, not one value per city."
            )

            else -> out.appendLine(
                "The deviations are not consistent enough to justify a simple per-city/per-event " +
                    "offset table. The original tables and the solar model diverge differently " +
                    "across the year, so tuning would need a much richer model. No offsets are " +
                    "implemented."
            )
        }
        val most = cityMeans.lastOrNull()
        val least = cityMeans.firstOrNull()
        if (most != null && least != null) {
            out.appendLine()
            out.appendLine(
                "Largest mean deviation: **${most.first}** (${signed(most.second)} min); " +
                    "smallest: **${least.first}** (${signed(least.second)} min)."
            )
        }
        out.appendLine()
        out.appendLine(
            "Note: the synthetic `HighLatitude-55N` rows are excluded here because the " +
                "original app has no latitude input and serves the Al-Quds table at 55°N; " +
                "their large deviations are expected and are the clearest demonstration of " +
                "why a real solar engine is needed."
        )
        return out.toString()
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun stats(values: List<Int>): Stats {
        if (values.isEmpty()) return Stats(0, 0.0, 0, 0, 0.0, 0.0)
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return Stats(
            n = values.size,
            mean = mean,
            min = values.min(),
            max = values.max(),
            meanAbs = values.map { abs(it) }.average(),
            sd = sqrt(variance)
        )
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun signed(value: Double): String = String.format(Locale.US, "%+.2f", value)

    private fun parseRows(text: String): List<VectorRow> {
        val table = parseCsv(text)
        val header = table.first().map { it.removePrefix("\uFEFF") }
        val index = header.withIndex().associate { it.value to it.index }
        return table.drop(1).filter { it.size >= header.size }.map { fields ->
            fun field(name: String) = fields[index.getValue(name)]
            VectorRow(
                id = field("case_id"),
                city = field("city"),
                latitude = field("latitude").toDouble(),
                longitude = field("longitude").toDouble(),
                date = LocalDate.parse(field("date")),
                profile = field("profile"),
                method = field("method"),
                minDiff = field("min_diff").toInt(),
                summer = field("summer_time") == "yes",
                expected = events.associateWith { event ->
                    val minutes = parseTime(field(event))
                    // The original app applies its manual +60 min toggle only to
                    // the "default" profile; Makkah/Madinah tables are never
                    // shifted even when the date falls in a DST period.
                    val shift = if (field("summer_time") == "yes" && field("profile") == "default") 60 else 0
                    (minutes - shift + 1440) % 1440
                }
            )
        }
    }

    private fun parseTime(value: String): Int {
        val parts = value.trim().split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /** Minimal RFC-4180 parser: commas, quoted fields and doubled quotes. */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var fields = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }

                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(field.toString())
                    field.clear()
                }

                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    fields.add(field.toString())
                    field.clear()
                    if (fields.any { it.isNotEmpty() }) rows.add(fields)
                    fields = ArrayList()
                }

                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(field.toString())
            rows.add(fields)
        }
        return rows
    }
}
