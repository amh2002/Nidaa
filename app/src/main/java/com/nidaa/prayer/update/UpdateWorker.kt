package com.nidaa.prayer.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mawaqiti.plus.BuildConfig
import com.mawaqiti.plus.data.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Background update check.
 *
 * Silent by contract: a disabled toggle, an offline device, an untrusted host,
 * a malformed release or any other failure returns success without UI, sound
 * or notification. Success only posts one notification per release version.
 */
class UpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        return try {
            val store = UpdatesStore(context)
            val enabled = store.enabledNow()
            val now = System.currentTimeMillis()
            if (!UpdatePolicy.shouldAutoCheck(enabled, store.lastAutoCheckMs(), now)) {
                return Result.success()
            }

            val release = UpdateClient().latestRelease()
            store.markAutoChecked(now)
            val current = SemanticVersion.parse(BuildConfig.VERSION_NAME)
            if (current != null && release.version > current) {
                store.setPending(release)
                if (store.notifiedTag() != release.tag) {
                    val language = SettingsRepository(context).settings.first().language
                    UpdateNotifier.notify(context, release, language)
                    store.markNotified(release.tag)
                }
            } else {
                store.setPending(null)
            }
            Result.success()
        } catch (error: Throwable) {
            // Silent by design: the owner never sees an update failure.
            Result.success()
        }
    }
}
