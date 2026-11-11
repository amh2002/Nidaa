package com.mawaqiti.plus

import com.mawaqiti.plus.engine.AsrMadhab
import com.mawaqiti.plus.engine.CalculationMethod
import com.mawaqiti.plus.engine.CalculationSettings
import com.mawaqiti.plus.engine.HighLatitudeRule
import com.mawaqiti.plus.engine.PrayerDay
import com.mawaqiti.plus.engine.PrayerEngine
import com.mawaqiti.plus.engine.table.TableEngine
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Informative comparison of the internal solar engine (MWL) with the official
 * table timings that the app actually displays.
 *
 * This test never fails on deviation: the tables are the product truth and the
 * solar engine exists only for verification and this calibration report.
 */
class CalibrationTest {

    private data class City(
        val nameAr: String,
        val nameEn: String,
        val country: String,
        val minDiff: Int,
        val profile: String,
        val latitude: Double,
        val longitude: Double
    )

    private data class Deviation(
        val city: City,
        val date: LocalDate,
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
    )

    private val events = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

    private val dates = listOf(
        "2027-01-01", "2027-02-28", "2027-03-01", "2027-06-21", "2027-12-31",
        "2028-02-29", "2028-06-21", "2028-12-31"
    ).map(LocalDate::parse)

    private val solarSettings = CalculationSettings(
        method = CalculationMethod.MWL,
        asrMadhab = AsrMadhab.SHAFI,
        highLatitudeRule = HighLatitudeRule.NIGHT_MIDDLE
    )

    @Test
    fun calibrateSolarEngineAgainstOfficialTables() {
        val cities = loadCities()
        assertEquals("all extracted cities must be calibrated", 90, cities.size)

        val deviations = ArrayList<Deviation>(cities.size * dates.size * events.size)
        for (city in cities) {
            val zone = ZoneOffset.ofHours(if (city.country == "SA") 3 else 2)
            for (date in dates) {
                val table = TableEngine.day(
                    profile = TableTestData.profile(city.profile),
                    minDiff = city.minDiff,
                    date = date,
                    summerTime = false
                )
                val solar = PrayerEngine.calculate(
                    date = date,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    zone = zone,
                    settings = solarSettings
                )
                for (event in events) {
                    val expected = table.minutesFor(TableTestData.eventName(event))
                    val actual = solar.eventMinutes(event, solar)
                    deviations += Deviation(city, date, event, shortest(actual - expected))
                }
            }
        }

        writeReport(cities, deviations)

        val overall = stats(deviations.map { it.minutes })
        println(
            "CALIBRATION cities=${cities.size} dates=${dates.size} " +
                "comparisons=${deviations.size} meanAbs=${format(overall.meanAbs)} " +
                "maxAbs=${maxOf(deviations)}"
        )
        assertTrue("no calibration comparisons were executed", deviations.isNotEmpty())
    }

    private fun PrayerDay.eventMinutes(event: String, day: PrayerDay): Int = when (event) {
        "fajr" -> day.fajr.minuteOfDay
        "sunrise" -> day.sunrise.minuteOfDay
        "dhuhr" -> day.dhuhr.minuteOfDay
        "asr" -> day.asr.minuteOfDay
        "maghrib" -> day.maghrib.minuteOfDay
        else -> day.isha.minuteOfDay
    }

    private fun shortest(value: Int): Int {
        var minutes = value
        if (minutes > 720) minutes -= 1440
        if (minutes < -720) minutes += 1440
        return minutes
    }

    private fun maxOf(deviations: List<Deviation>): Int =
        deviations.maxOfOrNull { abs(it.minutes) } ?: 0

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
                nameAr = item.getString("nameAr").trim(),
                nameEn = item.getString("nameEn"),
                country = item.getString("country"),
                minDiff = item.getInt("minDiff"),
                profile = item.getString("profile"),
                latitude = item.getDouble("lat"),
                longitude = item.getDouble("lon")
            )
        }
    }

    private fun writeReport(cities: List<City>, deviations: List<Deviation>) {
        val reportDir = File(System.getProperty("mawaqiti.reportDir") ?: "../report")
        reportDir.mkdirs()

        val all = stats(deviations.map { it.minutes })
        val worst = deviations.sortedByDescending { abs(it.minutes) }.take(15)
        val byCity = deviations.groupBy { it.city.nameEn }
            .mapValues { stats(it.value.map { deviation -> deviation.minutes }) }
            .toList()
            .sortedByDescending { it.second.meanAbs }
        val byCountry = deviations.groupBy { it.city.country }
            .mapValues { stats(it.value.map { deviation -> deviation.minutes }) }

        val builder = StringBuilder()
        builder.appendLine("# CALIBRATION.md")
        builder.appendLine()
        builder.appendLine("Solar engine (MWL) versus the official timetable that Nidaa")
        builder.appendLine("displays. **Informative only:** the table path is the product truth.")
        builder.appendLine()
        builder.appendLine("## Method")
        builder.appendLine()
        builder.appendLine("| Item | Value |")
        builder.appendLine("|---|---|")
        builder.appendLine("| Table engine | `TableEngine` (same tables as the original app) |")
        builder.appendLine("| Solar engine | `PrayerEngine`, internal/test-only |")
        builder.appendLine("| Solar parameters | Muslim World League, Fajr 18 deg, Isha 17 deg |")
        builder.appendLine("| Asr | Shafi'i (shadow factor 1) |")
        builder.appendLine("| High latitudes | Middle of the night |")
        builder.appendLine("| Summer time | OFF on both sides (standard time) |")
        builder.appendLine("| Zones | Palestine UTC+2, Saudi Arabia UTC+3 |")
        builder.appendLine("| Cities | ${cities.size} (${cities.count { it.country == "PS" }} PS, ${cities.count { it.country == "SA" }} SA) |")
        builder.appendLine("| Dates | ${dates.size} (${dates.joinToString(", ")}) |")
        builder.appendLine("| Comparisons | ${deviations.size} |")
        builder.appendLine()
        builder.appendLine("Deviation = solar - official table, in minutes; positive means the")
        builder.appendLine("astronomical result is later. Midnight wrap is handled with the shortest")
        builder.appendLine("signed difference. The official table includes each city's `minDiff` plus")
        builder.appendLine("the Palestinian Authority's own seasonal adjustments, which a pure MWL")
        builder.appendLine("calculation cannot reproduce exactly.")
        builder.appendLine()
        builder.appendLine("## Overall")
        builder.appendLine()
        builder.appendLine("| n | Mean (min) | Min (min) | Max (min) | Mean abs (min) | SD | Max abs (min) |")
        builder.appendLine("|---:|---:|---:|---:|---:|---:|---:|")
        builder.appendLine(
            "| ${all.n} | ${signed(all.mean)} | ${all.min} | ${all.max} | " +
                "${format(all.meanAbs)} | ${format(all.sd)} | ${maxOf(deviations)} |"
        )
        builder.appendLine()
        builder.appendLine("## Per event")
        builder.appendLine()
        builder.appendLine("| Event | n | Mean (min) | Min | Max | Mean abs | SD |")
        builder.appendLine("|---|---:|---:|---:|---:|---:|---:|")
        for (event in events) {
            val subset = stats(deviations.filter { it.event == event }.map { it.minutes })
            builder.appendLine(
                "| $event | ${subset.n} | ${signed(subset.mean)} | ${subset.min} | " +
                    "${subset.max} | ${format(subset.meanAbs)} | ${format(subset.sd)} |"
            )
        }
        builder.appendLine()
        builder.appendLine("## Per country")
        builder.appendLine()
        builder.appendLine("| Country | n | Mean (min) | Mean abs (min) | Max abs (min) |")
        builder.appendLine("|---|---:|---:|---:|---:|")
        for ((country, stats) in byCountry.toSortedMap()) {
            val maxAbs = deviations.filter { it.city.country == country }.maxOf { abs(it.minutes) }
            builder.appendLine(
                "| $country | ${stats.n} | ${signed(stats.mean)} | ${format(stats.meanAbs)} | $maxAbs |"
            )
        }
        builder.appendLine()
        builder.appendLine("## Cities with the largest mean absolute deviation")
        builder.appendLine()
        builder.appendLine("| City | Country | minDiff | Mean abs (min) | Mean (min) |")
        builder.appendLine("|---|---|---:|---:|---:|")
        for ((name, stats) in byCity.take(15)) {
            val city = cities.first { it.nameEn == name }
            builder.appendLine(
                "| $name | ${city.country} | ${city.minDiff} | ${format(stats.meanAbs)} | " +
                    "${signed(stats.mean)} |"
            )
        }
        builder.appendLine()
        builder.appendLine("## Largest individual deviations")
        builder.appendLine()
        builder.appendLine("| City | Date | Event | Deviation (min) |")
        builder.appendLine("|---|---|---|---:|")
        for (deviation in worst) {
            builder.appendLine(
                "| ${deviation.city.nameEn} | ${deviation.date} | ${deviation.event} | " +
                    "${signed(deviation.minutes.toDouble())} |"
            )
        }
        builder.appendLine()
        builder.appendLine("## Why table mode is the default")
        builder.appendLine()
        builder.appendLine(
            "Across ${deviations.size} comparisons the solar MWL model differs from the official " +
                "timetable by ${format(all.meanAbs)} minutes on average and up to " +
                "${maxOf(deviations)} minutes. That gap is systematic (local sighting-based " +
                "adjustments, city `minDiff`, and the Palestinian seasonal policy), not random. " +
                "A table lookup is therefore the only way to guarantee minute-for-minute " +
                "agreement with the original app, so it is the primary and only user-facing " +
                "calculation path."
        )
        builder.appendLine()
        builder.appendLine("The solar engine stays in the codebase purely for the independent")
        builder.appendLine("cross-check in `ENGINE_CROSSCHECK.md`; it is not reachable from the UI.")
        builder.appendLine()

        File(reportDir, "CALIBRATION.md").writeText(builder.toString(), Charsets.UTF_8)
    }

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
}
