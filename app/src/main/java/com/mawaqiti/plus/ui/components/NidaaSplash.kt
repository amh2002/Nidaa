package com.mawaqiti.plus.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.GoldenPalettes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * beta23: the opening animation.
 *
 * Why a Compose overlay instead of the Android 12+ SplashScreen API
 * ----------------------------------------------------------------
 * `minSdk` is 24, so the platform splash covers barely half of the install base
 * and would need `core-splashscreen` plus a second, visually different path for
 * older devices. A Compose overlay draws the SAME animation on 24..36, needs no
 * new dependency, and - the deciding point for the task - it can be tapped to
 * skip, which the system SplashScreen API does not let an app do.
 *
 * It never delays data: the overlay is drawn on top of an already composed
 * [com.mawaqiti.plus.MawaqitiApp], so the ViewModel, the DataStore read and the
 * first table lookup all run underneath it.
 *
 * Timing: scale/fade finish by 740 ms and the overlay removes itself at
 * [SPLASH_TOTAL_MS] = 900 ms, inside the 900 ms budget. Tokens only - the
 * gradient and gold come from [GoldenPalettes.Night].
 */
const val SPLASH_TOTAL_MS = 900

@Composable
fun NidaaSplash(onFinish: () -> Unit) {
    val palette = GoldenPalettes.Night
    val scale = remember { Animatable(0.82f) }
    val alpha = remember { Animatable(0f) }
    val shimmer = remember { Animatable(-1f) }
    val interaction = remember { MutableInteractionSource() }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(durationMillis = 280, easing = LinearOutSlowInEasing)) }
        launch { scale.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing)) }
        launch {
            shimmer.animateTo(
                1f,
                tween(durationMillis = 620, delayMillis = 120, easing = LinearEasing)
            )
        }
    }
    // Hard stop: the animation can never hold the app past 900 ms.
    LaunchedEffect(Unit) {
        delay(SPLASH_TOTAL_MS.toLong())
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette.gradient))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onFinish
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(SPLASH_LOGO_SIZE)
                    .clip(CircleShape)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
                    .drawWithContent {
                        drawContent()
                        // A single narrow gold band crossing the mark once.
                        val band = size.width * 0.55f
                        val x = shimmer.value * (size.width + band * 2f) - band
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    palette.accent.copy(alpha = 0.30f),
                                    Color.Transparent
                                ),
                                start = Offset(x, 0f),
                                end = Offset(x + band, size.height)
                            )
                        )
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.brand_name),
                fontFamily = AmiriBold,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 54.sp,
                color = palette.text,
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            )
        }
    }
}

private val SPLASH_LOGO_SIZE = 132.dp
