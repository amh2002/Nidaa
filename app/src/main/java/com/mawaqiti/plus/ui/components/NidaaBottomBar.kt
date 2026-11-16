package com.mawaqiti.plus.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * One entry of the beta11 bottom navigation bar.
 *
 * [route] is the bar's own stable id (it is not an engine or navigation-graph
 * concept); [labelRes] and [icon] are the only presentation inputs, so a new
 * destination never needs a new composable or a new colour decision.
 */
data class NidaaNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

/**
 * beta11: the single source of truth for the bottom bar.
 *
 * Adding a future feature is deliberately one entry plus its screen:
 *
 *   1. add `NidaaNavItem("widgets", R.string.footer_widgets, Icons.Filled.Widgets)`;
 *   2. the bar picks it up automatically (order = list order, so in RTL the
 *      first entry is the rightmost one);
 *   3. `MawaqitiApp` maps `index + 1` to the tab that hosts that screen, so the
 *      new tab index follows from the list position; `TAB_ABOUT` sits far above
 *      that range so About never has to be renumbered.
 */
val NidaaNavItems: List<NidaaNavItem> = listOf(
    NidaaNavItem(route = "qibla", labelRes = R.string.footer_qibla, icon = Icons.Filled.Explore),
    NidaaNavItem(route = "settings", labelRes = R.string.footer_settings, icon = Icons.Filled.Settings)
)

/**
 * Tab index that hosts [route] in a bar driven by [items]: the bar owns tabs
 * `1..items.size`, so the index is `position + 1`. Returns [fallback] (home)
 * when the route is not part of this bar.
 */
fun navTabIndex(items: List<NidaaNavItem>, route: String, fallback: Int = 0): Int {
    val index = items.indexOfFirst { it.route == route }
    return if (index >= 0) index + 1 else fallback
}

/**
 * Themed bottom navigation, replacing the two standalone beta9 footer buttons.
 *
 * Visual contract with the Golden Night identity: the surface is the same warm
 * `sheet` ink as the cards, faded at the top so it melts into the sky, with a
 * gold hairline along its top edge. Because the surface is slightly translucent
 * and the content above it is padded by the Scaffold, the mosque silhouette and
 * the official-timetable note stay visible and are never covered by an item.
 *
 * Accessibility: every item is a `Role.Tab` with `selected` state, at least
 * 56 dp tall (>= the 48 dp minimum), a ripple bounded to its own rounded cell,
 * and the visible label doubles as its TalkBack name (the icon is decorative).
 */
@Composable
fun NidaaBottomBar(
    selectedRoute: String?,
    onSelect: (NidaaNavItem) -> Unit,
    modifier: Modifier = Modifier,
    items: List<NidaaNavItem> = NidaaNavItems
) {
    val palette = skyPalette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.sheet.copy(alpha = 0.52f),
                        palette.sheet.copy(alpha = 0.94f)
                    )
                )
            )
            // The bar owns the gesture-navigation inset; the Scaffold then hands
            // that height to the screens as content padding.
            .navigationBarsPadding()
    ) {
        // Gold hairline, brightest in the middle and fading into both corners.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.accent.copy(alpha = 0.02f),
                            palette.accent.copy(alpha = 0.38f),
                            palette.accent.copy(alpha = 0.02f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                NidaaBottomBarItem(
                    item = item,
                    selected = item.route == selectedRoute,
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NidaaBottomBarItem(
    item: NidaaNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    // Selected: full gold. Unselected: the palette's muted cream, damped a
    // little further so the gold capsule and label read at a glance (the
    // composite still clears 4.5:1 on the bar surface).
    val tint = if (selected) palette.accent else palette.textMuted.copy(alpha = 0.62f)
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Selection indicator: a short gold capsule above the icon. Unselected
        // items keep the same 3 dp slot so labels never jump.
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (selected) palette.accent else Color.Transparent)
        )
        Spacer(Modifier.height(5.dp))
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
