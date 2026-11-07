package com.mawaqiti.plus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.engine.CalculationMethod

@Composable
fun isArabicLocale(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.locales.size() > 0 && configuration.locales[0].language == "ar"
}

@Composable
fun cityDisplayName(settings: AppSettings): String =
    if (isArabicLocale()) settings.cityNameAr else settings.cityNameEn

@Composable
fun methodLabel(method: CalculationMethod): String = when (method) {
    CalculationMethod.MWL -> stringResource(R.string.method_mwl)
    CalculationMethod.ISNA -> stringResource(R.string.method_isna)
    CalculationMethod.EGYPT -> stringResource(R.string.method_egypt)
    CalculationMethod.UMM_AL_QURA -> stringResource(R.string.method_umm_al_qura)
    CalculationMethod.KARACHI -> stringResource(R.string.method_karachi)
    CalculationMethod.CUSTOM -> stringResource(R.string.method_custom)
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
