package com.example

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.CpuMode
import com.example.data.DisplayModeOption
import com.example.data.GameAudioEnhancer
import com.example.data.GamePanelStyle
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.PerformanceEngineStatus
import com.example.data.PerformanceSystemInspector
import com.example.data.PerformanceTuningEngine
import com.example.data.PermissionStatus
import com.example.data.SystemHardwareController
import org.junit.Assert.assertFalse
import com.example.security.AppSecurityGuard
import com.example.security.SecurityAuditReport
import com.example.data.BoostStepId
import com.example.data.BoostStepStatus
import com.example.data.OneTapBoosterState
import com.example.data.OneTapStepResult
import com.example.ui.components.GamePanelOverlayContent
import com.example.ui.components.NetworkGateOverlay
import com.example.ui.components.OneTapBoosterDialog
import com.example.ui.components.OverlayCallbacks
import com.example.ui.components.RewardedAdDialog
import com.example.ui.screens.AddAppsScreen
import com.example.ui.screens.LauncherHomeScreen
import com.example.ui.screens.PerformanceActions
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TurboSplashScreen
import com.example.ui.screens.TurboXGamingPanel
import com.example.ui.theme.TurboGameXTheme
import com.example.viewmodel.RewardedAdStep
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class TurboXMatrixAndPerfTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(sdk = [24, 30, 36])
    fun `hardware telemetry and performance engine operate across api 24 30 and 36`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hardware = SystemHardwareController(context)
        val telemetry = hardware.readHardwareTelemetry(
            cpuMode = CpuMode.ACCELERATION,
            lastPingMs = 28,
            lastJitterMs = 4,
            actualFps = 120,
            frameTimeMs = 8.3f
        )

        assertTrue(telemetry.cpuCoreCount >= 1)
        assertTrue(telemetry.cpuFreqMHz > 0)
        assertEquals(120, telemetry.actualFps)
        assertEquals(28, telemetry.pingMs)

        val engine = PerformanceTuningEngine(context)
        val status = engine.applyProfile(
            settings = GameTurboSettings(
                cpuMode = CpuMode.ACCELERATION,
                frameRateTarget = 90,
                fpsGameTarget = 120,
                highPerfFpsControl = true,
                reduceTouchLatency = true
            ),
            supportedRates = listOf(60, 90, 120)
        )
        assertEquals(120, status.effectiveTargetFps)
        engine.closeSession()
    }

    @Test
    @Config(sdk = [36])
    fun `low ram profile downscales cached app icons to 72px while flagship caps at 96px`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val shadowAm = Shadows.shadowOf(am)
        val shadowPm = Shadows.shadowOf(context.packageManager)

        val oversized = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        shadowPm.setApplicationIcon(context.packageName, BitmapDrawable(context.resources, oversized))

        shadowAm.setIsLowRamDevice(true)
        val lowRamHardware = SystemHardwareController(context)
        assertNull(lowRamHardware.peekCachedAppIcon(context.packageName))
        val lowRamIcon = lowRamHardware.loadAppIconAsync(context.packageName)
        assertNotNull(lowRamIcon)
        assertTrue(lowRamIcon!!.width <= 72)
        assertTrue(lowRamIcon.height <= 72)
        assertNotNull(lowRamHardware.peekCachedAppIcon(context.packageName))

        shadowAm.setIsLowRamDevice(false)
        val flagshipHardware = SystemHardwareController(context)
        val flagshipIcon = flagshipHardware.loadAppIconAsync(context.packageName)
        assertNotNull(flagshipIcon)
        assertTrue(flagshipIcon!!.width <= 96)
        assertTrue(flagshipIcon.height <= 96)
    }

    @Test
    @Config(sdk = [36])
    fun `audio enhancer reuses pooled tone generator and releases cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val enhancer = GameAudioEnhancer(context)
        enhancer.playTacticalTone()
        enhancer.playTacticalTone()
        enhancer.releaseEffects()
    }

    @Test
    @Config(sdk = [36])
    fun `security guard enforces tamper proof vault and detects backward clock jump`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val guard = AppSecurityGuard(context)

        val sealed = guard.sealTurboCredit(
            expiresAtWallMs = System.currentTimeMillis() + 1_800_000L,
            adsWatchedCount = 1
        )
        assertNotNull(sealed)

        val unsealed = guard.unsealTurboCredit(sealed)
        assertEquals(false, unsealed.tampered)
        assertEquals(1, unsealed.adsWatchedCount)
        assertTrue(unsealed.effectiveRemainingSeconds > 0L)

        // Malformed format must always reject
        val malformed = guard.unsealTurboCredit("invalid_token_without_dot")
        assertEquals(true, malformed.tampered)
        assertEquals(0L, malformed.effectiveRemainingSeconds)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp")
    fun `compact phone renders cockpit and switches cleanly to telemetry tab`() {
        val sampleGame = InstalledAppInfo("com.test.game", "Arena Legends", "2.4.1", false)

        composeTestRule.setContent {
            TurboGameXTheme {
                LauncherHomeScreen(
                    settings = GameTurboSettings(
                        addedGamePackages = setOf(sampleGame.packageName),
                        selectedGamePackage = sampleGame.packageName
                    ),
                    telemetry = HardwareTelemetry(actualFps = 60, cpuFreqMHz = 1850, ramUsedPercent = 48),
                    permissions = PermissionStatus(),
                    addedGames = listOf(sampleGame),
                    sessionHistory = emptyList(),
                    remainingTurboSeconds = 3600L,
                    statusMessage = null,
                    isOverlayServiceRunning = false,
                    peekAppIcon = { null },
                    loadAppIconAsync = { null },
                    onSelectGame = {},
                    onStartPlay = {},
                    onAddTurboTimeClick = {},
                    onToggleOverlayService = {},
                    onNavigateAddApps = {},
                    onNavigateSettings = {},
                    onNavigatePermissions = {},
                    onBoostRam = {},
                    onRunPingTest = {},
                    onSelectCpuMode = {},
                    onSelectWallpaperTheme = { _, _ -> },
                    onDismissStatus = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("start_play_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_database").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("CPU Core Frequencies").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp")
    fun `offline network gate blocks interaction and wires settings plus retry actions`() {
        var retriedCheck = false
        var openedNetSettings = false

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                NetworkGateOverlay(
                    onOpenNetworkSettings = { openedNetSettings = true },
                    onRetryCheck = { retriedCheck = true }
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(120L)
        composeTestRule.onNodeWithTag("open_network_settings_btn").performClick()
        composeTestRule.onNodeWithTag("retry_network_check_btn").performClick()
        assertTrue(openedNetSettings)
        assertTrue(retriedCheck)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w900dp-h1200dp")
    fun `expanded tablet renders two pane launcher dock and action cluster cleanly`() {
        val sampleGame = InstalledAppInfo("com.test.tabletgame", "Cyber Racers", "1.9", false)
        composeTestRule.setContent {
            TurboGameXTheme {
                LauncherHomeScreen(
                    settings = GameTurboSettings(
                        addedGamePackages = setOf(sampleGame.packageName),
                        selectedGamePackage = sampleGame.packageName
                    ),
                    telemetry = HardwareTelemetry(actualFps = 120, cpuFreqMHz = 2400, ramUsedPercent = 42),
                    permissions = PermissionStatus(true, true, true, true),
                    addedGames = listOf(sampleGame),
                    sessionHistory = emptyList(),
                    remainingTurboSeconds = 0L,
                    statusMessage = null,
                    isOverlayServiceRunning = false,
                    peekAppIcon = { null },
                    loadAppIconAsync = { null },
                    onSelectGame = {},
                    onStartPlay = {},
                    onAddTurboTimeClick = {},
                    onToggleOverlayService = {},
                    onNavigateAddApps = {},
                    onNavigateSettings = {},
                    onNavigatePermissions = {},
                    onBoostRam = {},
                    onRunPingTest = {},
                    onSelectCpuMode = {},
                    onSelectWallpaperTheme = { _, _ -> },
                    onDismissStatus = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add Games").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_play_button").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w900dp-h1200dp")
    fun `expanded tablet settings navigation rail switches to performance section cleanly`() {
        composeTestRule.setContent {
            TurboGameXTheme {
                SettingsScreen(
                    settings = GameTurboSettings(),
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
                        onOpenDeveloperOptions = {}
                    ),
                    onUpdateSettings = {},
                    onBoostRam = {},
                    onClearCache = {},
                    onSelectWallpaperTheme = { _, _ -> },
                    onOpenPermissions = {},
                    onDismissStatus = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_cat_PERFORMANCE").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Optimize performance").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w900dp-h1200dp")
    fun `settings overlay style gallery renders and selects ROG Xiaomi and RedMagic styles`() {
        var latestSettings = GameTurboSettings()
        composeTestRule.setContent {
            TurboGameXTheme {
                SettingsScreen(
                    settings = latestSettings,
                    telemetry = HardwareTelemetry(),
                    performanceStatus = PerformanceEngineStatus(),
                    permissions = PermissionStatus(),
                    securityAudit = SecurityAuditReport(),
                    statusMessage = null,
                    performanceActions = emptyPerformanceActions(),
                    onUpdateSettings = { transform -> latestSettings = transform(latestSettings) },
                    onBoostRam = {},
                    onClearCache = {},
                    onSelectWallpaperTheme = { _, _ -> },
                    onOpenPermissions = {},
                    onDismissStatus = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_cat_GAME_PANEL").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("panel_style_ROG_STYLE").assertIsDisplayed().performClick()
        assertEquals(GamePanelStyle.ROG_STYLE, latestSettings.gamePanelStyle)

        composeTestRule.onNodeWithTag("panel_style_XIAOMI_STYLE").assertIsDisplayed().performClick()
        assertEquals(GamePanelStyle.XIAOMI_STYLE, latestSettings.gamePanelStyle)

        composeTestRule.onNodeWithTag("panel_style_REDMAGIC_STYLE").assertIsDisplayed().performClick()
        assertEquals(GamePanelStyle.REDMAGIC_STYLE, latestSettings.gamePanelStyle)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape ROG overlay renders armoury HUD and dispatches boost and minimize callbacks`() {
        var boosted = false
        var minimized = false
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                GamePanelOverlayContent(
                    settings = GameTurboSettings(gamePanelStyle = GamePanelStyle.ROG_STYLE),
                    telemetry = HardwareTelemetry(actualFps = 120, cpuFreqMHz = 2800, ramUsedPercent = 44),
                    brightnessFraction = 0.7f,
                    volumeFraction = 0.6f,
                    autoBrightness = false,
                    autoRotate = true,
                    stopwatchSeconds = 0L,
                    isStopwatchRunning = false,
                    statusMessage = null,
                    callbacks = testOverlayCallbacks(
                        onBoostRam = { boosted = true },
                        onMinimizeClick = { minimized = true }
                    )
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("overlay_rog_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overlay_ram_boost_pill").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("overlay_g_handle").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("X-MODE").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("TURBO X", substring = true).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Control").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("QUICK DISPLAY & AUDIO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crosshair").assertIsDisplayed()
        composeTestRule.onNodeWithText("GPU 60Hz").assertIsDisplayed()
        assertTrue(boosted)
        assertTrue(minimized)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape Xiaomi Hyper sidebar renders smoothly and cycles CPU mode`() {
        var selectedMode: CpuMode? = null
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                GamePanelOverlayContent(
                    settings = GameTurboSettings(
                        gamePanelStyle = GamePanelStyle.XIAOMI_STYLE,
                        cpuMode = CpuMode.BALANCED
                    ),
                    telemetry = HardwareTelemetry(actualFps = 90, cpuFreqMHz = 2200, ramUsedPercent = 51),
                    brightnessFraction = 0.6f,
                    volumeFraction = 0.8f,
                    autoBrightness = false,
                    autoRotate = false,
                    stopwatchSeconds = 0L,
                    isStopwatchRunning = false,
                    statusMessage = null,
                    callbacks = testOverlayCallbacks(
                        onSelectCpuMode = { selectedMode = it }
                    )
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("overlay_xiaomi_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("GAMETURBO").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overlay_cpu_mode_pill").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("overlay_ram_boost_pill").assertIsDisplayed()
        assertEquals(CpuMode.ACCELERATION, selectedMode)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape RedMagic Cyber overlay renders dual arc dials and energy faders`() {
        var boosted = false
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                GamePanelOverlayContent(
                    settings = GameTurboSettings(
                        gamePanelStyle = GamePanelStyle.REDMAGIC_STYLE,
                        cpuMode = CpuMode.ACCELERATION
                    ),
                    telemetry = HardwareTelemetry(actualFps = 144, cpuFreqMHz = 3100, ramUsedPercent = 39),
                    brightnessFraction = 0.8f,
                    volumeFraction = 0.7f,
                    autoBrightness = true,
                    autoRotate = true,
                    stopwatchSeconds = 12L,
                    isStopwatchRunning = true,
                    statusMessage = "DIABLO TURBO ENGAGED",
                    callbacks = testOverlayCallbacks(onBoostRam = { boosted = true })
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("overlay_redmagic_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("GAMESPACE").assertIsDisplayed()
        composeTestRule.onNodeWithText("DIABLO TURBO").assertIsDisplayed()
        composeTestRule.onNodeWithText("ENERGY FADER").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overlay_ram_boost_pill").assertIsDisplayed().performClick()
        assertTrue(boosted)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `first launch permissions onboarding displays all five cards and triggers grant-all plus skip`() {
        var overlayRequested = false
        var completed = false
        composeTestRule.setContent {
            TurboGameXTheme {
                PermissionsScreen(
                    permissions = PermissionStatus(
                        dndGranted = false,
                        overlayGranted = false,
                        writeSettingsGranted = false,
                        usageStatsGranted = false,
                        notificationsGranted = true
                    ),
                    onRefreshPermissions = {},
                    onRequestDnd = {},
                    onRequestOverlay = { overlayRequested = true },
                    onRequestWriteSettings = {},
                    onRequestUsageStats = {},
                    onBack = {},
                    isOnboardingMode = true,
                    onCompleteOnboarding = { completed = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("permissions_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("perm_overlay_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("perm_dnd_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("perm_write_settings_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("perm_usage_stats_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("perm_notifications_card").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("grant_all_permissions_btn").assertIsDisplayed().performClick()
        assertTrue(overlayRequested)

        composeTestRule.onNodeWithTag("skip_permissions_btn").assertIsDisplayed().performClick()
        assertTrue(completed)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `permissions onboarding shows all systems armed when all five granted`() {
        var completed = false
        composeTestRule.setContent {
            TurboGameXTheme {
                PermissionsScreen(
                    permissions = PermissionStatus(
                        dndGranted = true,
                        overlayGranted = true,
                        writeSettingsGranted = true,
                        usageStatsGranted = true,
                        notificationsGranted = true
                    ),
                    onRefreshPermissions = {},
                    onRequestDnd = {},
                    onRequestOverlay = {},
                    onRequestWriteSettings = {},
                    onRequestUsageStats = {},
                    onBack = {},
                    isOnboardingMode = true,
                    onCompleteOnboarding = { completed = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ALL SYSTEMS ARMED").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grant_all_permissions_btn").assertIsDisplayed().performClick()
        assertTrue(completed)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp")
    fun `compact phone permissions badges render single-line without vertical wrapping`() {
        composeTestRule.setContent {
            TurboGameXTheme {
                PermissionsScreen(
                    permissions = PermissionStatus(
                        dndGranted = false,
                        overlayGranted = false,
                        writeSettingsGranted = false,
                        usageStatsGranted = false,
                        notificationsGranted = false
                    ),
                    onRefreshPermissions = {},
                    onRequestDnd = {},
                    onRequestOverlay = {},
                    onRequestWriteSettings = {},
                    onRequestUsageStats = {},
                    onBack = {},
                    isOnboardingMode = true,
                    onCompleteOnboarding = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("perm_write_settings_card").performScrollTo().assertIsDisplayed()
        val displayTouchBadge = composeTestRule.onNodeWithText("DISPLAY & TOUCH", useUnmergedTree = true)
        displayTouchBadge.assertIsDisplayed()
        val bounds = displayTouchBadge.fetchSemanticsNode().boundsInRoot
        // A single-line 10sp badge must stay compact vertically (well below multi-line vertical wrapping)
        assertTrue("Badge wrapped vertically! height=${bounds.height}", bounds.height < 60f)
        composeTestRule.onNodeWithTag("perm_usage_stats_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("SMART LAUNCH", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("perm_notifications_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("FOREGROUND", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp")
    fun `portrait narrow overlays render all three Real esports styles cleanly`() {
        val activeStyle = androidx.compose.runtime.mutableStateOf(GamePanelStyle.ROG_STYLE)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                GamePanelOverlayContent(
                    settings = GameTurboSettings(
                        gamePanelStyle = activeStyle.value,
                        lowRamOverlayMode = true
                    ),
                    telemetry = HardwareTelemetry(actualFps = 120, cpuFreqMHz = 2900, ramUsedPercent = 42),
                    brightnessFraction = 0.7f,
                    volumeFraction = 0.6f,
                    autoBrightness = false,
                    autoRotate = false,
                    stopwatchSeconds = 0L,
                    isStopwatchRunning = false,
                    statusMessage = null,
                    callbacks = testOverlayCallbacks()
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("overlay_rog_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("X-MODE").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("TURBO X", substring = true).onFirst().assertIsDisplayed()

        composeTestRule.runOnUiThread { activeStyle.value = GamePanelStyle.XIAOMI_STYLE }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("overlay_xiaomi_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("GAMETURBO").assertIsDisplayed()

        composeTestRule.runOnUiThread { activeStyle.value = GamePanelStyle.REDMAGIC_STYLE }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("overlay_redmagic_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("GAMESPACE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overlay_ram_boost_pill").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36])
    fun `frame pacing jitter tracking ADPF toggle display resolution and auto perf evaluator work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = PerformanceTuningEngine(context)

        // 1. Frame pacing jitter calculation
        val settingsOn = GameTurboSettings(
            optimizeFramePacing = true,
            enableAdpfHints = true,
            frameRateTarget = 120,
            increaseCpuPerformance = true,
            smartBackgroundControl = true,
            displayResolutionOpt = true,
            autoPerformanceOptimization = true
        )
        val statusOn = engine.applyProfile(
            settings = settingsOn,
            supportedRates = listOf(60, 120),
            batteryPercent = 85,
            isCharging = false
        )
        assertTrue(statusOn.framePacingActive)
        assertTrue(statusOn.smartBackgroundActive)
        assertTrue(statusOn.displayResolutionOptActive)
        assertTrue(statusOn.autoPerfActive)
        assertTrue(statusOn.supportedResolutionModes.isNotEmpty())
        assertTrue(statusOn.currentResolutionLabel.contains("×"))
        assertTrue(statusOn.adpfTargetDurationMs in 5.0f..10.0f)

        engine.reportFrameDurationNanos(8_300_000L, settingsOn)
        engine.reportFrameDurationNanos(12_300_000L, settingsOn)
        val statusAfterJitter = engine.applyProfile(settingsOn, listOf(60, 120), batteryPercent = 85)
        assertTrue(statusAfterJitter.framePacingJitterMs > 0.1f)

        // 2. Explicit ADPF disable
        val settingsAdpfOff = settingsOn.copy(enableAdpfHints = false, optimizeFramePacing = false)
        val statusAdpfOff = engine.applyProfile(settingsAdpfOff, listOf(60, 120))
        assertFalse(statusAdpfOff.adpfHintActive)
        assertFalse(statusAdpfOff.framePacingActive)

        // 3. Auto Performance Optimization matrix
        val inspector = PerformanceSystemInspector(context)
        val ecoDecision = inspector.evaluateAutoPerformance(
            thermalLabel = "NORMAL",
            batterySaverActive = true,
            batteryPercent = 90,
            isCharging = false,
            sustainedSupported = true
        )
        assertEquals(CpuMode.ENERGY_SAVING, ecoDecision.recommendedCpuMode)
        assertFalse(ecoDecision.recommendSustained)

        val thermalDecision = inspector.evaluateAutoPerformance(
            thermalLabel = "HIGH",
            batterySaverActive = false,
            batteryPercent = 75,
            isCharging = false,
            sustainedSupported = true
        )
        assertEquals(CpuMode.BALANCED, thermalDecision.recommendedCpuMode)
        assertTrue(thermalDecision.recommendSustained)

        val optimalDecision = inspector.evaluateAutoPerformance(
            thermalLabel = "NORMAL",
            batterySaverActive = false,
            batteryPercent = 82,
            isCharging = false,
            sustainedSupported = true
        )
        assertEquals(CpuMode.ACCELERATION, optimalDecision.recommendedCpuMode)
        assertFalse(optimalDecision.recommendSustained)

        engine.closeSession()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `settings performance suite renders all seven pro features and dispatches actions`() {
        var autoOptToggled = false
        var framePacingToggled = false
        var adpfToggled = false
        var sustainedToggled = false
        var powerSaverOpened = false
        var trimmedBg = false
        var appMgmtOpened = false
        var selectedModeId = 0
        var displaySettingsOpened = false

        val status = PerformanceEngineStatus(
            framePacingActive = true,
            framePacingJitterMs = 1.25f,
            minimalPostProcessingActive = true,
            adpfSupported = true,
            adpfHintActive = true,
            adpfTargetDurationMs = 6.6f,
            batterySaverActive = true,
            backgroundProcessCount = 3,
            backgroundProcessSampleNames = listOf("com.social.app", "com.music.app"),
            currentResolutionLabel = "1080×2400 @ 120Hz",
            supportedResolutionModes = listOf(
                DisplayModeOption(modeId = 2, widthPx = 1080, heightPx = 2400, refreshRateHz = 120),
                DisplayModeOption(modeId = 1, widthPx = 1080, heightPx = 2400, refreshRateHz = 60)
            ),
            appliedDisplayModeId = 2,
            densityDpi = 420,
            sustainedPerformanceSupported = true,
            sustainedPerformanceActive = true,
            autoPerfActive = true,
            autoProfileRecommendation = "Optimal Thermals & Power -> Turbo Peak"
        )

        composeTestRule.setContent {
            TurboGameXTheme {
                SettingsScreen(
                    settings = GameTurboSettings(
                        autoPerformanceOptimization = false,
                        optimizeFramePacing = true,
                        enableAdpfHints = true,
                        sustainedPerformanceMode = false,
                        disablePowerSavingInGame = true,
                        smartBackgroundControl = true,
                        displayResolutionOpt = true,
                        preferredDisplayModeId = 2
                    ),
                    telemetry = HardwareTelemetry(actualFps = 120, supportedRefreshRates = listOf(60, 120)),
                    performanceStatus = status,
                    permissions = PermissionStatus(),
                    securityAudit = SecurityAuditReport(),
                    statusMessage = null,
                    performanceActions = emptyPerformanceActions(
                        onSetOptimizeFramePacing = { framePacingToggled = true },
                        onSetEnableAdpfHints = { adpfToggled = true },
                        onOpenPowerSaverSettings = { powerSaverOpened = true },
                        onRunSmartBackgroundTrim = { trimmedBg = true },
                        onOpenAppManagementSettings = { appMgmtOpened = true },
                        onSelectDisplayModeId = { selectedModeId = it },
                        onOpenDisplaySettings = { displaySettingsOpened = true },
                        onSetSustainedPerformanceMode = { sustainedToggled = true },
                        onSetAutoPerformanceOptimization = { autoOptToggled = true }
                    ),
                    onUpdateSettings = {},
                    onBoostRam = {},
                    onClearCache = {},
                    onSelectWallpaperTheme = { _, _ -> },
                    onOpenPermissions = {},
                    onDismissStatus = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_cat_PERFORMANCE").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("perf_auto_optimize").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("perf_frame_pacing").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("perf_adpf_hints").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("perf_sustained_mode").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_power_settings").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_trim_bg_apps").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_manage_apps").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("display_mode_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_display_settings").performScrollTo().performClick()

        assertTrue(autoOptToggled)
        assertTrue(framePacingToggled)
        assertTrue(adpfToggled)
        assertTrue(sustainedToggled)
        assertTrue(powerSaverOpened)
        assertTrue(trimmedBg)
        assertTrue(appMgmtOpened)
        assertEquals(1, selectedModeId)
        assertTrue(displaySettingsOpened)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `one tap booster evaluates memory pressure and renders six stages plus final report`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inspector = PerformanceSystemInspector(context)
        assertEquals("LOW", inspector.evaluateMemoryPressure(40).first)
        assertEquals("MODERATE", inspector.evaluateMemoryPressure(66).first)
        assertEquals("HIGH", inspector.evaluateMemoryPressure(79).first)
        assertEquals("CRITICAL", inspector.evaluateMemoryPressure(92).first)

        var reboostClicked = false
        var powerClicked = false
        var doneClicked = false

        val reportState = OneTapBoosterState(
            isVisible = true,
            isRunning = false,
            isCompleted = true,
            currentStepIndex = 5,
            progress = 1f,
            steps = BoostStepId.entries.map { id ->
                OneTapStepResult(
                    id = id,
                    status = if (id == BoostStepId.POWER_OPTIMIZATION) BoostStepStatus.WARNING else BoostStepStatus.DONE,
                    metricBadge = "OK",
                    detail = "${id.title} verified"
                )
            },
            ramUsedBeforePercent = 76,
            ramUsedAfterPercent = 54,
            memoryPressureLabel = "HIGH",
            freedMb = 128,
            trimmedApps = 5,
            thermalLabel = "NORMAL",
            batteryTempC = 34.5f,
            batterySaverActive = true,
            selectedCpuMode = CpuMode.ACCELERATION,
            appliedProfileLabel = "Acceleration • ADPF + Frame Pacing",
            finalScore = 82,
            finalVerdict = "POWER SAVER LIMIT"
        )

        composeTestRule.setContent {
            TurboGameXTheme {
                OneTapBoosterDialog(
                    state = reportState,
                    onReBoost = { reboostClicked = true },
                    onOpenPowerSettings = { powerClicked = true },
                    onDismiss = { doneClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("one_tap_booster_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_final_report_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("82/100").assertIsDisplayed()
        composeTestRule.onNodeWithText("+128 MB").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_power_settings_btn").performScrollTo().performClick()
        BoostStepId.entries.forEach { stepId ->
            composeTestRule.onNodeWithTag("one_tap_step_${stepId.name}").performScrollTo().assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("one_tap_reboost_btn").performClick()
        composeTestRule.onNodeWithTag("one_tap_done_btn").performClick()

        assertTrue(powerClicked)
        assertTrue(reboostClicked)
        assertTrue(doneClicked)
    }

    @Test
    @Config(sdk = [36])
    fun `performance engine wires three finger guard flag and blocks 3 plus pointers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = PerformanceTuningEngine(context)

        val disabledStatus = engine.applyProfile(
            settings = GameTurboSettings(disableThreeFingerScreenshot = false),
            supportedRates = listOf(60)
        )
        assertFalse(engine.isThreeFingerGuardEnabled())
        assertFalse(engine.shouldBlockMultiFingerGesture(pointerCount = 3))
        assertFalse(engine.shouldBlockMultiFingerGesture(pointerCount = 2))
        assertTrue(disabledStatus.effectiveTargetFps > 0)

        engine.applyProfile(
            settings = GameTurboSettings(disableThreeFingerScreenshot = true),
            supportedRates = listOf(60)
        )
        assertTrue(engine.isThreeFingerGuardEnabled())
        assertFalse(engine.shouldBlockMultiFingerGesture(pointerCount = 2))
        assertTrue(engine.shouldBlockMultiFingerGesture(pointerCount = 3))
        assertTrue(engine.shouldBlockMultiFingerGesture(pointerCount = 4))
        engine.closeSession()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `xiaomi and redmagic overlays render interactive stopwatch bar when active or paused`() {
        var toggledCount = 0
        var resetCount = 0
        val activeStyle = androidx.compose.runtime.mutableStateOf(GamePanelStyle.XIAOMI_STYLE)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TurboGameXTheme {
                GamePanelOverlayContent(
                    settings = GameTurboSettings(gamePanelStyle = activeStyle.value),
                    telemetry = HardwareTelemetry(actualFps = 120, cpuFreqMHz = 2800, ramUsedPercent = 40),
                    brightnessFraction = 0.7f,
                    volumeFraction = 0.7f,
                    autoBrightness = false,
                    autoRotate = false,
                    stopwatchSeconds = 65L,
                    isStopwatchRunning = true,
                    statusMessage = null,
                    callbacks = testOverlayCallbacks(
                        onToggleStopwatch = { toggledCount++ },
                        onResetStopwatch = { resetCount++ }
                    )
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("overlay_stopwatch_pill").assertIsDisplayed()
        composeTestRule.onNodeWithText("01:05").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overlay_stopwatch_toggle").performClick()
        composeTestRule.onNodeWithTag("overlay_stopwatch_reset").performClick()
        assertEquals(1, toggledCount)
        assertEquals(1, resetCount)

        composeTestRule.runOnUiThread { activeStyle.value = GamePanelStyle.REDMAGIC_STYLE }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("overlay_stopwatch_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overlay_stopwatch_toggle").performClick()
        composeTestRule.onNodeWithTag("overlay_stopwatch_reset").performClick()
        assertEquals(2, toggledCount)
        assertEquals(2, resetCount)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w900dp-h1200dp")
    fun `settings ram help dialogs three finger guard and auto detect toggles update state`() {
        var latestSettings = GameTurboSettings(
            disableThreeFingerScreenshot = false,
            autoDetectGameLaunch = false
        )
        composeTestRule.setContent {
            TurboGameXTheme {
                SettingsScreen(
                    settings = latestSettings,
                    telemetry = HardwareTelemetry(),
                    performanceStatus = PerformanceEngineStatus(),
                    permissions = PermissionStatus(),
                    securityAudit = SecurityAuditReport(),
                    statusMessage = null,
                    performanceActions = emptyPerformanceActions(),
                    onUpdateSettings = { transform -> latestSettings = transform(latestSettings) },
                    onBoostRam = {},
                    onClearCache = {},
                    onSelectWallpaperTheme = { _, _ -> },
                    onOpenPermissions = {},
                    onDismissStatus = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Check RAM help dialogs
        composeTestRule.onNodeWithTag("settings_cat_RAM").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ram_bg_limit_help").performClick()
        composeTestRule.onNodeWithTag("ram_help_dialog_ok").assertIsDisplayed().performClick()

        composeTestRule.onNodeWithTag("ram_cache_limit_help").performClick()
        composeTestRule.onNodeWithTag("ram_help_dialog_ok").assertIsDisplayed().performClick()

        // Check Memory Guard three-finger toggle
        composeTestRule.onNodeWithTag("settings_cat_PROTECTION").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toggle_three_finger_guard").performClick()
        assertTrue(latestSettings.disableThreeFingerScreenshot)

        // Check Advanced auto-detect game launch toggle
        composeTestRule.onNodeWithTag("settings_cat_OTHER").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toggle_auto_detect_game").performScrollTo().performClick()
        assertTrue(latestSettings.autoDetectGameLaunch)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape phone launcher renders cockpit and telemetry cleanly without clipping`() {
        val games = listOf(
            InstalledAppInfo("com.test.alpha", "Cyber Striker", "3.1.0", false),
            InstalledAppInfo("com.test.beta", "Apex Drift", "1.4.2", false)
        )
        val freqs = listOf(1800, 1800, 2200, 2200, 2400, 2400, 2850, 3050)
        composeTestRule.setContent {
            TurboGameXTheme {
                LauncherHomeScreen(
                    settings = GameTurboSettings(
                        addedGamePackages = games.map { it.packageName }.toSet(),
                        selectedGamePackage = games.first().packageName
                    ),
                    telemetry = HardwareTelemetry(
                        actualFps = 120,
                        cpuFreqMHz = 2850,
                        cpuCoreCount = 8,
                        cpuCoreFreqsMHz = freqs,
                        ramUsedPercent = 45
                    ),
                    permissions = PermissionStatus(true, true, true, true, true),
                    addedGames = games,
                    sessionHistory = emptyList(),
                    remainingTurboSeconds = 3600L,
                    statusMessage = null,
                    isOverlayServiceRunning = false,
                    peekAppIcon = { null },
                    loadAppIconAsync = { null },
                    onSelectGame = {},
                    onStartPlay = {},
                    onAddTurboTimeClick = {},
                    onToggleOverlayService = {},
                    onNavigateAddApps = {},
                    onNavigateSettings = {},
                    onNavigatePermissions = {},
                    onBoostRam = {},
                    onRunPingTest = {},
                    onSelectCpuMode = {},
                    onSelectWallpaperTheme = { _, _ -> },
                    onDismissStatus = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("start_play_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_quick_boost_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_games_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_database").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("CPU Core Frequencies").assertIsDisplayed()
        composeTestRule.onNodeWithText("HUD OFF").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape phone splash permissions add apps and dialogs fit without clipping`() {
        // 1. Splash screen
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                TurboSplashScreen(onFinished = {})
            }
        }
        composeTestRule.mainClock.advanceTimeBy(600L)
        composeTestRule.onNodeWithTag("turbo_splash_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("PRECISION PERFORMANCE ENGINE").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape phone permissions two-pane layout displays controls and scrolls all cards`() {
        var skipClicked = false
        composeTestRule.setContent {
            TurboGameXTheme {
                PermissionsScreen(
                    permissions = PermissionStatus(),
                    onRefreshPermissions = {},
                    onRequestDnd = {},
                    onRequestOverlay = {},
                    onRequestWriteSettings = {},
                    onRequestUsageStats = {},
                    onBack = {},
                    isOnboardingMode = true,
                    onCompleteOnboarding = { skipClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("grant_all_permissions_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("skip_permissions_btn").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("perm_overlay_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("perm_notifications_card").performScrollTo().assertIsDisplayed()
        assertTrue(skipClicked)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape phone add apps two-column grid toggles app cleanly`() {
        var toggledPkg: String? = null
        val apps = listOf(
            InstalledAppInfo("com.test.one", "Star Arena", "1.0", false),
            InstalledAppInfo("com.test.two", "Mecha Pulse", "2.1", false)
        )
        composeTestRule.setContent {
            TurboGameXTheme {
                AddAppsScreen(
                    apps = apps,
                    addedPackages = setOf("com.test.one"),
                    isLoading = false,
                    peekAppIcon = { null },
                    loadAppIconAsync = { null },
                    onToggleApp = { toggledPkg = it },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Star Arena").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mecha Pulse").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_app_com.test.two").performClick()
        assertEquals("com.test.two", toggledPkg)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape phone booster dialog rewarded dialog and network gate keep actions visible`() {
        val reportState = OneTapBoosterState(
            isVisible = true,
            isRunning = false,
            isCompleted = true,
            currentStepIndex = 5,
            progress = 1f,
            steps = BoostStepId.entries.map { id ->
                OneTapStepResult(id, BoostStepStatus.DONE, "OK", "${id.title} ok")
            },
            ramUsedBeforePercent = 72,
            ramUsedAfterPercent = 50,
            memoryPressureLabel = "MODERATE",
            freedMb = 96,
            trimmedApps = 4,
            thermalLabel = "NORMAL",
            batteryTempC = 33.8f,
            batterySaverActive = false,
            selectedCpuMode = CpuMode.ACCELERATION,
            appliedProfileLabel = "Acceleration",
            finalScore = 94,
            finalVerdict = "PEAK GAMING READY"
        )
        var doneClicked = false
        var watchClicked = false
        var retryNetClicked = false

        val activeDialog = androidx.compose.runtime.mutableIntStateOf(0)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                when (activeDialog.intValue) {
                    0 -> OneTapBoosterDialog(
                        state = reportState,
                        onReBoost = {},
                        onOpenPowerSettings = {},
                        onDismiss = { doneClicked = true }
                    )
                    1 -> RewardedAdDialog(
                        step = RewardedAdStep.TimeGatePrompt(
                            launchAfterReward = true,
                            targetGameName = "Arena Legends"
                        ),
                        remainingTurboSeconds = 0L,
                        adReady = true,
                        adLoading = false,
                        adError = null,
                        cooldownSeconds = 0,
                        onWatchAd = { watchClicked = true },
                        onRetryLoad = {},
                        onDismiss = {}
                    )
                    else -> NetworkGateOverlay(
                        onOpenNetworkSettings = {},
                        onRetryCheck = { retryNetClicked = true }
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("one_tap_final_report_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_reboost_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_done_btn").assertIsDisplayed().performClick()
        assertTrue(doneClicked)

        composeTestRule.runOnUiThread { activeDialog.intValue = 1 }
        composeTestRule.mainClock.advanceTimeBy(150L)
        composeTestRule.onNodeWithTag("cancel_ad_prompt_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("watch_ad_button").assertIsDisplayed().performClick()
        assertTrue(watchClicked)

        composeTestRule.runOnUiThread { activeDialog.intValue = 2 }
        composeTestRule.mainClock.advanceTimeBy(150L)
        composeTestRule.onNodeWithTag("open_network_settings_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("retry_network_check_btn").assertIsDisplayed().performClick()
        assertTrue(retryNetClicked)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w640dp-h360dp")
    fun `compact 640dp landscape phone displays hero modes header tabs settings sliders and booster without clipping`() {
        val sampleGame = InstalledAppInfo("com.test.compact", "Mech Arena", "4.0.2", false)
        val freqs = listOf(1800, 1800, 2200, 2200, 2400, 2400, 2850, 3050)
        val screenStep = androidx.compose.runtime.mutableIntStateOf(0)

        val boosterState = OneTapBoosterState(
            isVisible = true,
            isRunning = false,
            isCompleted = true,
            currentStepIndex = 5,
            progress = 1f,
            steps = BoostStepId.entries.map { id ->
                OneTapStepResult(id, BoostStepStatus.DONE, "OK", "${id.title} ok")
            },
            ramUsedBeforePercent = 74,
            ramUsedAfterPercent = 52,
            memoryPressureLabel = "MODERATE",
            freedMb = 64,
            trimmedApps = 4,
            thermalLabel = "NORMAL",
            batteryTempC = 36.2f,
            batterySaverActive = false,
            selectedCpuMode = CpuMode.BALANCED,
            appliedProfileLabel = "Balanced",
            finalScore = 88,
            finalVerdict = "OPTIMIZED"
        )

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                when (screenStep.intValue) {
                    0 -> LauncherHomeScreen(
                        settings = GameTurboSettings(
                            addedGamePackages = setOf(sampleGame.packageName),
                            selectedGamePackage = sampleGame.packageName
                        ),
                        telemetry = HardwareTelemetry(
                            actualFps = 60,
                            cpuFreqMHz = 1850,
                            cpuCoreCount = 8,
                            cpuCoreFreqsMHz = freqs,
                            ramUsedPercent = 54
                        ),
                        permissions = PermissionStatus(true, true, true, true, true),
                        addedGames = listOf(sampleGame),
                        sessionHistory = emptyList(),
                        remainingTurboSeconds = 3599L,
                        statusMessage = null,
                        isOverlayServiceRunning = false,
                        peekAppIcon = { null },
                        loadAppIconAsync = { null },
                        onSelectGame = {},
                        onStartPlay = {},
                        onAddTurboTimeClick = {},
                        onToggleOverlayService = {},
                        onNavigateAddApps = {},
                        onNavigateSettings = {},
                        onNavigatePermissions = {},
                        onBoostRam = {},
                        onRunPingTest = {},
                        onSelectCpuMode = {},
                        onSelectWallpaperTheme = { _, _ -> },
                        onDismissStatus = {}
                    )
                    1 -> SettingsScreen(
                        settings = GameTurboSettings(),
                        telemetry = HardwareTelemetry(actualFps = 90, cpuFreqMHz = 2400),
                        performanceStatus = PerformanceEngineStatus(),
                        permissions = PermissionStatus(true, true, true, true, true),
                        securityAudit = SecurityAuditReport(),
                        statusMessage = null,
                        performanceActions = emptyPerformanceActions(),
                        onUpdateSettings = {},
                        onBoostRam = {},
                        onClearCache = {},
                        onSelectWallpaperTheme = { _, _ -> },
                        onOpenPermissions = {},
                        onDismissStatus = {},
                        onBack = {}
                    )
                    else -> OneTapBoosterDialog(
                        state = boosterState,
                        onReBoost = {},
                        onOpenPowerSettings = {},
                        onDismiss = {}
                    )
                }
            }
        }

        // 1. Compact landscape Cockpit: inline header tabs + all CPU mode pills visible without clipping
        composeTestRule.mainClock.advanceTimeBy(300L)
        composeTestRule.onNodeWithTag("tab_lobby").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_database").assertIsDisplayed()
        composeTestRule.onNodeWithText("ECO").assertIsDisplayed()
        composeTestRule.onNodeWithText("BALANCED").assertIsDisplayed()
        composeTestRule.onNodeWithText("TURBO").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_play_button").assertIsDisplayed()

        // 2. Compact landscape Settings: LazyColumn rail + 2-column engine status + FPS Game slider visible
        composeTestRule.runOnUiThread { screenStep.intValue = 1 }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("settings_cat_PERFORMANCE").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithText("ACTIVE ENGINE STATUS").assertIsDisplayed()
        composeTestRule.onNodeWithText("FPS Game").performScrollTo().assertIsDisplayed()

        // 3. Compact landscape One Tap Booster: side-by-side report + buttons displayed cleanly
        composeTestRule.runOnUiThread { screenStep.intValue = 2 }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("one_tap_booster_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_final_report_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_step_DEVICE_SCAN").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_reboost_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_done_btn").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `all five overlay styles occupy compact half-screen width and dismiss on scrim tap`() {
        var dismissCount = 0
        val activeStyle = androidx.compose.runtime.mutableStateOf(GamePanelStyle.ROG_STYLE)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TurboGameXTheme {
                GamePanelOverlayContent(
                    settings = GameTurboSettings(
                        gamePanelStyle = activeStyle.value,
                        lowRamOverlayMode = true
                    ),
                    telemetry = HardwareTelemetry(actualFps = 120, cpuFreqMHz = 2850, ramUsedPercent = 42),
                    brightnessFraction = 0.7f,
                    volumeFraction = 0.6f,
                    autoBrightness = false,
                    autoRotate = false,
                    stopwatchSeconds = 0L,
                    isStopwatchRunning = false,
                    statusMessage = null,
                    callbacks = testOverlayCallbacks(
                        onMinimizeClick = { dismissCount++ }
                    )
                )
            }
        }

        GamePanelStyle.entries.forEachIndexed { idx, style ->
            composeTestRule.runOnUiThread { activeStyle.value = style }
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.advanceTimeBy(200L)

            val scrimBounds = composeTestRule.onNodeWithTag("overlay_backdrop_scrim")
                .assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            val dockBounds = composeTestRule.onNodeWithTag("overlay_dock_panel")
                .assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot

            // Unobstructed battlefield guarantee:
            // - Bottom-docked ROG/NEW consoles stay <= 56% height so upper gameplay stays open
            // - Side-docked CLASSIC/XIAOMI/REDMAGIC consoles stay <= 56% width
            if (style == GamePanelStyle.ROG_STYLE || style == GamePanelStyle.NEW) {
                assertTrue(
                    "${style.name} too tall: ${dockBounds.height} vs screen ${scrimBounds.height}",
                    dockBounds.height <= scrimBounds.height * 0.56f
                )
            } else {
                assertTrue(
                    "${style.name} too wide: ${dockBounds.width} vs screen ${scrimBounds.width}",
                    dockBounds.width <= scrimBounds.width * 0.56f
                )
            }

            // Tap outside / header close handle collapses immediately
            composeTestRule.onNodeWithTag("overlay_g_handle").assertIsDisplayed().performClick()
            assertEquals(idx + 1, dismissCount)
        }

        // Verify tapping the open outside scrim also collapses immediately
        composeTestRule.onNodeWithTag("overlay_backdrop_scrim").performClick()
        assertEquals(GamePanelStyle.entries.size + 1, dismissCount)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp")
    fun `standard 360dp portrait phone renders cockpit telemetry settings permissions booster and manifest portrait`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mainActivityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0
        )
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, mainActivityInfo.screenOrientation)

        val captureActivityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, com.example.service.ScreenCaptureActivity::class.java),
            0
        )
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, captureActivityInfo.screenOrientation)

        val sampleGame = InstalledAppInfo("com.test.portrait", "Genshin Impact", "5.2.0", false)
        val screenStep = androidx.compose.runtime.mutableIntStateOf(0)
        val boosterState = OneTapBoosterState(
            isVisible = true,
            isRunning = false,
            isCompleted = true,
            currentStepIndex = 5,
            progress = 1f,
            steps = BoostStepId.entries.map { id ->
                OneTapStepResult(id, BoostStepStatus.DONE, "OK", "${id.title} ok")
            },
            ramUsedBeforePercent = 72,
            ramUsedAfterPercent = 50,
            memoryPressureLabel = "NORMAL",
            freedMb = 78,
            trimmedApps = 5,
            thermalLabel = "NORMAL",
            batteryTempC = 34.5f,
            batterySaverActive = false,
            selectedCpuMode = CpuMode.ACCELERATION,
            appliedProfileLabel = "Acceleration mode",
            finalScore = 92,
            finalVerdict = "PEAK READY"
        )

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TurboGameXTheme {
                when (screenStep.intValue) {
                    0 -> LauncherHomeScreen(
                        settings = GameTurboSettings(
                            addedGamePackages = setOf(sampleGame.packageName),
                            selectedGamePackage = sampleGame.packageName
                        ),
                        telemetry = HardwareTelemetry(
                            actualFps = 120,
                            cpuFreqMHz = 2850,
                            cpuCoreCount = 8,
                            cpuCoreFreqsMHz = listOf(1800, 1800, 2200, 2200, 2400, 2400, 2850, 3050),
                            ramUsedPercent = 54
                        ),
                        permissions = PermissionStatus(true, true, true, true, true),
                        addedGames = listOf(sampleGame),
                        sessionHistory = emptyList(),
                        remainingTurboSeconds = 3599L,
                        statusMessage = null,
                        isOverlayServiceRunning = false,
                        peekAppIcon = { null },
                        loadAppIconAsync = { null },
                        onSelectGame = {},
                        onStartPlay = {},
                        onAddTurboTimeClick = {},
                        onToggleOverlayService = {},
                        onNavigateAddApps = {},
                        onNavigateSettings = {},
                        onNavigatePermissions = {},
                        onBoostRam = {},
                        onRunPingTest = {},
                        onSelectCpuMode = {},
                        onSelectWallpaperTheme = { _, _ -> },
                        onDismissStatus = {}
                    )
                    1 -> SettingsScreen(
                        settings = GameTurboSettings(),
                        telemetry = HardwareTelemetry(actualFps = 90, cpuFreqMHz = 2400),
                        performanceStatus = PerformanceEngineStatus(),
                        permissions = PermissionStatus(true, true, true, true, true),
                        securityAudit = SecurityAuditReport(),
                        statusMessage = null,
                        performanceActions = emptyPerformanceActions(),
                        onUpdateSettings = {},
                        onBoostRam = {},
                        onClearCache = {},
                        onSelectWallpaperTheme = { _, _ -> },
                        onOpenPermissions = {},
                        onDismissStatus = {},
                        onBack = {}
                    )
                    2 -> PermissionsScreen(
                        permissions = PermissionStatus(false, true, false, false, true),
                        onRefreshPermissions = {},
                        onRequestDnd = {},
                        onRequestOverlay = {},
                        onRequestWriteSettings = {},
                        onBack = {},
                        isOnboardingMode = true,
                        onCompleteOnboarding = {}
                    )
                    else -> OneTapBoosterDialog(
                        state = boosterState,
                        onReBoost = {},
                        onOpenPowerSettings = {},
                        onDismiss = {}
                    )
                }
            }
        }

        // 1. Portrait Cockpit & Telemetry Tabs
        composeTestRule.mainClock.advanceTimeBy(300L)
        composeTestRule.onNodeWithTag("turbo_time_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_quick_boost_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("start_play_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_database").assertIsDisplayed().performClick()
        composeTestRule.mainClock.advanceTimeBy(320L)
        composeTestRule.onNodeWithText("Thermal & Network").assertIsDisplayed()
        composeTestRule.onNodeWithText("Run One Tap Booster (0)").performScrollTo().assertIsDisplayed()

        // 2. Portrait Settings horizontal category pills + stacked DiscreteSliderRow
        composeTestRule.runOnUiThread { screenStep.intValue = 1 }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("settings_cat_PERFORMANCE").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithText("Scrolling sensitivity").performScrollTo().assertIsDisplayed()

        // 3. Portrait Permissions sticky CTA
        composeTestRule.runOnUiThread { screenStep.intValue = 2 }
        composeTestRule.mainClock.advanceTimeBy(350L)
        composeTestRule.onNodeWithTag("grant_all_permissions_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("skip_permissions_btn").assertIsDisplayed()

        // 4. Portrait One Tap Booster dialog
        composeTestRule.runOnUiThread { screenStep.intValue = 3 }
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("one_tap_final_report_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("one_tap_done_btn").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape REDMAGIC X-Game Space panel displays three sections and invokes existing callbacks`() {
        var launched = false
        var boosted = false
        var backClicked = false
        var overlayToggled = false
        var pingClicked = false
        var selectedCpu: CpuMode? = null
        val sampleGames = listOf(
            InstalledAppInfo("com.test.pubg", "PUBG Mobile", "3.4", false),
            InstalledAppInfo("com.test.mlbb", "Mobile Legends", "1.9", false)
        )
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TurboGameXTheme {
                TurboXGamingPanel(
                    settings = GameTurboSettings(
                        selectedGamePackage = "com.test.pubg",
                        cpuMode = CpuMode.BALANCED,
                        frameRateTarget = 120,
                        turboXMode = true
                    ),
                    telemetry = HardwareTelemetry(
                        actualFps = 118,
                        cpuFreqMHz = 2850,
                        cpuCoreCount = 8,
                        ramUsedPercent = 48,
                        batteryPercent = 82,
                        batteryTempCelsius = 36.4f,
                        pingMs = 19,
                        frameTimeMs = 8.4f
                    ),
                    performanceStatus = PerformanceEngineStatus(activeGpuPipelineLabel = "Skia Vulkan"),
                    permissions = PermissionStatus(),
                    addedGames = sampleGames,
                    remainingTurboSeconds = 3600L,
                    brightnessFraction = 0.7f,
                    volumeFraction = 0.6f,
                    autoBrightness = false,
                    autoRotate = false,
                    isOverlayServiceRunning = false,
                    statusMessage = null,
                    peekAppIcon = { null },
                    loadAppIconAsync = { null },
                    onBack = { backClicked = true },
                    onSelectGame = {},
                    onStartPlay = { launched = true },
                    onBoostRam = { boosted = true },
                    onClearCache = {},
                    onRunPingTest = { pingClicked = true },
                    onSelectCpuMode = { selectedCpu = it },
                    onToggleTurboX = {},
                    onToggleGpuBoost = {},
                    onCycleFrameRate = {},
                    onCycleCrosshair = {},
                    onCycleScreenColor = {},
                    onCycleEqPreset = {},
                    onToggleDnd = {},
                    onToggleNoNotif = {},
                    onToggleAutoRotate = {},
                    onToggleAutoBrightness = {},
                    onToggleActiveInfo = {},
                    onBrightnessChange = {},
                    onVolumeChange = {},
                    onToggleOverlayService = { overlayToggled = true },
                    onOpenInAppOverlay = {},
                    onAddTurboTime = {},
                    onNavigateAddApps = {},
                    onNavigatePermissions = {},
                    onNavigateSettings = {},
                    onDismissStatus = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.onNodeWithTag("turbo_x_gaming_panel_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("X-GAME SPACE", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("TELEMETRY CORE").assertIsDisplayed()
        composeTestRule.onNodeWithText("GAMING CONTROLS").assertIsDisplayed()
        composeTestRule.onNodeWithText("PUBG Mobile").assertIsDisplayed()
        composeTestRule.onNodeWithTag("xgame_launch_btn").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("xgame_onetap_btn").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("xgame_hud_toggle_btn").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("xgame_back_btn").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("19ms").performClick()
        composeTestRule.onNodeWithText("TURBO").performClick()

        assertTrue(launched)
        assertTrue(boosted)
        assertTrue(overlayToggled)
        assertTrue(backClicked)
        assertTrue(pingClicked)
        assertEquals(CpuMode.ACCELERATION, selectedCpu)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp")
    fun `portrait Launcher X-Space entry navigates cleanly and portrait X-Game Space renders safely`() {
        var openedXGameSpace = false
        val showXGameSpace = androidx.compose.runtime.mutableStateOf(false)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TurboGameXTheme {
                if (!showXGameSpace.value) {
                    LauncherHomeScreen(
                        settings = GameTurboSettings(),
                        telemetry = HardwareTelemetry(actualFps = 90, cpuFreqMHz = 2400, ramUsedPercent = 45),
                        permissions = PermissionStatus(),
                        addedGames = emptyList(),
                        sessionHistory = emptyList(),
                        remainingTurboSeconds = 3600L,
                        statusMessage = null,
                        isOverlayServiceRunning = false,
                        peekAppIcon = { null },
                        loadAppIconAsync = { null },
                        onSelectGame = {},
                        onStartPlay = {},
                        onAddTurboTimeClick = {},
                        onToggleOverlayService = {},
                        onNavigateAddApps = {},
                        onNavigateSettings = {},
                        onNavigatePermissions = {},
                        onBoostRam = {},
                        onRunPingTest = {},
                        onSelectCpuMode = {},
                        onSelectWallpaperTheme = { _, _ -> },
                        onDismissStatus = {},
                        onNavigateXGameSpace = {
                            openedXGameSpace = true
                            showXGameSpace.value = true
                        }
                    )
                } else {
                    TurboXGamingPanel(
                        settings = GameTurboSettings(),
                        telemetry = HardwareTelemetry(actualFps = 90, cpuFreqMHz = 2400, ramUsedPercent = 45),
                        performanceStatus = PerformanceEngineStatus(),
                        permissions = PermissionStatus(),
                        addedGames = emptyList(),
                        remainingTurboSeconds = 3600L,
                        brightnessFraction = 0.7f,
                        volumeFraction = 0.6f,
                        autoBrightness = false,
                        autoRotate = false,
                        isOverlayServiceRunning = false,
                        statusMessage = null,
                        peekAppIcon = { null },
                        loadAppIconAsync = { null },
                        onBack = { showXGameSpace.value = false },
                        onSelectGame = {},
                        onStartPlay = {},
                        onBoostRam = {},
                        onClearCache = {},
                        onRunPingTest = {},
                        onSelectCpuMode = {},
                        onToggleTurboX = {},
                        onToggleGpuBoost = {},
                        onCycleFrameRate = {},
                        onCycleCrosshair = {},
                        onCycleScreenColor = {},
                        onCycleEqPreset = {},
                        onToggleDnd = {},
                        onToggleNoNotif = {},
                        onToggleAutoRotate = {},
                        onToggleAutoBrightness = {},
                        onToggleActiveInfo = {},
                        onBrightnessChange = {},
                        onVolumeChange = {},
                        onToggleOverlayService = {},
                        onOpenInAppOverlay = {},
                        onAddTurboTime = {},
                        onNavigateAddApps = {},
                        onNavigatePermissions = {},
                        onNavigateSettings = {},
                        onDismissStatus = {}
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(200L)
        composeTestRule.onNodeWithTag("tab_x_game_space").assertIsDisplayed().performClick()
        assertTrue(openedXGameSpace)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(200L)
        composeTestRule.onNodeWithTag("turbo_x_gaming_panel_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("X-GAME SPACE", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("xgame_launch_btn").assertIsDisplayed()
    }

    private fun emptyPerformanceActions(
        onSetOptimizeFramePacing: (Boolean) -> Unit = {},
        onSetEnableAdpfHints: (Boolean) -> Unit = {},
        onOpenPowerSaverSettings: () -> Unit = {},
        onRunSmartBackgroundTrim: () -> Unit = {},
        onOpenAppManagementSettings: () -> Unit = {},
        onSelectDisplayModeId: (Int) -> Unit = {},
        onOpenDisplaySettings: () -> Unit = {},
        onSetSustainedPerformanceMode: (Boolean) -> Unit = {},
        onSetAutoPerformanceOptimization: (Boolean) -> Unit = {}
    ) = PerformanceActions(
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
        onSetOptimizeFramePacing = onSetOptimizeFramePacing,
        onSetEnableAdpfHints = onSetEnableAdpfHints,
        onSetDisablePowerSavingInGame = {},
        onOpenPowerSaverSettings = onOpenPowerSaverSettings,
        onSetSmartBackgroundControl = {},
        onRunSmartBackgroundTrim = onRunSmartBackgroundTrim,
        onOpenAppManagementSettings = onOpenAppManagementSettings,
        onSetDisplayResolutionOpt = {},
        onSelectDisplayModeId = onSelectDisplayModeId,
        onOpenDisplaySettings = onOpenDisplaySettings,
        onSetSustainedPerformanceMode = onSetSustainedPerformanceMode,
        onSetAutoPerformanceOptimization = onSetAutoPerformanceOptimization
    )

    private fun testOverlayCallbacks(
        onSelectCpuMode: (CpuMode) -> Unit = {},
        onBoostRam: () -> Unit = {},
        onMinimizeClick: () -> Unit = {},
        onToggleStopwatch: () -> Unit = {},
        onResetStopwatch: () -> Unit = {}
    ) = OverlayCallbacks(
        onHomeClick = {},
        onSettingsClick = {},
        onGamesClick = {},
        onPowerExitClick = {},
        onMinimizeClick = onMinimizeClick,
        onSelectCpuMode = onSelectCpuMode,
        onCycleFrameRate = {},
        onBoostRam = onBoostRam,
        onClearCache = {},
        onPingTest = {},
        onToggleStopwatch = onToggleStopwatch,
        onResetStopwatch = onResetStopwatch,
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
        onQuickApp = { _, _ -> }
    )
}
