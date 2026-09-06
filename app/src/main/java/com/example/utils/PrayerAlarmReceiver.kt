package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.data.model.AdhanSoundMode
import com.example.data.model.Muezzin
import com.example.data.model.PrayerType
import com.example.data.repository.PrayerTimesRepository

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "PrayerTimes:AlarmWakeLock"
        )
        try {
            wakeLock?.acquire(15000) // 15 seconds max
        } catch (e: Exception) {
            // ignore
        }

        try {
            val prayerTypeName = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: return
            val prayerType = try {
                PrayerType.valueOf(prayerTypeName)
            } catch (e: Exception) {
                return
            }

            val timeStr = intent.getStringExtra(EXTRA_TIME_STR) ?: ""
            val cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: "مدينتك"
            val repeatIteration = intent.getIntExtra(EXTRA_REPEAT_ITERATION, 0)
            val offsetMinutes = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 0)

            // Check if user enabled notifications and get per-prayer custom config
            val repository = PrayerTimesRepository(context)
            val config = repository.getPrayerAlarmConfig(prayerType)

            if (config.isEnabled) {
                val formattedTime = if (timeStr.isNotEmpty()) {
                    PrayerCalculator.formatTo12hArabic(timeStr)
                } else {
                    ""
                }

                val isAdhanPrayer = prayerType in listOf(
                    PrayerType.FAJR,
                    PrayerType.DHUHR,
                    PrayerType.ASR,
                    PrayerType.MAGHRIB,
                    PrayerType.ISHA
                )

                // Select muezzin: specific for this prayer if configured, otherwise global selected
                val muezzin = if (!config.specificMuezzinId.isNullOrBlank()) {
                    Muezzin.fromId(config.specificMuezzinId)
                } else {
                    repository.selectedMuezzin.value
                }

                val isAudioGloballyEnabled = repository.isAdhanAudioEnabled.value
                val soundMode = config.soundMode

                val shouldPlayAdhanAudio = isAdhanPrayer &&
                        isAudioGloballyEnabled &&
                        (soundMode == AdhanSoundMode.FULL_ADHAN || soundMode == AdhanSoundMode.SHORT_TAKBIR)

                val playAlertTone = (soundMode == AdhanSoundMode.BEEP_ALERT) ||
                        (!isAudioGloballyEnabled && soundMode != AdhanSoundMode.VIBRATE_ONLY)

                // Show top banner/lockscreen notification
                PrayerNotificationHelper.showPrayerNotification(
                    context = context,
                    prayerType = prayerType,
                    timeFormatted = formattedTime,
                    cityName = cityName,
                    playAlertSound = playAlertTone,
                    repeatIteration = repeatIteration,
                    offsetMinutes = offsetMinutes
                )

                // Play custom adhan audio in foreground service if enabled
                if (shouldPlayAdhanAudio) {
                    AdhanAudioService.startAdhan(
                        context = context,
                        muezzin = muezzin,
                        prayerType = prayerType,
                        cityName = cityName,
                        timeFormatted = formattedTime,
                        soundMode = soundMode,
                        volumePercent = config.customVolumePercent
                    )
                } else if (soundMode == AdhanSoundMode.VIBRATE_ONLY) {
                    AudioPlayerHelper.vibratePattern(context)
                } else if (soundMode == AdhanSoundMode.BEEP_ALERT) {
                    AudioPlayerHelper.playBeep()
                    AudioPlayerHelper.vibratePattern(context)
                }
            }

            // Reschedule alarms for upcoming days if prayer data exists
            val cachedData = repository.prayerTimesData.value
            val city = repository.selectedCity.value
            if (cachedData != null) {
                PrayerNotificationScheduler.scheduleAllPrayerNotifications(
                    context = context,
                    prayerData = cachedData,
                    cityName = city.nameAr
                )
            }
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    companion object {
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_TIME_STR = "extra_time_str"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_REPEAT_ITERATION = "extra_repeat_iteration"
        const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
    }
}
