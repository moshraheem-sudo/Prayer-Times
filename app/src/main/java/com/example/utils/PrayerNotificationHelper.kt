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
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PrayerType

object PrayerNotificationHelper {

    const val CHANNEL_ID = "prayer_times_alerts_channel_v3"
    const val CHANNEL_NAME = "تنبيهات مواقيت الصلاة والأذان (فائق الأهمية)"
    const val CHANNEL_DESC = "تنبيهات ورنين قوي ومباشر عند حلول موعد كل صلاة لإيقاظ وتنبيه المستخدم بدقة"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 150, 450, 150, 450, 300, 650, 200, 650)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPrayerNotification(
        context: Context,
        prayerType: PrayerType,
        timeFormatted: String,
        cityName: String,
        playAlertSound: Boolean = true,
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

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val cityDisplay = cityName.ifBlank { "النجف الأشرف" }
        val messageText = when {
            offsetMinutes < 0 -> "تنبيه مسبق: يتبقى ${-offsetMinutes} دقائق على موعد أذان ${prayerType.arName} في $cityDisplay"
            repeatIteration > 0 -> "تذكير متكرر ($repeatIteration): حان موعد أذان ${prayerType.arName} في مدينة $cityDisplay ($timeFormatted)"
            else -> "حان الآن موعد أذان ${prayerType.arName} في مدينة $cityDisplay"
        }
        val boldMessage = SpannableString(messageText).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
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
            .setContentTitle(boldMessage)
            .setContentText(boldMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(boldMessage)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .apply {
                if (playAlertSound) {
                    setSound(soundUri)
                } else {
                    setSound(null)
                }
            }
            .setVibrate(longArrayOf(0, 450, 150, 450, 150, 450, 300, 650, 200, 650))
            .setLights(Color.RED, 500, 500)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF14B8A6.toInt())
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(prayerType.ordinal * 10 + repeatIteration + 100, notification)

        // Trigger vibration
        try {
            AudioPlayerHelper.vibratePattern(context)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun showTestNotification(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val repo = com.example.data.repository.PrayerTimesRepository(context)
        val cityName = repo.selectedCity.value.nameAr.ifBlank { "النجف الأشرف" }
        val messageText = "حان الآن موعد أذان المغرب في مدينة $cityName"
        val boldMessage = SpannableString(messageText).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
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
            .setContentTitle(boldMessage)
            .setContentText(boldMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(boldMessage)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 450, 150, 450, 150, 450, 300, 650, 200, 650))
            .setLights(Color.RED, 500, 500)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF14B8A6.toInt())
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(999, notification)

        try {
            AudioPlayerHelper.vibratePattern(context)
            // Play muezzin preview directly
            AdhanAudioService.previewMuezzin(context, repo.selectedMuezzin.value)
        } catch (e: Exception) {
            // ignore
        }
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
}
