package com.mawaqiti.plus.notify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.mawaqiti.plus.R
import com.mawaqiti.plus.format.HijriDateFormatter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

/**
 * The drawing behind the transparent clock widget.
 *
 * The composition is *drawn*, not stacked: huge digits that fade out downwards,
 * with a calligraphic Arabic day name in front of them, crossing their lower
 * part, and a small Hijri date under the word. RemoteViews cannot express a text
 * gradient, real z-order overlap or calligraphic shaping, so everything is
 * rendered into one ARGB bitmap here and handed to a single ImageView
 * (`ClockWidgetProvider`).
 *
 * Every size is derived from the bitmap's own W/H, so the same code produces the
 * same composition at the 4x2 default and at every resized shape:
 *
 *   digits     cap-height 48% of H, ink top at 2% of H, horizontally centred,
 *              white fading to 15% alpha over the cap (the fade is what keeps
 *              the white day name readable where it crosses them)
 *   separator  two stacked squares drawn as rects (never the font's round
 *              colon), sized 13% of the cap height and centred on the digit
 *              cap, where the eye expects a time colon
 *   day name   ink height 72% of H (width follows the face), ink centre at
 *              54.5% of H, solid white, drawn after the digits
 *   Hijri      one small line under the day name, centred at 95.5% of H
 *
 * Measurement is done by *rendering* the text into a scratch bitmap and scanning
 * its alpha channel (`measureInk`). Paint.getTextBounds under-reports shaped
 * Arabic (it returned 196 px for a word that draws 550 px wide on API 24), so
 * anything derived from it would mis-size and mis-place the day name.
 *
 * beta29: the day name is Amiri Bold (full tashkeel with real OpenType mark
 * anchoring, all hamza forms), the digits are Barlow SemiCondensed ExtraBold,
 * the time separator is two squares, and the Hijri date is computed with the
 * app's own Umm al-Qura converter ([HijriDateFormatter]).
 */
internal object ClockWidgetRenderer {

    /** Digit ink top, as a fraction of the bitmap height. */
    private const val DIGIT_TOP_FRACTION = 0.02f

    /**
     * Digit cap-height, as a fraction of the bitmap height. 48 %, down from 54 %,
     * to hand the day name back the height it lost in beta29 without deepening
     * the crossing.
     */
    private const val DIGIT_CAP_FRACTION = 0.48f

    /** Alpha the digit gradient reaches at the bottom of the cap. */
    private const val DIGIT_END_ALPHA = 40

    /** Digit ink width cap, as a fraction of the bitmap width (safety only). */
    private const val DIGIT_WIDTH_FRACTION = 0.94f

    // Time separator: two squares, all fractions of the digit cap-height.
    private const val SEP_SIDE_FRACTION = 0.13f
    private const val SEP_GAP_FRACTION = 0.10f
    private const val SEP_PAD_FRACTION = 0.13f

    /**
     * Top square's ink top, as a fraction of the cap below the digits' ink top.
     * The two squares plus their gap are 36 % of the cap, so 0.32 centres the
     * pair on the cap: it reads as "12:05", not as two ticks near the top.
     */
    private const val SEP_TOP_FRACTION = 0.32f

    /** Day-name ink width cap, as a fraction of the bitmap width (safety only). */
    private const val DAY_WIDTH_FRACTION = 0.98f

    /**
     * Day-name ink height, as a fraction of the bitmap height: beta28's 72 %, so
     * the word sweeps again instead of sitting as a caption between the time and
     * the date.
     */
    private const val DAY_HEIGHT_FRACTION = 0.72f

    /**
     * Day-name ink centre, as a fraction of the bitmap height. Low enough that the
     * 72 %-tall word stops just above the Hijri line at 95.5 %.
     */
    private const val DAY_CENTER_FRACTION = 0.545f

    /** Day-name alpha (solid, a touch under pure white as in the reference). */
    private const val DAY_ALPHA = 245

    /** Hijri line ink height cap, as a fraction of the bitmap height. */
    private const val HIJRI_HEIGHT_FRACTION = 0.078f

    /** Hijri line ink width cap, as a fraction of the bitmap width. */
    private const val HIJRI_WIDTH_FRACTION = 0.66f

    /** Hijri line ink centre, as a fraction of the bitmap height. */
    private const val HIJRI_CENTER_FRACTION = 0.955f

    /** Hijri line alpha: a quiet subtitle under the day name. */
    private const val HIJRI_ALPHA = 205

    /** Soft separation glow behind the white text, in dp. */
    private const val GLOW_BLUR_DP = 2.5f

    /** Size the ink of a string is measured at, in px (units are normalised out). */
    private const val PROBE_SIZE = 64f

    /** Tabular figures, so the time block's width never wobbles between minutes. */
    private const val TABULAR_FIGURES = "tnum"

    /** U+200F RIGHT-TO-LEFT MARK: fixes the Hijri line's base direction. */
    private const val RTL_MARK = "\u200F"

    private var digitTypeface: Typeface? = null
    private var dayTypeface: Typeface? = null
    private var hijriTypefaceRef: Typeface? = null
    private val inkCache = HashMap<String, Ink>()

    /** Ink box of a string, normalised by the text size (1.0 == one em). */
    private class Ink(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val centerX: Float get() = (left + right) / 2f
        val centerY: Float get() = (top + bottom) / 2f
    }

    /** Pixel geometry of the time block at one digit size. */
    private class TimeBox(
        val capPx: Float,
        val side: Float,
        val gap: Float,
        val pad: Float,
        val totalWidth: Float
    )

    /** Draws the whole widget into a new bitmap of exactly [widthPx] x [heightPx]. */
    fun render(context: Context, widthPx: Int, heightPx: Int): Bitmap {
        val width = widthPx.coerceAtLeast(1).toFloat()
        val height = heightPx.coerceAtLeast(1).toFloat()
        val bitmap = Bitmap.createBitmap(
            widthPx.coerceAtLeast(1),
            heightPx.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        // Fully transparent: no card, no scrim, the wallpaper shows through.

        val white = ContextCompat.getColor(context, R.color.widget_clock_text)
        val glow = ContextCompat.getColor(context, R.color.widget_clock_shadow)
        val density = context.resources.displayMetrics.density

        drawTime(context, canvas, width, height, white, glow, density)
        drawDayName(context, canvas, width, height, white, glow, density)
        drawHijri(context, canvas, width, height, white, glow, density)

        return bitmap
    }

    /**
     * Hour, square separator, minute.
     *
     * The time is drawn in three pieces rather than as one string so the
     * separator is two small squares of our own making instead of the round
     * dots of the font's colon. The pieces are laid out left to right
     * explicitly, so the digits stay LTR whatever the RTL layout direction is.
     */
    private fun drawTime(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        white: Int,
        glow: Int,
        density: Float
    ) {
        val typeface = digitTypeface(context)
        val (hour, minute) = timeParts(context)
        val capInk = measureInk("0", typeface)
        val hourInk = measureInk(hour, typeface)
        val minuteInk = measureInk(minute, typeface)

        fun boxAt(size: Float): TimeBox {
            val cap = capInk.height * size
            val side = cap * SEP_SIDE_FRACTION
            val pad = cap * SEP_PAD_FRACTION
            val gap = cap * SEP_GAP_FRACTION
            val total = hourInk.width * size + pad * 2f + side + minuteInk.width * size
            return TimeBox(cap, side, gap, pad, total)
        }

        var digitSize = height * DIGIT_CAP_FRACTION / capInk.height
        var box = boxAt(digitSize)
        val maxWidth = width * DIGIT_WIDTH_FRACTION
        if (box.totalWidth > maxWidth) {
            digitSize *= maxWidth / box.totalWidth
            box = boxAt(digitSize)
        }

        val inkTop = height * DIGIT_TOP_FRACTION
        // Both pieces share the "0" ink top, so their digits sit on one baseline.
        val baseline = inkTop - capInk.top * digitSize
        var x = width / 2f - box.totalWidth / 2f

        val gradient = LinearGradient(
            0f,
            inkTop,
            0f,
            inkTop + box.capPx,
            withAlpha(white, 255),
            withAlpha(white, DIGIT_END_ALPHA),
            Shader.TileMode.CLAMP
        )
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
            color = white
            textSize = digitSize
            shader = gradient
            // Tabular figures: a clock that changes "11:11" to "00:00" must not
            // shift sideways because one digit is narrower than another.
            fontFeatureSettings = TABULAR_FIGURES
            setShadowLayer(GLOW_BLUR_DP * density, 0f, 0f, glow)
        }
        val squarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            shader = gradient
            setShadowLayer(GLOW_BLUR_DP * density, 0f, 0f, glow)
        }

        canvas.drawText(hour, x - hourInk.left * digitSize, baseline, textPaint)
        x += hourInk.width * digitSize + box.pad

        val squareTop = inkTop + box.capPx * SEP_TOP_FRACTION
        canvas.drawRect(x, squareTop, x + box.side, squareTop + box.side, squarePaint)
        canvas.drawRect(
            x,
            squareTop + box.side + box.gap,
            x + box.side,
            squareTop + box.side * 2f + box.gap,
            squarePaint
        )
        x += box.side + box.pad

        canvas.drawText(minute, x - minuteInk.left * digitSize, baseline, textPaint)
    }

    /** The calligraphic day name, drawn in front of the digits. */
    private fun drawDayName(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        white: Int,
        glow: Int,
        density: Float
    ) {
        val typeface = dayTypeface(context)
        val text = dayName(context)
        if (text.isEmpty()) return
        val ink = measureInk(text, typeface)

        val size = minOf(
            width * DAY_WIDTH_FRACTION / ink.width,
            height * DAY_HEIGHT_FRACTION / ink.height
        )
        val baseline = height * DAY_CENTER_FRACTION - ink.centerY * size
        val x = width / 2f - ink.centerX * size

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
            color = white
            alpha = DAY_ALPHA
            textSize = size
            setShadowLayer(GLOW_BLUR_DP * density, 0f, 0f, glow)
        }
        canvas.drawText(text, x, baseline, paint)
    }

    /** The Hijri date, small, under the day name. */
    private fun drawHijri(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        white: Int,
        glow: Int,
        density: Float
    ) {
        val typeface = hijriTypeface(context)
        val text = hijriText(context)
        if (text.isEmpty()) return
        val ink = measureInk(text, typeface)
        if (ink.width <= 0f || ink.height <= 0f) return

        val size = minOf(
            width * HIJRI_WIDTH_FRACTION / ink.width,
            height * HIJRI_HEIGHT_FRACTION / ink.height
        )
        val baseline = height * HIJRI_CENTER_FRACTION - ink.centerY * size
        val x = width / 2f - ink.centerX * size

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
            color = white
            alpha = HIJRI_ALPHA
            textSize = size
            setShadowLayer(GLOW_BLUR_DP * density, 0f, 0f, glow)
        }
        canvas.drawText(text, x, baseline, paint)
    }

    /** Hour and minute, "HH"/"hh" and "mm" following the system 12/24h setting. */
    fun timeParts(context: Context): Pair<String, String> {
        val is24 = DateFormat.is24HourFormat(context)
        val now = Calendar.getInstance().time
        val hour = SimpleDateFormat(if (is24) "HH" else "hh", Locale.US).format(now)
        val minute = SimpleDateFormat("mm", Locale.US).format(now)
        return hour to minute
    }

    /** The Arabic weekday, taken from [R.array.day_names_ar] (never the device locale). */
    fun dayName(context: Context): String {
        val names = context.resources.getStringArray(R.array.day_names_ar)
        val index = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        return if (index in names.indices) names[index].trim() else names.firstOrNull()?.trim().orEmpty()
    }

    /**
     * Today's Hijri date as the widget draws it: `١٤ رمضان ١٤٤٧هـ`.
     *
     * The conversion is the app's own Umm al-Qura one ([HijriDateFormatter], which
     * uses java.time.chrono.HijrahDate and core-library desugaring), so the widget
     * and the app cannot disagree about the date. Only the month names and the
     * format live in resources; the digits are converted to Arabic-Indic here.
     */
    fun hijriText(context: Context): String {
        val hijri = runCatching { HijriDateFormatter.toHijri(LocalDate.now()) }.getOrNull() ?: return ""
        val months = context.resources.getStringArray(R.array.hijri_months_ar)
        val month = months.getOrNull(hijri.month - 1)?.trim().orEmpty()
        if (month.isEmpty()) return ""
        // A right-to-left mark first, so the line is laid out as Arabic: the day
        // number on the right, then the month, then the year. Without it the
        // paragraph starts with a digit (a weak direction) and the whole line
        // falls back to left-to-right order, putting the year first.
        return RTL_MARK + context.getString(
            R.string.clock_widget_hijri,
            arabicIndic(hijri.day),
            month,
            arabicIndic(hijri.year)
        )
    }

    /** 14 -> "١٤" (U+0660..U+0669), without touching any other character. */
    private fun arabicIndic(value: Int): String {
        val text = value.toString()
        val out = StringBuilder(text.length)
        for (ch in text) {
            out.append(if (ch in '0'..'9') '\u0660' + (ch - '0') else ch)
        }
        return out.toString()
    }

    /**
     * The real ink box of [text] in [typeface], normalised by the text size.
     *
     * The string is drawn into a scratch bitmap at [PROBE_SIZE] and its alpha
     * channel scanned, so the result includes exactly the shaping the final draw
     * will use (Paint.getTextBounds does not report the shaped Arabic width).
     * Cached per string: the box is scale-invariant.
     */
    private fun measureInk(text: String, typeface: Typeface): Ink {
        val key = text + "|" + typeface.hashCode()
        inkCache[key]?.let { return it }
        val size = PROBE_SIZE
        val pad = size
        val scratchWidth = (text.length * size + 2 * pad).coerceAtLeast(2 * pad).toInt()
        val scratchHeight = (size * 4).toInt()
        val ink = runCatching {
            val scratch = Bitmap.createBitmap(scratchWidth, scratchHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(scratch)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                this.typeface = typeface
                textAlign = Paint.Align.LEFT
                color = android.graphics.Color.WHITE
                textSize = size
                // Same feature as the draw call, so the measured box is the drawn
                // box: tabular figures change the advances of the digits.
                fontFeatureSettings = TABULAR_FIGURES
            }
            val originX = pad.toFloat()
            val baseline = size * 3f
            canvas.drawText(text, originX, baseline, paint)

            val row = IntArray(scratchWidth)
            var minX = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var minY = Int.MAX_VALUE
            var maxY = Int.MIN_VALUE
            for (y in 0 until scratchHeight) {
                scratch.getPixels(row, 0, scratchWidth, 0, y, scratchWidth, 1)
                for (x in 0 until scratchWidth) {
                    if (row[x] ushr 24 > 8) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
            scratch.recycle()
            if (minX == Int.MAX_VALUE) {
                Ink(0f, 0f, size, size)
            } else {
                Ink(
                    (minX - originX) / size,
                    (minY - baseline) / size,
                    (maxX + 1 - originX) / size,
                    (maxY + 1 - baseline) / size
                )
            }
        }.getOrElse { Ink(0f, -0.7f, size, 0f) }
        inkCache[key] = ink
        return ink
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    /**
     * The digits are Barlow SemiCondensed **ExtraBold** (SIL OFL 1.1): a clean,
     * heavy grotesque whose 0-9 match the reference's bold time, and narrow
     * enough that a full "23:59" still fills more than half the widget's height
     * inside the 4x2 cell.
     */
    private fun digitTypeface(context: Context): Typeface =
        digitTypeface ?: loadTypeface(context, R.font.barlow_semicondensed_extrabold, Typeface.DEFAULT_BOLD)
            .also { digitTypeface = it }

    /**
     * The day name is Amiri **Bold** (SIL OFL 1.1): the flattest of the
     * tashkeel-correct Naskh faces evaluated (it carries 12 mark-to-base GPOS
     * lookups and every harakah plus every hamza form), and a calligraphic face
     * with the thick/thin contrast of the reference. It also matches the app's
     * wordmark, which already uses Amiri Bold.
     */
    private fun dayTypeface(context: Context): Typeface =
        dayTypeface ?: loadTypeface(context, R.font.amiri_bold, Typeface.DEFAULT_BOLD)
            .also { dayTypeface = it }

    /** The Hijri subtitle uses the app's UI face (Tajawal Medium), not the display face. */
    private fun hijriTypeface(context: Context): Typeface =
        hijriTypefaceRef ?: loadTypeface(context, R.font.tajawal_medium, Typeface.DEFAULT_BOLD)
            .also { hijriTypefaceRef = it }

    private fun loadTypeface(context: Context, resId: Int, fallback: Typeface): Typeface =
        runCatching { ResourcesCompat.getFont(context, resId) ?: fallback }.getOrDefault(fallback)
}
