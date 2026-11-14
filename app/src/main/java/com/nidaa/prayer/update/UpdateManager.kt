package com.nidaa.prayer.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mawaqiti.plus.BuildConfig
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide update state machine.
 *
 * This object never touches the engine, the widgets or the alarm pipeline; it
 * only knows about releases, the app cache, WorkManager and the installer.
 * Failures are exposed as state (manual check) or swallowed (automatic check),
 * never as a crash.
 */
object UpdateManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

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
            } catch (error: Throwable) {
                target.delete()
                _state.value = UpdateState.Failed(release, kindOf(error))
            }
        }
    }

    /** Starts the platform installer; verification has already happened. */
    fun install(context: Context) {
        val release = when (val current = _state.value) {
            is UpdateState.ReadyToInstall -> current.release
            is UpdateState.Available -> current.release
            is UpdateState.Failed -> current.release
            else -> null
        } ?: return
        val apk = UpdateStorage.apkFile(context.applicationContext, release)
        if (!apk.exists()) {
            _state.value = UpdateState.Failed(release, FailureKind.DOWNLOAD)
            return
        }
        when (ApkInstaller.install(context, apk)) {
            InstallStart.Started -> Unit
            InstallStart.NeedsPermission -> {
                ApkInstaller.openPermissionSettings(context)
                _state.value = UpdateState.Failed(release, FailureKind.PERMISSION)
            }
            is InstallStart.Failed -> _state.value = UpdateState.Failed(release, FailureKind.INSTALL)
        }
    }

    /** Called by [InstallResultReceiver]. */
    fun onInstallFinished(context: Context, success: Boolean) {
        val release = when (val current = _state.value) {
            is UpdateState.ReadyToInstall -> current.release
            is UpdateState.Available -> current.release
            else -> null
        }
        if (success) _state.value = UpdateState.Idle
        if (success && release != null) {
            val app = context.applicationContext
            scope.launch {
                runCatching {
                    val store = UpdatesStore(app)
                    store.setPending(null)
                    store.markNotified(release.tag)
                }
            }
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        scope.launch { runCatching { UpdatesStore(app).setEnabled(enabled) } }
        UpdateScheduler.sync(app, enabled)
    }

    /** Browser fallback for a failed manual check. */
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
