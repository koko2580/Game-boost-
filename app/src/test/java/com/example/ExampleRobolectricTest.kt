package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AD_REWARD_DURATION_MS
import com.example.ui.components.formatCompactTurboClock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Turbo Game X", appName)
  }

  @Test
  fun `ad reward grants one hour and formats clock`() {
    assertEquals(3_600_000L, AD_REWARD_DURATION_MS)
    assertEquals("0:00:00", formatCompactTurboClock(0L))
    assertEquals("1:00:00", formatCompactTurboClock(3600L))
    assertEquals("0:59:59", formatCompactTurboClock(3599L))
  }

  @Test
  fun `admob unit ids match configured publisher units`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("ca-app-pub-5984576938417142~8737463815", context.getString(R.string.admob_app_id))
    assertEquals("ca-app-pub-5984576938417142/2994020841", com.example.ads.AdUnitIds.BANNER)
    assertEquals("ca-app-pub-5984576938417142/3117455560", com.example.ads.AdUnitIds.INTERSTITIAL)
    assertEquals("ca-app-pub-5984576938417142/5620184189", com.example.ads.AdUnitIds.REWARDED)
    assertEquals("ca-app-pub-5984576938417142/2587838493", com.example.ads.AdUnitIds.APP_OPEN)
  }
}
