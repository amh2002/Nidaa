package com.mawaqiti.plus.format

/**
 * beta30: the single digit-mapping utility of the app.
 *
 * The «الأرقام الهندية» setting is presentation-only: it rewrites the glyphs of
 * an already formatted string, so no stored value, engine minute, alarm epoch
 * or widget remote-view ever changes. Home surfaces ask [TimeFormatter] for a
 * string and opt in; every other caller (notifications, settings preview, the
 * clock widget's own renderer) keeps the Latin default.
 *
 * One table, two directions: [toArabicIndic] for display, [toLatin] for the
 * reverse (tests and any future paste-back path). Characters outside the table
 * - separators, the 12-hour suffix, spaces - pass through untouched, so the
 * mapping is safe on a whole formatted line.
 */
object Numerals {

    private const val LATIN = "0123456789"
    private const val ARABIC_INDIC = "\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669"

    /** Rewrites every ASCII digit in [text] as its Arabic-Indic counterpart. */
    fun toArabicIndic(text: String): String = translate(text, LATIN, ARABIC_INDIC)

    /** Rewrites every Arabic-Indic digit in [text] back to ASCII. */
    fun toLatin(text: String): String = translate(text, ARABIC_INDIC, LATIN)

    private fun translate(text: String, from: String, to: String): String {
        var needsRewrite = false
        for (ch in text) {
            if (from.indexOf(ch) >= 0) {
                needsRewrite = true
                break
            }
        }
        // The vast majority of strings carry no digits at all.
        if (!needsRewrite) return text
        val builder = StringBuilder(text.length)
        for (ch in text) {
            val index = from.indexOf(ch)
            builder.append(if (index >= 0) to[index] else ch)
        }
        return builder.toString()
    }
}
