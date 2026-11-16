package com.mawaqiti.plus.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * beta12: the ONE registry of dock destinations.
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
 * beta12 floating golden dock, replacing the beta11 edge-to-edge bar.
 *
 * Shape and surface: a full-radius pill, inset 20 dp from both screen edges,
 * floating 14 dp above the navigation inset. Surface is the theme's `sheet`
 * ink at 86 % so the sky gradient and the mosque silhouette still read through
 * it, a 1 dp golden border from the `accent` token, a soft golden halo painted
 * behind the pill (so the glow is gold on every API level, not only where
 * coloured elevation shadows exist) plus a real elevation shadow underneath.
 * No colour is hardcoded: every value derives from [SkyPalette].
 *
 * Interaction: three discrete buttons, each with its own bounded ripple and a
 * 0.97 press scale - the pill is never one continuous touch surface. The
 * selected destination is a filled golden pill with dark ink icon and label;
 * the others are transparent with the muted light text colour. Every button is
 * at least 48 dp tall, is a `Role.Tab` with `selected` semantics, and the
 * visible label doubles as its TalkBack name.
 */
@Composable
fun NidaaDock(
    selectedTab: Int,
    onSelect: (DockDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    val pill = CircleShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                .padding(horizontal = 8.dp, vertical = 7.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockDestination.ordered.forEach { destination ->
                DockButton(
                    destination = destination,
                    selected = destination.tab == selectedTab,
                    onClick = { onSelect(destination) }
                )
            }
        }
    }
}

@Composable
private fun DockButton(
    destination: DockDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = skyPalette
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "dockPress"
    )
    val tint = if (selected) palette.sheet else palette.textMuted

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(if (selected) palette.accent else Color.Transparent)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(destination.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
