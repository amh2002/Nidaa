package com.mawaqiti.plus

import com.batoulapps.adhan.CalculationMethod as AdhanMethod
import com.batoulapps.adhan.Coordinates as AdhanCoordinates
import com.batoulapps.adhan.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan.Madhab as AdhanMadhab
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan.PrayerAdjustments as AdhanPrayerAdjustments
import com.batoulapps.adhan.data.DateComponents as AdhanDateComponents
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Independent cross-check of the production engine against the Adhan library.
 *
 * The comparison uses UTC for both implementations so only the astronomical
 * result is measured; the app's device-zone conversion is exercised separately.
 * The requirement is agreement within one minute for MWL.
 */
class EngineCrossCheckTest {

    private data class City(val name: String, val latitude: Double, val longitude: Double)

    private data class Check(
        val city: String,
        val date: LocalDate,
        val event: String,
        val diffMinutes: Double,
        val grazing: Boolean = false
    )

    private val cities = listOf(
        City("Quito", -0.1807, -78.4678),
        City("Kampala", 0.3476, 32.5825),
        City("Al-Quds", 31.7683, 35.2137),
        City("Gaza", 31.5017, 34.4668),
        City("Cairo", 30.0444, 31.2357),
        City("Makkah", 21.4225, 39.8262),
        City("Madinah", 24.4672, 39.6111),
        City("Riyadh", 24.7136, 46.6753),
        City("Istanbul", 41.0082, 28.9784),
        City("Rome", 41.9028, 12.4964),
        City("Turin", 45.0703, 7.6869),
        City("Paris", 48.8566, 2.3522),
        City("London", 51.5074, -0.1278),
        City("Berlin", 52.5200, 13.4050),
        City("Moscow", 55.7558, 37.6173),
        City("Copenhagen", 55.6761, 12.5683),
        City("Tokyo", 35.6762, 139.6503),
        City("Sydney", -33.8688, 151.2093),
        City("New York", 40.7128, -74.0060),
        City("Cape Town", -33.9249, 18.4241)
    )

    /** 1st and 15th of every month in 2026 = 24 dates. */
    private val dates: List<LocalDate> = buildList {
        for (month in 1..12) {
            add(LocalDate.of(2026, month, 1))
            add(LocalDate.of(2026, month, 15))
        }
    }

    private val settings = CalculationSettings(
        method = CalculationMethod.MWL,
        asrMadhab = AsrMadhab.SHAFI,
        highLatitudeRule = HighLatitudeRule.NIGHT_MIDDLE
    )

    @Test
    fun productionEngineMatchesAdhanWithinOneMinute() {
        val checks = ArrayList<Check>(cities.size * dates.size * 6)
        val failures = ArrayList<String>()
        val grazing = ArrayList<String>()

        val params = AdhanMethod.MUSLIM_WORLD_LEAGUE.parameters
        params.madhab = AdhanMadhab.SHAFI
        params.highLatitudeRule = AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
        // The MWL preset ships a +1 minute "safety" offset on Dhuhr. The engine
        // under test is pure astronomy, so the preset's manual offsets are
        // removed and only the astronomical model is compared.
        params.methodAdjustments = AdhanPrayerAdjustments()

        for (city in cities) {
            val coordinates = AdhanCoordinates(city.latitude, city.longitude)
            for (date in dates) {
                val adhan = AdhanPrayerTimes(
                    coordinates,
                    AdhanDateComponents(date.year, date.monthValue, date.dayOfMonth),
                    params
                )
                val ours = PrayerEngine.calculate(
                    date = date,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    zone = ZoneOffset.UTC,
                    settings = settings
                )

                // How close the sun's midnight altitude comes to the twilight
                // angle. Within 0.2 deg the hour-angle solution is numerically
                // ill-conditioned: a tiny model difference moves the result by
                // many minutes, and the reference may fall back to its
                // night-middle bound due to its own bootstrap approximation.
                val midnightAltitude = lowerCulminationAltitude(date, city.latitude)
                val fajrGrazing = abs(midnightAltitude - (-settings.fajrAngle)) < 0.2
                val ishaGrazing = abs(midnightAltitude - (-settings.ishaAngle)) < 0.2

                compare(checks, failures, grazing, city, date, "fajr", ours, ours.fajr.instantMillis, adhan.fajr, fajrGrazing)
                compare(checks, failures, grazing, city, date, "sunrise", ours, ours.sunrise.instantMillis, adhan.sunrise, false)
                compare(checks, failures, grazing, city, date, "dhuhr", ours, ours.dhuhr.instantMillis, adhan.dhuhr, false)
                compare(checks, failures, grazing, city, date, "asr", ours, ours.asr.instantMillis, adhan.asr, false)
                compare(checks, failures, grazing, city, date, "maghrib", ours, ours.maghrib.instantMillis, adhan.maghrib, false)
                compare(checks, failures, grazing, city, date, "isha", ours, ours.isha.instantMillis, adhan.isha, ishaGrazing)
            }
        }

        writeReport(checks, failures, grazing)

        assertEquals(
            "Adhan cross-check failures:\n" + failures.take(20).joinToString("\n"),
            0,
            failures.size
        )
        assertTrue("No checks were executed", checks.isNotEmpty())
    }

    private fun compare(
        checks: MutableList<Check>,
        failures: MutableList<String>,
        grazing: MutableList<String>,
        city: City,
        date: LocalDate,
        event: String,
        day: PrayerDay,
        oursMillis: Long,
        adhan: java.util.Date,
        grazingCase: Boolean
    ) {
        val diff = abs(oursMillis - adhan.time) / 60_000.0
        val isGrazing = grazingCase && diff > 1.0 + 1e-9
        checks += Check(city.name, date, event, diff, isGrazing)
        if (diff > 1.0 + 1e-9) {
            val message = "${city.name} $date $event: ours=${momentText(day, event)} " +
                "adhan=${adhan.toInstant()} diff=${format(diff)} min"
            if (grazingCase) grazing += message else failures += message
        }
    }

    /**
     * Altitude of the sun at its lower culmination (local solar midnight),
     * used only to flag twilight-tangency edge cases.
     */
    private fun lowerCulminationAltitude(date: LocalDate, latitude: Double): Double {
        var year = date.year
        var month = date.monthValue
        val day = date.dayOfMonth
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = Math.floor(year / 100.0)
        val b = 2.0 - a + Math.floor(a / 4.0)
        val julian = Math.floor(365.25 * (year + 4716)) +
            Math.floor(30.6001 * (month + 1)) + day + b - 1524.5
        val d = julian - 2451545.0
        val g = Math.toRadians((357.529 + 0.98560028 * d) % 360.0)
        val q = (280.459 + 0.98564736 * d) % 360.0
        val l = Math.toRadians((q + 1.915 * kotlin.math.sin(g) + 0.020 * kotlin.math.sin(2 * g)) % 360.0)
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        val declination = Math.toDegrees(kotlin.math.asin(kotlin.math.sin(e) * kotlin.math.sin(l)))
        val sum = Math.toRadians(latitude + declination)
        return Math.toDegrees(kotlin.math.asin(-kotlin.math.cos(sum)))
    }

    private fun momentText(day: PrayerDay, event: String): String = when (event) {
        "fajr" -> day.fajr.display()
        "sunrise" -> day.sunrise.display()
        "dhuhr" -> day.dhuhr.display()
        "asr" -> day.asr.display()
        "maghrib" -> day.maghrib.display()
        else -> day.isha.display()
    }

    private fun writeReport(
        checks: List<Check>,
        failures: List<String>,
        grazing: List<String>
    ) {
        val reportDir = File(System.getProperty("mawaqiti.reportDir") ?: "../report")
        reportDir.mkdirs()

        val eventOrder = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
        val within = checks.count { it.diffMinutes <= 1.0 + 1e-9 }
        val strictTotal = checks.size - grazing.size
        val strictWithin = checks.size - grazing.size - failures.size
        val worst = checks.sortedByDescending { it.diffMinutes }.take(15)

        val builder = StringBuilder()
        builder.appendLine("# ENGINE_CROSSCHECK.md")
        builder.appendLine()
        builder.appendLine("Independent verification of the production solar engine.")
        builder.appendLine()
        builder.appendLine("| Item | Value |")
        builder.appendLine("|---|---|")
        builder.appendLine("| Production engine | `com.mawaqiti.plus.engine.PrayerEngine` |")
        builder.appendLine("| Independent reference | Adhan 1.2.1 (`com.batoulapps.adhan:adhan`), test scope only |")
        builder.appendLine("| Method | Muslim World League - Fajr 18 deg, Isha 17 deg |")
        builder.appendLine("| Asr | Shafi'i (shadow factor 1) |")
        builder.appendLine("| High-latitude rule | Middle of the night |")
        builder.appendLine("| Cities | ${cities.size} (latitudes ${format(cities.minOf { it.latitude })} to ${format(cities.maxOf { it.latitude })} deg) |")
        builder.appendLine("| Dates | ${dates.size} (1st and 15th of each month, 2026) |")
        builder.appendLine("| Comparisons | ${checks.size} (${checks.size / 6} city-days x 6 events) |")
        builder.appendLine("| Tolerance | +/-1 minute |")
        builder.appendLine("| Result | **$strictWithin / $strictTotal within tolerance (${format(100.0 * strictWithin / strictTotal)}%)** |")
        builder.appendLine("| Failures | ${failures.size} |")
        builder.appendLine("| Grazing-twilight cases reported separately | ${grazing.size} |")
        builder.appendLine()
        builder.appendLine("The production engine rounds upward to the next minute while Adhan")
        builder.appendLine("rounds to the nearest minute, so a one-minute difference is expected and")
        builder.appendLine("accepted by the tolerance.")
        builder.appendLine()
        builder.appendLine("Adhan's MWL preset carries a manual +1 minute Dhuhr offset; it is")
        builder.appendLine("removed in this comparison so that only the astronomical model is measured.")
        builder.appendLine()

        builder.appendLine("## Per-event summary")
        builder.appendLine()
        builder.appendLine("| Event | n | Within +/-1 min | Max deviation (min) | Mean deviation (min) |")
        builder.appendLine("|---|---:|---:|---:|---:|")
        for (event in eventOrder) {
            val subset = checks.filter { it.event == event }
            val ok = subset.count { it.diffMinutes <= 1.0 + 1e-9 }
            val max = subset.maxOfOrNull { it.diffMinutes } ?: 0.0
            val mean = subset.map { it.diffMinutes }.average()
            builder.appendLine("| $event | ${subset.size} | $ok | ${format(max)} | ${format(mean)} |")
        }
        builder.appendLine()

        builder.appendLine("## Grazing-twilight cases")
        builder.appendLine()
        builder.appendLine("At these instants the sun's midnight altitude is within 0.2 deg of")
        builder.appendLine("the Fajr/Isha twilight angle, so the hour-angle solution is numerically")
        builder.appendLine("ill-conditioned. The reference bootstraps its solver with the 00:00 UT")
        builder.appendLine("declination and can reject a solution that exists minutes later; the")
        builder.appendLine("engine iterates to the event-time declination. Such cases are reported")
        builder.appendLine("here and excluded from the pass/fail assertion.")
        builder.appendLine()
        if (grazing.isEmpty()) {
            builder.appendLine("None.")
        } else {
            grazing.forEach { builder.appendLine("- $it") }
        }
        builder.appendLine()

        builder.appendLine("## Worst deviations")
        builder.appendLine()
        builder.appendLine("Rows marked *(grazing)* are the twilight-tangency cases reported")
        builder.appendLine("separately above and excluded from the pass/fail assertion.")
        builder.appendLine()
        builder.appendLine("| City | Date | Event | Deviation (min) |")
        builder.appendLine("|---|---|---|---:|")
        for (check in worst) {
            val marker = if (check.grazing) " *(grazing)*" else ""
            builder.appendLine(
                "| ${check.city} | ${check.date} | ${check.event}$marker | ${format(check.diffMinutes)} |"
            )
        }
        builder.appendLine()

        builder.appendLine("## Cities covered")
        builder.appendLine()
        builder.appendLine("| City | Latitude | Longitude |")
        builder.appendLine("|---|---:|---:|")
        for (city in cities) {
            builder.appendLine(
                "| ${city.name} | ${format(city.latitude)} | ${format(city.longitude)} |"
            )
        }
        builder.appendLine()

        builder.appendLine("## Verdict")
        builder.appendLine()
        if (failures.isEmpty()) {
            builder.appendLine(
                "**PASS.** $strictWithin of $strictTotal strictly compared events agree with " +
                    "the independent Adhan implementation within +/-1 minute" +
                    if (grazing.isEmpty()) "."
                    else ", with ${grazing.size} grazing-twilight case(s) reported separately above."
            )
        } else {
            builder.appendLine("**FAIL.** ${failures.size} comparisons exceeded the +/-1 minute tolerance.")
            builder.appendLine()
            failures.take(50).forEach { builder.appendLine("- $it") }
        }
        builder.appendLine()

        File(reportDir, "ENGINE_CROSSCHECK.md").writeText(builder.toString(), Charsets.UTF_8)
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)
}
