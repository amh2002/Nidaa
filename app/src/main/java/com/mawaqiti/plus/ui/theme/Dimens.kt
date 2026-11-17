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
}
