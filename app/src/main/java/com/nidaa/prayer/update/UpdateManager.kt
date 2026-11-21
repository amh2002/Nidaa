package com.nidaa.prayer.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.mawaqiti.plus.BuildConfig
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.SettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Process-wide update state machine.
 *
 * This object never touches the engine, the widgets or the alarm pipeline; it
 * only knows about releases, the app cache, WorkManager and the installer.
 * Failures are exposed as state (manual action) or swallowed (automatic check),
 * never as a crash. Install is only ever started from a user tap in the
 * foreground: background work stops at [UpdateState.ReadyToInstall] and posts a
 * notification.
 */
object UpdateManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Release tags already retried through the MIUI system-installer path. */
    private val miuiRerouted: MutableSet<String> =
        java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Increments whenever the owner must be shown the permission explainer. */
    private val _permissionPrompt = MutableStateFlow(0L)
    val permissionPrompt: StateFlow<Long> = _permissionPrompt.asStateFlow()

    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    val currentVersion: SemanticVersion?
        get() = SemanticVersion.parse(BuildConfig.VERSION_NAME)

    /** Restores a release found by the background worker, so the banner shows. */
    fun loadPending(context: Context) {
        val app = context.applicationContext
        scope.launch {
            val pending = runCatching { UpdatesStore(app).pendingRelease() }.getOrNull()
                ?: return@launch
            val current = currentVersion ?: return@launch
            if (pending.version > current && _state.value is UpdateState.Idle) {
                _state.value = UpdateState.Available(pending)
            }
        }
    }

    /**
     * Network check. [manual] is true only for the «تحقق الآن» button: a manual
     * failure becomes visible state and offers the browser fallback, while an
     * automatic failure stays silent.
     */
    fun checkNow(context: Context, manual: Boolean = true) {
        val app = context.applicationContext
        checkJob?.cancel()
        _state.value = UpdateState.Checking
        checkJob = scope.launch {
            try {
                val release = UpdateClient().latestRelease()
                val store = UpdatesStore(app)
                store.markAutoChecked(System.currentTimeMillis())
                val current = currentVersion
                if (current != null && release.version > current) {
                    store.setPending(release)
                    _state.value = UpdateState.Available(release)
                } else {
                    store.setPending(null)
                    _state.value = UpdateState.UpToDate
                }
            } catch (error: Throwable) {
                if (manual) {
                    _state.value = UpdateState.Failed(
                        release = (_state.value as? UpdateState.Available)?.release,
                        kind = kindOf(error)
                    )
                } else {
                    _state.value = UpdateState.Idle
                }
            }
        }
    }

    /** Downloads and verifies the APK; install is only offered after a match. */
    fun download(context: Context) {
        val release = when (val current = _state.value) {
            is UpdateState.Available -> current.release
            is UpdateState.Failed -> current.release
            else -> null
        } ?: return
        if (release.sha256 == null) {
            _state.value = UpdateState.Failed(release, FailureKind.CHECKSUM)
            return
        }

        val app = context.applicationContext
        downloadJob?.cancel()
        _state.value = UpdateState.Downloading(release, 0)
        downloadJob = scope.launch {
            val target = UpdateStorage.apkFile(app, release)
            try {
                UpdateClient().download(release, target) { progress ->
                    _state.value = UpdateState.Downloading(release, progress)
                }
                _state.value = UpdateState.ReadyToInstall(release)
                // Download completion never installs. If this ran in the
                // background (or the app is left), the notification is the way
                // back into the foreground install UI.
                UpdateNotifier.notifyReady(app, release, languageOf(app))
            } catch (error: Throwable) {
                target.delete()
                _state.value = UpdateState.Failed(release, kindOf(error))
            }
        }
    }

    /**
     * Foreground entry point (settings install button). The caller must pass a
     * foreground context: [UpdateWorker] and the receiver never call this.
     */
    fun install(context: Context) {
        val release = when (val current = _state.value) {
            is UpdateState.ReadyToInstall -> current.release
            is UpdateState.NeedsPermission -> current.release
            is UpdateState.Failed -> current.release
            is UpdateState.Available -> current.release
            else -> null
        } ?: return

        if (!ApkInstaller.canInstall(context)) {
            requestPermission(release)
            return
        }
        startInstall(context, release)
    }

    /** Called from `Activity.onResume`: continue after the system settings page. */
    fun resumeAfterPermission(context: Context) {
        val release = when (val current = _state.value) {
            is UpdateState.NeedsPermission -> current.release
            else -> null
        } ?: return
        if (!ApkInstaller.canInstall(context)) return
        startInstall(context, release)
    }

    fun openPermissionSettings(context: Context) {
        ApkInstaller.openPermissionSettings(context)
    }

    private fun requestPermission(release: ReleaseInfo) {
        _state.value = UpdateState.NeedsPermission(release)
        _permissionPrompt.value = _permissionPrompt.value + 1
    }

    private fun startInstall(app: Context, release: ReleaseInfo) {
        val apk = UpdateStorage.apkFile(app, release)
        if (!apk.exists()) {
            recordAttempt(app, InstallStatusCode.FAILURE, InstallOutcome.START_FAILED, "APK missing")
            _state.value = UpdateState.Failed(release, FailureKind.DOWNLOAD)
            return
        }
        _state.value = UpdateState.Installing(release)
        when (val started = ApkInstaller.install(app, apk)) {
            InstallStart.Started -> Log.i(ApkInstaller.TAG, "installer started for ${release.tag}")

            InstallStart.NeedsPermission -> requestPermission(release)

            is InstallStart.Failed -> {
                Log.w(ApkInstaller.TAG, "installer did not start: ${started.message}")
                recordAttempt(
                    app,
                    InstallStatusCode.FAILURE,
                    InstallOutcome.START_FAILED,
                    started.message
                )
                _state.value = UpdateState.Failed(release, FailureKind.INSTALL)
            }
        }
    }

    /** Called by [InstallResultReceiver] while the system confirmation shows. */
    fun onInstallPending(context: Context) {
        val release = releaseOfState() ?: return
        if (_state.value is UpdateState.Installing) return
        _state.value = UpdateState.Installing(release)
    }

    /**
     * Called by [InstallResultReceiver] for every terminal STATUS_*. Persists
     * the attempt (status code + system message) and updates the visible state.
     */
    fun onInstallStatus(context: Context, statusCode: Int, statusMessage: String?) {
        val app = context.applicationContext
        // beta23: on MIUI a blocked result is the vendor user-restriction rule,
        // and the APK is immediately retried through the system installer.
        val miui = MiuiDevices.isMiuiOnDevice()
        val outcome = InstallOutcome.forStatus(statusCode, miui)
        scope.launch {
            val store = runCatching { UpdatesStore(app) }.getOrNull()
            val release = releaseOfState() ?: store?.pendingRelease()
            runCatching { store?.recordInstallAttempt(InstallAttempt(
                timestampMs = System.currentTimeMillis(),
                statusCode = statusCode,
                outcome = outcome,
                message = statusMessage
            )) }

            when (outcome) {
                InstallOutcome.SUCCESS -> {
                    runCatching {
                        store?.setPending(null)
                        if (release != null) store?.markNotified(release.tag)
                    }
                    _state.value = UpdateState.Idle
                    UpdateNotifier.cancelAll(app)
                    Log.i(ApkInstaller.TAG, "install succeeded${release?.let { " for ${it.tag}" } ?: ""}")
                }

                InstallOutcome.PENDING_USER_ACTION -> Unit

                else -> {
                    Log.w(ApkInstaller.TAG, "install failed: ${outcome.name}")
                    if (release != null) {
                        val rerouted = outcome == InstallOutcome.MIUI_RESTRICTED &&
                            rerouteToSystemInstaller(app, release)
                        if (!rerouted) {
                            _state.value = UpdateState.Failed(release, FailureKind.INSTALL)
                        }
                    }
                }
            }
        }
    }

    /**
     * beta23: one-shot MIUI fallback. The status receiver has already persisted
     * the Xiaomi-specific reason, so this only has to hand the verified APK to
     * the system installer - once per release, so a phone that keeps blocking
     * cannot loop.
     */
    private fun rerouteToSystemInstaller(app: Context, release: ReleaseInfo): Boolean {
        if (!miuiRerouted.add(release.tag)) return false
        val apk = UpdateStorage.apkFile(app, release)
        if (!apk.exists()) return false
        val started = ApkInstaller.retryWithSystemInstaller(app, apk)
        val ok = started is InstallStart.Started
        Log.i(ApkInstaller.TAG, "MIUI reroute to system installer: ${if (ok) "started" else "failed"}")
        return ok
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        scope.launch { runCatching { UpdatesStore(app).setEnabled(enabled) } }
        UpdateScheduler.sync(app, enabled)
    }

    /** Browser fallback for a failed manual check / blocked install. */
    fun openReleasesPage(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ReleaseInfo.RELEASES_PAGE))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun dismiss() {
        checkJob?.cancel()
        downloadJob?.cancel()
        _state.value = UpdateState.Idle
    }

    private fun releaseOfState(): ReleaseInfo? = when (val current = _state.value) {
        is UpdateState.Available -> current.release
        is UpdateState.ReadyToInstall -> current.release
        is UpdateState.NeedsPermission -> current.release
        is UpdateState.Installing -> current.release
        is UpdateState.Failed -> current.release
        else -> null
    }

    private fun recordAttempt(
        context: Context,
        code: Int,
        outcome: InstallOutcome,
        message: String?
    ) {
        val app = context.applicationContext
        scope.launch {
            runCatching {
                UpdatesStore(app).recordInstallAttempt(
                    InstallAttempt(System.currentTimeMillis(), code, outcome, message)
                )
            }
        }
    }

    private suspend fun languageOf(context: Context): AppLanguage = runCatching {
        SettingsRepository(context).settings.first().language
    }.getOrDefault(AppLanguage.ARABIC)

    private fun kindOf(error: Throwable): FailureKind = when (error) {
        is MissingChecksumException, is ChecksumMismatchException -> FailureKind.CHECKSUM
        is UntrustedHostException -> FailureKind.NETWORK
        is java.io.IOException -> FailureKind.NETWORK
        else -> FailureKind.NETWORK
    }
}

/** App-writable cache location; no storage permission is involved. */
object UpdateStorage {
    fun apkFile(context: Context, release: ReleaseInfo): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val safeVersion = release.version.toString().replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(directory, "nidaa-$safeVersion.apk")
    }
}
