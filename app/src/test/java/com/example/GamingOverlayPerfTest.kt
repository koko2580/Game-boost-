package com.example

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.CapabilityStatus
import com.example.data.CpuMode
import com.example.data.DisplayModeOption
import com.example.data.FrameDropRiskLevel
import com.example.data.GamePanelStyle
import com.example.data.GameTurboSettings
import com.example.data.GamingOverlayPerfController
import com.example.data.GamingOverlayPerfUiState
import com.example.data.HardwareTelemetry
import com.example.data.PerformanceEngineStatus
import com.example.data.SystemHardwareController
import com.example.ui.components.GamePanelOverlayContent
import com.example.ui.components.OverlayCallbacks
import com.example.ui.theme.TurboGameXTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GamingOverlayPerfTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun noopCallbacks(
        onToggleCooling: (Boolean) -> Unit = {},
        onApplyCoolingPrep: () -> Unit = {},
        onToggleEsports: (Boolean) -> Unit = {},
        onToggle60Fps: (Boolean) -> Unit = {},
        onTogglePerfBoost: (Boolean) -> Unit = {},
        onToggleFrameDrop: (Boolean) -> Unit = {},
        onSnoozeFrameDrop: () -> Unit = {},
        onApplyMitigation: () -> Unit = {},
        onSelectFpsOpt: (Int) -> Unit = {},
        onSetConsent: (Boolean) -> Unit = {},
        onConfigureHighFps: (Boolean, Int) -> Unit = { _, _ -> }
    ): OverlayCallbacks = OverlayCallbacks(
        onHomeClick = {},
        onSettingsClick = {},
        onGamesClick = {},
        onPowerExitClick = {},
        onMinimizeClick = {},
        onSelectCpuMode = {},
        onCycleFrameRate = {},
        onBoostRam = {},
        onClearCache = {},
        onPingTest = {},
        onToggleStopwatch = {},
        onResetStopwatch = {},
        onToggleActiveInfo = {},
        onToggleTurboX = {},
        onToggleDataPanel = {},
        onCycleCrosshair = {},
        onCycleScreenColor = {},
        onToggleScreenZoom = {},
        onToggleBackgroundControl = {},
        onToggleEffectSound = {},
        onToggleScreenScaling = {},
        onToggleKeyMap = {},
        onBrightnessChange = {},
        onVolumeChange = {},
        onToggleAutoRotate = {},
        onToggleAutoBrightness = {},
        onToggleNoNotifications = {},
        onToggleDnd = {},
        onOpenWifi = {},
        onScreenshotInfo = {},
        onQuickApp = { _, _ -> },
        onToggleCoolingEngine = onToggleCooling,
        onApplyCoolingPrep = onApplyCoolingPrep,
        onToggleEsportsMode = onToggleEsports,
        onToggle60FpsStable = onToggle60Fps,
        onTogglePerformanceBoost = onTogglePerfBoost,
        onToggleFrameDropProtection = onToggleFrameDrop,
        onSnoozeFrameDropWarning = onSnoozeFrameDrop,
        onApplyFrameDropMitigation = onApplyMitigation,
        onSelectFpsOptTarget = onSelectFpsOpt,
        onSetFpsOptDisplayConsent = onSetConsent,
        onConfigureHighFps = onConfigureHighFps
    )

    @Test
    fun `X Cooling Engine never auto-starts and toggles manually with timestamped reading`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = GamingOverlayPerfController(SystemHardwareController(context))

        // Initial state: never auto-started
        assertFalse(controller.uiState.value.xCoolingEngineActive)

        // Refreshing diagnostics alone must NOT auto-start cooling
        val telemetry = HardwareTelemetry(batteryTempCelsius = 38.4f, isCharging = true)
        val perfStatus = PerformanceEngineStatus(thermalStatusLabel = "MODERATE", thermalStatusCode = PowerManager.THERMAL_STATUS_MODERATE)
        controller.refreshDiagnostics(GameTurboSettings(), telemetry, perfStatus, nowMs = 1_700_000_000_000L)
        assertFalse(controller.uiState.value.xCoolingEngineActive)

        // Manual start records snapshot and timestamp
        val snap = controller.toggleCoolingEngine(true, telemetry, perfStatus, nowMs = 1_700_000_005_000L)
        assertTrue(controller.uiState.value.xCoolingEngineActive)
        assertEquals(38.4f, snap.batteryTempCelsius, 0.01f)
        assertTrue(snap.isCharging)
        assertNotNull(controller.uiState.value.latestCoolingReading)

        // Manual stop halts active cooling state
        controller.toggleCoolingEngine(false, telemetry, perfStatus, nowMs = 1_700_000_010_000L)
        assertFalse(controller.uiState.value.xCoolingEngineActive)
    }

    @Test
    fun `Esports Mode saves and restores prior Turbo X settings cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = GamingOverlayPerfController(SystemHardwareController(context))

        var settings = GameTurboSettings(
            cpuMode = CpuMode.BALANCED,
            lowRamOverlayMode = false,
            reduceTouchLatency = false,
            enhanceTouchSensitivity = false,
            optimizeFramePacing = false,
            smartBackgroundControl = false,
            backgroundControlEnabled = false,
            enableAdpfHints = false
        )

        controller.toggleEsportsMode(
            enable = true,
            currentSettings = settings,
            knownPackages = emptyList(),
            updateSettings = { transform -> settings = transform(settings) }
        )
        assertTrue(settings.esportsModeEnabled)
        assertEquals(CpuMode.ACCELERATION, settings.cpuMode)
        assertTrue(settings.reduceTouchLatency)
        assertTrue(settings.enhanceTouchSensitivity)
        assertTrue(settings.optimizeFramePacing)
        assertTrue(settings.lowRamOverlayMode)

        controller.toggleEsportsMode(
            enable = false,
            currentSettings = settings,
            knownPackages = emptyList(),
            updateSettings = { transform -> settings = transform(settings) }
        )
        assertFalse(settings.esportsModeEnabled)
        assertEquals(CpuMode.BALANCED, settings.cpuMode)
        assertFalse(settings.reduceTouchLatency)
        assertFalse(settings.enhanceTouchSensitivity)
        assertFalse(settings.optimizeFramePacing)
        assertFalse(settings.lowRamOverlayMode)
    }

    @Test
    fun `60FPS Stable, Performance Boost, FPS Optimization, and High FPS Mode persist and restore cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = GamingOverlayPerfController(SystemHardwareController(context))
        val modes = listOf(
            DisplayModeOption(1, 1080, 2400, 60),
            DisplayModeOption(2, 1080, 2400, 90),
            DisplayModeOption(3, 1080, 2400, 120)
        )

        var settings = GameTurboSettings(
            frameRateTarget = 90,
            fpsGameTarget = 90,
            highPerfFpsControl = true,
            displayResolutionOpt = false,
            preferredDisplayModeId = 0,
            openingAnimation = true,
            homeAnimation = true,
            lowRamOverlayMode = false
        )

        // 60 FPS Stable enable -> restore
        controller.toggle60FpsStable(true, modes) { t -> settings = t(settings) }
        assertTrue(settings.fps60StableEnabled)
        assertEquals(60, settings.frameRateTarget)
        assertEquals(1, settings.preferredDisplayModeId)

        controller.toggle60FpsStable(false, modes) { t -> settings = t(settings) }
        assertFalse(settings.fps60StableEnabled)
        assertEquals(90, settings.frameRateTarget)
        assertEquals(90, settings.fpsGameTarget)
        assertTrue(settings.highPerfFpsControl)

        // Performance Boost enable -> log actions -> restore
        val boostMsg = controller.togglePerformanceBoost(true, settings, emptyList()) { t -> settings = t(settings) }
        assertTrue(settings.performanceBoostActive)
        assertTrue(settings.lowRamOverlayMode)
        assertFalse(settings.openingAnimation)
        assertTrue(boostMsg.contains("Cleared"))

        controller.togglePerformanceBoost(false, settings, emptyList()) { t -> settings = t(settings) }
        assertFalse(settings.performanceBoostActive)
        assertFalse(settings.lowRamOverlayMode)
        assertTrue(settings.openingAnimation)

        // FPS Optimization requires consent before mutating display preference
        controller.selectFpsOptimizationTarget(120, consentGranted = false, supportedModes = modes) { t -> settings = t(settings) }
        assertEquals(120, settings.fpsOptimizationTarget)
        assertEquals(90, settings.frameRateTarget) // unchanged without consent

        controller.selectFpsOptimizationTarget(120, consentGranted = true, supportedModes = modes) { t -> settings = t(settings) }
        assertEquals(120, settings.frameRateTarget)
        assertEquals(3, settings.preferredDisplayModeId)

        controller.selectFpsOptimizationTarget(0, consentGranted = true, supportedModes = modes) { t -> settings = t(settings) }
        assertEquals(0, settings.fpsOptimizationTarget)
        assertEquals(90, settings.frameRateTarget)

        // High FPS Mode 90Hz enable -> restore
        controller.configureHighFpsMode(true, 90, modes) { t -> settings = t(settings) }
        assertTrue(settings.highFpsModeEnabled)
        assertEquals(90, settings.highFpsTargetHz)
        assertEquals(2, settings.preferredDisplayModeId)

        controller.configureHighFpsMode(false, 90, modes) { t -> settings = t(settings) }
        assertFalse(settings.highFpsModeEnabled)
        assertEquals(0, settings.preferredDisplayModeId)
    }

    @Test
    fun `Frame Drop Protection evaluates thermal and RAM pressure and enforces snooze cooldown`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = GamingOverlayPerfController(SystemHardwareController(context))

        val now = 1_700_000_000_000L
        val stressedTelemetry = HardwareTelemetry(
            batteryTempCelsius = 43.2f,
            ramUsedPercent = 88,
            supportedRefreshRates = listOf(60, 120)
        )
        val stressedPerf = PerformanceEngineStatus(
            thermalStatusLabel = "HIGH",
            thermalStatusCode = PowerManager.THERMAL_STATUS_SEVERE,
            thermalHeadroomSeconds = 0.96f,
            batterySaverActive = true
        )
        var settings = GameTurboSettings(frameDropProtectionEnabled = true)

        val highRisk = GamingOverlayPerfController.evaluateFrameDropRisk(stressedTelemetry, stressedPerf, settings, now)
        assertEquals(FrameDropRiskLevel.HIGH, highRisk.level)
        assertTrue(highRisk.bannerVisible)

        // Snooze 10m suppresses banner during cooldown
        controller.snoozeFrameDropWarning(10, nowMs = now) { t -> settings = t(settings) }
        val duringSnooze = GamingOverlayPerfController.evaluateFrameDropRisk(stressedTelemetry, stressedPerf, settings, now + 60_000L)
        assertFalse(duringSnooze.bannerVisible)
        assertTrue(duringSnooze.snoozedRemainingMinutes > 0)

        // After snooze expires banner can show again
        val afterSnooze = GamingOverlayPerfController.evaluateFrameDropRisk(stressedTelemetry, stressedPerf, settings, now + 11 * 60_000L)
        assertTrue(afterSnooze.bannerVisible)

        // Capability checks distinguish unsupported high-Hz vs supported vs app-pacing 30FPS
        assertEquals(CapabilityStatus.APP_PACING, GamingOverlayPerfController.evaluateFpsOptTierCapability(30, listOf(60)))
        assertEquals(CapabilityStatus.UNSUPPORTED, GamingOverlayPerfController.evaluateFpsOptTierCapability(120, listOf(60)))
        assertEquals(CapabilityStatus.SUPPORTED, GamingOverlayPerfController.evaluateFpsOptTierCapability(120, listOf(60, 120)))
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape overlay opens Perf Suite across styles and switches all 7 feature tabs cleanly`() {
        var currentSettings by mutableStateOf(
            GameTurboSettings(
                gamePanelStyle = GamePanelStyle.ROG_STYLE,
                lowRamOverlayMode = true
            )
        )
        var currentPerfState by mutableStateOf(
            GamingOverlayPerfUiState(
                xCoolingEngineActive = false,
                frameDropRisk = GamingOverlayPerfController.evaluateFrameDropRisk(
                    HardwareTelemetry(batteryTempCelsius = 42.5f, ramUsedPercent = 87),
                    PerformanceEngineStatus(thermalStatusCode = PowerManager.THERMAL_STATUS_SEVERE, thermalStatusLabel = "HIGH"),
                    GameTurboSettings(frameDropProtectionEnabled = true)
                )
            )
        )

        composeTestRule.setContent {
            TurboGameXTheme {
                GamePanelOverlayContent(
                    settings = currentSettings,
                    telemetry = HardwareTelemetry(
                        actualFps = 60,
                        displayRefreshRateHz = 60,
                        supportedRefreshRates = listOf(60, 90),
                        batteryTempCelsius = 42.5f,
                        ramUsedPercent = 87
                    ),
                    brightnessFraction = 0.7f,
                    volumeFraction = 0.6f,
                    autoBrightness = false,
                    autoRotate = true,
                    stopwatchSeconds = 0L,
                    isStopwatchRunning = false,
                    statusMessage = null,
                    performanceStatus = PerformanceEngineStatus(
                        thermalStatusLabel = "HIGH",
                        thermalStatusCode = PowerManager.THERMAL_STATUS_SEVERE
                    ),
                    overlayPerfState = currentPerfState,
                    callbacks = noopCallbacks(
                        onToggleCooling = { en -> currentPerfState = currentPerfState.copy(xCoolingEngineActive = en) },
                        onToggleEsports = { en -> currentSettings = currentSettings.copy(esportsModeEnabled = en) },
                        onToggle60Fps = { en -> currentSettings = currentSettings.copy(fps60StableEnabled = en) },
                        onTogglePerfBoost = { en -> currentSettings = currentSettings.copy(performanceBoostActive = en) },
                        onSnoozeFrameDrop = {
                            currentPerfState = currentPerfState.copy(
                                frameDropRisk = currentPerfState.frameDropRisk.copy(bannerVisible = false)
                            )
                        }
                    )
                )
            }
        }

        composeTestRule.waitForIdle()
        // Dismissible Frame Drop Risk banner is visible and snoozeable without spamming
        composeTestRule.onNodeWithTag("frame_drop_alert_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("frame_drop_banner_snooze").performClick()
        composeTestRule.waitForIdle()
        assertFalse(currentPerfState.frameDropRisk.bannerVisible)

        // Open 7-Mode Perf Suite drawer
        composeTestRule.onNodeWithTag("perf_suite_trigger_chip").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("perf_suite_sheet_root").assertIsDisplayed()

        // 1. X Cooling Engine manual toggle
        composeTestRule.onNodeWithTag("cooling_toggle_btn").performScrollTo().performClick()
        assertTrue(currentPerfState.xCoolingEngineActive)

        // 2. Gaming Esports Mode
        composeTestRule.onNodeWithTag("perf_tab_ESPORTS").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("esports_mode_toggle_btn").performScrollTo().performClick()
        assertTrue(currentSettings.esportsModeEnabled)

        // 3. 60 FPS Stable
        composeTestRule.onNodeWithTag("perf_tab_FPS60").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("fps60_stable_toggle_btn").performScrollTo().performClick()
        assertTrue(currentSettings.fps60StableEnabled)

        // 4. Performance Boost
        composeTestRule.onNodeWithTag("perf_tab_BOOST").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("perf_boost_toggle_btn").performScrollTo().performClick()
        assertTrue(currentSettings.performanceBoostActive)

        // 5. Frame Drop Protection
        composeTestRule.onNodeWithTag("perf_tab_FRAME_DROP").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("frame_drop_mitigate_btn").assertIsDisplayed()

        // 6. FPS Optimization
        composeTestRule.onNodeWithTag("perf_tab_FPS_OPT").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("fps_opt_card_60").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fps_opt_card_120").assertIsDisplayed()

        // 7. High FPS Mode
        composeTestRule.onNodeWithTag("perf_tab_HIGH_FPS").performScrollTo().performClick()
        composeTestRule.onAllNodesWithText("High FPS Mode").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithTag("high_fps_chip_90").assertIsDisplayed()

        // Close drawer cleanly
        composeTestRule.onNodeWithTag("perf_suite_close_btn").performClick()
        composeTestRule.waitForIdle()
    }
}
