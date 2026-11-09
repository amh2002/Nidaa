package com.mawaqiti.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * Glass surface used by every card in the app.
 *
 * Real-time backdrop blur is not available below API 31 without a RenderEffect
 * trade-off, so the glass is built from the spec's translucent fill, a 1px
 * light border and a soft top sheen. On the Living Sky gradients this reads as
 * frosted glass and keeps text contrast deterministic.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = skyPalette
    val shape = RoundedCornerShape(corner)
    val fill = if (emphasized) palette.heroFill else palette.cardFill
    val border = if (emphasized) palette.heroBorder else palette.cardBorder

    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(fill)
        .background(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                startY = 0f,
                endY = 220f
            )
        )
        .border(1.dp, border, shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(contentPadding)

    Box(modifier = base, content = content)
}

/** Dashed outline used by the last-third card. */
fun Modifier.dashedOutline(
    color: Color,
    corner: Dp = 18.dp,
    width: Dp = 1.5.dp,
    dash: FloatArray = floatArrayOf(13f, 9f)
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(corner.toPx(), corner.toPx()),
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(dash, 0f)
        )
    )
}
