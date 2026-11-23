package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * beta30 (Task 3): the lightweight «الأذكار» screen.
 *
 * Two sections - «بعد الأذان» and «بعد الصلاة» - and nothing else: one
 * scrollable column, no network, no new dependency, no navigation library. It
 * is drawn as a full-size overlay by [com.mawaqiti.plus.MawaqitiApp], so the
 * dock, the home and the widget are untouched.
 *
 * Every line of Arabic comes from `strings.xml`; this file holds no literal of
 * the adhkar. The background is the active golden-night sheet token, never a
 * colour literal, so the screen follows the five palettes like the rest of the
 * app.
 */
@Composable
fun AdhkarScreen(onBack: () -> Unit) {
    val palette = skyPalette

    // The overlay's own backdrop is the palette's vertical gradient, so the
    // full-screen sheet is opaque over whatever it covers while still being
    // painted only with theme tokens.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette.gradient))
            .testTag("adhkar_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenTopBar(
                title = stringResource(R.string.adhkar_title),
                palette = palette,
                onBack = onBack
            )

            SectionTitle(stringResource(R.string.adhkar_section_after_adhan))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_after_adhan_dua),
                    testTag = "adhkar_after_adhan_dua"
                )
                Spacer(Modifier.height(8.dp))
                Caption(stringResource(R.string.adhkar_after_adhan_caption))
            }

            Spacer(Modifier.height(18.dp))

            SectionTitle(stringResource(R.string.adhkar_section_after_prayer))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_istighfar),
                    testTag = "adhkar_istighfar"
                )
                Spacer(Modifier.height(6.dp))
                Caption(stringResource(R.string.adhkar_istighfar_caption))
            }

            Spacer(Modifier.height(10.dp))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_after_prayer_salam),
                    testTag = "adhkar_salam"
                )
            }

            Spacer(Modifier.height(10.dp))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_after_prayer_tahlil),
                    testTag = "adhkar_tahlil"
                )
            }

            Spacer(Modifier.height(10.dp))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_after_prayer_tasbih_dua),
                    testTag = "adhkar_tasbih_dua"
                )
            }

            Spacer(Modifier.height(10.dp))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_tasbih_subhan),
                    testTag = "adhkar_tasbih_subhan"
                )
                Spacer(Modifier.height(6.dp))
                Caption(stringResource(R.string.adhkar_tasbih_caption))
            }

            Spacer(Modifier.height(10.dp))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_tasbih_hamd),
                    testTag = "adhkar_tasbih_hamd"
                )
                Spacer(Modifier.height(6.dp))
                Caption(stringResource(R.string.adhkar_tasbih_caption))
            }

            Spacer(Modifier.height(10.dp))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_tasbih_akbar),
                    testTag = "adhkar_tasbih_akbar"
                )
                Spacer(Modifier.height(6.dp))
                Caption(stringResource(R.string.adhkar_tasbih_caption))
            }

            Spacer(Modifier.height(10.dp))

            DhikrCard {
                DhikrText(
                    text = stringResource(R.string.adhkar_tasbih_completion),
                    testTag = "adhkar_tasbih_completion"
                )
                Spacer(Modifier.height(8.dp))
                Caption(stringResource(R.string.adhkar_tasbih_completion_caption))
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val palette = skyPalette
    Text(
        text = text,
        fontFamily = AmiriBold,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = palette.accent,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp)
    )
}

/**
 * One dhikr card on the palette's card fill with its border token. Sized by its
 * content only, so a long supplication grows the card instead of clipping it.
 */
@Composable
private fun DhikrCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val palette = skyPalette
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = palette.cardFill,
        contentColor = palette.text,
        border = BorderStroke(1.dp, palette.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            content = content
        )
    }
}

@Composable
private fun DhikrText(text: String, testTag: String) {
    val palette = skyPalette
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 18.sp,
        lineHeight = 32.sp,
        color = palette.text,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun Caption(text: String) {
    val palette = skyPalette
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = palette.textMuted
    )
}
