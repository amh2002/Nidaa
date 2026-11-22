package com.mawaqiti.plus.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.mawaqiti.plus.R

/**
 * beta27: the fallback tap target of the clock widget.
 *
 * [ClockWidgetProvider] only routes the tap here when the device resolves no
 * activity for `AlarmClock.ACTION_SHOW_ALARMS`; the widget's normal tap opens
 * the Clock app directly. The message therefore explains a real absence rather
 * than a transient failure.
 */
class ClockWidgetLaunchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(
            context.applicationContext,
            R.string.clock_widget_no_clock_app,
            Toast.LENGTH_SHORT
        ).show()
    }
}
