package com.example

import com.example.data.model.PredefinedCities
import com.example.utils.PrayerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testCleanTimeString() {
    assertEquals("05:18", PrayerCalculator.cleanTimeString("5:18 "))
    assertEquals("11:53", PrayerCalculator.cleanTimeString("11:53 "))
  }

  @Test
  fun testFormat12hArabic() {
    assertEquals("05:18 ص", PrayerCalculator.formatTo12hArabic("05:18"))
    assertEquals("11:53 ص", PrayerCalculator.formatTo12hArabic("11:53"))
    assertEquals("05:15 م", PrayerCalculator.formatTo12hArabic("17:15"))
  }

  @Test
  fun testCalculateMidnight() {
    val midnight = PrayerCalculator.calculateMidnight("18:36", "04:35")
    assertNotNull(midnight)
    assertTrue(midnight.contains(":"))
  }

  @Test
  fun testQiblaBearing() {
    // Karbala coordinates (32.6160, 44.0324) -> Kaaba is southwest
    val bearing = PrayerCalculator.calculateQiblaBearing(32.6160, 44.0324)
    assertTrue(bearing in 180.0..250.0)
  }

  @Test
  fun testPredefinedCitiesNotEmpty() {
    assertTrue(PredefinedCities.list.isNotEmpty())
    assertNotNull(PredefinedCities.defaultCity)
  }
}
