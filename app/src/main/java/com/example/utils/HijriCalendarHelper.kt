package com.example.utils

import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class HijriSnapshot(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNameAr: String,
    val refGregorianDate: String
)

data class ResolvedHijriDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNameAr: String,
    val formattedAr: String
)

object HijriCalendarHelper {

    val ARABIC_HIJRI_MONTHS = arrayOf(
        "محرم الحرام",
        "صفر الخير",
        "ربيع الأول",
        "ربيع الثاني",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب الأصب",
        "شعبان المعظم",
        "رمضان المبارك",
        "شوال المكرم",
        "ذو القعدة",
        "ذو الحجة"
    )

    val CRITICAL_TRANSITION_DAYS = setOf(28, 29, 30, 1)

    fun isCriticalTransitionDay(hijriDay: Int): Boolean {
        return hijriDay in CRITICAL_TRANSITION_DAYS
    }

    fun getDeviceHijriDay(date: Date = Date()): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val icuCalendar = android.icu.util.IslamicCalendar()
                icuCalendar.time = date
                icuCalendar.get(android.icu.util.IslamicCalendar.DAY_OF_MONTH)
            } else {
                1
            }
        } catch (_: Exception) {
            1
        }
    }

    fun formatLocalGregorianKey(date: Date = Date()): String {
        val cal = Calendar.getInstance().apply { time = date }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    /**
     * Parses hijri.json from GitHub.
     * Supports:
     * {
     *   "hijri_day": 6,
     *   "hijri_month": 6,
     *   "hijri_year": 1448,
     *   "hijri_month_name": "جمادى الآخرة",
     *   "gregorian_date": "2026-11-17",
     *   "gregorian_anchor_date": "2026-11-12"
     * }
     * Notice: `hijri_day` corresponds to `gregorian_date` (NOT `gregorian_anchor_date` which is month start).
     */
    fun parseHijriJson(jsonStr: String, fallbackDate: Date = Date()): HijriSnapshot {
        val json = JSONObject(jsonStr)
        val hDay = json.getInt("hijri_day")
        val hMonth = json.getInt("hijri_month")
        val hYear = json.getInt("hijri_year")
        val monthName = json.optString("hijri_month_name", "").trim()
        val gregorianDate = json.optString("gregorian_date", "").trim()
        val anchorDate = json.optString("gregorian_anchor_date", "").trim()

        val refDate = when {
            isValidIsoDate(gregorianDate) -> gregorianDate
            isValidIsoDate(anchorDate) -> anchorDate
            else -> formatLocalGregorianKey(fallbackDate)
        }

        val resolvedMonthName = monthName.ifBlank {
            ARABIC_HIJRI_MONTHS.getOrElse(hMonth - 1) { "هجري" }
        }

        return HijriSnapshot(
            day = hDay,
            month = hMonth,
            year = hYear,
            monthNameAr = resolvedMonthName,
            refGregorianDate = refDate
        )
    }

    private fun isValidIsoDate(dateStr: String): Boolean {
        return dateStr.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))
    }

    /**
     * Computes exact calendar day difference between refDateStr (yyyy-MM-dd) and targetDate's local date,
     * immune to Daylight Saving Time and time-of-day truncation.
     */
    fun calendarDaysBetween(refDateStr: String, targetDate: Date = Date()): Int {
        val parts = refDateStr.trim().split("-")
        if (parts.size != 3) return 0
        val refY = parts[0].toIntOrNull() ?: return 0
        val refM = (parts[1].toIntOrNull() ?: return 0) - 1
        val refD = parts[2].toIntOrNull() ?: return 0

        val localCal = Calendar.getInstance().apply { time = targetDate }
        val targetY = localCal.get(Calendar.YEAR)
        val targetM = localCal.get(Calendar.MONTH)
        val targetD = localCal.get(Calendar.DAY_OF_MONTH)

        val utcTz = TimeZone.getTimeZone("UTC")
        val refUtc = Calendar.getInstance(utcTz).apply {
            clear()
            set(refY, refM, refD, 0, 0, 0)
        }
        val targetUtc = Calendar.getInstance(utcTz).apply {
            clear()
            set(targetY, targetM, targetD, 0, 0, 0)
        }

        return ((targetUtc.timeInMillis - refUtc.timeInMillis) / 86_400_000L).toInt()
    }

    fun resolveHijriDate(
        snapshot: HijriSnapshot,
        targetDate: Date = Date(),
        manualDayOffset: Int = 0
    ): ResolvedHijriDate {
        val diffDays = calendarDaysBetween(snapshot.refGregorianDate, targetDate) + manualDayOffset
        var currentDay = snapshot.day + diffDays
        var currentMonth = snapshot.month
        var currentYear = snapshot.year

        while (currentDay > 30) {
            currentDay -= 30
            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
        }
        while (currentDay <= 0) {
            currentDay += 30
            currentMonth--
            if (currentMonth < 1) {
                currentMonth = 12
                currentYear--
            }
        }

        val monthName = if (currentMonth == snapshot.month && snapshot.monthNameAr.isNotBlank()) {
            snapshot.monthNameAr
        } else {
            ARABIC_HIJRI_MONTHS.getOrElse(currentMonth - 1) { "هجري" }
        }

        val formattedAr = "$currentDay $monthName $currentYear هـ"
        return ResolvedHijriDate(
            day = currentDay,
            month = currentMonth,
            year = currentYear,
            monthNameAr = monthName,
            formattedAr = formattedAr
        )
    }
}
