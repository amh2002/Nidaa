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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * beta28: the drawing behind the transparent clock widget.
 *
 * The reference is a *composed* image, not a pair of stacked text views: huge
 * digits that fade out downwards, with a calligraphic Arabic day name drawn in
 * front of them and overlapping their lower part. RemoteViews cannot express a
 * text gradient, real z-order overlap or calligraphic shaping, so the whole
 * composition is drawn into one ARGB bitmap here and handed to a single
 * ImageView (`ClockWidgetProvider`).
 *
 * Every size is derived from the bitmap's own W/H, so the same code produces the
 * same composition at the 4x2 default and at every resized shape:
 *
 *   digits   cap-height 55% of H, ink top at 4% of H, horizontally centred,
 *            white fading to 15% alpha over the cap height (the fade is what
 *            keeps the white day name readable where it crosses them)
 *   day name ink width 90% of W (height-capped so it can never clip), ink
 *            centre at 62% of H, solid white, drawn after the digits
 *
 * Measurement is done by *rendering* the text into a scratch bitmap and scanning
 * its alpha channel (`measureInk`). Paint.getTextBounds under-reports shaped
 * Arabic (it returned 196 px for a word that draws 550 px wide on API 24), so
 * anything derived from it would mis-size and mis-place the day name.
 */
internal object ClockWidgetRenderer {

    /** Digit ink top, as a fraction of the bitmap height. */
    private const val DIGIT_TOP_FRACTION = 0.04f

    /** Digit cap-height, as a fraction of the bitmap height. */
    private const val DIGIT_CAP_FRACTION = 0.55f

    /** Alpha the digit gradient reaches at the bottom of the cap. */
    private const val DIGIT_END_ALPHA = 40

    /** Digit ink width cap, as a fraction of the bitmap width (safety only). */
    private const val DIGIT_WIDTH_FRACTION = 0.96f

    /** Day-name ink width, as a fraction of the bitmap width. */
    private const val DAY_WIDTH_FRACTION = 0.90f

    /** Day-name ink centre, as a fraction of the bitmap height. */
    private const val DAY_CENTER_FRACTION = 0.62f

    /**
     * Day-name ink height cap. 0.72 leaves a 2% bottom margin, so the descenders
     * of a word that is width-limited still land inside the bitmap.
     */
    private const val DAY_HEIGHT_FRACTION = 0.72f

    /** Day-name alpha (solid, a touch under pure white as in the reference). */
    private const val DAY_ALPHA = 245

    /** Soft separation glow behind the white text, in dp. */
    private const val GLOW_BLUR_DP = 2.5f

    /** Size the ink of a string is measured at, in px (units are normalised out). */
    private const val PROBE_SIZE = 64f

    private var digitTypeface: Typeface? = null
    private var dayTypeface: Typeface? = null
    private val inkCache = HashMap<String, Ink>()

    /** Ink box of a string, normalised by the text size (1.0 == one em). */
    private class Ink(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val centerX: Float get() = (left + right) / 2f
        val centerY: Float get() = (top + bottom) / 2f
    }

    /** Draws the whole widget into a new bitmap of exactly [widthPx] x [heightPx]. */
    fun render(context: Context, widthPx: Int, heightPx: Int): Bitmap {
        val width = widthPx.coerceAtLeast(1).toFloat()
        val height = heightPx.coerceAtLeast(1).toFloat()
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Fully transparent: no card, no scrim, the wallpaper shows through.

        val white = ContextCompat.getColor(context, R.color.widget_clock_text)
        val glow = ContextCompat.getColor(context, R.color.widget_clock_shadow)
        val density = context.resources.displayMetrics.density

        // ---- digits -------------------------------------------------------
        val digits = digitTypeface(context)
        val timeText = timeText(context)
        val capInk = measureInk("0", digits)
        val timeInk = measureInk(timeText, digits)

        val capTarget = height * DIGIT_CAP_FRACTION
        var digitSize = capTarget / capInk.height
        if (timeInk.width * digitSize > width * DIGIT_WIDTH_FRACTION) {
            digitSize = width * DIGIT_WIDTH_FRACTION / timeInk.width
        }
        val digitTop = height * DIGIT_TOP_FRACTION
        val digitBottom = digitTop + capInk.height * digitSize
        val digitBaseline = digitTop - timeInk.top * digitSize
        val digitX = width / 2f - timeInk.centerX * digitSize

        val digitPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = digits
            textAlign = Paint.Align.LEFT
            color = white
            textSize = digitSize
            shader = LinearGradient(
                0f,
                digitTop,
                0f,
                digitBottom,
                withAlpha(white, 255),
                withAlpha(white, DIGIT_END_ALPHA),
                Shader.TileMode.CLAMP
            )
            setShadowLayer(GLOW_BLUR_DP * density, 0f, 0f, glow)
        }
        canvas.drawText(timeText, digitX, digitBaseline, digitPaint)

        // ---- day name (drawn in front of the digits) -----------------------
        val dayFont = dayTypeface(context)
        val dayText = dayName(context)
        val dayInk = measureInk(dayText, dayFont)

        val daySize = minOf(
            width * DAY_WIDTH_FRACTION / dayInk.width,
            height * DAY_HEIGHT_FRACTION / dayInk.height
        )
        val dayBaseline = height * DAY_CENTER_FRACTION - dayInk.centerY * daySize
        val dayX = width / 2f - dayInk.centerX * daySize

        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = dayFont
            textAlign = Paint.Align.LEFT
            color = white
            alpha = DAY_ALPHA
            textSize = daySize
            setShadowLayer(GLOW_BLUR_DP * density, 0f, 0f, glow)
        }
        canvas.drawText(dayText, dayX, dayBaseline, dayPaint)

        return bitmap
    }

    /** "hh:mm" or "HH:mm" following the system 12/24h setting, no AM/PM. */
    fun timeText(context: Context): String {
        val is24 = DateFormat.is24HourFormat(context)
        return SimpleDateFormat(if (is24) "HH:mm" else "hh:mm", Locale.US)
            .format(Calendar.getInstance().time)
    }

    /** The Arabic weekday, taken from [R.array.day_names_ar] (never the device locale). */
    fun dayName(context: Context): String {
        val names = context.resources.getStringArray(R.array.day_names_ar)
        val index = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        return if (index in names.indices) names[index].trim() else names.firstOrNull()?.trim().orEmpty()
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
     * The digits use the platform's condensed bold sans rather than Tajawal: at a
     * 4x2 cell (about 1.73:1 here) a five-glyph "10:58" set in Tajawal is so wide
     * that the cap-height would have to drop to 38% of H to fit. The condensed
     * face keeps the reference's 50%+ cap-height inside the same width, and still
     * reads as the same clean bold sans as the rest of the app.
     */
    private fun digitTypeface(context: Context): Typeface =
        digitTypeface ?: runCatching {
            Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }.getOrNull() ?: loadTypeface(context, R.font.tajawal_bold, Typeface.DEFAULT_BOLD)
            .also { digitTypeface = it }

    private fun dayTypeface(context: Context): Typeface =
        dayTypeface ?: loadTypeface(context, R.font.arefruqaa_bold, Typeface.DEFAULT_BOLD)
            .also { dayTypeface = it }

    private fun loadTypeface(context: Context, resId: Int, fallback: Typeface): Typeface =
        runCatching { ResourcesCompat.getFont(context, resId) ?: fallback }.getOrDefault(fallback)
}
