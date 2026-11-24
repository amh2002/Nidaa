package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * beta32 (Task 3): one adhkar section, opened from the hub.
 *
 * A back-to-hub top bar and a [LazyColumn] of dhikr cards. Each card is the
 * item title as a small accent label, the full text - verbatim, heavy tashkeel
 * included - at 1.78x line height so the diacritics never collide or clip at
 * any font scale, and, when the dhikr is repeated, a word-based chip in the
 * accent tint (`مرتان`, `ثلاث مرات`, ...). There is no digit anywhere on this
 * screen, and the numerals toggle therefore cannot reach it.
 *
 * The text is never truncated: no `maxLines`, no ellipsis, the card grows with
 * its content and wraps right-to-left by the layout direction. Colours are the
 * palette's card/accent/text tokens only.
 */
@Composable
internal fun AdhkarSectionScreen(section: AdhkarSection, onBack: () -> Unit) {
    val palette = skyPalette

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("adhkar_section_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "section_bar") {
                ScreenTopBar(
                    title = stringResource(section.titleRes),
                    palette = palette,
                    onBack = onBack
                )
            }

            items(section.items, key = { it.textRes }) { item ->
                DhikrCard(item = item)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun DhikrCard(item: AdhkarItem) {
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
            verticalArrangement = Arrangement.Top
        ) {
            // The item's own label: small, accent, clearly above the body.
            Text(
                text = stringResource(item.titleRes),
                fontFamily = AmiriBold,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 28.sp,
                color = palette.accent,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            // The dhikr itself. 18 sp on a 32 sp line (1.78x) so the marks of
            // fully vocalized Arabic sit clear of the line above and below.
            Text(
                text = stringResource(item.textRes),
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                lineHeight = 32.sp,
                color = palette.text,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            if (item.repeatRes != null) {
                Spacer(Modifier.height(10.dp))
                RepetitionChip(text = stringResource(item.repeatRes))
            }
        }
    }
}

/** `مرتان`, `ثلاث مرات`, ... - an outlined chip in the accent tint, no digit. */
@Composable
private fun RepetitionChip(text: String) {
    val palette = skyPalette
    Surface(
        modifier = Modifier.widthIn(min = 0.dp),
        shape = RoundedCornerShape(50),
        color = palette.cardFill,
        contentColor = palette.accent,
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.55f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
