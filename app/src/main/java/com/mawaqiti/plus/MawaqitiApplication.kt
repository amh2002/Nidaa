package com.mawaqiti.plus

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.mawaqiti.plus.data.SettingsRepository
import com.mawaqiti.plus.notify.NotificationChannels
import com.mawaqiti.plus.notify.NotificationScheduler
import com.mawaqiti.plus.notify.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-wide startup work:
 *  - release the SAF grant and delete the persisted adhan URI left by betas
 *    1-5 (beta6 has one bundled adhan file and no picker),
 *  - create/refresh the three notification channels,
 *  - rebuild the rolling 48-hour alarm window (also covers a process that was
 *    killed before rescheduling),
 *  - refresh the home-screen widget.
 */
class MawaqitiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        purgeLegacyAdhanUri()
        NotificationScheduler.scheduleAllAsync(this)
        WidgetUpdater.updateAsync(this)
    }

    /**
     * One-time cleanup of the removed "choose an adhan file" feature. The old
     * key is deleted from DataStore and, if the user had granted the app a
     * persisted read permission on that document, the grant is released.
     */
    private fun purgeLegacyAdhanUri() {
        val app = this
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val legacy = runCatching {
                SettingsRepository(app).purgeLegacyAdhanUri()
            }.getOrNull()
            if (!legacy.isNullOrBlank()) {
                runCatching {
                    app.contentResolver.releasePersistableUriPermission(
                        Uri.parse(legacy),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
        }
    }
}
