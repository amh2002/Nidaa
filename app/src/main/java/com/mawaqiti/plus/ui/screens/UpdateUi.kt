package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.components.GlassCard
import com.mawaqiti.plus.ui.components.SettingsCard
import com.mawaqiti.plus.ui.components.SettingsSmallButton
import com.mawaqiti.plus.ui.components.SettingsToggleRow
import com.mawaqiti.plus.ui.theme.skyPalette
import com.nidaa.prayer.update.FailureKind
import com.nidaa.prayer.update.ReleaseInfo
import com.nidaa.prayer.update.UpdateState

/** Display label for a release tag (`v1.0.0-beta7.95` -> `1.0.0-beta7.95`). */
private fun versionLabel(release: ReleaseInfo): String = release.tag.removePrefix("v")

/**
 * Main-screen banner. One line of information plus a «عرض» affordance; the
 * real controls live in Settings → التحديثات.
 */
@Composable
fun UpdateBanner(
    release: ReleaseInfo,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    GlassCard(
        modifier = modifier.testTag("update_banner"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(
                    R.string.settings_updates_banner,
                    versionLabel(release)
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_updates_view),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
        }
    }
}

/**
 * Settings → «التحديثات». A normal [SettingsCard] inside the single settings
 * list, so the framework rule (no stacked free-floating texts) holds.
 */
@Composable
fun UpdatesSettingsCard(
    enabled: Boolean,
    state: UpdateState,
    onEnabledChange: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleases: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    val release = when (state) {
        is UpdateState.Available -> state.release
        is UpdateState.Downloading -> state.release
        is UpdateState.ReadyToInstall -> state.release
        is UpdateState.Failed -> state.release
        else -> null
    }

    SettingsCard(
        title = stringResource(R.string.settings_updates_title),
        description = stringResource(R.string.settings_updates_desc),
        modifier = modifier.testTag("updates_card")
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.settings_updates_auto),
            description = stringResource(R.string.settings_updates_auto_desc),
            checked = enabled,
            onCheckedChange = onEnabledChange,
            toggleTestTag = "updates_auto_switch"
        )

        Spacer(Modifier.height(12.dp))

        when (state) {
            UpdateState.Checking -> StatusLine(
                text = stringResource(R.string.settings_updates_checking),
                color = palette.textMuted
            )

            UpdateState.UpToDate -> StatusLine(
                text = stringResource(R.string.settings_updates_up_to_date),
                color = palette.textMuted
            )

            is UpdateState.Available -> StatusLine(
                text = stringResource(
                    R.string.settings_updates_available,
                    versionLabel(state.release)
                ),
                color = palette.accent,
                bold = true
            )

            is UpdateState.Downloading -> {
                StatusLine(
                    text = if (state.progress >= 0) {
                        stringResource(
                            R.string.settings_updates_downloading,
                            state.progress
                        )
                    } else {
                        stringResource(R.string.settings_updates_downloading_unknown)
                    },
                    color = palette.textMuted
                )
                Spacer(Modifier.height(8.dp))
                if (state.progress >= 0) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("updates_progress")
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("updates_progress")
                    )
                }
            }

            is UpdateState.ReadyToInstall -> StatusLine(
                text = stringResource(R.string.settings_updates_verified),
                color = palette.accent,
                bold = true
            )

            is UpdateState.Failed -> StatusLine(
                text = stringResource(failureMessage(state.kind)),
                color = palette.accent,
                bold = false
            )

            UpdateState.Idle -> Unit
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state !is UpdateState.Checking && state !is UpdateState.Downloading) {
                SettingsSmallButton(
                    label = stringResource(R.string.settings_updates_check_now),
                    onClick = onCheckNow,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("updates_check_now")
                )
            }

            when (state) {
                is UpdateState.Available -> SettingsSmallButton(
                    label = stringResource(R.string.settings_updates_download),
                    onClick = onDownload,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("updates_download")
                )

                is UpdateState.ReadyToInstall -> SettingsSmallButton(
                    label = stringResource(R.string.settings_updates_install),
                    onClick = onInstall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("updates_install")
                )

                is UpdateState.Failed -> when (state.kind) {
                    FailureKind.NETWORK -> SettingsSmallButton(
                        label = stringResource(R.string.settings_updates_open_releases),
                        onClick = onOpenReleases,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("updates_open_releases")
                    )

                    FailureKind.CHECKSUM,
                    FailureKind.DOWNLOAD -> SettingsSmallButton(
                        label = stringResource(R.string.settings_updates_retry),
                        onClick = onDownload,
                        enabled = state.release != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("updates_download")
                    )

                    FailureKind.PERMISSION,
                    FailureKind.INSTALL -> SettingsSmallButton(
                        label = stringResource(R.string.settings_updates_install),
                        onClick = onInstall,
                        enabled = state.release != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("updates_install")
                    )
                }

                else -> Unit
            }
        }

        if (release != null && state is UpdateState.Failed) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = versionLabel(release),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
        }
    }
}

@Composable
private fun StatusLine(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    bold: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color
    )
}

private fun failureMessage(kind: FailureKind): Int = when (kind) {
    FailureKind.NETWORK -> R.string.settings_updates_error_network
    FailureKind.CHECKSUM -> R.string.settings_updates_error_checksum
    FailureKind.DOWNLOAD -> R.string.settings_updates_error_download
    FailureKind.PERMISSION -> R.string.settings_updates_error_permission
    FailureKind.INSTALL -> R.string.settings_updates_error_install
}
