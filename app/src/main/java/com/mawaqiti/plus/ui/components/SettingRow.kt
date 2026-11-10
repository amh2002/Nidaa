package com.mawaqiti.plus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * Pure width rule used by [SettingRow].
 *
 * A settings row can only put the control on the trailing side when what is
 * left for the title/description is still a readable column. Otherwise the
 * control moves to its own line underneath the text. Keeping the rule as a
 * plain function makes it unit-testable without a device.
 */
object SettingRowMetrics {

    fun shouldStack(
        contentWidthPx: Int,
        controlWidthPx: Int,
        gapPx: Int,
        minLabelWidthPx: Int
    ): Boolean = contentWidthPx - controlWidthPx - gapPx < minLabelWidthPx
}

/**
 * One settings row: title on line 1, optional description on line 2 directly
 * below it, and a control on the trailing side.
 *
 * Unlike a plain `Row` with a weighted `Text`, this measures the control first
 * and moves it under the label block when the pair cannot share the width. The
 * old layout put the title and a fixed-width segmented control on one line and
 * clipped the title at large font scales / narrow RTL widths. Nothing here is
 * absolutely positioned: both the trailing and stacked positions are computed
 * from the measured sizes, and the row uses [heightIn]-style content sizing,
 * never a fixed height.
 */
@Composable
fun SettingRow(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    rowPadding: Dp = 12.dp,
    control: @Composable () -> Unit
) {
    val palette = skyPalette
    val gap = 12.dp
    val minLabelWidth = 132.dp

    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = rowPadding)
    ) { constraints ->
        val maxWidth = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val minLabelWidthPx = minLabelWidth.roundToPx()

        // Loose measurement so a Switch or a pair of pills reports its natural
        // size instead of stretching to the card width.
        val controlPlaceables = subcompose(Slot.Control) { control() }
            .map { measurable -> measurable.measure(Constraints(maxWidth = maxWidth)) }
        val controlWidth = controlPlaceables.maxOfOrNull { it.width } ?: 0
        val controlHeight = controlPlaceables.maxOfOrNull { it.height } ?: 0

        val stacked = SettingRowMetrics.shouldStack(
            contentWidthPx = maxWidth,
            controlWidthPx = controlWidth,
            gapPx = gapPx,
            minLabelWidthPx = minLabelWidthPx
        )
        val labelWidth = if (stacked) {
            maxWidth
        } else {
            (maxWidth - controlWidth - gapPx).coerceAtLeast(minLabelWidthPx)
        }

        val labelPlaceable = subcompose(Slot.Label) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text
                )
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted
                    )
                }
            }
        }.first().measure(Constraints(maxWidth = labelWidth))

        val contentHeight = if (stacked) {
            labelPlaceable.height + gapPx + controlHeight
        } else {
            maxOf(labelPlaceable.height, controlHeight)
        }

        layout(maxWidth, contentHeight) {
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val labelX = if (isRtl) maxWidth - labelPlaceable.width else 0
            val controlX = if (isRtl) 0 else maxWidth - controlWidth

            if (stacked) {
                labelPlaceable.place(labelX, 0)
                controlPlaceables.forEach { it.place(controlX, labelPlaceable.height + gapPx) }
            } else {
                val labelY = (contentHeight - labelPlaceable.height) / 2
                val controlY = (contentHeight - controlHeight) / 2
                labelPlaceable.place(labelX, labelY)
                controlPlaceables.forEach { it.place(controlX, controlY) }
            }
        }
    }
}

private enum class Slot { Label, Control }
