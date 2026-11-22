package com.mawaqiti.plus.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mawaqiti.plus.BuildConfig
import com.mawaqiti.plus.R
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.skyPalette

/**
 * Where the dialog's three buttons lead.
 *
 * Only data lives here - the visible labels are all in `strings.xml`, and the
 * surfaces/colours all come from [skyPalette]. The repository URL is the
 * `origin` remote of this project (https form, without `.git`).
 */
private const val DEVELOPER_EMAIL = "amh.2002@outlook.com"
private const val REPOSITORY_URL = "https://github.com/amh2002/Nidaa"
private const val WEBSITE_URL = "https://amh-store-nine.vercel.app/"

/**
 * beta24 (Task 4): the About dialog behind the «نداء» wordmark.
 *
 * beta10 opened a whole About *screen* (`TAB_ABOUT`) from the wordmark. The
 * owner asked for a popup instead, so this is one themed [Dialog] over the home
 * screen: the wordmark, the version read from [BuildConfig.VERSION_NAME] (never
 * a literal, so it follows `versionName` in build.gradle automatically) and the
 * three actions - email the developer, open the GitHub repository, open the
 * official website.
 *
 * The dialog sits on [com.mawaqiti.plus.ui.theme.SkyPalette.sheet], the opaque
 * sheet token of the active phase, so it reads as themed glass in all five
 * golden-night palettes without a single literal colour.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val palette = skyPalette
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                    text = stringResource(R.string.settings_about_brand),
                    fontFamily = AmiriBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 44.sp,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    // The version is an LTR token ("2.2.0-beta2") inside an RTL
                    // sentence: without an isolate the bidi algorithm reorders
                    // it to "beta2-2.2.0". LRI ... PDI pins the token's internal
                    // order while the sentence itself stays RTL.
                    text = stringResource(
                        R.string.settings_version,
                        "\u2066${BuildConfig.VERSION_NAME}\u2069"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.sheetMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))
                AboutAction(
                    labelRes = R.string.about_contact_developer,
                    onClick = { sendEmail(context) }
                )
                Spacer(Modifier.height(10.dp))
                AboutAction(
                    labelRes = R.string.about_github,
                    onClick = { openUrl(context, REPOSITORY_URL) }
                )
                Spacer(Modifier.height(10.dp))
                AboutAction(
                    labelRes = R.string.about_website,
                    onClick = { openUrl(context, WEBSITE_URL) }
                )
            }
        }
    }
}

/**
 * One full-width action row. Text-only: the label is centred, the whole row is
 * the tap target, and it is a plain [Text] in a `fillMaxWidth` box, so the RTL
 * layout mirrors with no forced alignment of its own.
 */
@Composable
private fun AboutAction(labelRes: Int, onClick: () -> Unit) {
    val palette = skyPalette
    val shape = RoundedCornerShape(14.dp)
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = palette.sheetText,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.chipFill)
            .border(1.dp, palette.chipBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    )
}

/**
 * `ACTION_SENDTO` with a `mailto:` URI - the only form that lands in a mail
 * composer instead of a generic share sheet. A device without a mail client
 * throws `ActivityNotFoundException`, which becomes the friendly toast the
 * owner asked for rather than a crash.
 */
private fun sendEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$DEVELOPER_EMAIL")).apply {
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.about_email_subject))
    }
    val opened = runCatching { context.startActivity(intent) }.isSuccess
    if (!opened) {
        Toast.makeText(context, R.string.about_no_email_app, Toast.LENGTH_SHORT).show()
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
