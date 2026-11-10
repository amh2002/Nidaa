package com.mawaqiti.plus

import android.app.Application
import com.mawaqiti.plus.notify.NotificationChannels
import com.mawaqiti.plus.notify.NotificationScheduler
import com.mawaqiti.plus.notify.WidgetUpdater

/**
 * Process-wide startup work:
 *  - create the three notification channels once,
 *  - rebuild the rolling 48-hour alarm window (also covers a process that was
 *    killed before rescheduling),
 *  - refresh the home-screen widget.
 */
class MawaqitiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        NotificationScheduler.scheduleAllAsync(this)
        WidgetUpdater.updateAsync(this)
    }
}
