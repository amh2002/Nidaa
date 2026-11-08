package com.mawaqiti.plus

import com.mawaqiti.plus.engine.table.TableEngine
import com.mawaqiti.plus.engine.table.TableProfile
import java.time.LocalDate
import java.util.Locale
import org.json.JSONArray

/**
 * Shared loaders for the reference data extracted from the original APK.
 *
 * Nothing here is produced by the engine under test: both files come from the
 * verified Python reference (`reference_impl/prayer_times.py`) and were written
 * by `tools/export_table_data.py`.
 */
internal object TableTestData {

    val events = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

    data class VectorRow(
        val id: String,
        val city: String,
        val date: LocalDate,
        val profile: TableProfile,
        val minDiff: Int,
        val summer: Boolean,
        val expected: Map<String, Int>,
        val lastThird: Int,
        val fajrTomorrow: Int
    )

    data class Evaluation(
        val total: Int,
        val passed: Int,
        val comparisons: Int,
        val mismatches: List<String>
    ) {
        val failed: Int get() = total - passed
    }

    fun loadVectors(): List<VectorRow> {
        val text = TableTestData::class.java
            .getResourceAsStream("/test_vectors.csv")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("test_vectors.csv not found in test resources")

        val table = parseCsv(text)
        val header = table.first().map { it.removePrefix("\uFEFF") }
        val index = header.withIndex().associate { it.value to it.index }

        return table.drop(1)
            .filter { it.size >= header.size && it[index.getValue("case_id")].isNotBlank() }
            .map { fields ->
                fun field(name: String) = fields[index.getValue(name)]
                VectorRow(
                    id = field("case_id"),
                    city = field("city"),
                    date = LocalDate.parse(field("date")),
                    profile = profile(field("profile")),
                    minDiff = field("min_diff").toInt(),
                    summer = field("summer_time") == "yes",
                    expected = events.associateWith { parseTime(field(it)) },
                    lastThird = parseTime(field("last_third_of_night")),
                    fajrTomorrow = parseTime(field("fajr_tomorrow"))
                )
            }
    }

    fun evaluate(rows: List<VectorRow>): Evaluation {
        val mismatches = ArrayList<String>()
        var passed = 0
        var comparisons = 0

        for (row in rows) {
            val day = TableEngine.day(row.profile, row.minDiff, row.date, row.summer)
            var rowOk = true
            for (event in events) {
                comparisons++
                val actual = TableEngine.format(day.minutesFor(eventName(event)))
                val expected = format(row.expected.getValue(event))
                if (actual != expected) {
                    rowOk = false
                    mismatches += "${row.id} ${row.city.trim()} ${row.date} ${row.profile} " +
                        "summer=${row.summer} $event: expected=$expected actual=$actual"
                }
            }
            comparisons += 2
            val lastThird = TableEngine.format(day.lastThird)
            if (lastThird != format(row.lastThird)) {
                rowOk = false
                mismatches += "${row.id} lastThird: expected=${format(row.lastThird)} actual=$lastThird"
            }
            val fajrTomorrow = TableEngine.format(day.fajrTomorrow)
            if (fajrTomorrow != format(row.fajrTomorrow)) {
                rowOk = false
                mismatches += "${row.id} fajrTomorrow: expected=${format(row.fajrTomorrow)} " +
                    "actual=$fajrTomorrow"
            }
            if (rowOk) passed++
        }
        return Evaluation(rows.size, passed, comparisons, mismatches)
    }

    fun loadSweep(): JSONArray {
        val text = TableTestData::class.java
            .getResourceAsStream("/table_expected.json")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("table_expected.json not found in test resources")
        return JSONArray(text)
    }

    fun profile(value: String): TableProfile = when (value.lowercase(Locale.US)) {
        "makkah" -> TableProfile.MAKKAH
        "madinah" -> TableProfile.MADINAH
        else -> TableProfile.DEFAULT
    }

    fun eventName(name: String) = when (name) {
        "fajr" -> com.mawaqiti.plus.engine.table.TableEvent.FAJR
        "sunrise" -> com.mawaqiti.plus.engine.table.TableEvent.SUNRISE
        "dhuhr" -> com.mawaqiti.plus.engine.table.TableEvent.DHUHR
        "asr" -> com.mawaqiti.plus.engine.table.TableEvent.ASR
        "maghrib" -> com.mawaqiti.plus.engine.table.TableEvent.MAGHRIB
        else -> com.mawaqiti.plus.engine.table.TableEvent.ISHA
    }

    private fun parseTime(value: String): Int {
        val parts = value.trim().split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun format(minutes: Int): String = TableEngine.format(minutes)

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
