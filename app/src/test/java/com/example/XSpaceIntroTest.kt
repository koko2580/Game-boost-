package com.example

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.PerformanceEngineStatus
import com.example.data.PermissionStatus
import com.example.data.XSpaceIntroAudioEngine
import com.example.security.SecurityAuditReport
import com.example.ui.screens.PerformanceActions
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.XSpaceBlackHoleIntroScreen
import com.example.ui.screens.resolveXSpacePerformanceProfile
import com.example.ui.theme.TurboGameXTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class XSpaceIntroTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `audio synthesis produces phased non-clipping PCM across all intro stages`() {
        val pcm = XSpaceIntroAudioEngine.synthesizeSoundscapePcm()
        val expectedSamples = (XSpaceIntroAudioEngine.SAMPLE_RATE_HZ * XSpaceIntroAudioEngine.DURATION_MS) / 1000
        assertEquals(expectedSamples, pcm.size)

        // Check energy in logo whoosh (~0.35s), singularity charge/rumble (~1.55s), suction (~2.65s), warp impact (~2.95s)
        fun peakInWindow(startSec: Double, endSec: Double): Int {
            val s = (startSec * XSpaceIntroAudioEngine.SAMPLE_RATE_HZ).toInt().coerceIn(0, pcm.size - 1)
            val e = (endSec * XSpaceIntroAudioEngine.SAMPLE_RATE_HZ).toInt().coerceIn(s + 1, pcm.size)
            var maxAbs = 0
            for (i in s until e) {
                val a = abs(pcm[i].toInt())
                if (a > maxAbs) maxAbs = a
            }
            return maxAbs
        }

        val logoPeak = peakInWindow(0.15, 0.70)
        val chargePeak = peakInWindow(1.10, 1.80)
        val suctionPeak = peakInWindow(2.30, 2.90)
        val impactPeak = peakInWindow(2.90, 3.15)
        val tailEndPeak = peakInWindow(3.32, 3.35)

        assertTrue("Logo whoosh should have audible signal", logoPeak > 1500)
        assertTrue("Singularity charge/rumble should have audible signal", chargePeak > 2500)
        assertTrue("Vortex suction should build strongly", suctionPeak > 3000)
        assertTrue("Final warp impact should peak cleanly", impactPeak > 6000)
        assertTrue("Audio should fade smoothly at end", tailEndPeak < 1200)
    }

    @Test
    fun `performance profile scales down particles and shaders for weak or low-ram mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val highTier = resolveXSpacePerformanceProfile(
            context = context,
            settings = GameTurboSettings(lowRamOverlayMode = false, openingAnimation = true),
            sdkInt = Build.VERSION_CODES.TIRAMISU
        )
        val constrainedTier = resolveXSpacePerformanceProfile(
            context = context,
            settings = GameTurboSettings(lowRamOverlayMode = true, openingAnimation = true),
            sdkInt = Build.VERSION_CODES.TIRAMISU
        )
        val legacyApiTier = resolveXSpacePerformanceProfile(
            context = context,
            settings = GameTurboSettings(lowRamOverlayMode = false, openingAnimation = true),
            sdkInt = Build.VERSION_CODES.R
        )
        val fallbackTier = resolveXSpacePerformanceProfile(
            context = context,
            settings = GameTurboSettings(openingAnimation = false),
            sdkInt = Build.VERSION_CODES.TIRAMISU
        )

        assertTrue(constrainedTier.particleCount <= highTier.particleCount)
        assertFalse(constrainedTier.useShaderBackdrop)
        assertFalse(legacyApiTier.useShaderBackdrop)
        assertTrue(fallbackTier.useSimplifiedFallback)
        assertFalse(fallbackTier.useShaderBackdrop)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp")
    fun `portrait X Space intro displays controls toggles mute and prevents duplicate skip taps`() {
        var finishCount = 0
        val muteCallbacks = mutableListOf<Boolean>()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TurboGameXTheme {
                XSpaceBlackHoleIntroScreen(
                    settings = GameTurboSettings(
                        xSpaceIntroEnabled = true,
                        xSpaceIntroSoundEnabled = true
                    ),
                    onToggleMute = { soundEnabled -> muteCallbacks += soundEnabled },
                    onFinished = { finishCount++ }
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(450L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("xspace_black_hole_intro_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("TURBO X").assertIsDisplayed()
        composeTestRule.onNodeWithText("X-SPACE PORTAL").assertIsDisplayed()
        composeTestRule.onNodeWithTag("xspace_intro_mute_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("xspace_intro_skip_btn").assertIsDisplayed()

        // Toggle Mute -> sound disabled (false), then Unmute -> sound enabled (true)
        composeTestRule.onNodeWithTag("xspace_intro_mute_btn").performClick()
        composeTestRule.mainClock.advanceTimeBy(50L)
        composeTestRule.onNodeWithTag("xspace_intro_mute_btn").performClick()
        composeTestRule.mainClock.advanceTimeBy(50L)
        assertEquals(listOf(false, true), muteCallbacks)

        // Rapid double tap on Skip Intro should dispatch finish only once
        composeTestRule.onNodeWithTag("xspace_intro_skip_btn").performClick()
        composeTestRule.onNodeWithTag("xspace_intro_skip_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, finishCount)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape X Space intro progresses through black hole phases and completes once`() {
        var finishCount = 0
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TurboGameXTheme {
                XSpaceBlackHoleIntroScreen(
                    settings = GameTurboSettings(
                        xSpaceIntroEnabled = true,
                        xSpaceIntroSoundEnabled = false,
                        lowRamOverlayMode = true
                    ),
                    onToggleMute = {},
                    onFinished = { finishCount++ }
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(300L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("COSMIC LINK INITIALIZED").assertIsDisplayed()

        // Event Horizon phase (~1.4s total)
        composeTestRule.mainClock.advanceTimeBy(1100L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("EVENT HORIZON ENGAGED").assertIsDisplayed()

        // Singularity Absorption phase (~2.5s total)
        composeTestRule.mainClock.advanceTimeBy(1100L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SINGULARITY ABSORPTION").assertIsDisplayed()

        // Finish full 3350ms timeline
        composeTestRule.mainClock.advanceTimeBy(1100L)
        composeTestRule.waitForIdle()
        assertEquals(1, finishCount)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h915dp")
    fun `openingAnimation false uses fast fallback transition into X Space`() {
        var finishCount = 0
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TurboGameXTheme {
                XSpaceBlackHoleIntroScreen(
                    settings = GameTurboSettings(
                        xSpaceIntroEnabled = true,
                        openingAnimation = false
                    ),
                    onToggleMute = {},
                    onFinished = { finishCount++ }
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(450L)
        composeTestRule.waitForIdle()
        assertEquals(1, finishCount)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h915dp")
    fun `settings screen persists X Space intro and sound toggles`() {
        var currentSettings by mutableStateOf(GameTurboSettings())
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TurboGameXTheme {
                SettingsScreen(
                    settings = currentSettings,
                    telemetry = HardwareTelemetry(),
                    performanceStatus = PerformanceEngineStatus(),
                    permissions = PermissionStatus(),
                    securityAudit = SecurityAuditReport(),
                    statusMessage = null,
                    performanceActions = PerformanceActions(
                        onSetFpsGameTarget = {},
                        onSetFrameRateTarget = {},
                        onSetSlidingFrictionIndex = {},
                        onSetScrollingSensitivity = {},
                        onSetEnhanceTouchSensitivity = {},
                        onSetReduceTouchLatency = {},
                        onSetIncreaseCpuPerformance = {},
                        onSetIncreaseGpuPerformance = {},
                        onSetForceGpuOpenGl = {},
                        onSetEnableSkiaVulkan = {},
                        onSetHighPerfFpsControl = {},
                        onOpenPermissions = {},
                        onOpenDeveloperOptions = {},
                        onSetOptimizeFramePacing = {},
                        onSetEnableAdpfHints = {},
                        onSetDisablePowerSavingInGame = {},
                        onOpenPowerSaverSettings = {},
                        onSetSmartBackgroundControl = {},
                        onRunSmartBackgroundTrim = {},
                        onOpenAppManagementSettings = {},
                        onSetDisplayResolutionOpt = {},
                        onSelectDisplayModeId = {},
                        onOpenDisplaySettings = {},
                        onSetSustainedPerformanceMode = {},
                        onSetAutoPerformanceOptimization = {}
                    ),
                    onUpdateSettings = { transform -> currentSettings = transform(currentSettings) },
                    onBoostRam = {},
                    onClearCache = {},
                    onSelectWallpaperTheme = { _, _ -> },
                    onOpenPermissions = {},
                    onDismissStatus = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(100L)
        composeTestRule.onNodeWithTag("settings_cat_OTHER").performScrollTo().performClick()
        composeTestRule.mainClock.advanceTimeBy(150L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_x_space_intro").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("toggle_x_space_intro_sound").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertFalse(currentSettings.xSpaceIntroEnabled)
        assertFalse(currentSettings.xSpaceIntroSoundEnabled)
    }
}
