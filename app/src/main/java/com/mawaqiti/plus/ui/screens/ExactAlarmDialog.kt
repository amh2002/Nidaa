package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * beta30 (Task 1): the exact-alarm rationale, on golden-night tokens.
 *
 * Shown on API 31+ only, and only while `canScheduleExactAlarms()` is false.
 * «فتح الإعدادات» deep-links to this package's entry on the system
 * `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` page; returning from it re-reads the
 * grant, reschedules the rolling plan and lets the dialog disappear by itself.
 *
 * Colours come from [skyPalette] like the About dialog, so there is no literal
 * colour and no second visual language.
 */
@Composable
fun ExactAlarmDialog(onAllow: () -> Unit, onDismiss: () -> Unit) {
    val palette = skyPalette
    val shape = RoundedCornerShape(26.dp)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exact_alarm_dialog"),
            shape = shape,
            color = palette.sheet,
            contentColor = palette.sheetText,
            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.exact_alarm_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.exact_alarm_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.sheetText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))
                DialogButton(
                    label = stringResource(R.string.exact_alarm_dialog_allow),
                    onClick = onAllow,
                    emphasized = true,
                    testTag = "exact_alarm_allow"
                )
                Spacer(Modifier.height(10.dp))
                DialogButton(
                    label = stringResource(R.string.exact_alarm_dialog_later),
                    onClick = onDismiss,
                    emphasized = false,
                    testTag = "exact_alarm_later"
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    onClick: () -> Unit,
    emphasized: Boolean,
    testTag: String
) {
    val palette = skyPalette
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (emphasized) palette.accent else palette.chipFill)
            .border(
                width = 1.dp,
                color = if (emphasized) palette.accent else palette.chipBorder,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasized) palette.sheet else palette.sheetText,
            textAlign = TextAlign.Center
        )
    }
}
