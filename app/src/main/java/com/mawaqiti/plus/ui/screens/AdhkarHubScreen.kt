package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * beta32 (Task 1): the «الأذكار» hub - the screen behind the dock's second
 * destination.
 *
 * It is one scrollable list of the thirteen section cards in
 * [AdhkarCatalog.sections] order. A card carries only its section title: no
 * counts, no digits, no decorative icons that would compete with the app's
 * existing iconography - just the title, a hairline card border and the
 * auto-mirrored "forward" chevron, which points left in Arabic RTL and right in
 * English without a second asset.
 *
 * Every word comes from `strings.xml`, every colour from [skyPalette], so all
 * five golden-night phases are covered without a literal in this file.
 */
@Composable
internal fun AdhkarHubScreen(onOpenSection: (AdhkarSection) -> Unit) {
    val palette = skyPalette

    Box(modifier = Modifier.fillMaxSize().testTag("adhkar_hub")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "hub_bar") {
                // No back arrow: the hub is a dock destination, so back leaves
                // the tab exactly like it does from Home or Qibla.
                ScreenTopBar(
                    title = stringResource(R.string.adhkar_title),
                    palette = palette,
                    onBack = null
                )
            }

            items(AdhkarCatalog.sections, key = { it.stem }) { section ->
                SectionCard(section = section, onClick = { onOpenSection(section) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(section: AdhkarSection, onClick: () -> Unit) {
    val palette = skyPalette
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("adhkar_section_${section.stem}"),
        shape = shape,
        color = palette.cardFill,
        contentColor = palette.text,
        border = BorderStroke(1.dp, palette.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(section.titleRes),
                    fontFamily = AmiriBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 32.sp,
                    color = palette.accent,
                    textAlign = TextAlign.Start
                )
            }
            Spacer(Modifier.size(10.dp))
            // Auto-mirrored: a left-pointing "forward" arrow in Arabic.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
