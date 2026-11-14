package com.nidaa.prayer.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed interface InstallStart {
    data object Started : InstallStart
    data object NeedsPermission : InstallStart
    data class Failed(val message: String) : InstallStart
}

/**
 * APK installation without a storage permission.
 *
 *  - API 26+: PackageInstaller session, the platform's own install flow.
 *  - API 24/25: FileProvider + ACTION_VIEW with the package-archive MIME type.
 *
 * In both paths the APK lives in the app's private cache directory and has
 * already passed the SHA-256 check performed by [UpdateClient].
 */
object ApkInstaller {

    fun install(context: Context, apk: File): InstallStart {
        if (!apk.exists()) return InstallStart.Failed("APK is missing")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                InstallStart.NeedsPermission
            } else {
                installWithSession(context, apk)
            }
        } else {
            installLegacy(context, apk)
        }
    }

    fun openPermissionSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun installWithSession(context: Context, apk: File): InstallStart {
        return try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    apk.inputStream().use { input -> input.copyTo(output) }
                    session.fsync(output)
                }
                val callback = Intent(context, InstallResultReceiver::class.java)
                    .setPackage(context.packageName)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        0
                    })
                val pending = PendingIntent.getBroadcast(context, sessionId, callback, flags)
                session.commit(pending.intentSender)
            }
            InstallStart.Started
        } catch (error: Throwable) {
            InstallStart.Failed(error.message ?: "PackageInstaller failed")
        }
    }

    private fun installLegacy(context: Context, apk: File): InstallStart {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.update.fileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                )
            context.startActivity(intent)
            InstallStart.Started
        } catch (error: Throwable) {
            InstallStart.Failed(error.message ?: "Installer intent failed")
        }
    }
}
