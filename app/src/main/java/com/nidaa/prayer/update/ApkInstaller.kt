package com.nidaa.prayer.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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
 *  - API 26+: PackageInstaller session with the APK size declared up front,
 *    a flushed + fsynced stream and a mutable status PendingIntent (API 31+).
 *    The session is never abandoned after `commit`, so every STATUS_* outcome
 *    can reach [InstallResultReceiver].
 *  - API 24/25: FileProvider + ACTION_INSTALL_PACKAGE with the package-archive
 *    MIME type, read grant and NEW_TASK.
 *
 * In both paths the APK lives in the app's private cache directory and has
 * already passed the SHA-256 check performed by [UpdateClient].
 */
object ApkInstaller {

    const val TAG = "NidaaUpdate"
    const val APK_MIME = "application/vnd.android.package-archive"
    const val FILE_PROVIDER_SUFFIX = ".update.fileprovider"

    /** `REQUEST_INSTALL_PACKAGES` gate; API 24/25 are always allowed. */
    fun canInstall(context: Context): Boolean {
        val sdk = Build.VERSION.SDK_INT
        val canRequest = sdk < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
        return installGateFor(sdk, canRequest) == InstallGate.ALLOWED
    }

    fun install(context: Context, apk: File): InstallStart {
        if (!apk.exists() || !apk.isFile) return InstallStart.Failed("APK is missing")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!canInstall(context)) {
                InstallStart.NeedsPermission
            } else {
                installOnThisDevice(context, apk)
            }
        } else {
            installLegacy(context, apk)
        }
    }

    /**
     * beta23: the OEM-dependent choice of install path.
     *
     * MIUI / HyperOS kill a third-party `PackageInstaller` session with
     * `INSTALL_FAILED_USER_RESTRICTED`, so the system installer intent
     * (`ACTION_INSTALL_PACKAGE` + `FLAG_GRANT_READ_URI_PERMISSION`) is the
     * PRIMARY path there. Every other OEM keeps the session path unchanged.
     */
    fun installOnThisDevice(context: Context, apk: File): InstallStart =
        if (MiuiDevices.isMiuiOnDevice()) {
            Log.i(TAG, "MIUI/HyperOS detected: using the system installer")
            installWithSystemInstaller(context, apk)
        } else {
            installWithSession(context, apk)
        }

    /**
     * Forces the system-installer path. Used as the MIUI fallback after a
     * `STATUS_FAILURE_BLOCKED` result or a `SecurityException`, so a blocked
     * session still reaches MIUI's own confirmation dialog.
     */
    fun retryWithSystemInstaller(context: Context, apk: File): InstallStart =
        installWithSystemInstaller(context, apk)

    private fun installWithSystemInstaller(context: Context, apk: File): InstallStart = try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}$FILE_PROVIDER_SUFFIX",
            apk
        )
        context.startActivity(systemInstallerIntent(context, uri))
        InstallStart.Started
    } catch (error: Throwable) {
        Log.w(TAG, "system installer intent failed", error)
        InstallStart.Failed(error.message ?: "Installer intent failed")
    }

    /** The `ACTION_INSTALL_PACKAGE` intent; shared with the API 24/25 path. */
    fun systemInstallerIntent(context: Context, uri: Uri): Intent =
        Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, APK_MIME)
            .addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            )

    /** Status PendingIntent flags: UPDATE_CURRENT always, MUTABLE on API 31+. */
    fun statusPendingIntentFlags(sdkInt: Int): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (sdkInt >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    /** Intent that opens this app's "install unknown apps" system page. */
    fun permissionIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun openPermissionSettings(context: Context) {
        runCatching { context.startActivity(permissionIntent(context)) }
            .onFailure { Log.w(TAG, "could not open install-permission settings", it) }
    }

    /**
     * API 24/25 install intent. Exposed so the flags, action, MIME type and
     * read grant are covered by unit tests without touching the filesystem.
     */
    fun legacyInstallIntent(context: Context, uri: Uri): Intent =
        systemInstallerIntent(context, uri)

    private fun installWithSession(context: Context, apk: File): InstallStart {
        val installer = context.packageManager.packageInstaller
        var sessionId = -1
        var committed = false
        return try {
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                // Without the declared size some OEM installers reject the
                // session at commit time with STATUS_FAILURE_INVALID.
                setSize(apk.length())
            }
            sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    apk.inputStream().use { input -> input.copyTo(output) }
                    output.flush()
                    session.fsync(output)
                }
                val callback = Intent(context, InstallResultReceiver::class.java)
                    .setPackage(context.packageName)
                val pending = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callback,
                    statusPendingIntentFlags(Build.VERSION.SDK_INT)
                )
                session.commit(pending.intentSender)
                committed = true
            }
            Log.i(TAG, "PackageInstaller session $sessionId committed")
            InstallStart.Started
        } catch (error: Throwable) {
            // Only abandon when commit never happened: after commit the
            // session belongs to the platform and must produce a status.
            if (!committed && sessionId >= 0) {
                runCatching { installer.abandonSession(sessionId) }
            }
            // beta23: MIUI reports the block as a SecurityException before any
            // session exists; hand the same APK to the system installer instead
            // of failing the update.
            if (error is SecurityException && MiuiDevices.isMiuiOnDevice()) {
                Log.w(TAG, "MIUI blocked the session; rerouting to the system installer", error)
                return installWithSystemInstaller(context, apk)
            }
            Log.w(TAG, "PackageInstaller session failed", error)
            InstallStart.Failed(error.message ?: "PackageInstaller failed")
        }
    }

    private fun installLegacy(context: Context, apk: File): InstallStart = try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}$FILE_PROVIDER_SUFFIX",
            apk
        )
        context.startActivity(legacyInstallIntent(context, uri))
        InstallStart.Started
    } catch (error: Throwable) {
        Log.w(TAG, "legacy installer intent failed", error)
        InstallStart.Failed(error.message ?: "Installer intent failed")
    }
}
