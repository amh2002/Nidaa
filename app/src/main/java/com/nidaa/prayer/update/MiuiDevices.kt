package com.nidaa.prayer.update

import android.os.Build
import java.util.Locale

/**
 * beta23: MIUI / HyperOS detection.
 *
 * Xiaomi, Redmi and POCO phones ship an installer service that rejects a
 * `PackageInstaller` session opened by a third-party app with
 * `INSTALL_FAILED_USER_RESTRICTED` (the session dies before the confirmation
 * dialog, so nothing ever reaches [InstallResultReceiver]). The system
 * installer intent - `ACTION_INSTALL_PACKAGE` + a granted FileProvider URI -
 * goes through MIUI's own install flow and is the only reliable path there.
 *
 * Detection is property-based, exactly as the task specifies: MIUI and HyperOS
 * both expose `ro.miui.ui.version.*` (HyperOS adds `ro.mi.os.version.*`).
 *
 * [isMiui] is a pure function over a property lookup so the rule is unit
 * testable on the JVM; [isMiuiOnDevice] supplies the real `SystemProperties`
 * reader through reflection (`android.os.SystemProperties` is not public API).
 */
object MiuiDevices {

    /** Every MIUI/HyperOS build exposes at least one of these. */
    val MARKERS = listOf(
        "ro.miui.ui.version.name",
        "ro.miui.ui.version.code",
        "ro.mi.os.version.name"
    )

    fun isMiui(readProperty: (String) -> String?): Boolean =
        MARKERS.any { !readProperty(it).isNullOrBlank() }

    /**
     * Vendor fallback for the case where the property bridge is unavailable
     * (some hardened ROMs block the reflection). A Xiaomi-built device with no
     * readable MIUI property is still treated as MIUI: the system-installer path
     * works everywhere, so a false positive costs nothing.
     */
    fun isMiuiVendor(manufacturer: String?, brand: String?): Boolean {
        fun matches(value: String?): Boolean {
            val text = value.orEmpty().lowercase(Locale.ROOT)
            return text.contains("xiaomi") || text.contains("redmi") || text.contains("poco")
        }
        return matches(manufacturer) || matches(brand)
    }

    fun isMiuiOnDevice(): Boolean {
        val props = systemPropertiesReader()
        if (props != null && isMiui(props)) return true
        // No readable property bridge: fall back to the vendor string.
        if (props == null) return isMiuiVendor(Build.MANUFACTURER, Build.BRAND)
        return false
    }

    /** `android.os.SystemProperties.get(String, String)`, or null when blocked. */
    fun systemPropertiesReader(): ((String) -> String?)? = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java, String::class.java)
        val reader: (String) -> String? = { key ->
            runCatching { get.invoke(null, key, "") as? String }.getOrNull()
        }
        reader
    } catch (_: Throwable) {
        null
    }
}
