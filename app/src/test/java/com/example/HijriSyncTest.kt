package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PrayerTimesRepository
import com.example.utils.HijriCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HijriSyncTest {

    private fun localDate(year: Int, month1Based: Int, day: Int, hour: Int = 10): java.util.Date {
        return Calendar.getInstance().apply {
            clear()
            set(year, month1Based - 1, day, hour, 30, 0)
        }.time
    }

    @Test
    fun `parseHijriJson uses gregorian_date instead of double-counting gregorian_anchor_date`() {
        val sampleJson = """
            {
              "hijri_day": 6,
              "hijri_month": 6,
              "hijri_year": 1448,
              "hijri_month_name": "جمادى الآخرة",
              "day_name": "الثلاثاء",
              "gregorian_date": "2026-11-17",
              "gregorian_anchor_date": "2026-11-12",
              "time_najaf": "09:31:24",
              "status": "verified",
              "source": "https://www.sistani.org/",
              "last_updated": "2026-11-17T09:31:18+03:00"
            }
        """.trimIndent()

        val nov17 = localDate(2026, 11, 17, 10)
        val snapshot = HijriCalendarHelper.parseHijriJson(sampleJson, nov17)
        val resolvedNov17 = HijriCalendarHelper.resolveHijriDate(snapshot, nov17)

        assertEquals(6, resolvedNov17.day)
        assertEquals(6, resolvedNov17.month)
        assertEquals(1448, resolvedNov17.year)
        assertEquals("6 جمادى الآخرة 1448 هـ", resolvedNov17.formattedAr)

        // Next day offline should automatically advance to 7
        val nov18 = localDate(2026, 11, 18, 0)
        val resolvedNov18 = HijriCalendarHelper.resolveHijriDate(snapshot, nov18)
        assertEquals(7, resolvedNov18.day)
        assertEquals("7 جمادى الآخرة 1448 هـ", resolvedNov18.formattedAr)
    }

    @Test
    fun `critical transition days identify 28, 29, 30, and 1`() {
        assertTrue(HijriCalendarHelper.isCriticalTransitionDay(28))
        assertTrue(HijriCalendarHelper.isCriticalTransitionDay(29))
        assertTrue(HijriCalendarHelper.isCriticalTransitionDay(30))
        assertTrue(HijriCalendarHelper.isCriticalTransitionDay(1))
        assertFalse(HijriCalendarHelper.isCriticalTransitionDay(6))
        assertFalse(HijriCalendarHelper.isCriticalTransitionDay(15))
        assertFalse(HijriCalendarHelper.isCriticalTransitionDay(27))
    }

    @Test
    fun `fresh repository requires initial hijri sync`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PrayerTimesRepository(context, autoStartSync = false)
        assertFalse(repo.hasCompletedInitialHijriSync())
        assertTrue(repo.shouldAutoSyncHijriNow())
    }
}
