package com.mawaqiti.plus.ui.screens

import androidx.annotation.StringRes
import com.mawaqiti.plus.R

/**
 * beta32: one adhkar item - its label, its body and, when it is said more than
 * once, the word-based repetition badge. Every field is a string resource;
 * this file holds no Arabic and no digit.
 */
internal data class AdhkarItem(
    @StringRes val titleRes: Int,
    @StringRes val textRes: Int,
    @StringRes val repeatRes: Int? = null
)

/** One hub section: a stable stem, its title and its items in reading order. */
internal data class AdhkarSection(
    val stem: String,
    @StringRes val titleRes: Int,
    val items: List<AdhkarItem>
)

/**
 * beta32: the adhkar hub catalogue.
 *
 * Only the ORDER of the stems and the resource ids live here - every word the
 * user reads is in `strings.xml`. [sections] is the hub's list in the order the
 * owner specified, and the hub renders exactly this order.
 */
internal object AdhkarCatalog {

    val sections: List<AdhkarSection> = listOf(
        AdhkarSection(
            stem = "morning",
            titleRes = R.string.adhkar_section_morning,
            items = listOf(
                AdhkarItem(R.string.adhkar_morning_item01_title, R.string.adhkar_morning_item01_text),
                AdhkarItem(R.string.adhkar_morning_item02_title, R.string.adhkar_morning_item02_text, R.string.adhkar_morning_item02_repeat),
                AdhkarItem(R.string.adhkar_morning_item03_title, R.string.adhkar_morning_item03_text),
                AdhkarItem(R.string.adhkar_morning_item04_title, R.string.adhkar_morning_item04_text),
                AdhkarItem(R.string.adhkar_morning_item05_title, R.string.adhkar_morning_item05_text),
                AdhkarItem(R.string.adhkar_morning_item06_title, R.string.adhkar_morning_item06_text, R.string.adhkar_morning_item06_repeat),
                AdhkarItem(R.string.adhkar_morning_item07_title, R.string.adhkar_morning_item07_text, R.string.adhkar_morning_item07_repeat),
                AdhkarItem(R.string.adhkar_morning_item08_title, R.string.adhkar_morning_item08_text, R.string.adhkar_morning_item08_repeat),
                AdhkarItem(R.string.adhkar_morning_item09_title, R.string.adhkar_morning_item09_text),
                AdhkarItem(R.string.adhkar_morning_item10_title, R.string.adhkar_morning_item10_text),
            )
        ),
        AdhkarSection(
            stem = "evening",
            titleRes = R.string.adhkar_section_evening,
            items = listOf(
                AdhkarItem(R.string.adhkar_evening_item01_title, R.string.adhkar_evening_item01_text),
                AdhkarItem(R.string.adhkar_evening_item02_title, R.string.adhkar_evening_item02_text),
                AdhkarItem(R.string.adhkar_evening_item03_title, R.string.adhkar_evening_item03_text, R.string.adhkar_evening_item03_repeat),
                AdhkarItem(R.string.adhkar_evening_item04_title, R.string.adhkar_evening_item04_text),
                AdhkarItem(R.string.adhkar_evening_item05_title, R.string.adhkar_evening_item05_text, R.string.adhkar_evening_item05_repeat),
                AdhkarItem(R.string.adhkar_evening_item06_title, R.string.adhkar_evening_item06_text, R.string.adhkar_evening_item06_repeat),
                AdhkarItem(R.string.adhkar_evening_item07_title, R.string.adhkar_evening_item07_text, R.string.adhkar_evening_item07_repeat),
                AdhkarItem(R.string.adhkar_evening_item08_title, R.string.adhkar_evening_item08_text, R.string.adhkar_evening_item08_repeat),
                AdhkarItem(R.string.adhkar_evening_item09_title, R.string.adhkar_evening_item09_text, R.string.adhkar_evening_item09_repeat),
                AdhkarItem(R.string.adhkar_evening_item10_title, R.string.adhkar_evening_item10_text, R.string.adhkar_evening_item10_repeat),
                AdhkarItem(R.string.adhkar_evening_item11_title, R.string.adhkar_evening_item11_text, R.string.adhkar_evening_item11_repeat),
            )
        ),
        AdhkarSection(
            stem = "sleep",
            titleRes = R.string.adhkar_section_sleep,
            items = listOf(
                AdhkarItem(R.string.adhkar_sleep_item01_title, R.string.adhkar_sleep_item01_text),
                AdhkarItem(R.string.adhkar_sleep_item02_title, R.string.adhkar_sleep_item02_text),
                AdhkarItem(R.string.adhkar_sleep_item03_title, R.string.adhkar_sleep_item03_text, R.string.adhkar_sleep_item03_repeat),
                AdhkarItem(R.string.adhkar_sleep_item04_title, R.string.adhkar_sleep_item04_text),
                AdhkarItem(R.string.adhkar_sleep_item05_title, R.string.adhkar_sleep_item05_text),
            )
        ),
        AdhkarSection(
            stem = "waking",
            titleRes = R.string.adhkar_section_waking,
            items = listOf(
                AdhkarItem(R.string.adhkar_waking_item01_title, R.string.adhkar_waking_item01_text),
                AdhkarItem(R.string.adhkar_waking_item02_title, R.string.adhkar_waking_item02_text),
            )
        ),
        AdhkarSection(
            stem = "mosque",
            titleRes = R.string.adhkar_section_mosque,
            items = listOf(
                AdhkarItem(R.string.adhkar_mosque_item01_title, R.string.adhkar_mosque_item01_text),
                AdhkarItem(R.string.adhkar_mosque_item02_title, R.string.adhkar_mosque_item02_text),
            )
        ),
        AdhkarSection(
            stem = "wudu",
            titleRes = R.string.adhkar_section_wudu,
            items = listOf(
                AdhkarItem(R.string.adhkar_wudu_item01_title, R.string.adhkar_wudu_item01_text),
                AdhkarItem(R.string.adhkar_wudu_item02_title, R.string.adhkar_wudu_item02_text),
                AdhkarItem(R.string.adhkar_wudu_item03_title, R.string.adhkar_wudu_item03_text),
            )
        ),
        AdhkarSection(
            stem = "khala",
            titleRes = R.string.adhkar_section_khala,
            items = listOf(
                AdhkarItem(R.string.adhkar_khala_item01_title, R.string.adhkar_khala_item01_text),
                AdhkarItem(R.string.adhkar_khala_item02_title, R.string.adhkar_khala_item02_text),
            )
        ),
        AdhkarSection(
            stem = "food",
            titleRes = R.string.adhkar_section_food,
            items = listOf(
                AdhkarItem(R.string.adhkar_food_item01_title, R.string.adhkar_food_item01_text),
                AdhkarItem(R.string.adhkar_food_item02_title, R.string.adhkar_food_item02_text),
                AdhkarItem(R.string.adhkar_food_item03_title, R.string.adhkar_food_item03_text),
                AdhkarItem(R.string.adhkar_food_item04_title, R.string.adhkar_food_item04_text),
                AdhkarItem(R.string.adhkar_food_item05_title, R.string.adhkar_food_item05_text),
            )
        ),
        AdhkarSection(
            stem = "travel",
            titleRes = R.string.adhkar_section_travel,
            items = listOf(
                AdhkarItem(R.string.adhkar_travel_item01_title, R.string.adhkar_travel_item01_text),
                AdhkarItem(R.string.adhkar_travel_item02_title, R.string.adhkar_travel_item02_text),
                AdhkarItem(R.string.adhkar_travel_item03_title, R.string.adhkar_travel_item03_text),
                AdhkarItem(R.string.adhkar_travel_item04_title, R.string.adhkar_travel_item04_text),
            )
        ),
        AdhkarSection(
            stem = "distress",
            titleRes = R.string.adhkar_section_distress,
            items = listOf(
                AdhkarItem(R.string.adhkar_distress_item01_title, R.string.adhkar_distress_item01_text),
                AdhkarItem(R.string.adhkar_distress_item02_title, R.string.adhkar_distress_item02_text),
                AdhkarItem(R.string.adhkar_distress_item03_title, R.string.adhkar_distress_item03_text),
                AdhkarItem(R.string.adhkar_distress_item04_title, R.string.adhkar_distress_item04_text),
                AdhkarItem(R.string.adhkar_distress_item05_title, R.string.adhkar_distress_item05_text),
            )
        ),
        AdhkarSection(
            stem = "weather",
            titleRes = R.string.adhkar_section_weather,
            items = listOf(
                AdhkarItem(R.string.adhkar_weather_item01_title, R.string.adhkar_weather_item01_text),
                AdhkarItem(R.string.adhkar_weather_item02_title, R.string.adhkar_weather_item02_text),
                AdhkarItem(R.string.adhkar_weather_item03_title, R.string.adhkar_weather_item03_text),
                AdhkarItem(R.string.adhkar_weather_item04_title, R.string.adhkar_weather_item04_text),
            )
        ),
        AdhkarSection(
            stem = "after_adhan",
            titleRes = R.string.adhkar_section_after_adhan,
            items = listOf(
                AdhkarItem(R.string.adhkar_after_adhan_item01_title, R.string.adhkar_after_adhan_dua),
            )
        ),
        AdhkarSection(
            stem = "after_prayer",
            titleRes = R.string.adhkar_section_after_prayer,
            items = listOf(
                AdhkarItem(R.string.adhkar_after_prayer_item01_title, R.string.adhkar_istighfar, R.string.adhkar_repeat_3),
                AdhkarItem(R.string.adhkar_after_prayer_item02_title, R.string.adhkar_after_prayer_salam),
                AdhkarItem(R.string.adhkar_after_prayer_item03_title, R.string.adhkar_after_prayer_tahlil),
                AdhkarItem(R.string.adhkar_after_prayer_item04_title, R.string.adhkar_after_prayer_tasbih_dua),
                AdhkarItem(R.string.adhkar_after_prayer_item05_title, R.string.adhkar_tasbih_subhan, R.string.adhkar_repeat_33),
                AdhkarItem(R.string.adhkar_after_prayer_item06_title, R.string.adhkar_tasbih_hamd, R.string.adhkar_repeat_33),
                AdhkarItem(R.string.adhkar_after_prayer_item07_title, R.string.adhkar_tasbih_akbar, R.string.adhkar_repeat_34),
                AdhkarItem(R.string.adhkar_after_prayer_item08_title, R.string.adhkar_tasbih_completion),
            )
        ),
    )

    /** The section owning [stem], or null for a stem that is not in the hub. */
    fun section(stem: String?): AdhkarSection? =
        if (stem == null) null else sections.firstOrNull { it.stem == stem }

    /** Total number of adhkar in the hub (used by the unit test, not the UI). */
    val itemCount: Int = sections.sumOf { it.items.size }
}
