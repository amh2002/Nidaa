package com.mawaqiti.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * Settings framework, permanent rule:
 *
 * The Settings screen is ONE scrollable list. Every item is a [SettingsCard];
 * rows inside it use [SettingsToggleRow] / [SegmentedPillRow]; the page ends
 * with the About card (or, if a legal footer is ever needed, exactly one
 * [FooterCard]). Nothing here stacks children in a Box or uses offsets/fixed
 * heights, so two texts can never paint over each other again.
 */
@Composable
fun SettingsCard(
    title: String? = null,
    description: String? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = skyPalette
    GlassCard(
        modifier = modifier,
        contentPadding = contentPadding,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SettingRow(
        title = title,
        description = description,
        modifier = modifier
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = mawaqitiSwitchColors()
        )
    }
}

@Composable
fun mawaqitiSwitchColors(): SwitchColors {
    val palette = skyPalette
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = SkyPalette.BrandGreen,
        checkedBorderColor = SkyPalette.BrandGreen,
        uncheckedThumbColor = palette.textMuted,
        uncheckedTrackColor = palette.text.copy(alpha = 0.16f),
        uncheckedBorderColor = palette.text.copy(alpha = 0.30f)
    )
}

data class PillOption(val label: String, val selected: Boolean)

/**
 * The segmented control used by language, clock, per-event mode and apply-all.
 * Pills flow in a plain [Row]; at narrow widths / large font scales the
 * parent [SettingRow] moves the whole control under the label, so text is
 * never clipped or overlapped.
 */
@Composable
fun SegmentedPillRow(
    options: List<PillOption>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, option ->
            ChoicePill(
                label = option.label,
                selected = option.selected,
                modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
fun ChoicePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = skyPalette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) SkyPalette.BrandGreen else palette.text.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else palette.text,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SettingsSmallButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = palette.accent,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Non-interactive info chip for the About card ("Fully offline",
 * "Collects no data"). It is content inside a [SettingsCard], never a page
 * item, and it has automatic height, so a large font scale wraps instead of
 * clipping.
 */
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier) {
    val palette = skyPalette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.text.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = palette.text,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Exactly one Text, always the last list item. The test tag is what the
 * regression guard counts: it must resolve to a single semantics node.
 *
 * beta5 note: the Settings page no longer renders a footer card — its claims
 * moved into the single About card, so nothing is duplicated. The component
 * stays in the framework for any future legal footer.
 */
@Composable
fun FooterCard(text: String, modifier: Modifier = Modifier) {
    val palette = skyPalette
    GlassCard(
        modifier = modifier,
        corner = 18.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_footer")
        )
    }
}
