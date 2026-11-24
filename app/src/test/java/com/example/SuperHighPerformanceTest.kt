package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdUnitIds
import com.example.data.GameTurboPreferencesRepository
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.PermissionStatus
import com.example.data.SystemHardwareController
import com.example.extreme.ActivationAttemptResult
import com.example.extreme.BackgroundOptimizer
import com.example.extreme.ExtremeModeState
import com.example.extreme.ExtremePerformanceManager
import com.example.extreme.ExtremeSessionServiceBridge
import com.example.extreme.ExtremeWarningSpec
import com.example.extreme.ForegroundAppInspector
import com.example.extreme.ForegroundDecision
import com.example.extreme.OptimizationAggressiveness
import com.example.extreme.RawThermalReading
import com.example.extreme.SelectedGameManager
import com.example.extreme.SelectedGameMonitor
import com.example.extreme.ThermalProtectionLevel
import com.example.extreme.ThermalProtectionManager
import com.example.extreme.ThermalTelemetryReader
import com.example.extreme.UnlockStepResult
import com.example.ui.components.SuperHighPerformanceWarningDialog
import com.example.ui.screens.SuperHighPerformanceScreen
import com.example.ui.theme.TurboGameXTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SuperHighPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val pubgPkg = "com.tencent.ig"
    private val mlbbPkg = "com.mobile.legends"
    private val codmPkg = "com.activision.callofduty.shooter"
    private val launcherPkg = "com.android.launcher3"
    private val socialPkg = "com.example.chatapp"

    private val testApps = listOf(
        InstalledAppInfo(pubgPkg, "PUBG Mobile", "3.4.0", isSystemApp = false),
        InstalledAppInfo(mlbbPkg, "Mobile Legends", "1.9.0", isSystemApp = false),
        InstalledAppInfo(codmPkg, "Call of Duty Mobile", "1.0.4", isSystemApp = false),
        InstalledAppInfo(socialPkg, "Chat App", "2.0.1", isSystemApp = false)
    )

    private class FakeThermalReader(
        var reading: RawThermalReading = RawThermalReading(
            batteryTempCelsius = 33.5f,
            thermalStatusCode = 0,
            batteryPercent = 82,
            isCharging = false
        )
    ) : ThermalTelemetryReader {
        override fun readRaw(): RawThermalReading = reading
    }

    private class FakeForegroundInspector(
        var usageGranted: Boolean = true,
        var foregroundPkg: String? = null
    ) : ForegroundAppInspector {
        override fun hasUsageAccess(): Boolean = usageGranted
        override fun currentForegroundPackage(): String? = foregroundPkg
    }

    private class FakeSessionServiceBridge : ExtremeSessionServiceBridge {
        var startedCount = 0
        var stoppedCount = 0
        var lastGamePkg: String? = null
        private var running = false
        private var wakeLock = false

        override val isServiceRunning: Boolean get() = running
        override val isWakeLockHeld: Boolean get() = wakeLock

        override fun startSessionService(
            gameName: String,
            gamePackage: String,
            acquireWakeLock: Boolean
        ) {
            startedCount++
            lastGamePkg = gamePackage
            running = true
            wakeLock = acquireWakeLock
        }

        override fun stopSessionService() {
            stoppedCount++
            running = false
            wakeLock = false
            lastGamePkg = null
        }
    }

    private fun createHarness(
        repo: GameTurboPreferencesRepository,
        thermalReader: FakeThermalReader = FakeThermalReader(),
        fgInspector: FakeForegroundInspector = FakeForegroundInspector(),
        serviceBridge: FakeSessionServiceBridge = FakeSessionServiceBridge()
    ): Quad {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hardware = SystemHardwareController(context)
        val installedSet = testApps.map { it.packageName }.toSet()
        val selectedGameManager = SelectedGameManager(context, repo) { it in installedSet }
        val thermalManager = ThermalProtectionManager(context, thermalReader)
        val bgOptimizer = BackgroundOptimizer(context, hardware)
        val monitor = SelectedGameMonitor(context, fgInspector)
        val manager = ExtremePerformanceManager(
            context = context,
            repo = repo,
            hardware = hardware,
            selectedGameManager = selectedGameManager,
            thermalManager = thermalManager,
            backgroundOptimizer = bgOptimizer,
            selectedGameMonitor = monitor,
            serviceBridge = serviceBridge
        )
        return Quad(context, hardware, manager, serviceBridge)
    }

    private data class Quad(
        val context: Context,
        val hardware: SystemHardwareController,
        val manager: ExtremePerformanceManager,
        val bridge: FakeSessionServiceBridge
    )

    private suspend fun seedUnlockedWithGames(
        repo: GameTurboPreferencesRepository,
        selectedPkg: String? = null
    ): GameTurboSettings {
        repo.updateSettings {
            it.copy(
                addedGamePackages = setOf(pubgPkg, mlbbPkg, codmPkg),
                superPerfSelectedGamePackage = selectedPkg,
                superPerfAd1Watched = true,
                superPerfAd2Watched = true,
                superPerfUnlocked = true,
                superPerfUnlockedAtMs = System.currentTimeMillis(),
                superPerfWarningAccepted = true,
                superPerfAutoStartOnLaunch = true
            )
        }
        return repo.settingsFlow.first()
    }

    private val defaultTelemetry = HardwareTelemetry(
        cpuFreqMHz = 2400,
        actualFps = 90,
        frameTimeMs = 11.1f,
        displayRefreshRateHz = 120,
        supportedRefreshRates = listOf(60, 90, 120),
        networkType = "WiFi",
        pingMs = 24,
        pingJitterMs = 4
    )

    private val fullPerms = PermissionStatus(
        dndGranted = true,
        overlayGranted = true,
        writeSettingsGranted = true,
        usageStatsGranted = true,
        notificationsGranted = true
    )

    // Scenario 1: Selecting PUBG optimizes only PUBG
    @Test
    fun `1 selecting PUBG optimizes only PUBG`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val harness = createHarness(repo)
        val s = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)

        val result = harness.manager.activateForSelectedGame(
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = listOf(socialPkg, mlbbPkg)
        )
        assertTrue(result is ActivationAttemptResult.Activated)
        assertEquals(pubgPkg, harness.manager.activePackageName)
        assertEquals(ExtremeModeState.ACTIVE, harness.manager.uiState.value.modeState)
        assertEquals(pubgPkg, harness.manager.uiState.value.metrics.targetPackage)
        assertEquals(pubgPkg, harness.bridge.lastGamePkg)
        assertTrue(harness.bridge.isServiceRunning)
    }

    // Scenario 2: Selecting MLBB optimizes only MLBB
    @Test
    fun `2 selecting MLBB optimizes only MLBB`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val harness = createHarness(repo)
        val s = seedUnlockedWithGames(repo, selectedPkg = mlbbPkg)

        val result = harness.manager.activateForSelectedGame(
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = listOf(socialPkg, pubgPkg)
        )
        assertTrue(result is ActivationAttemptResult.Activated)
        assertEquals(mlbbPkg, harness.manager.activePackageName)
        assertEquals(mlbbPkg, harness.manager.uiState.value.metrics.targetPackage)
        assertEquals(mlbbPkg, harness.bridge.lastGamePkg)
    }

    // Scenario 3: Launching non-selected game does not activate Extreme Mode
    @Test
    fun `3 launching non-selected game does not activate Extreme Mode`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val harness = createHarness(repo)
        val s = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)

        val eval = harness.manager.evaluateForegroundApp(
            foregroundPackage = mlbbPkg,
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = listOf(socialPkg)
        )
        assertEquals(ForegroundDecision.IGNORE_NON_SELECTED, eval.decision)
        assertFalse(harness.manager.isSessionActive)
        assertNull(harness.manager.activePackageName)
        assertFalse(harness.bridge.isServiceRunning)
    }

    // Scenario 4: Exiting the selected game stops all optimizations
    @Test
    fun `4 exiting selected game stops all optimizations and restores settings`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val harness = createHarness(repo)
        val s = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)

        harness.manager.evaluateForegroundApp(
            foregroundPackage = pubgPkg,
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = listOf(socialPkg)
        )
        assertTrue(harness.manager.isSessionActive)
        assertTrue(harness.bridge.isServiceRunning)
        assertTrue(harness.manager.backgroundOptimizer.turboWorkloadsThrottled.value)

        val exitEval = harness.manager.evaluateForegroundApp(
            foregroundPackage = launcherPkg,
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = listOf(socialPkg)
        )
        assertEquals(ForegroundDecision.EXIT_STOP, exitEval.decision)
        assertFalse(harness.manager.isSessionActive)
        assertFalse(harness.bridge.isServiceRunning)
        assertFalse(harness.bridge.isWakeLockHeld)
        assertFalse(harness.manager.backgroundOptimizer.turboWorkloadsThrottled.value)
        assertEquals(ExtremeModeState.INACTIVE, harness.manager.uiState.value.modeState)
        assertEquals(OptimizationAggressiveness.STOPPED, harness.manager.uiState.value.metrics.aggressiveness)
    }

    // Scenario 5: Switching from one selected game to another stops previous session first
    @Test
    fun `5 switching selected game stops previous session before saving new target`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val harness = createHarness(repo)
        val s = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)

        harness.manager.activateForSelectedGame(
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = listOf(socialPkg)
        )
        assertTrue(harness.manager.isSessionActive)
        assertEquals(pubgPkg, harness.manager.activePackageName)
        val stopsBefore = harness.bridge.stoppedCount

        harness.manager.selectGame(
            packageName = mlbbPkg,
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms
        )

        assertFalse(harness.manager.isSessionActive)
        assertTrue(harness.bridge.stoppedCount > stopsBefore)
        assertFalse(harness.bridge.isServiceRunning)
        assertEquals(mlbbPkg, repo.settingsFlow.first().superPerfSelectedGamePackage)
    }

    // Scenario 6: Watching Ad 1 alone does not unlock + prevents duplicate/skipped rewards
    @Test
    fun `6 watching Ad 1 alone keeps mode locked and blocks duplicate or skipped rewards`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        repo.updateSettings {
            it.copy(
                superPerfAd1Watched = false,
                superPerfAd2Watched = false,
                superPerfUnlocked = false
            )
        }
        val harness = createHarness(repo)
        val unlockMgr = harness.manager.unlockManager
        unlockMgr.reloadPersistedState()

        // Skipped ad does not grant progress
        val skippedToken = unlockMgr.beginAdAttempt(1)
        assertNotNull(skippedToken)
        unlockMgr.recordAdSkippedOrFailed("Ad closed early")
        val afterSkip = unlockMgr.verifyAdRewardEarned(1, skippedToken!!)
        assertTrue(afterSkip is UnlockStepResult.Rejected)
        assertFalse(unlockMgr.state.value.ad1Watched)
        assertFalse(unlockMgr.state.value.isUnlocked)

        // Valid Ad 1 grants step 1 only — still LOCKED
        val token1 = unlockMgr.beginAdAttempt(1)!!
        val res1 = unlockMgr.verifyAdRewardEarned(1, token1)
        assertTrue(res1 is UnlockStepResult.Step1Completed)
        assertTrue(unlockMgr.state.value.ad1Watched)
        assertFalse(unlockMgr.state.value.ad2Watched)
        assertFalse(unlockMgr.state.value.isUnlocked)

        // Replaying Ad 1 token fails duplicate guard
        val replay = unlockMgr.verifyAdRewardEarned(1, token1)
        assertTrue(replay is UnlockStepResult.Rejected)
        assertFalse(unlockMgr.state.value.isUnlocked)
    }

    // Scenario 7: Watching Ad 1 and Ad 2 unlocks Super High Performance Mode
    @Test
    fun `7 watching Ad 1 and Ad 2 unlocks Super High Performance Mode`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        repo.updateSettings {
            it.copy(
                superPerfAd1Watched = false,
                superPerfAd2Watched = false,
                superPerfUnlocked = false
            )
        }
        val harness = createHarness(repo)
        val unlockMgr = harness.manager.unlockManager
        unlockMgr.reloadPersistedState()

        val token1 = unlockMgr.beginAdAttempt(1)!!
        assertTrue(unlockMgr.verifyAdRewardEarned(1, token1) is UnlockStepResult.Step1Completed)
        assertFalse(unlockMgr.state.value.isUnlocked)

        val token2 = unlockMgr.beginAdAttempt(2)!!
        val res2 = unlockMgr.verifyAdRewardEarned(2, token2)
        assertTrue(res2 is UnlockStepResult.Unlocked)
        assertTrue(unlockMgr.state.value.ad1Watched)
        assertTrue(unlockMgr.state.value.ad2Watched)
        assertTrue(unlockMgr.state.value.isUnlocked)
    }

    // Scenario 8: App restart preserves unlock state
    @Test
    fun `8 app restart preserves unlock state and selected game persistently`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        repo.updateSettings {
            it.copy(
                addedGamePackages = setOf(pubgPkg, mlbbPkg),
                superPerfSelectedGamePackage = pubgPkg,
                superPerfAd1Watched = false,
                superPerfAd2Watched = false,
                superPerfUnlocked = false
            )
        }
        val firstHarness = createHarness(repo)
        val t1 = firstHarness.manager.unlockManager.beginAdAttempt(1)!!
        firstHarness.manager.unlockManager.verifyAdRewardEarned(1, t1)
        val t2 = firstHarness.manager.unlockManager.beginAdAttempt(2)!!
        firstHarness.manager.unlockManager.verifyAdRewardEarned(2, t2)

        // Simulate cold restart with fresh Repository & ExtremePerformanceManager instances
        val restartedRepo = GameTurboPreferencesRepository(context)
        val restartedHarness = createHarness(restartedRepo)
        val reloaded = restartedHarness.manager.unlockManager.reloadPersistedState()
        val persistedSettings = restartedRepo.settingsFlow.first()

        assertTrue(reloaded.ad1Watched)
        assertTrue(reloaded.ad2Watched)
        assertTrue(reloaded.isUnlocked)
        assertEquals(pubgPkg, persistedSettings.superPerfSelectedGamePackage)
    }

    // Scenario 9: High thermal / critical temp triggers Thermal Protection
    @Test
    fun `9 high thermal critical temp engages protection and stops aggressive optimizations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val thermalReader = FakeThermalReader(
            RawThermalReading(34.0f, PowerManager.THERMAL_STATUS_NONE, 85, false)
        )
        val harness = createHarness(repo, thermalReader = thermalReader)
        val s = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)

        harness.manager.activateForSelectedGame(
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = listOf(socialPkg)
        )
        assertEquals(ExtremeModeState.ACTIVE, harness.manager.uiState.value.modeState)

        // Moderate warm heat reduces aggressiveness safely
        thermalReader.reading = RawThermalReading(40.2f, PowerManager.THERMAL_STATUS_MODERATE, 79, false)
        harness.manager.evaluateThermalTick(s, testApps, defaultTelemetry, fullPerms)
        assertEquals(ThermalProtectionLevel.REDUCED, harness.manager.uiState.value.metrics.thermalSnapshot.protectionLevel)
        assertEquals(OptimizationAggressiveness.REDUCED, harness.manager.uiState.value.metrics.aggressiveness)
        assertFalse(harness.bridge.isWakeLockHeld)

        // Critical temp immediately stops optimization and activates THERMAL_PROTECTED
        thermalReader.reading = RawThermalReading(45.5f, PowerManager.THERMAL_STATUS_CRITICAL, 72, false)
        harness.manager.evaluateThermalTick(s, testApps, defaultTelemetry, fullPerms)
        assertEquals(ThermalProtectionLevel.PROTECTED, harness.manager.uiState.value.metrics.thermalSnapshot.protectionLevel)
        assertEquals(ExtremeModeState.THERMAL_PROTECTED, harness.manager.uiState.value.modeState)
        assertFalse(harness.manager.isSessionActive)
        assertFalse(harness.bridge.isServiceRunning)
    }

    // Scenario 10: Incoming phone call & essential services continuity
    @Test
    fun `10 essential call SMS alarm emergency services are never killed and call pauses game session cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val harness = createHarness(repo)
        val s = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)

        val essentialPackages = listOf(
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.android.incallui",
            "com.google.android.apps.messaging",
            "com.google.android.deskclock",
            "com.android.emergency",
            "com.android.systemui"
        )
        val trimList = harness.manager.backgroundOptimizer.selectSafeTrimCandidates(
            knownPackages = essentialPackages + listOf(socialPkg),
            selectedGamePackage = pubgPkg,
            addedGames = setOf(pubgPkg, mlbbPkg)
        )
        assertEquals(listOf(socialPkg), trimList)

        harness.manager.activateForSelectedGame(
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = essentialPackages + listOf(socialPkg)
        )
        assertTrue(harness.manager.isSessionActive)

        // Incoming phone call takes foreground
        val callEval = harness.manager.evaluateForegroundApp(
            foregroundPackage = "com.android.incallui",
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = essentialPackages + listOf(socialPkg)
        )
        assertEquals(ForegroundDecision.EXIT_STOP, callEval.decision)
        assertFalse(harness.manager.isSessionActive)
        assertFalse(harness.bridge.isServiceRunning)
    }

    // Scenario 11: Reboot stops service cleanly (no boot auto-start)
    @Test
    fun `11 reboot stops service cleanly with no RECEIVE_BOOT_COMPLETED persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val perms = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(perms.contains("android.permission.RECEIVE_BOOT_COMPLETED"))

        val repo = GameTurboPreferencesRepository(context)
        val s = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)
        val freshAfterReboot = createHarness(repo)
        freshAfterReboot.manager.syncState(s, testApps, defaultTelemetry, fullPerms)

        assertFalse(freshAfterReboot.manager.isSessionActive)
        assertFalse(freshAfterReboot.bridge.isServiceRunning)
        assertEquals(ExtremeModeState.UNLOCKED, freshAfterReboot.manager.uiState.value.modeState)
        assertEquals(OptimizationAggressiveness.STOPPED, freshAfterReboot.manager.uiState.value.metrics.aggressiveness)
    }

    // Scenario 12: Disabling mode manually restores all temporary settings and releases wakelock/service
    @Test
    fun `12 disabling mode manually restores ADPF GameState workloads and releases wakelock service`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val harness = createHarness(repo)
        val s = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)

        harness.manager.activateForSelectedGame(
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms,
            knownNonEssentialPackages = listOf(socialPkg)
        )
        assertTrue(harness.manager.isSessionActive)
        assertTrue(harness.bridge.isServiceRunning)
        assertTrue(harness.bridge.isWakeLockHeld)
        assertTrue(harness.manager.backgroundOptimizer.turboWorkloadsThrottled.value)

        harness.manager.deactivateAndRestoreAll(
            reason = "Manual disable test",
            settings = s,
            installedApps = testApps,
            telemetry = defaultTelemetry,
            permissions = fullPerms
        )

        assertFalse(harness.manager.isSessionActive)
        assertFalse(harness.bridge.isServiceRunning)
        assertFalse(harness.bridge.isWakeLockHeld)
        assertFalse(harness.manager.backgroundOptimizer.turboWorkloadsThrottled.value)
        assertFalse(harness.hardware.performanceEngine.status.value.adpfHintActive)
        assertFalse(harness.hardware.performanceEngine.status.value.gameStateActive)
        assertEquals(ExtremeModeState.INACTIVE, harness.manager.uiState.value.modeState)
    }

    @Test
    fun `debug builds use Google test rewarded ad unit id and release builds use production unit id`() {
        assertEquals("ca-app-pub-3940256099942544/5224354917", AdUnitIds.effectiveRewarded(isDebug = true))
        assertEquals("ca-app-pub-5984576938417142/5620184189", AdUnitIds.effectiveRewarded(isDebug = false))
    }

    @Test
    fun `warning dialog displays exact required title all 7 effects essential notice and action buttons`() {
        var continued = false
        var cancelled = false

        composeTestRule.setContent {
            TurboGameXTheme {
                SuperHighPerformanceWarningDialog(
                    visible = true,
                    onContinue = { continued = true },
                    onCancel = { cancelled = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(ExtremeWarningSpec.TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(ExtremeWarningSpec.INTRO).assertIsDisplayed()
        ExtremeWarningSpec.EFFECTS.forEach { effect ->
            composeTestRule.onNodeWithText(effect).performScrollTo().assertIsDisplayed()
        }
        composeTestRule.onNodeWithText(ExtremeWarningSpec.IMPORTANT_NOTICE).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("super_perf_warning_cancel_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("super_perf_warning_continue_btn").assertIsDisplayed().performClick()
        assertTrue(continued)
        assertFalse(cancelled)
    }

    @Test
    fun `super high performance screen disables activation when no game is selected and renders selected game details`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameTurboPreferencesRepository(context)
        val harness = createHarness(repo)
        val unlockedNoGame = seedUnlockedWithGames(repo, selectedPkg = null)
        harness.manager.syncState(unlockedNoGame, testApps, defaultTelemetry, fullPerms)

        composeTestRule.setContent {
            TurboGameXTheme {
                SuperHighPerformanceScreen(
                    uiState = harness.manager.uiState.value,
                    adReady = true,
                    adLoading = false,
                    adError = null,
                    adCooldownSeconds = 0,
                    statusMessage = null,
                    peekAppIcon = { null },
                    loadAppIconAsync = { null },
                    onSelectGame = {},
                    onWatchUnlockAd = {},
                    onActivate = {},
                    onDeactivate = {},
                    onToggleAutoStart = {},
                    onViewWarningInfo = {},
                    onRequestUsageAccess = {},
                    onNavigateAddGames = {},
                    onRunPingTest = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("super_perf_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("super_perf_activate_btn").assertIsNotEnabled()
        composeTestRule.onNodeWithText("PUBG Mobile").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(pubgPkg).performScrollTo().assertIsDisplayed()

        // Now verify button enables once unlocked + PUBG selected
        val withPubg = seedUnlockedWithGames(repo, selectedPkg = pubgPkg)
        harness.manager.syncState(withPubg, testApps, defaultTelemetry, fullPerms)
        assertTrue(harness.manager.uiState.value.canActivate)
    }
}
