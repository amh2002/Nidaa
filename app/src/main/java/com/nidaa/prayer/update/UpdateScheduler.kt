package com.nidaa.prayer.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager wiring: exactly one periodic 24-hour check, constrained to a
 * connected network. The toggle simply schedules or cancels this work; a
 * cancelled schedule means zero network traffic from the app.
 */
object UpdateScheduler {

    private const val WORK_NAME = "nidaa_update_check"

    fun sync(context: Context, enabled: Boolean) {
        val manager = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } else {
            manager.cancelUniqueWork(WORK_NAME)
        }
    }
}
