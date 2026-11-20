package com.nidaa.prayer.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

/**
 * Receives the PackageInstaller result for API 26+.
 *
 * `STATUS_PENDING_USER_ACTION` carries the system confirmation activity, which
 * must be started here. Every other status is mapped to an [InstallOutcome],
 * persisted in DataStore and surfaced in «التحديثات».
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val outcome = InstallOutcome.fromStatusCode(status)
        Log.i(
            ApkInstaller.TAG,
            "install status=$status outcome=${outcome.name} message=${statusMessage ?: "-"}"
        )

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT)
            }
            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirmation) }
                    .onFailure {
                        Log.w(ApkInstaller.TAG, "could not open system install dialog", it)
                    }
            }
            UpdateManager.onInstallPending(context)
            return
        }

        UpdateManager.onInstallStatus(context, status, statusMessage)
    }
}
