package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object HijriSyncScheduler {

    private const val TAG = "HijriSyncScheduler"
    private const val PERIODIC_WORK_NAME = "hijri_periodic_auto_sync"
    private const val IMMEDIATE_WORK_NAME = "hijri_immediate_sync"
    private const val CRITICAL_FOLLOWUP_WORK_NAME = "hijri_critical_day_followup"

    private fun connectedConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    /**
     * Schedules a reliable background periodic sync every 6 hours whenever network is connected.
     */
    fun schedulePeriodicSync(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<HijriSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(connectedConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic Hijri sync: ${e.message}")
        }
    }

    /**
     * Schedules an immediate one-time sync (runs as soon as internet is connected),
     * ideal for first install while offline or after device boot / app update.
     */
    fun scheduleImmediateSync(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<HijriSyncWorker>()
                .setConstraints(connectedConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule immediate Hijri sync: ${e.message}")
        }
    }

    /**
     * Schedules an extra follow-up sync in 3 hours during critical end/start-of-month days (28, 29, 30, 1).
     */
    fun scheduleCriticalDayFollowUp(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<HijriSyncWorker>()
                .setInitialDelay(3, TimeUnit.HOURS)
                .setConstraints(connectedConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                CRITICAL_FOLLOWUP_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule critical-day follow-up sync: ${e.message}")
        }
    }
}
