package com.mawaqiti.plus

import android.content.Intent
import android.net.Uri
import com.nidaa.prayer.update.ApkInstaller
import com.nidaa.prayer.update.InstallOutcome
import com.nidaa.prayer.update.InstallStatusCode
import com.nidaa.prayer.update.MiuiDevices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta23: the MIUI / HyperOS install path.
 *
 * MIUI rejects a third-party `PackageInstaller` session with
 * `INSTALL_FAILED_USER_RESTRICTED`, so the app must (a) recognise MIUI from its
 * build properties, (b) use `ACTION_INSTALL_PACKAGE` as the primary path there,
 * and (c) report the Xiaomi-specific remedy instead of a generic block.
 */
// The app targets SDK 36; Robolectric 4.14 tops out at 35, so pin the sandbox.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MiuiInstallTest {

    private fun props(vararg pairs: Pair<String, String>): (String) -> String? {
        val map = pairs.toMap()
        return { key -> map[key] }
    }

    @Test
    fun `detects miui from its build properties`() {
        assertTrue(
            "ro.miui.ui.version.name",
            MiuiDevices.isMiui(props("ro.miui.ui.version.name" to "V14.0.6.0"))
        )
        assertTrue(
            "ro.miui.ui.version.code",
            MiuiDevices.isMiui(props("ro.miui.ui.version.code" to "14"))
        )
        assertTrue(
            "HyperOS",
            MiuiDevices.isMiui(props("ro.mi.os.version.name" to "OS1.0.2.0"))
        )
        assertFalse(
            "stock AOSP device",
            MiuiDevices.isMiui(props("ro.build.version.sdk" to "34"))
        )
        // An empty property value is not a MIUI marker.
        assertFalse(
            "empty marker",
            MiuiDevices.isMiui(props("ro.miui.ui.version.name" to ""))
        )
    }

    @Test
    fun `vendor fallback covers xiaomi redmi and poco`() {
        assertTrue(MiuiDevices.isMiuiVendor("Xiaomi", "Xiaomi"))
        assertTrue(MiuiDevices.isMiuiVendor("Xiaomi", "Redmi"))
        assertTrue(MiuiDevices.isMiuiVendor("Xiaomi", "POCO"))
        assertFalse(MiuiDevices.isMiuiVendor("samsung", "SM-G991B"))
        assertFalse(MiuiDevices.isMiuiVendor(null, null))
    }

    @Test
    fun `the system installer intent is the primary miui path`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://com.nidaa.prayer.update.fileprovider/cache/nidaa.apk")
        val intent = ApkInstaller.systemInstallerIntent(context, uri)

        assertEquals(Intent.ACTION_INSTALL_PACKAGE, intent.action)
        assertEquals(ApkInstaller.APK_MIME, intent.type)
        assertEquals(uri, intent.data)
        assertTrue(
            "the installer needs a read grant for the FileProvider URI",
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
        assertTrue(
            "a broadcast receiver starts this from a non-activity context",
            intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
        )
    }

    @Test
    fun `a blocked install on miui reports the xiaomi remedy`() {
        assertEquals(
            InstallOutcome.MIUI_RESTRICTED,
            InstallOutcome.forStatus(InstallStatusCode.FAILURE_BLOCKED, miui = true)
        )
        assertEquals(
            InstallOutcome.MIUI_RESTRICTED,
            InstallOutcome.forStatus(InstallStatusCode.FAILURE, miui = true)
        )
        // Non-MIUI devices keep exactly the previous behaviour.
        assertEquals(
            InstallOutcome.BLOCKED,
            InstallOutcome.forStatus(InstallStatusCode.FAILURE_BLOCKED, miui = false)
        )
        assertEquals(
            InstallOutcome.FAILED,
            InstallOutcome.forStatus(InstallStatusCode.FAILURE, miui = false)
        )
        // Success and pending are never rewritten.
        assertEquals(
            InstallOutcome.SUCCESS,
            InstallOutcome.forStatus(InstallStatusCode.SUCCESS, miui = true)
        )
        assertEquals(R.string.install_outcome_xiaomi_restricted, InstallOutcome.MIUI_RESTRICTED.messageRes)
    }
}
