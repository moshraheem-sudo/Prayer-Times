package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PrayerTimesData
import com.example.data.model.PrayerType
import com.example.data.repository.PrayerTimesRepository

object PrayerNotificationHelper {

    const val CHANNEL_ID = "prayer_times_alerts_channel_v4"
    const val CHANNEL_NAME = "إشعارات تطبيق صلاتي"
    const val CHANNEL_DESC = "إشعارات تطبيق صلاتي ومواقيت الصلاة والمناسبات الدينية"

    const val PRE_ADHAN_CHANNEL_ID = "prayer_pre_reminders_channel_v1"
    const val PRE_ADHAN_CHANNEL_NAME = "تنبيهات الاستعداد للصلاة (قبل الأذان)"
    const val PRE_ADHAN_CHANNEL_DESC = "تنبيهات عادية تسبق موعد الأذان بـ 10 دقائق ثم 5 دقائق للاستعداد والوضوء"

    const val ONGOING_CHANNEL_ID = "prayer_ongoing_times_bar_v1"
    const val ONGOING_CHANNEL_NAME = "شريط مواقيت الصلاة الدائم"
    const val ONGOING_CHANNEL_DESC = "بطاقة دائمة في لوحة الإشعارات تعرض مواقيت الصلاة لليوم والمدينة المحددة بدقة"

    const val SILENT_CHANNEL_ID = "prayer_silent_updates_channel"
    const val SILENT_CHANNEL_NAME = "تحديثات الخلفية"
    const val SILENT_CHANNEL_DESC = "إشعارات التحديثات الصامتة للتقويم والموقع"

    const val ONGOING_NOTIFICATION_ID = 10001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Clean up legacy notification channels that had loud ringtones assigned
            try {
                notificationManager.deleteNotificationChannel("prayer_times_alerts_channel_v3")
                notificationManager.deleteNotificationChannel("prayer_times_alerts_channel_v2")
                notificationManager.deleteNotificationChannel("prayer_times_alerts_channel")
            } catch (e: Exception) {
                // ignore
            }

            // 1. Silent channel for exact adhan time to prevent ringtone conflict with Adhan voice
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(false)
                setSound(null, null) // Completely silent channel to prevent ringtone conflict with Adhan
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)

            // 2. Pre-Adhan notification channel for 10m and 5m before Adhan with standard notification chime
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val preAdhanChannel = NotificationChannel(
                PRE_ADHAN_CHANNEL_ID,
                PRE_ADHAN_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = PRE_ADHAN_CHANNEL_DESC
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                setSound(defaultSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(preAdhanChannel)

            // 3. Ongoing Persistent Channel for Prayer Times Notification Card
            val ongoingChannel = NotificationChannel(
                ONGOING_CHANNEL_ID,
                ONGOING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ONGOING_CHANNEL_DESC
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(ongoingChannel)

            // 4. Low priority silent channel for background updates (Hijri Sync)
            val silentChannel = NotificationChannel(
                SILENT_CHANNEL_ID,
                SILENT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = SILENT_CHANNEL_DESC
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(silentChannel)
        }
    }

    /**
     * Shows regular gentle pre-adhan notification (at -10 minutes and -5 minutes)
     * with standard notification sound and preparation reminder.
     */
    fun showPreAdhanReminderNotification(
        context: Context,
        prayerType: PrayerType,
        cityName: String,
        offsetMinutes: Int
    ) {
        createNotificationChannel(context)

        val cityDisplay = cityName.ifBlank { "النجف الأشرف" }
        val remainingMinutes = -offsetMinutes

        val title = "اقترب موعد أذان ${prayerType.arName}"
        val body = when (remainingMinutes) {
            10 -> "يتبقى 10 دقائق لرفع أذان ${prayerType.arName} في $cityDisplay • حان وقت الاستعداد والوضوء"
            5 -> "يتبقى 5 دقائق لرفع أذان ${prayerType.arName} في $cityDisplay • تهيأ للصلاة المباركة"
            else -> "يتبقى $remainingMinutes دقائق لرفع أذان ${prayerType.arName} في $cityDisplay"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val iterIndex = if (remainingMinutes == 10) 1 else 2
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal * 10 + iterIndex + 5000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_app_icon)
        } catch (e: Exception) {
            null
        }

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, PRE_ADHAN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .apply {
                if (appIconBitmap != null) {
                    setLargeIcon(appIconBitmap)
                }
            }
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)
            .setColor(0xFF14B8A6.toInt())
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = prayerType.ordinal * 10 + iterIndex + 3000
        notificationManager.notify(notificationId, notification)
    }

    fun showPrayerNotification(
        context: Context,
        prayerType: PrayerType,
        timeFormatted: String,
        cityName: String,
        playAlertSound: Boolean = false,
        repeatIteration: Int = 0,
        offsetMinutes: Int = 0
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal * 10 + repeatIteration,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cityDisplay = cityName.ifBlank { "النجف الأشرف" }

        val titleText = when {
            offsetMinutes < 0 -> "اقتراب موعد الصلاة"
            repeatIteration > 0 -> "تذكير: أذان ${prayerType.arName}"
            else -> "أذان ${prayerType.arName}"
        }

        val messageText = when {
            offsetMinutes < 0 -> "يتبقى ${-offsetMinutes} دقائق على أذان ${prayerType.arName} في $cityDisplay"
            repeatIteration > 0 -> "حان موعد الأذان في $cityDisplay ($timeFormatted)"
            else -> "حان الآن موعد الأذان في $cityDisplay"
        }

        val appIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_app_icon)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .apply {
                if (appIconBitmap != null) {
                    setLargeIcon(appIconBitmap)
                }
            }
            .setContentTitle(titleText)
            .setContentText(messageText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(messageText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(null)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF14B8A6.toInt())
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(prayerType.ordinal * 10 + repeatIteration + 100, notification)
    }

    fun showTestNotification(context: Context) {
        val repo = com.example.data.repository.PrayerTimesRepository(context)
        // Directly preview the Adhan audio cleanly without any conflicting alert ringtones
        AdhanAudioService.previewMuezzin(context, repo.selectedMuezzin.value)
    }

    fun showHijriSyncNotification(context: Context, hijriDate: String, isAuto: Boolean = true) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🌙 تحديث التقويم الهجري"
        val subtitle = if (isAuto) "تمت المزامنة والتحديث التلقائي للتقويم بنجاح" else "تمت مزامنة التقويم الهجري بنجاح"
        val bigText = "$subtitle\nالتاريخ الهجري المعتمد في التطبيق حالياً:\n$hijriDate"

        val appIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_app_icon)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, SILENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .apply {
                if (appIconBitmap != null) {
                    setLargeIcon(appIconBitmap)
                }
            }
            .setContentTitle(title)
            .setContentText("التاريخ الهجري المحدث: $hijriDate")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(bigText)
                    .setSummaryText("التقويم الهجري")
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF14B8A6.toInt())
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(888, notification)
    }

    fun showGithubDispatchNotification(context: Context, isSuccess: Boolean, message: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            889,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSuccess) "🚀 تم إرسال إشعار التحديث التلقائي" else "⚠️ تعذر إرسال إشعار التحديث"

        val appIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_app_icon)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .apply {
                if (appIconBitmap != null) {
                    setLargeIcon(appIconBitmap)
                }
            }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(message)
                    .setSummaryText("تحديث التقويم")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(if (isSuccess) 0xFF14B8A6.toInt() else 0xFFEF4444.toInt())
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(889, notification)
    }

    /**
     * Updates or shows the persistent prayer times notification card in the Android notification drawer.
     * Matches the compact, elegant multi-column layout requested (Noor Al-Itrah style) with our app branding "صلاتي".
     * Texts are tuned to small sizes to guarantee zero overflow or line collision on all device screens.
     */
    fun updateOngoingPrayerNotification(
        context: Context,
        prayerData: PrayerTimesData,
        cityName: String
    ) {
        val repo = PrayerTimesRepository(context)
        if (!repo.isOngoingNotificationEnabled.value) {
            cancelOngoingPrayerNotification(context)
            return
        }

        createNotificationChannel(context)

        val cityDisplay = cityName.ifBlank { prayerData.city.nameAr }.ifBlank { "النجف الأشرف" }

        val fajr12 = PrayerCalculator.formatTo12hArabic(prayerData.fajir)
        val sunrise12 = PrayerCalculator.formatTo12hArabic(prayerData.sunrise)
        val dhuhr12 = PrayerCalculator.formatTo12hArabic(prayerData.doher)
        val asr12 = PrayerCalculator.formatTo12hArabic(prayerData.asr)
        val maghrib12 = PrayerCalculator.formatTo12hArabic(prayerData.maghrib)
        val isha12 = PrayerCalculator.formatTo12hArabic(prayerData.isha)
        val midnight12 = PrayerCalculator.formatTo12hArabic(prayerData.midnight)

        val nextInfo = PrayerCalculator.getNextPrayerInfo(prayerData)
        val nextPrayerText = if (nextInfo != null) {
            "⏳ الصلاة القادمة: ${nextInfo.prayerType.arName}"
        } else {
            "⏳ صلاتي • مواقيت الصلاة"
        }

        // 1. Collapsed View (4 Core Columns: الفجر، الشروق، الظهر، المغرب)
        val remoteCollapsed = RemoteViews(context.packageName, R.layout.notification_prayer_card).apply {
            setTextViewText(R.id.tv_fajr_title, "الفجر")
            setTextViewText(R.id.tv_fajr_time, fajr12)

            setTextViewText(R.id.tv_sunrise_title, "الشروق")
            setTextViewText(R.id.tv_sunrise_time, sunrise12)

            setTextViewText(R.id.tv_dhuhr_title, "الظهر")
            setTextViewText(R.id.tv_dhuhr_time, dhuhr12)

            setTextViewText(R.id.tv_maghrib_title, "المغرب")
            setTextViewText(R.id.tv_maghrib_time, maghrib12)
        }

        // 2. Expanded View (4 Core Times: الفجر، الشروق، الظهر، المغرب + City & Hijri Date + Next Prayer Countdown)
        val remoteExpanded = RemoteViews(context.packageName, R.layout.notification_prayer_card_expanded).apply {
            setTextViewText(R.id.tv_expanded_city, "📍 $cityDisplay")
            setTextViewText(R.id.tv_expanded_hijri, prayerData.hijriDate.ifBlank { "التقويم الهجري" })

            setTextViewText(R.id.tv_exp_fajr_title, "الفجر")
            setTextViewText(R.id.tv_exp_fajr_time, fajr12)

            setTextViewText(R.id.tv_exp_sunrise_title, "الشروق")
            setTextViewText(R.id.tv_exp_sunrise_time, sunrise12)

            setTextViewText(R.id.tv_exp_dhuhr_title, "الظهر")
            setTextViewText(R.id.tv_exp_dhuhr_time, dhuhr12)

            setTextViewText(R.id.tv_exp_maghrib_title, "المغرب")
            setTextViewText(R.id.tv_exp_maghrib_time, maghrib12)

            setTextViewText(R.id.tv_expanded_next_prayer, nextPrayerText)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_app_icon)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setCustomContentView(remoteCollapsed)
            .setCustomBigContentView(remoteExpanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF14B8A6.toInt())
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    fun cancelOngoingPrayerNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ONGOING_NOTIFICATION_ID)
    }
}
