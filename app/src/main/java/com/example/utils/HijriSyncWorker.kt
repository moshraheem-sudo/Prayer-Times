package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.PrayerTimesRepository

class HijriSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = PrayerTimesRepository(applicationContext, autoStartSync = false)
            val needsInitialSync = !repository.hasCompletedInitialHijriSync()
            val autoEnabled = repository.githubAutoSyncEnabled.value

            if (!autoEnabled && !needsInitialSync) {
                return Result.success()
            }

            val syncResult = repository.syncHijriDateWithGithub(isAuto = true, notifyUser = false)
            if (syncResult.isSuccess) {
                // Keep ongoing prayer notification in sync with updated Hijri date
                val updatedData = repository.prayerTimesData.value
                val city = repository.selectedCity.value
                if (updatedData != null) {
                    try {
                        PrayerNotificationHelper.updateOngoingPrayerNotification(
                            applicationContext,
                            updatedData,
                            city.nameAr
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Ongoing notification refresh skipped: ${e.message}")
                    }
                }

                // On critical moon-sighting days (28, 29, 30, 1), schedule an extra follow-up check
                if (repository.isCriticalHijriTransitionDay()) {
                    HijriSyncScheduler.scheduleCriticalDayFollowUp(applicationContext)
                }
                Result.success()
            } else {
                Log.w(TAG, "Hijri sync worker failed: ${syncResult.exceptionOrNull()?.message}")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "HijriSyncWorker exception: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "HijriSyncWorker"
    }
}
