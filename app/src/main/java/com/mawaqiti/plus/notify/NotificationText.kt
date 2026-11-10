package com.mawaqiti.plus.notify

import android.content.Context
import android.content.res.Configuration
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppLanguage
import java.util.Locale

/**
 * Builds a context pinned to the app's chosen language. Receivers run with the
 * system locale, so without this a notification could switch language
 * independently of the in-app setting.
 */
object NotificationText {

    fun localizedContext(context: Context, language: AppLanguage): Context {
        val locale = if (language == AppLanguage.ARABIC) {
            Locale.forLanguageTag("ar")
        } else {
            Locale.ENGLISH
        }
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    fun eventLabel(context: Context, event: NotifyEvent): String = context.getString(
        when (event) {
            NotifyEvent.FAJR -> R.string.prayer_fajr
            NotifyEvent.SUNRISE -> R.string.prayer_sunrise
            NotifyEvent.DHUHR -> R.string.prayer_dhuhr
            NotifyEvent.ASR -> R.string.prayer_asr
            NotifyEvent.MAGHRIB -> R.string.prayer_maghrib
            NotifyEvent.ISHA -> R.string.prayer_isha
            NotifyEvent.LAST_THIRD -> R.string.last_third
        }
    )

    fun title(context: Context, event: NotifyEvent): String =
        if (event == NotifyEvent.LAST_THIRD) {
            context.getString(R.string.notify_title_last_third)
        } else {
            context.getString(R.string.notify_title_prayer, eventLabel(context, event))
        }

    fun body(context: Context, cityLabel: String, time: String): String =
        context.getString(R.string.notify_body, cityLabel, time)
}
