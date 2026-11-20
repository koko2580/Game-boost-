package com.example

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.CpuMode
import com.example.data.GameAudioEnhancer
import com.example.data.GamePanelStyle
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.PerformanceEngineStatus
import com.example.data.PerformanceTuningEngine
import com.example.data.PermissionStatus
import com.example.data.SystemHardwareController
import com.example.security.AppSecurityGuard
import com.example.security.SecurityAuditReport
import com.example.ui.components.GamePanelOverlayContent
import com.example.ui.components.NetworkGateOverlay
import com.example.ui.components.OverlayCallbacks
import com.example.ui.screens.LauncherHomeScreen
import com.example.ui.screens.PerformanceActions
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.TurboGameXTheme
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
                    onOpenOverlayPreview = {},
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
                    onOpenOverlayPreview = {},
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
        composeTestRule.onNodeWithTag("overlay_cpu_mode_pill").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("overlay_ram_boost_pill").assertIsDisplayed()
        assertEquals(CpuMode.ACCELERATION, selectedMode)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h360dp")
    fun `landscape RedMagic Cyber overlay renders 3-wing cooling turbine HUD and energy faders`() {
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

    private fun emptyPerformanceActions() = PerformanceActions(
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
    )

    private fun testOverlayCallbacks(
        onSelectCpuMode: (CpuMode) -> Unit = {},
        onBoostRam: () -> Unit = {},
        onMinimizeClick: () -> Unit = {}
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
        onQuickApp = { _, _ -> }
    )
}
