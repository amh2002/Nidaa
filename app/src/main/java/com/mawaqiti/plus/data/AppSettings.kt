package com.mawaqiti.plus.data

import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.notify.AdhanSound
import com.mawaqiti.plus.notify.AdhanVolume
import com.mawaqiti.plus.notify.NotificationSettings

enum class AppLanguage(val tag: String) {
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ARABIC

        /** Null when [tag] is absent or not one of the two known tags. */
        fun fromTagOrNull(tag: String?): AppLanguage? =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
    }
}

/**
 * beta10 appearance choice, persisted like every other beta setting.
 *
 * AUTO follows the five Golden Night phases of the day (night, dawn, morning,
 * afternoon, sunset); NIGHT pins the deep-night golden look at any hour. It is
 * presentation-only: no engine value depends on it.
 */
enum class AppTheme(val storageKey: String) {
    AUTO("auto"),
    NIGHT("night");

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: AUTO
    }
}

/**
 * beta21: display layout, chosen in Settings → «وضع العرض».
 *
 * [MOBILE] is the unchanged post-beta20 phone layout (the default), [TV] is the
 * landscape at-a-glance dashboard for Android-powered screens. The choice is
 * presentation-only - no engine value depends on it - and is persisted in the
 * same DataStore as every other setting, so it survives restarts.
 */
enum class LayoutMode(val storageKey: String) {
    MOBILE("mobile"),
    TV("tv");

    companion object {
        fun fromKey(key: String?): LayoutMode =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: MOBILE
    }
}

/**
 * beta22: how many minutes before the adhan the silent pre-alert fires.
 *
 * 0 (the default) disables every pre-alert: nothing is scheduled and no alarm
 * remains. The four other values are the only legal ones - anything else falls
 * back to [DISABLED] - and the value is persisted as an Int under
 * `pre_alert_minutes` in the same DataStore as every other setting.
 */
object PreAlertMinutes {
    const val DISABLED = 0

    /** The picker options, in display order. */
    val OPTIONS = listOf(0, 5, 10, 15, 30)

    fun normalize(minutes: Int): Int = if (minutes in OPTIONS) minutes else DISABLED
}

/**
 * Everything the beta persists. Times and the qibla bearing are derived from
 * [cityOrder] alone, so there is no latitude/longitude state to keep in sync.
 */
data class AppSettings(
    val cityOrder: Int = City.DEFAULT_ORDER,
    /** Manual +1 hour toggle, shown only for Palestine. Default OFF. */
    val summerTime: Boolean = false,
    val language: AppLanguage = AppLanguage.ARABIC,
    /** False only before the very first launch picks Arabic explicitly. */
    val languageInitialized: Boolean = false,
    /** Display-only clock preference; default is the original app's 24h. */
    val clockFormat: ClockFormat = ClockFormat.H24,
    /** beta10: AUTO follows the golden phases, NIGHT pins the golden night look. */
    val theme: AppTheme = AppTheme.AUTO,
    /** beta21: MOBILE keeps the post-beta20 phone layout; TV is the landscape dashboard. */
    val layoutMode: LayoutMode = LayoutMode.MOBILE,
    /** beta22: minutes before the adhan for the silent pre-alert; 0 = disabled. */
    val preAlertMinutes: Int = PreAlertMinutes.DISABLED,
    /**
     * beta 7.9: in-app text multiplier over the system font scale
     * (0.8 … 1.2, one of [FontScale.STEPS]).
     */
    val fontScale: Float = FontScale.DEFAULT,
    /** Per-event notification configuration, stored independently per event. */
    val notifications: NotificationSettings = NotificationSettings(),
    /**
     * beta14: the adhan played for ADHAN-mode events. One persisted choice,
     * shared by the settings picker and the playback service; the default is
     * أذان الأقصى تكبيرة ([AdhanSound.DEFAULT]).
     */
    val adhanSound: AdhanSound = AdhanSound.DEFAULT,
    /**
     * beta17: adhan loudness in percent (0…100). 100 is the default, so every
     * existing install keeps the pre-beta17 loudness until the slider moves.
     * Read at fire time by [com.mawaqiti.plus.notify.AlarmReceiver] and by the
     * settings preview, exactly like [adhanSound].
     */
    val adhanVolume: Int = AdhanVolume.DEFAULT,
    /**
     * Widget-only language override (`widget.lang` in DataStore). Null means
     * "never chosen": the widget then follows [language], which is exactly the
     * spec's "default = the app's current language at first render".
     */
    val widgetLanguage: AppLanguage? = null,
    /**
     * beta25: the three optional home-screen widgets. Dhikr is on by default;
     * the countdown and the moon phase are opt-in. All three are hints only:
     * the slot hides any widget that does not fit the leftover height, so a
     * small screen never clips the home column to show one.
     */
    val homeDhikr: Boolean = true,
    val homeCountdown: Boolean = false,
    val homeMoonPhase: Boolean = false,
    /**
     * beta30: «الأرقام الهندية» on the home surfaces only. Default OFF, so the
     * accepted beta29 rendering is what every existing install keeps until the
     * toggle is switched on. It never reaches the widget or a notification:
     * those do not read this value at all.
     */
    val arabicIndicNumerals: Boolean = false
) {
    /** The language the widget must render with right now. */
    val effectiveWidgetLanguage: AppLanguage
        get() = widgetLanguage ?: language
}
