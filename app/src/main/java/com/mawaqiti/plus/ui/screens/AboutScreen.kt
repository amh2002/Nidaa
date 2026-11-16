package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.BuildConfig
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.components.SettingsCard
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * beta10 About screen, opened by tapping the «نداء» wordmark on the main screen.
 *
 * It reuses the strings that already existed for the settings About card - the
 * brand name, `settings_version` fed by `BuildConfig.VERSION_NAME` (which comes
 * from `versionName` in build.gradle) and the existing credit line that carries
 * the creator's name - so no new identity strings were invented for it.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val palette = skyPalette

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        ScreenTopBar(
            title = stringResource(R.string.settings_about_title),
            palette = palette,
            onBack = onBack
        )

        SettingsCard {
            Text(
                text = stringResource(R.string.settings_about_brand),
                fontFamily = AmiriBold,
                fontSize = 34.sp,
                lineHeight = 44.sp,
                color = palette.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = palette.text.copy(alpha = 0.12f))
            Spacer(Modifier.height(12.dp))
            // Creator, from the existing `settings_about_credit` string.
            Text(
                text = stringResource(R.string.settings_about_credit),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}
