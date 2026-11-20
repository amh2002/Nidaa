package com.nidaa.prayer.update

/** What the settings card and the main-screen banner should show. */
sealed interface UpdateState {

    data object Idle : UpdateState

    data object Checking : UpdateState

    data object UpToDate : UpdateState

    data class Available(val release: ReleaseInfo) : UpdateState

    data class Downloading(val release: ReleaseInfo, val progress: Int) : UpdateState

    data class ReadyToInstall(val release: ReleaseInfo) : UpdateState

    /** The owner must grant "install unknown apps"; the UI shows the explainer. */
    data class NeedsPermission(val release: ReleaseInfo) : UpdateState

    /** Session committed / system confirmation on screen. */
    data class Installing(val release: ReleaseInfo) : UpdateState

    data class Failed(val release: ReleaseInfo?, val kind: FailureKind) : UpdateState
}

enum class FailureKind {
    NETWORK,
    CHECKSUM,
    DOWNLOAD,
    PERMISSION,
    INSTALL
}
