package com.example.data

enum class CpuMode(val label: String, val description: String) {
    ENERGY_SAVING(
        label = "Energy saving mode",
        description = "Entering energy saving mode to extend battery endurance."
    ),
    BALANCED(
        label = "Balanced mode",
        description = "Entering balanced mode by optimizing performance."
    ),
    ACCELERATION(
        label = "Acceleration mode",
        description = "Entering performance mode to improve overall performance."
    )
}

enum class CpuUnitFormat {
    GHZ,
    MHZ
}

enum class ControlPanelLayout(val title: String, val subtitle: String) {
    LEFT_EDGE_SWIPE("Left Edge Handle", "Drag right from left screen edge to open Game Mode"),
    RIGHT_EDGE_MENU("Right Edge Menu", "Compact vertical bar: Hide, Open Game Mode, Settings, Close"),
    LEFT_EDGE_MINIMAL("Left Edge Slim", "Minimal edge indicator bar for distraction-free play"),
    TOP_G_HANDLE("Top 'G' Handle", "Floating top 'G' tab — tap or swipe down to open Game Mode")
}

enum class GamePanelStyle(val label: String) {
    CLASSIC("Classic"),
    NEW("New")
}

enum class WallpaperTheme(val label: String) {
    OBSIDIAN_CRIMSON("Obsidian Crimson"),
    CYBER_AMBER("Cyber Amber"),
    ELECTRIC_CYAN("Electric Cyan"),
    STEALTH_CARBON("Stealth Carbon"),
    CUSTOM("Custom Photo")
}

enum class CrosshairStyle(val label: String) {
    NONE("Off"),
    DOT("Red Dot"),
    RETICLE("Cyber Reticle"),
    SNIPER("Precision Cross")
}

enum class ScreenColorMode(val label: String) {
    DEFAULT("Standard"),
    VIVID("Vivid Boost"),
    NIGHT_VISION("Night Contrast"),
    WARM_EYE("Warm Shield")
}

enum class EqPreset(val label: String) {
    OFF("Standard"),
    FPS_FOOTSTEPS("FPS Footsteps"),
    MOBA_CLARITY("MOBA Voice"),
    BASS_BOOST("Turbo Bass")
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean
)

data class GameSessionRecord(
    val packageName: String,
    val appName: String,
    val timestampMillis: Long,
    val cpuMode: CpuMode
)

data class HardwareTelemetry(
    val cpuFreqMHz: Int = 1334,
    val cpuCoreCount: Int = Runtime.getRuntime().availableProcessors(),
    val cpuCoreFreqsMHz: List<Int> = emptyList(),
    val ramUsedPercent: Int = 58,
    val ramAvailableMb: Long = 3400L,
    val ramTotalMb: Long = 8192L,
    val batteryPercent: Int = 80,
    val batteryTempCelsius: Float = 32.5f,
    val batteryVoltageMv: Int = 3950,
    val isCharging: Boolean = false,
    val displayRefreshRateHz: Int = 60,
    val supportedRefreshRates: List<Int> = listOf(60),
    val actualFps: Int = 60,
    val frameTimeMs: Float = 16.6f,
    val gpuRenderer: String = "OpenGL ES / Vulkan Ready",
    val deviceModel: String = android.os.Build.MODEL,
    val androidVersion: String = android.os.Build.VERSION.RELEASE ?: "14",
    val pingMs: Int? = null,
    val pingJitterMs: Int? = null,
    val networkType: String = "Offline",
    val wifiLinkMbps: Int? = null,
    val wifiRssiDbm: Int? = null
)

data class PermissionStatus(
    val dndGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val writeSettingsGranted: Boolean = false,
    val usageStatsGranted: Boolean = false
)

data class BoostResult(
    val freedMb: Int,
    val trimmedPackages: Int,
    val clearedCacheKb: Long
)

data class GameTurboSettings(
    // Disturbance Settings
    val autoDndOnEnter: Boolean = true,
    val muteNotificationsInGame: Boolean = false,
    // Protection Settings
    val preventMemoryLeaks: Boolean = true,
    val disableThreeFingerScreenshot: Boolean = false,
    // CPU Settings
    val cpuUnitFormat: CpuUnitFormat = CpuUnitFormat.MHZ,
    val cpuMode: CpuMode = CpuMode.ACCELERATION,
    // Performance Settings
    val fpsGameTarget: Int = 0, // 0 = OFF, 90, 120, 144
    val frameRateTarget: Int = 60, // 60, 90, 120, 144
    val slidingFrictionIndex: Int = 0, // 0=OFF, 1=2.0, 2=1.0, 3=0.8, 4=0.5, 5=0.1, 6=0.05, 7=0.01
    val scrollingSensitivity: Int = 0, // -7..+7
    val enhanceTouchSensitivity: Boolean = false,
    val reduceTouchLatency: Boolean = false,
    val increaseCpuPerformance: Boolean = false,
    val increaseGpuPerformance: Boolean = false,
    val forceGpuOpenGl: Boolean = false,
    val enableSkiaVulkan: Boolean = false,
    val highPerfFpsControl: Boolean = false,
    // RAM Settings
    val backgroundLimitIndex: Int = 0, // 0=Default, 1=No background, 2=At most 1, 3=At most 2, 4=At most 3
    val cacheLimitIndex: Int = 0, // 0=Default, 1=Minimal, 2=Strict
    // Wallpaper & Visuals
    val wallpaperTheme: WallpaperTheme = WallpaperTheme.OBSIDIAN_CRIMSON,
    val customWallpaperUri: String? = null,
    // Control Panel & Game Panel
    val controlPanelLayout: ControlPanelLayout = ControlPanelLayout.RIGHT_EDGE_MENU,
    val gamePanelStyle: GamePanelStyle = GamePanelStyle.NEW,
    // Other Options
    val notificationMuteMode: Boolean = false,
    val dndMode: Boolean = false,
    val lockBrightness: Boolean = false,
    val lockedBrightnessValue: Float = 0.75f,
    val rotationLock: Boolean = false,
    val openingAnimation: Boolean = true,
    val homeAnimation: Boolean = true,
    val lowRamOverlayMode: Boolean = false,
    val autoDetectGameLaunch: Boolean = true,
    // In-Game HUD active toggles
    val crosshairStyle: CrosshairStyle = CrosshairStyle.NONE,
    val screenColorMode: ScreenColorMode = ScreenColorMode.DEFAULT,
    val screenZoomEnabled: Boolean = false,
    val zoomScale: Float = 2.0f,
    val activeInfoHud: Boolean = false,
    val turboXMode: Boolean = false,
    val effectSoundEnabled: Boolean = true,
    val eqPreset: EqPreset = EqPreset.FPS_FOOTSTEPS,
    val screenScalingWide: Boolean = false,
    val keyMapTriggerVisible: Boolean = false,
    val dataPanelVisible: Boolean = false,
    val reducePingEnabled: Boolean = false,
    val backgroundControlEnabled: Boolean = true,
    // Selected Games & Stats
    val addedGamePackages: Set<String> = emptySet(),
    val selectedGamePackage: String? = null,
    val totalBoostCount: Int = 0,
    val lastFreedMb: Int = 0,
    val totalUsageMinutes: Int = 0,
    // Turbo Time Credit (Rewarded Ad Gate)
    val turboTimeExpiresAtMs: Long = 0L,
    val adsWatchedCount: Int = 0
)

const val AD_REWARD_DURATION_MS: Long = 3_600_000L // 1 hour per ad

val SLIDING_FRICTION_LABELS = listOf("OFF", "2.0", "1.0", "0.8", "0.5", "0.1", "0.05", "0.01")
val SLIDING_FRICTION_FACTORS = listOf(1.0f, 1.08f, 1.15f, 1.25f, 1.38f, 1.55f, 1.75f, 2.0f)
val BACKGROUND_LIMIT_LABELS = listOf("Default", "No background", "At most 1", "At most 2", "At most 3")
val CACHE_LIMIT_LABELS = listOf("Default", "Minimal", "Strict")
val FPS_GAME_OPTIONS = listOf(0, 90, 120, 144)
val FRAME_RATE_OPTIONS = listOf(60, 90, 120, 144)
val SCROLLING_SENSITIVITY_TICKS = listOf(-7, -5, -4, -2, 0, 2, 4, 5, 7)
