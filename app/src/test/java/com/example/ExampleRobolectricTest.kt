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
    assertEquals("Turbo X", appName)
    assertEquals("com.turbo.x", context.packageName)
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

  @Test
  fun `performance tuning engine applies high fps touch sensitivity and gpu pipeline`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = com.example.data.PerformanceTuningEngine(context)

    val baseSettings = com.example.data.GameTurboSettings(
      frameRateTarget = 60,
      fpsGameTarget = 90,
      highPerfFpsControl = true,
      scrollingSensitivity = 7,
      slidingFrictionIndex = 7,
      enhanceTouchSensitivity = true,
      reduceTouchLatency = true,
      increaseCpuPerformance = true,
      increaseGpuPerformance = true,
      forceGpuOpenGl = true,
      enableSkiaVulkan = false
    )

    val status = engine.applyProfile(
      settings = baseSettings,
      supportedRates = listOf(60, 90, 120)
    )

    assertEquals(120, status.effectiveTargetFps)
    assertEquals(true, status.unbufferedTouchActive)
    assertEquals(2.0f, status.frictionFactor, 0.01f)
    assertEquals(true, status.scrollMultiplier > 1.5f)
    assertEquals(true, status.activeGpuPipelineLabel.contains("OpenGL ES"))
  }

  @Test
  fun `security guard seals and verifies turbo credit vault and rejects tampering`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val guard = com.example.security.AppSecurityGuard(context)

    val futureExpiry = System.currentTimeMillis() + 3_600_000L
    val sealed = guard.sealTurboCredit(expiresAtWallMs = futureExpiry, adsWatchedCount = 2)
    org.junit.Assert.assertNotNull(sealed)

    val valid = guard.unsealTurboCredit(sealed)
    assertEquals(false, valid.tampered)
    assertEquals(2, valid.adsWatchedCount)
    assertEquals(true, valid.effectiveRemainingSeconds in 3590L..3601L)

    // Tamper with ciphertext token and confirm AES-GCM authentication rejects it
    val tamperedToken = sealed!!.dropLast(4) + "AAAA"
    val rejected = guard.unsealTurboCredit(tamperedToken)
    assertEquals(true, rejected.tampered)
    assertEquals(0L, rejected.effectiveRemainingSeconds)

    val audit = guard.runSecurityAudit()
    assertEquals(true, audit.isSafeForGameplay)
    assertEquals(true, audit.packageIntegrityOk)
  }

  @Test
  fun `ad load policy enforces cooldown backoff and sanitizes raw sdk messages`() {
    val rawThrottle = "Error building request URL: Too many recently failed requests for ad unit ID: ca-app-pub-5984576938417142/5620184189. You must wait a few seconds before making another ad request. (code 1)"
    assertEquals(true, com.example.ads.AdLoadPolicy.isThrottleError(1, rawThrottle))
    assertEquals(60_000L, com.example.ads.AdLoadPolicy.computeBackoffMs(1, rawThrottle, 1))
    assertEquals(90_000L, com.example.ads.AdLoadPolicy.computeBackoffMs(1, rawThrottle, 2))
    assertEquals(15_000L, com.example.ads.AdLoadPolicy.computeBackoffMs(3, "No fill.", 1))
    assertEquals(30_000L, com.example.ads.AdLoadPolicy.computeBackoffMs(3, "No fill.", 2))

    val sanitized = com.example.ads.AdLoadPolicy.sanitizeUserErrorMessage(1, rawThrottle)
    assertEquals(false, sanitized.contains("ca-app-pub"))
    assertEquals(false, sanitized.contains("request URL"))
    assertEquals(false, sanitized.contains("code 1"))
  }
}
