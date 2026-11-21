package com.mawaqiti.plus.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    /**
     * beta18: horizontal space between the "متبقي" caption and the countdown
     * digits in the hero pill. The two are now separate [Text]s joined by
     * `Modifier.alignByBaseline()`, so the word gap is an explicit token instead
     * of a space character buried in a concatenated string. 4 dp reproduces the
     * advance of one space at the pill's 14 sp type size.
     */
    val countdownWordGap = 4.dp

    /**
     * beta18: the single card-to-card spacing used by the Settings list. Every
     * `ItemGap()` reads this token, so two neighbouring cards can never end up
     * flush against each other.
     */
    val cardGap = 12.dp

    /**
     * beta18: content padding of the system-status cards (exact alarms,
     * notification permission, battery). Applied on all four sides through the
     * card's `contentPadding`, so the description can never touch a border.
     */
    val statusCardPadding = 16.dp

    /**
     * beta21: minimum window width for the landscape TV dashboard. 600 dp is
     * the platform's large-screen line; below it (a phone in portrait) the home
     * falls back to the unchanged mobile layout instead of squeezing the
     * landscape composition.
     */
    val tvDashboardMinWidth = 600.dp

    /**
     * beta21: panel-height breakpoint between the full TV sizes and the
     * compact set. 720 dp-class panels use [TvDimens]; 540 dp-class panels use
     * [TvCompactDimens] so font scale 1.3 still fits without scrolling.
     */
    val tvCompactHeight = 600.dp
}

/**
 * beta21: size contract of the landscape TV dashboard.
 *
 * Every dp/sp the TV variant uses is declared here - the screen file holds no
 * magic sizes - so the dashboard can be tuned for a 540 dp-high panel and a
 * 720 dp-high panel from one place. The sp values are multiplied by the user's
 * in-app font scale at the app root, exactly like the phone layout.
 *
 * [TvDimens] is the >= 600 dp-high panel (1280 x 720 class);
 * [TvCompactDimens] is the same composition for short panels (960 x 540
 * class), tightened so font scale 1.3 still fits without scrolling.
 */
interface TvMetrics {
    val screenPaddingH: Dp
    val screenPaddingV: Dp
    val rowGap: Dp
    val columnGap: Dp
    val sectionGap: Dp
    val cardGap: Dp
    val cardTextGap: Dp
    val heroCorner: Dp
    val heroPaddingH: Dp
    val heroPaddingV: Dp
    val cardCorner: Dp
    val cardPaddingH: Dp
    val cardPaddingV: Dp
    val icon: Dp
    val footerIcon: Dp
    val headerGap: Dp
    val progressGap: Dp
    val progressHeight: Dp
    val pillBorder: Dp
    val pillPaddingH: Dp
    val pillPaddingV: Dp

    val brandSize: TextUnit
    val dateSize: TextUnit
    val sectionSize: TextUnit
    val heroLabelSize: TextUnit
    val heroNameSize: TextUnit
    val heroTimeSize: TextUnit
    val nowSize: TextUnit
    val countdownSize: TextUnit
    val prayerNameSize: TextUnit
    val prayerTimeSize: TextUnit
    val footerSize: TextUnit
    val footerTimeSize: TextUnit
}

/** 1280 x 720 dp class panels - the mosque/home display target. */
object TvDimens : TvMetrics {
    override val screenPaddingH = 34.dp
    override val screenPaddingV = 16.dp
    override val rowGap = 14.dp
    override val columnGap = 18.dp
    override val sectionGap = 8.dp
    override val cardGap = 12.dp
    override val cardTextGap = 6.dp
    override val heroCorner = 26.dp
    override val heroPaddingH = 24.dp
    override val heroPaddingV = 16.dp
    override val cardCorner = 20.dp
    override val cardPaddingH = 12.dp
    override val cardPaddingV = 14.dp
    override val icon = 30.dp
    override val footerIcon = 17.dp
    override val headerGap = 6.dp
    override val progressGap = 8.dp
    override val progressHeight = 6.dp
    override val pillBorder = 1.dp
    override val pillPaddingH = 14.dp
    override val pillPaddingV = 6.dp

    override val brandSize = 34.sp
    override val dateSize = 15.sp
    override val sectionSize = 18.sp
    override val heroLabelSize = 18.sp
    override val heroNameSize = 44.sp
    override val heroTimeSize = 62.sp
    override val nowSize = 22.sp
    override val countdownSize = 24.sp
    override val prayerNameSize = 22.sp
    override val prayerTimeSize = 34.sp
    override val footerSize = 14.sp
    override val footerTimeSize = 18.sp
}

/** 960 x 540 dp class panels - the same dashboard, tightened for short screens. */
object TvCompactDimens : TvMetrics {
    override val screenPaddingH = 22.dp
    override val screenPaddingV = 12.dp
    override val rowGap = 10.dp
    override val columnGap = 14.dp
    override val sectionGap = 6.dp
    override val cardGap = 10.dp
    override val cardTextGap = 4.dp
    override val heroCorner = 18.dp
    override val heroPaddingH = 18.dp
    override val heroPaddingV = 10.dp
    override val cardCorner = 15.dp
    override val cardPaddingH = 8.dp
    override val cardPaddingV = 10.dp
    override val icon = 22.dp
    override val footerIcon = 14.dp
    override val headerGap = 4.dp
    override val progressGap = 6.dp
    override val progressHeight = 5.dp
    override val pillBorder = 1.dp
    override val pillPaddingH = 10.dp
    override val pillPaddingV = 4.dp

    override val brandSize = 26.sp
    override val dateSize = 13.sp
    override val sectionSize = 15.sp
    override val heroLabelSize = 15.sp
    override val heroNameSize = 34.sp
    override val heroTimeSize = 44.sp
    override val nowSize = 16.sp
    override val countdownSize = 17.sp
    override val prayerNameSize = 17.sp
    override val prayerTimeSize = 26.sp
    override val footerSize = 12.sp
    override val footerTimeSize = 15.sp
}
