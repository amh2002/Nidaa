package com.mawaqiti.plus.data

/**
 * In-app text-size multiplier (beta 7.9).
 *
 * The app multiplies the system font scale by one of these five steps. It is a
 * display preference only: it never changes times, layout order or widget text.
 */
object FontScale {

    /** 80 % … 120 %, ascending. */
    val STEPS: List<Float> = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f)

    const val DEFAULT: Float = 1.0f

    /** Closest allowed step, so a stale or corrupt stored value cannot break UI. */
    fun normalize(value: Float): Float {
        if (value.isNaN()) return DEFAULT
        return STEPS.minByOrNull { kotlin.math.abs(it - value) } ?: DEFAULT
    }

    /** `80%` … `120%`, locale-neutral digits (same glyphs in AR and EN). */
    fun label(value: Float): String {
        val percent = Math.round(normalize(value) * 100)
        return "$percent%"
    }

    fun indexOf(value: Float): Int {
        val step = normalize(value)
        return STEPS.indexOf(step).coerceAtLeast(0)
    }
}
