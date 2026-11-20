package com.mawaqiti.plus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.ui.screens.installGuidanceRes
import com.nidaa.prayer.update.ApkInstaller
import com.nidaa.prayer.update.InstallGate
import com.nidaa.prayer.update.InstallOutcome
import com.nidaa.prayer.update.InstallStatusCode
import com.nidaa.prayer.update.OemFamily
import com.nidaa.prayer.update.UpdateNotifier
import com.nidaa.prayer.update.installGateFor
import com.nidaa.prayer.update.oemFamilyFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** beta20: install chain, permission gate, OEM guidance and deep links. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl")
class UpdateInstallHardeningTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ------------------------------------------------------- status mapping

    @Test
    fun `every package installer status maps to the documented outcome`() {
        assertEquals(InstallStatusCode.PENDING_USER_ACTION, PackageInstaller.STATUS_PENDING_USER_ACTION)
        assertEquals(InstallStatusCode.SUCCESS, PackageInstaller.STATUS_SUCCESS)
        assertEquals(InstallStatusCode.FAILURE, PackageInstaller.STATUS_FAILURE)
        assertEquals(InstallStatusCode.FAILURE_BLOCKED, PackageInstaller.STATUS_FAILURE_BLOCKED)
        assertEquals(InstallStatusCode.FAILURE_ABORTED, PackageInstaller.STATUS_FAILURE_ABORTED)
        assertEquals(InstallStatusCode.FAILURE_INVALID, PackageInstaller.STATUS_FAILURE_INVALID)
        assertEquals(InstallStatusCode.FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_CONFLICT)
        assertEquals(InstallStatusCode.FAILURE_STORAGE, PackageInstaller.STATUS_FAILURE_STORAGE)
        assertEquals(InstallStatusCode.FAILURE_INCOMPATIBLE, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE)
        assertEquals(InstallStatusCode.FAILURE_TIMEOUT, PackageInstaller.STATUS_FAILURE_TIMEOUT)

        assertEquals(InstallOutcome.PENDING_USER_ACTION, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_PENDING_USER_ACTION))
        assertEquals(InstallOutcome.SUCCESS, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_SUCCESS))
        assertEquals(InstallOutcome.BLOCKED, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_FAILURE_BLOCKED))
        assertEquals(InstallOutcome.ABORTED, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_FAILURE_ABORTED))
        assertEquals(InstallOutcome.INVALID, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_FAILURE_INVALID))
        assertEquals(InstallOutcome.CONFLICT, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_FAILURE_CONFLICT))
        assertEquals(InstallOutcome.STORAGE, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_FAILURE_STORAGE))
        assertEquals(InstallOutcome.INCOMPATIBLE, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE))
        assertEquals(InstallOutcome.TIMEOUT, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_FAILURE_TIMEOUT))
        assertEquals(InstallOutcome.FAILED, InstallOutcome.fromStatusCode(PackageInstaller.STATUS_FAILURE))
        assertEquals(InstallOutcome.UNKNOWN, InstallOutcome.fromStatusCode(999))
    }

    @Test
    fun `status messages are present and map to their Arabic resources`() {
        assertEquals(R.string.install_outcome_success, InstallOutcome.SUCCESS.messageRes)
        assertEquals(R.string.install_outcome_pending, InstallOutcome.PENDING_USER_ACTION.messageRes)
        assertEquals(R.string.install_outcome_blocked, InstallOutcome.BLOCKED.messageRes)
        assertEquals(R.string.install_outcome_aborted, InstallOutcome.ABORTED.messageRes)
        assertEquals(R.string.install_outcome_invalid, InstallOutcome.INVALID.messageRes)
        assertEquals(R.string.install_outcome_conflict, InstallOutcome.CONFLICT.messageRes)
        assertEquals(R.string.install_outcome_storage, InstallOutcome.STORAGE.messageRes)
        assertEquals(R.string.install_outcome_timeout, InstallOutcome.TIMEOUT.messageRes)
        assertEquals(R.string.install_outcome_incompatible, InstallOutcome.INCOMPATIBLE.messageRes)
        assertEquals(R.string.install_outcome_unknown, InstallOutcome.FAILED.messageRes)
        assertEquals(R.string.install_outcome_unknown, InstallOutcome.UNKNOWN.messageRes)
        assertEquals(R.string.install_outcome_unknown, InstallOutcome.START_FAILED.messageRes)

        InstallOutcome.entries.forEach { outcome ->
            assertTrue(outcome.name, context.getString(outcome.messageRes).isNotBlank())
        }
        assertNotEquals(
            context.getString(InstallOutcome.SUCCESS.messageRes),
            context.getString(InstallOutcome.BLOCKED.messageRes)
        )
    }

    // ---------------------------------------------------------- permission

    @Test
    fun `install gate only applies from api 26 and only when not granted`() {
        assertEquals(InstallGate.ALLOWED, installGateFor(24, canRequestPackageInstalls = false))
        assertEquals(InstallGate.ALLOWED, installGateFor(25, canRequestPackageInstalls = false))
        assertEquals(InstallGate.ALLOWED, installGateFor(26, canRequestPackageInstalls = true))
        assertEquals(InstallGate.NEEDS_PERMISSION, installGateFor(26, canRequestPackageInstalls = false))
        assertEquals(InstallGate.NEEDS_PERMISSION, installGateFor(36, canRequestPackageInstalls = false))
        assertEquals(InstallGate.ALLOWED, installGateFor(36, canRequestPackageInstalls = true))
    }

    @Test
    fun `permission intent targets this package's unknown-sources page`() {
        val intent = ApkInstaller.permissionIntent(context)
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    // -------------------------------------------------------------- intents

    @Test
    fun `legacy install intent has package archive type, read grant and new task`() {
        val uri = Uri.parse(
            "content://${context.packageName}.update.fileprovider/updates/nidaa-2.0.1.apk"
        )
        val intent = ApkInstaller.legacyInstallIntent(context, uri)
        assertEquals(Intent.ACTION_INSTALL_PACKAGE, intent.action)
        assertEquals(ApkInstaller.APK_MIME, intent.type)
        assertEquals(uri, intent.data)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertFalse(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    @Test
    fun `session status pending intent is mutable on api 31 plus`() {
        val sPlus = ApkInstaller.statusPendingIntentFlags(31)
        assertTrue(sPlus and android.app.PendingIntent.FLAG_UPDATE_CURRENT != 0)
        assertTrue(sPlus and android.app.PendingIntent.FLAG_MUTABLE != 0)

        val preS = ApkInstaller.statusPendingIntentFlags(30)
        assertTrue(preS and android.app.PendingIntent.FLAG_UPDATE_CURRENT != 0)
        assertEquals(0, preS and android.app.PendingIntent.FLAG_MUTABLE)
    }

    // ----------------------------------------------------------------- OEM

    @Test
    fun `oem family follows manufacturer and brand`() {
        assertEquals(OemFamily.HUAWEI_HONOR, oemFamilyFor("HUAWEI", "HUAWEI"))
        assertEquals(OemFamily.HUAWEI_HONOR, oemFamilyFor("HONOR", "HONOR"))
        assertEquals(OemFamily.XIAOMI, oemFamilyFor("Xiaomi", "Redmi"))
        assertEquals(OemFamily.XIAOMI, oemFamilyFor("Xiaomi", "POCO"))
        assertEquals(OemFamily.SAMSUNG, oemFamilyFor("samsung", "samsung"))
        assertEquals(OemFamily.GENERIC, oemFamilyFor("Google", "Pixel"))
        assertEquals(OemFamily.GENERIC, oemFamilyFor(null, null))
    }

    @Test
    fun `oem guidance picks a dedicated arabic string`() {
        val huawei = context.getString(installGuidanceRes(OemFamily.HUAWEI_HONOR))
        val xiaomi = context.getString(installGuidanceRes(OemFamily.XIAOMI))
        val samsung = context.getString(installGuidanceRes(OemFamily.SAMSUNG))
        val generic = context.getString(installGuidanceRes(OemFamily.GENERIC))
        assertEquals(4, setOf(huawei, xiaomi, samsung, generic).size)
        assertTrue(huawei.contains("Huawei"))
        assertTrue(xiaomi.contains("Xiaomi"))
        assertTrue(samsung.contains("Samsung"))
    }

    // ------------------------------------------------------------ deep link

    @Test
    fun `notification deep link opens the update screen with immutable flags`() {
        val intent = UpdateNotifier.openAppIntent(context)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.getBooleanExtra(MainActivity.EXTRA_OPEN_UPDATES, false))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)

        val flags = UpdateNotifier.contentPendingFlags()
        assertTrue(flags and android.app.PendingIntent.FLAG_IMMUTABLE != 0)
        assertTrue(flags and android.app.PendingIntent.FLAG_UPDATE_CURRENT != 0)
    }

    @Test
    fun `ready-to-install notification uses its own id`() {
        assertNotEquals(UpdateNotifier.NOTIFICATION_ID, UpdateNotifier.READY_NOTIFICATION_ID)
        assertTrue(
            context.getString(R.string.update_ready_notification_body, "v2.0.1")
                .contains("v2.0.1")
        )
    }
}
