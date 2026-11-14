package com.mawaqiti.plus.notify

/**
 * Compact next-prayer widget (2x2 by default, resizable 2x1 … 2x2).
 *
 * Shows only the next prayer name, its time and a short "in Xh Ym" text that is
 * computed once per update. It never ticks and schedules no alarms: it is
 * refreshed by the same [WidgetUpdater.updateAll] pass as the main widget, so
 * every existing refresh point keeps both widgets in sync.
 */
class NextPrayerWidgetProvider : SafeWidgetProvider() {
    override val kind: WidgetUpdater.WidgetKind
        get() = WidgetUpdater.WidgetKind.NEXT
}
