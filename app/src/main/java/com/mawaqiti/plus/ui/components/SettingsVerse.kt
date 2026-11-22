package com.mawaqiti.plus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.Tajawal
import com.mawaqiti.plus.ui.theme.skyPalette
import com.mawaqiti.plus.ui.theme.starTextExclusion

/** Key of the verse in the star-exclusion registry (beta24 mechanism). */
private const val VERSE_EXCLUSION_KEY = "settings_verse_an_nisa_103"

/** Top of the scene, in height fractions - mirrors `drawIslamicScene`. */
private const val SCENE_TOP_FRACTION = 0.955f - 0.19f * 1.08f

/** The finial bead's radius, as a fraction of the width. */
private const val SCENE_BEAD_WIDTH_FRACTION = 0.007f

/**
 * beta27: the verse of An-Nisa 103, centred in the open sky of the Settings
 * screen below the last collapsed header.
 *
 * The block is bottom-aligned just above the silhouette, so the empty band
 * above it stays available for the meteor (see [SettingsSkyAmbient], which
 * receives this block's top through [onTopChange]). It is composed only while
 * every accordion section is collapsed, exactly like the ambient layer, and it
 * is static - no animation is applied to the text itself.
 *
 * The verse is set in the bundled Amiri (an OFL Naskh face already used for the
 * wordmark), which is what makes the ornate Quranic brackets U+FD3E / U+FD3F
 * render instead of falling back to tofu.
 *
 * Layout contract: the band between [headerBottomPx] and the top of the
 * silhouette decides the type size - full at 140 dp or more, one step smaller
 * from 112 dp, the smallest step from 84 dp, and no verse at all below that, so
 * it can never overlap a header, the silhouette or the navigation bar.
 *
 * @param onTopChange reports this block's top in root pixels, or 0 when hidden.
 */
@Composable
fun SettingsVerse(
    headerBottomPx: Float,
    onTopChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    val density = LocalDensity.current
    val view = LocalView.current
    var originY by remember { mutableFloatStateOf(Float.NaN) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { originY = it.boundsInRoot().top }
    ) {
        val rootHeight = view.height.toFloat()
        val origin = if (originY.isNaN()) 0f else originY
        val laidOut = rootHeight > 0f && headerBottomPx > 0f && !originY.isNaN()
        val headerBottom = with(density) {
            (headerBottomPx - origin).coerceAtLeast(0f).toDp()
        }
        // The scene's highest point: the top of the central finial's bead. The
        // silhouette is painted by the window-filling background, so its height
        // must come from the window, not from this (shorter) content box.
        val silhouetteTop = with(density) {
            (
                rootHeight * SCENE_TOP_FRACTION -
                    view.width * SCENE_BEAD_WIDTH_FRACTION -
                    origin
                ).toDp()
        }
        val topPad = headerBottom + NidaaDimens.meteorBandGap
        val bottomPad = maxHeight - silhouetteTop + NidaaDimens.settingsVerseClearance
        val space = (maxHeight - topPad - bottomPad).coerceAtLeast(0.dp)
        val show = laidOut && space >= NidaaDimens.settingsVerseMinSpace

        // A hidden verse must release its slot in the meteor band, and a state
        // write never happens during composition.
        LaunchedEffect(show) { if (!show) onTopChange(0f) }
        if (!show) return@BoxWithConstraints

        val verseSize = when {
            space >= NidaaDimens.settingsVerseFullSpace -> NidaaDimens.settingsVerseSize
            space >= NidaaDimens.settingsVerseSmallSpace -> NidaaDimens.settingsVerseSmallSize
            else -> NidaaDimens.settingsVerseTinySize
        }
        val sidePad = maxWidth * NidaaDimens.settingsVerseSideInsetFraction

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPad, bottom = bottomPad, start = sidePad, end = sidePad),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.settings_verse_an_nisa_103),
                fontFamily = AmiriBold,
                fontSize = verseSize,
                lineHeight = verseSize * NidaaDimens.settingsVerseLineHeightFactor,
                color = palette.accent.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    // beta24: the star field and the meteor keep clear of the
                    // verse through the same registry every other text uses.
                    .starTextExclusion(VERSE_EXCLUSION_KEY)
                    .onGloballyPositioned { coordinates ->
                        onTopChange(coordinates.boundsInRoot().top)
                    }
            )
            Spacer(Modifier.height(NidaaDimens.settingsVerseReferenceGap))
            Text(
                text = stringResource(R.string.settings_verse_an_nisa_103_reference),
                fontFamily = Tajawal,
                fontSize = NidaaDimens.settingsVerseReferenceSize,
                color = palette.accent.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
