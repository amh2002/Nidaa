package com.mawaqiti.plus.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.theme.skyPalette
import com.mawaqiti.plus.ui.theme.starTextExclusion

/**
 * beta13: the ONE registry of dock destinations.
 *
 * Declaration order is the visual order from the RTL start: in Arabic the dock
 * reads Settings (right) → Home (middle) → Qibla (left); in an LTR locale the
 * same row is mirrored automatically by the layout direction, so no per-locale
 * list is needed.
 *
 * Each entry carries everything the dock needs - the tab it selects, its label
 * resource and its icon - so adding a future feature is exactly one entry here
 * plus its screen branch in `MawaqitiApp`. [tab] is the app's tab index, kept
 * stable and independent of the visual order.
 */
enum class DockDestination(
    val tab: Int,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    SETTINGS(tab = 2, labelRes = R.string.footer_settings, icon = Icons.Filled.Settings),
    HOME(tab = 0, labelRes = R.string.dock_home, icon = Icons.Filled.Home),
    QIBLA(tab = 1, labelRes = R.string.footer_qibla, icon = Icons.Filled.Explore);

    companion object {
        /** Declaration order; the dock renders exactly this list. */
        val ordered: List<DockDestination> get() = entries.toList()

        /** The dock entry owning [tab], or null when that tab is not a dock tab. */
        fun forTab(tab: Int): DockDestination? = entries.firstOrNull { it.tab == tab }
    }
}

/**
 * beta13 geometry, pulled out of the composable so the "equal gaps / constant
 * indicator" contract is asserted by [NidaaDockTest] instead of living only in
 * the layout.
 *
 * Design-A geometry: every destination owns the same fixed slot, the slots are
 * separated by the same fixed gap, and the dock is only as wide as those slots
 * plus the surface padding. The gold indicator is always exactly one slot big;
 * only its X offset changes.
 */
object DockMetrics {
    val SLOT_WIDTH = 64.dp
    val SLOT_HEIGHT = 52.dp
    val GAP = 6.dp
    val SURFACE_PADDING = 5.dp
    val BOTTOM_MARGIN = 16.dp
    val ICON_SIZE = 20.dp

    /** One slot plus one gap: how far the indicator travels per destination. */
    fun stride(): Dp = SLOT_WIDTH + GAP

    /** X offset of the indicator for a destination index (0 = RTL start). */
    fun indicatorOffset(index: Int): Dp = stride() * index

    /** Index of [tab] in [DockDestination.ordered]; 0 when it is not a dock tab. */
    fun indicatorIndex(tab: Int): Int =
        DockDestination.ordered.indexOfFirst { it.tab == tab }.coerceAtLeast(0)

    /** Width of the row of [count] fixed slots and their equal gaps. */
    fun dockWidth(count: Int): Dp =
        if (count <= 0) 0.dp else SLOT_WIDTH * count + GAP * (count - 1)

    /** The indicator never changes size - it is always exactly one slot. */
    fun indicatorSize(): DpSize = DpSize(SLOT_WIDTH, SLOT_HEIGHT)

    /** 280 ms FastOutSlowIn: no overshoot, so the pill always lands exactly. */
    const val INDICATOR_MS = 280

    /** Unselected items fade back; selected content is at full strength. */
    const val UNSELECTED_ALPHA = 0.55f

    /** Icon grows slightly when its slot becomes selected. */
    const val SELECTED_ICON_SCALE = 1.08f

    /**
     * Selected content is stacked (icon above label). The icon lifts by this
     * amount to make room for the label without ever clipping at 1.3x.
     */
    val SELECTED_ICON_LIFT = 10.dp
}

/**
 * beta13 floating golden dock.
 *
 * Geometry (Design A): wrap-content width, centred horizontally, 16 dp above
 * the navigation inset, and therefore always >= 16 dp from each screen edge.
 * The three destinations are identical 64 x 52 dp slots in one `Row` with one
 * identical 6 dp gap - no SpaceBetween/weight, so the gaps cannot drift.
 *
 * Selection: a SINGLE gold pill of constant 64 x 52 dp bounds slides from the
 * old slot to the new one with `animateDpAsState` (280 ms FastOutSlowIn, no
 * overshoot). Rapid switching simply retargets the same animation, which always
 * ends exactly on the selected slot. The selected slot shows its icon (scaled
 * 1.08 with a spring) plus a faded-in label; unselected slots show a centred
 * icon at 55 % alpha. Every slot keeps its own ripple and 0.97 press scale.
 *
 * Surface: full pill radius, `sheet` ink at 86 %, a 1 dp `accent` border and a
 * layered golden halo behind it. Every colour comes from [SkyPalette].
 */
@Composable
fun NidaaDock(
    selectedTab: Int,
    onSelect: (DockDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    val pill = CircleShape
    val destinations = DockDestination.ordered
    val selectedIndex = DockMetrics.indicatorIndex(selectedTab)

    val indicatorX by animateDpAsState(
        targetValue = DockMetrics.indicatorOffset(selectedIndex),
        animationSpec = tween(
            durationMillis = DockMetrics.INDICATOR_MS,
            easing = FastOutSlowInEasing
        ),
        label = "dockIndicator"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = DockMetrics.BOTTOM_MARGIN),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    // Soft golden halo. Painting several progressively larger
                    // rounded rects at a very low alpha (instead of one hard
                    // gradient block) gives a smooth falloff on every API level,
                    // including the ones without coloured elevation shadows.
                    val layers = 6
                    val maxInflate = 14.dp.toPx()
                    for (step in layers downTo 1) {
                        val inflate = maxInflate * step / layers
                        val radius = (size.height + inflate * 2f) / 2f
                        drawRoundRect(
                            color = palette.accent.copy(alpha = 0.016f),
                            topLeft = Offset(-inflate, -inflate),
                            size = Size(
                                width = size.width + inflate * 2f,
                                height = size.height + inflate * 2f
                            ),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                }
                .clip(pill)
                .background(palette.sheet.copy(alpha = 0.86f))
                .border(1.dp, palette.accent.copy(alpha = 0.32f), pill)
                .padding(DockMetrics.SURFACE_PADDING)
        ) {
            // The one and only selection indicator, behind the slots. Its size
            // never varies; `offset(x = ...)` is RTL-aware, so index 0 is the
            // RTL start (rightmost) and the row mirrors in English for free.
            Box(
                modifier = Modifier
                    .offset(x = indicatorX)
                    .size(
                        width = DockMetrics.indicatorSize().width,
                        height = DockMetrics.indicatorSize().height
                    )
                    .clip(pill)
                    .background(palette.accent)
            )

            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(DockMetrics.GAP),
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { destination ->
                    DockSlot(
                        destination = destination,
                        selected = destination.tab == selectedTab,
                        onClick = { onSelect(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockSlot(
    destination: DockDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = skyPalette
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "dockPress"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else DockMetrics.UNSELECTED_ALPHA,
        animationSpec = tween(durationMillis = 200),
        label = "dockSlotAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) DockMetrics.SELECTED_ICON_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dockIconScale"
    )
    val iconLift by animateDpAsState(
        targetValue = if (selected) -DockMetrics.SELECTED_ICON_LIFT else 0.dp,
        animationSpec = tween(durationMillis = DockMetrics.INDICATOR_MS),
        label = "dockIconLift"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "dockLabelAlpha"
    )

    // Selected content sits on gold, so it takes the dark sheet ink; unselected
    // content is muted light.
    val tint = if (selected) palette.sheet else palette.textMuted

    Box(
        modifier = Modifier
            .size(DockMetrics.SLOT_WIDTH, DockMetrics.SLOT_HEIGHT)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        contentAlignment = Alignment.Center
    ) {
        // Unselected: centred icon. Selected: the same icon lifts to make room
        // for the label, so both the slot and the pill keep constant bounds.
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .offset(y = iconLift)
                .size(DockMetrics.ICON_SIZE)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = contentAlpha
                }
        )
        Text(
            text = stringResource(destination.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            // Labels live inside the fixed 64 dp slot. If the largest system
            // font scale makes one wider than the slot, it shrinks instead of
            // clipping; the pill bounds never change.
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = MaterialTheme.typography.labelSmall.fontSize
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // beta24 (Task 3): the dock labels are text too - no night star
                // inside their 24 dp margin.
                .starTextExclusion("dock_label_${destination.labelRes}")
                .padding(start = 2.dp, end = 2.dp, bottom = 4.dp)
                .graphicsLayer { alpha = labelAlpha * contentAlpha }
        )
    }
}
