package com.nidaa.prayer.update

import com.mawaqiti.plus.R
import java.util.Locale

/**
 * Pure install diagnostics: no Android framework calls, so every rule here is
 * covered by JVM unit tests. The values mirror
 * `android.content.pm.PackageInstaller.STATUS_*` exactly.
 */
object InstallStatusCode {
    const val PENDING_USER_ACTION = -1
    const val SUCCESS = 0
    const val FAILURE = 1
    const val FAILURE_BLOCKED = 2
    const val FAILURE_ABORTED = 3
    const val FAILURE_INVALID = 4
    const val FAILURE_CONFLICT = 5
    const val FAILURE_STORAGE = 6
    const val FAILURE_INCOMPATIBLE = 7
    const val FAILURE_TIMEOUT = 8
}

/**
 * Normalised result of one install attempt. [messageRes] is the Arabic
 * explanation shown to the owner and persisted (by name) in DataStore.
 */
enum class InstallOutcome(val messageRes: Int) {
    SUCCESS(R.string.install_outcome_success),
    PENDING_USER_ACTION(R.string.install_outcome_pending),
    ABORTED(R.string.install_outcome_aborted),
    BLOCKED(R.string.install_outcome_blocked),
    INVALID(R.string.install_outcome_invalid),
    CONFLICT(R.string.install_outcome_conflict),
    STORAGE(R.string.install_outcome_storage),
    TIMEOUT(R.string.install_outcome_timeout),
    INCOMPATIBLE(R.string.install_outcome_incompatible),
    FAILED(R.string.install_outcome_unknown),
    UNKNOWN(R.string.install_outcome_unknown),
    START_FAILED(R.string.install_outcome_unknown),

    /**
     * beta23: the specific MIUI/HyperOS block. Shown and persisted in
     * «آخر محاولة تثبيت» with the exact steps that unblock it, instead of the
     * generic "the system prevented the install".
     */
    MIUI_RESTRICTED(R.string.install_outcome_xiaomi_restricted);

    companion object {
        /**
         * Maps a PackageInstaller status to a stable outcome.
         *
         * On MIUI a blocked session is the vendor's user-restriction rule, not
         * a generic block, so it gets its own actionable message.
         */
        fun forStatus(code: Int, miui: Boolean): InstallOutcome {
            val base = fromStatusCode(code)
            return if (miui && (base == BLOCKED || base == FAILED)) MIUI_RESTRICTED else base
        }

        /** Maps the PackageInstaller status int to a stable outcome. */
        fun fromStatusCode(code: Int): InstallOutcome = when (code) {
            InstallStatusCode.SUCCESS -> SUCCESS
            InstallStatusCode.PENDING_USER_ACTION -> PENDING_USER_ACTION
            InstallStatusCode.FAILURE_BLOCKED -> BLOCKED
            InstallStatusCode.FAILURE_ABORTED -> ABORTED
            InstallStatusCode.FAILURE_INVALID -> INVALID
            InstallStatusCode.FAILURE_CONFLICT -> CONFLICT
            InstallStatusCode.FAILURE_STORAGE -> STORAGE
            InstallStatusCode.FAILURE_INCOMPATIBLE -> INCOMPATIBLE
            InstallStatusCode.FAILURE_TIMEOUT -> TIMEOUT
            InstallStatusCode.FAILURE -> FAILED
            else -> UNKNOWN
        }
    }
}

/** Whether the platform install flow may be started right now. */
enum class InstallGate { ALLOWED, NEEDS_PERMISSION }

/**
 * The `REQUEST_INSTALL_PACKAGES` runtime gate only exists from API 26. On
 * API 24/25 the legacy installer intent is always allowed.
 */
fun installGateFor(sdkInt: Int, canRequestPackageInstalls: Boolean): InstallGate =
    if (sdkInt >= 26 && !canRequestPackageInstalls) {
        InstallGate.NEEDS_PERMISSION
    } else {
        InstallGate.ALLOWED
    }

/** OEM family used to pick the Arabic "why did the install not start" text. */
enum class OemFamily { HUAWEI_HONOR, XIAOMI, SAMSUNG, GENERIC }

fun oemFamilyFor(manufacturer: String?, brand: String?): OemFamily {
    val make = manufacturer.orEmpty().lowercase(Locale.ROOT)
    val model = brand.orEmpty().lowercase(Locale.ROOT)

    fun anyOf(vararg needles: String): Boolean =
        needles.any { make.contains(it) || model.contains(it) }

    return when {
        anyOf("huawei", "honor") -> OemFamily.HUAWEI_HONOR
        anyOf("xiaomi", "redmi", "poco") -> OemFamily.XIAOMI
        anyOf("samsung") -> OemFamily.SAMSUNG
        else -> OemFamily.GENERIC
    }
}

/** One persisted install attempt. [message] is the raw system blocker text. */
data class InstallAttempt(
    val timestampMs: Long,
    val statusCode: Int,
    val outcome: InstallOutcome,
    val message: String?
) {
    val succeeded: Boolean get() = outcome == InstallOutcome.SUCCESS
}
