package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_turbo_prefs")

class GameTurboPreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_DND = booleanPreferencesKey("auto_dnd_on_enter")
        val MUTE_NOTIF = booleanPreferencesKey("mute_notifications_in_game")
        val PREVENT_MEMORY_LEAKS = booleanPreferencesKey("prevent_memory_leaks")
        val DISABLE_THREE_FINGER = booleanPreferencesKey("disable_three_finger_screenshot")
        val CPU_UNIT_FORMAT = stringPreferencesKey("cpu_unit_format")
        val CPU_MODE = stringPreferencesKey("cpu_mode")

        val FPS_GAME_TARGET = intPreferencesKey("fps_game_target")
        val FRAME_RATE_TARGET = intPreferencesKey("frame_rate_target")
        val SLIDING_FRICTION_INDEX = intPreferencesKey("sliding_friction_index")
        val SCROLLING_SENSITIVITY = intPreferencesKey("scrolling_sensitivity")
        val ENHANCE_TOUCH = booleanPreferencesKey("enhance_touch")
        val REDUCE_TOUCH_LATENCY = booleanPreferencesKey("reduce_touch_latency")
        val INCREASE_CPU = booleanPreferencesKey("increase_cpu")
        val INCREASE_GPU = booleanPreferencesKey("increase_gpu")
        val FORCE_OPENGL = booleanPreferencesKey("force_opengl")
        val ENABLE_VULKAN = booleanPreferencesKey("enable_vulkan")
        val HIGH_PERF_FPS = booleanPreferencesKey("high_perf_fps")

        val BG_LIMIT_INDEX = intPreferencesKey("bg_limit_index")
        val CACHE_LIMIT_INDEX = intPreferencesKey("cache_limit_index")

        val WALLPAPER_THEME = stringPreferencesKey("wallpaper_theme")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val CONTROL_PANEL_LAYOUT = stringPreferencesKey("control_panel_layout")
        val GAME_PANEL_STYLE = stringPreferencesKey("game_panel_style")

        val NOTIF_MUTE_MODE = booleanPreferencesKey("notification_mute_mode")
        val DND_MODE = booleanPreferencesKey("dnd_mode")
        val LOCK_BRIGHTNESS = booleanPreferencesKey("lock_brightness")
        val LOCKED_BRIGHTNESS_VALUE = floatPreferencesKey("locked_brightness_value")
        val ROTATION_LOCK = booleanPreferencesKey("rotation_lock")
        val OPENING_ANIM = booleanPreferencesKey("opening_animation")
        val HOME_ANIM = booleanPreferencesKey("home_animation")
        val LOW_RAM_OVERLAY = booleanPreferencesKey("low_ram_overlay")
        val AUTO_DETECT_GAME = booleanPreferencesKey("auto_detect_game_launch")

        val CROSSHAIR_STYLE = stringPreferencesKey("crosshair_style")
        val SCREEN_COLOR_MODE = stringPreferencesKey("screen_color_mode")
        val SCREEN_ZOOM = booleanPreferencesKey("screen_zoom")
        val ZOOM_SCALE = floatPreferencesKey("zoom_scale")
        val ACTIVE_INFO_HUD = booleanPreferencesKey("active_info_hud")
        val TURBO_X_MODE = booleanPreferencesKey("turbo_x_mode")
        val EFFECT_SOUND = booleanPreferencesKey("effect_sound")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val SCREEN_SCALING = booleanPreferencesKey("screen_scaling")
        val KEYMAP_TRIGGER = booleanPreferencesKey("keymap_trigger")
        val DATA_PANEL = booleanPreferencesKey("data_panel")
        val REDUCE_PING = booleanPreferencesKey("reduce_ping")
        val BG_CONTROL = booleanPreferencesKey("bg_control")

        val ADDED_GAME_PACKAGES = stringSetPreferencesKey("added_game_packages")
        val SELECTED_GAME_PACKAGE = stringPreferencesKey("selected_game_package")
        val TOTAL_BOOST_COUNT = intPreferencesKey("total_boost_count")
        val LAST_FREED_MB = intPreferencesKey("last_freed_mb")
        val TOTAL_USAGE_MINUTES = intPreferencesKey("total_usage_minutes")
    }

    val settingsFlow: Flow<GameTurboSettings> = context.dataStore.data.map { prefs ->
        decode(prefs)
    }

    suspend fun updateSettings(transform: (GameTurboSettings) -> GameTurboSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(decode(prefs))
            prefs[Keys.AUTO_DND] = updated.autoDndOnEnter
            prefs[Keys.MUTE_NOTIF] = updated.muteNotificationsInGame
            prefs[Keys.PREVENT_MEMORY_LEAKS] = updated.preventMemoryLeaks
            prefs[Keys.DISABLE_THREE_FINGER] = updated.disableThreeFingerScreenshot
            prefs[Keys.CPU_UNIT_FORMAT] = updated.cpuUnitFormat.name
            prefs[Keys.CPU_MODE] = updated.cpuMode.name
            prefs[Keys.FPS_GAME_TARGET] = updated.fpsGameTarget
            prefs[Keys.FRAME_RATE_TARGET] = updated.frameRateTarget
            prefs[Keys.SLIDING_FRICTION_INDEX] = updated.slidingFrictionIndex
            prefs[Keys.SCROLLING_SENSITIVITY] = updated.scrollingSensitivity
            prefs[Keys.ENHANCE_TOUCH] = updated.enhanceTouchSensitivity
            prefs[Keys.REDUCE_TOUCH_LATENCY] = updated.reduceTouchLatency
            prefs[Keys.INCREASE_CPU] = updated.increaseCpuPerformance
            prefs[Keys.INCREASE_GPU] = updated.increaseGpuPerformance
            prefs[Keys.FORCE_OPENGL] = updated.forceGpuOpenGl
            prefs[Keys.ENABLE_VULKAN] = updated.enableSkiaVulkan
            prefs[Keys.HIGH_PERF_FPS] = updated.highPerfFpsControl
            prefs[Keys.BG_LIMIT_INDEX] = updated.backgroundLimitIndex
            prefs[Keys.CACHE_LIMIT_INDEX] = updated.cacheLimitIndex
            prefs[Keys.WALLPAPER_THEME] = updated.wallpaperTheme.name
            if (updated.customWallpaperUri != null) {
                prefs[Keys.CUSTOM_WALLPAPER_URI] = updated.customWallpaperUri
            } else {
                prefs.remove(Keys.CUSTOM_WALLPAPER_URI)
            }
            prefs[Keys.CONTROL_PANEL_LAYOUT] = updated.controlPanelLayout.name
            prefs[Keys.GAME_PANEL_STYLE] = updated.gamePanelStyle.name
            prefs[Keys.NOTIF_MUTE_MODE] = updated.notificationMuteMode
            prefs[Keys.DND_MODE] = updated.dndMode
            prefs[Keys.LOCK_BRIGHTNESS] = updated.lockBrightness
            prefs[Keys.LOCKED_BRIGHTNESS_VALUE] = updated.lockedBrightnessValue
            prefs[Keys.ROTATION_LOCK] = updated.rotationLock
            prefs[Keys.OPENING_ANIM] = updated.openingAnimation
            prefs[Keys.HOME_ANIM] = updated.homeAnimation
            prefs[Keys.LOW_RAM_OVERLAY] = updated.lowRamOverlayMode
            prefs[Keys.AUTO_DETECT_GAME] = updated.autoDetectGameLaunch
            prefs[Keys.CROSSHAIR_STYLE] = updated.crosshairStyle.name
            prefs[Keys.SCREEN_COLOR_MODE] = updated.screenColorMode.name
            prefs[Keys.SCREEN_ZOOM] = updated.screenZoomEnabled
            prefs[Keys.ZOOM_SCALE] = updated.zoomScale
            prefs[Keys.ACTIVE_INFO_HUD] = updated.activeInfoHud
            prefs[Keys.TURBO_X_MODE] = updated.turboXMode
            prefs[Keys.EFFECT_SOUND] = updated.effectSoundEnabled
            prefs[Keys.EQ_PRESET] = updated.eqPreset.name
            prefs[Keys.SCREEN_SCALING] = updated.screenScalingWide
            prefs[Keys.KEYMAP_TRIGGER] = updated.keyMapTriggerVisible
            prefs[Keys.DATA_PANEL] = updated.dataPanelVisible
            prefs[Keys.REDUCE_PING] = updated.reducePingEnabled
            prefs[Keys.BG_CONTROL] = updated.backgroundControlEnabled
            prefs[Keys.ADDED_GAME_PACKAGES] = updated.addedGamePackages
            if (updated.selectedGamePackage != null) {
                prefs[Keys.SELECTED_GAME_PACKAGE] = updated.selectedGamePackage
            } else {
                prefs.remove(Keys.SELECTED_GAME_PACKAGE)
            }
            prefs[Keys.TOTAL_BOOST_COUNT] = updated.totalBoostCount
            prefs[Keys.LAST_FREED_MB] = updated.lastFreedMb
            prefs[Keys.TOTAL_USAGE_MINUTES] = updated.totalUsageMinutes
        }
    }

    private fun decode(prefs: Preferences): GameTurboSettings = GameTurboSettings(
        autoDndOnEnter = prefs[Keys.AUTO_DND] ?: true,
        muteNotificationsInGame = prefs[Keys.MUTE_NOTIF] ?: false,
        preventMemoryLeaks = prefs[Keys.PREVENT_MEMORY_LEAKS] ?: true,
        disableThreeFingerScreenshot = prefs[Keys.DISABLE_THREE_FINGER] ?: false,
        cpuUnitFormat = enumOrDefault(prefs[Keys.CPU_UNIT_FORMAT], CpuUnitFormat.MHZ),
        cpuMode = enumOrDefault(prefs[Keys.CPU_MODE], CpuMode.ACCELERATION),
        fpsGameTarget = prefs[Keys.FPS_GAME_TARGET] ?: 0,
        frameRateTarget = prefs[Keys.FRAME_RATE_TARGET] ?: 60,
        slidingFrictionIndex = prefs[Keys.SLIDING_FRICTION_INDEX] ?: 0,
        scrollingSensitivity = prefs[Keys.SCROLLING_SENSITIVITY] ?: 0,
        enhanceTouchSensitivity = prefs[Keys.ENHANCE_TOUCH] ?: false,
        reduceTouchLatency = prefs[Keys.REDUCE_TOUCH_LATENCY] ?: false,
        increaseCpuPerformance = prefs[Keys.INCREASE_CPU] ?: false,
        increaseGpuPerformance = prefs[Keys.INCREASE_GPU] ?: false,
        forceGpuOpenGl = prefs[Keys.FORCE_OPENGL] ?: false,
        enableSkiaVulkan = prefs[Keys.ENABLE_VULKAN] ?: false,
        highPerfFpsControl = prefs[Keys.HIGH_PERF_FPS] ?: false,
        backgroundLimitIndex = prefs[Keys.BG_LIMIT_INDEX] ?: 0,
        cacheLimitIndex = prefs[Keys.CACHE_LIMIT_INDEX] ?: 0,
        wallpaperTheme = enumOrDefault(prefs[Keys.WALLPAPER_THEME], WallpaperTheme.OBSIDIAN_CRIMSON),
        customWallpaperUri = prefs[Keys.CUSTOM_WALLPAPER_URI],
        controlPanelLayout = enumOrDefault(prefs[Keys.CONTROL_PANEL_LAYOUT], ControlPanelLayout.RIGHT_EDGE_MENU),
        gamePanelStyle = enumOrDefault(prefs[Keys.GAME_PANEL_STYLE], GamePanelStyle.NEW),
        notificationMuteMode = prefs[Keys.NOTIF_MUTE_MODE] ?: false,
        dndMode = prefs[Keys.DND_MODE] ?: false,
        lockBrightness = prefs[Keys.LOCK_BRIGHTNESS] ?: false,
        lockedBrightnessValue = prefs[Keys.LOCKED_BRIGHTNESS_VALUE] ?: 0.75f,
        rotationLock = prefs[Keys.ROTATION_LOCK] ?: false,
        openingAnimation = prefs[Keys.OPENING_ANIM] ?: true,
        homeAnimation = prefs[Keys.HOME_ANIM] ?: true,
        lowRamOverlayMode = prefs[Keys.LOW_RAM_OVERLAY] ?: false,
        autoDetectGameLaunch = prefs[Keys.AUTO_DETECT_GAME] ?: true,
        crosshairStyle = enumOrDefault(prefs[Keys.CROSSHAIR_STYLE], CrosshairStyle.NONE),
        screenColorMode = enumOrDefault(prefs[Keys.SCREEN_COLOR_MODE], ScreenColorMode.DEFAULT),
        screenZoomEnabled = prefs[Keys.SCREEN_ZOOM] ?: false,
        zoomScale = prefs[Keys.ZOOM_SCALE] ?: 2.0f,
        activeInfoHud = prefs[Keys.ACTIVE_INFO_HUD] ?: false,
        turboXMode = prefs[Keys.TURBO_X_MODE] ?: false,
        effectSoundEnabled = prefs[Keys.EFFECT_SOUND] ?: true,
        eqPreset = enumOrDefault(prefs[Keys.EQ_PRESET], EqPreset.FPS_FOOTSTEPS),
        screenScalingWide = prefs[Keys.SCREEN_SCALING] ?: false,
        keyMapTriggerVisible = prefs[Keys.KEYMAP_TRIGGER] ?: false,
        dataPanelVisible = prefs[Keys.DATA_PANEL] ?: false,
        reducePingEnabled = prefs[Keys.REDUCE_PING] ?: false,
        backgroundControlEnabled = prefs[Keys.BG_CONTROL] ?: true,
        addedGamePackages = prefs[Keys.ADDED_GAME_PACKAGES] ?: emptySet(),
        selectedGamePackage = prefs[Keys.SELECTED_GAME_PACKAGE],
        totalBoostCount = prefs[Keys.TOTAL_BOOST_COUNT] ?: 0,
        lastFreedMb = prefs[Keys.LAST_FREED_MB] ?: 0,
        totalUsageMinutes = prefs[Keys.TOTAL_USAGE_MINUTES] ?: 0
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
