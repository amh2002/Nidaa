package com.mawaqiti.plus.ui.theme

import androidx.compose.ui.unit.dp

/**
 * beta15: shared spacing tokens for the next-prayer hero card.
 *
 * The label/prayer-name separation used to be a literal inside the screen file.
 * It lives here now so the "the two lines must never touch" rule has exactly one
 * editable home and so a unit test can assert the >= 8 dp floor without a
 * device. Adding a new gap to this card means adding one value here and using
 * it - never a magic number at a call site, never an absolute pixel offset.
 */
object NidaaDimens {

    /**
     * Minimum clear space between the "الصلاة القادمة" caption and the prayer
     * name below it. 8 dp is the beta4/beta12 value the owner approved; it is
     * the floor, so a future tweak may grow it but must not shrink it.
     */
    val nextPrayerLabelGap = 8.dp

    /**
     * beta17: space between the adhan picker and the sound-test button that
     * sits beside it. Kept small so both fit on one 360 dp row without
     * clipping at fontScale 1.3.
     */
    val adhanPickerGap = 8.dp

    /**
     * beta17: space between the picker/test row and the volume slider inside
     * the same adhan card.
     */
    val adhanVolumeGap = 14.dp

    /**
     * beta17: optical lift for the city-search hint.
     *
     * The magnifier is a 20 dp icon and centres exactly on the field's axis.
     * The hint is text: its line box centres on the font baseline, so the
     * glyph ink (the only part a person sees) sits above that axis by the
     * glyph descent - measured at 4 px / 1.5 dp in Arabic at 411 dp.
     * Compensating by exactly that descent puts the hint and the magnifier on
     * one optical line; it is a font-metric correction, not a layout fudge,
     * so it scales with density and font scale.
     */
    val searchHintOpticalLift = 1.5.dp
}
