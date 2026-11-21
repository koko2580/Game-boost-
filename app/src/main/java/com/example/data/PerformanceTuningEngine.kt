package com.example.data

import android.app.Activity
import android.app.ActivityManager
import android.app.GameManager
import android.app.GameState
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import android.os.PerformanceHintManager
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.Display
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class GpuPipelineInfo(
    val rendererName: String = "${Build.HARDWARE.uppercase()} GPU",
    val glEsVersion: String = "3.0",
    val vulkanSupported: Boolean = false,
    val vulkanVersionLabel: String = "Unavailable",
    val systemHwuiRenderer: String = "auto"
)

data class PerformanceEngineStatus(
    val effectiveTargetFps: Int = 60,
    val appliedDisplayModeHz: Int = 60,
    val adpfSupported: Boolean = false,
    val adpfHintActive: Boolean = false,
    val adpfTargetDurationMs: Float = 16.6f,
    val framePacingActive: Boolean = true,
    val framePacingJitterMs: Float = 0.0f,
    val minimalPostProcessingActive: Boolean = false,
    val disablePowerSavingActive: Boolean = true,
    val batterySaverActive: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
    val smartBackgroundActive: Boolean = true,
    val backgroundProcessCount: Int = 0,
    val backgroundProcessSampleNames: List<String> = emptyList(),
    val displayResolutionOptActive: Boolean = false,
    val currentResolutionLabel: String = "1080×2400 @ 60Hz",
    val supportedResolutionModes: List<DisplayModeOption> = emptyList(),
    val appliedDisplayModeId: Int = 0,
    val densityDpi: Int = 420,
    val sustainedPerformanceSupported: Boolean = false,
    val sustainedPerformanceActive: Boolean = false,
    val autoPerfActive: Boolean = false,
    val autoProfileRecommendation: String = "Manual Control",
    val gameStateActive: Boolean = false,
    val unbufferedTouchActive: Boolean = false,
    val systemPointerSpeedApplied: Boolean = false,
    val appliedPointerSpeed: Int = 0,
    val scrollMultiplier: Float = 1.0f,
    val frictionFactor: Float = 1.0f,
    val activeGpuPipelineLabel: String = "System Auto",
    val thermalStatusLabel: String = "NORMAL",
    val threadPriorityBoosted: Boolean = false,
    val gpuInfo: GpuPipelineInfo = GpuPipelineInfo()
)

class PerformanceTuningEngine(private val context: Context) {

    private val _status = MutableStateFlow(PerformanceEngineStatus())
    val status: StateFlow<PerformanceEngineStatus> = _status.asStateFlow()

    private val inspector = PerformanceSystemInspector(context)

    @Volatile
    private var cachedGpuInfo: GpuPipelineInfo? = null

    private var adpfSession: Any? = null // PerformanceHintManager.Session (API 31+)
    private var lastAdpfTargetNs: Long = 0L
    private var lastAdpfReportMs: Long = 0L
    private var previousDeltaNs: Long = 0L
    @Volatile
    private var smoothedJitterMs: Float = 0.0f

    @Volatile
    private var unbufferedTouchEnabled: Boolean = false

    @Volatile
    private var threeFingerGuardEnabled: Boolean = false

    fun isThreeFingerGuardEnabled(): Boolean = threeFingerGuardEnabled

    fun shouldBlockMultiFingerGesture(pointerCount: Int, guardEnabled: Boolean = threeFingerGuardEnabled): Boolean {
        return guardEnabled && pointerCount >= 3
    }

    fun queryGpuPipelineInfo(): GpuPipelineInfo {
        cachedGpuInfo?.let { return it }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val glEsVersion = runCatching {
            am?.deviceConfigurationInfo?.glEsVersion ?: "3.0"
        }.getOrDefault("3.0")

        val pm = context.packageManager
        val vulkanSupported = runCatching {
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        }.getOrDefault(false)

        val vulkanVersionLabel = if (!vulkanSupported) {
            "Not supported"
        } else {
            val rawVer = runCatching {
                val infos = pm.systemAvailableFeatures
                infos.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }?.version ?: 0
            }.getOrDefault(0)
            if (rawVer > 0) {
                val major = (rawVer shr 22) and 0x3FF
                val minor = (rawVer shr 12) and 0x3FF
                "Vulkan $major.$minor"
            } else {
                "Vulkan Ready"
            }
        }

        val hwui = readSystemProperty("debug.hwui.renderer")
            .ifBlank { readSystemProperty("ro.hwui.use_vulkan") }
            .ifBlank { "auto" }

        val eglRenderer = if (isRobolectric()) {
            "${Build.HARDWARE.uppercase()} • Virtual GPU"
        } else {
            queryEglRendererString() ?: "${Build.HARDWARE.uppercase()} GPU"
        }

        val info = GpuPipelineInfo(
            rendererName = eglRenderer,
            glEsVersion = glEsVersion,
            vulkanSupported = vulkanSupported,
            vulkanVersionLabel = vulkanVersionLabel,
            systemHwuiRenderer = hwui
        )
        cachedGpuInfo = info
        return info
    }

    private fun isRobolectric(): Boolean =
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    private fun readSystemProperty(key: String): String {
        return runCatching {
            val spClass = Class.forName("android.os.SystemProperties")
            val getMethod = spClass.getMethod("get", String::class.java, String::class.java)
            (getMethod.invoke(null, key, "") as? String).orEmpty()
        }.getOrDefault("")
    }

    private fun queryEglRendererString(): String? {
        return runCatching {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return null
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null
            try {
                val attribList = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (!EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, numConfigs, 0) ||
                    numConfigs[0] <= 0 || configs[0] == null
                ) {
                    return null
                }
                val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                val eglContext = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
                if (eglContext == null || eglContext == EGL14.EGL_NO_CONTEXT) return null
                val surfAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                val eglSurface = EGL14.eglCreatePbufferSurface(display, configs[0], surfAttribs, 0)
                if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroyContext(display, eglContext)
                    return null
                }
                try {
                    EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)
                    val renderer = GLES20.glGetString(GLES20.GL_RENDERER)?.trim().orEmpty()
                    renderer.ifBlank { null }
                } finally {
                    EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    EGL14.eglDestroySurface(display, eglSurface)
                    EGL14.eglDestroyContext(display, eglContext)
                }
            } finally {
                EGL14.eglTerminate(display)
            }
        }.getOrNull()
    }

    fun computeEffectiveTargetFps(
        settings: GameTurboSettings,
        supportedRates: List<Int>
    ): Int {
        val maxPanelHz = supportedRates.maxOrNull() ?: 60
        return when {
            settings.highPerfFpsControl -> maxOf(settings.frameRateTarget, settings.fpsGameTarget, maxPanelHz)
            settings.fpsGameTarget > 0 -> maxOf(settings.frameRateTarget, settings.fpsGameTarget)
            else -> settings.frameRateTarget
        }.coerceIn(60, 144)
    }

    fun computeScrollMultiplier(settings: GameTurboSettings): Float {
        val sensScale = (1.0f + settings.scrollingSensitivity * 0.08f).coerceIn(0.45f, 1.75f)
        val touchScale = if (settings.enhanceTouchSensitivity) 1.18f else 1.0f
        return (sensScale * touchScale).coerceIn(0.5f, 2.0f)
    }

    fun computeFrictionFactor(settings: GameTurboSettings): Float {
        val idx = settings.slidingFrictionIndex.coerceIn(0, SLIDING_FRICTION_FACTORS.lastIndex)
        return SLIDING_FRICTION_FACTORS[idx]
    }

    fun readThermalStatusLabel(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "NORMAL"
        return runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return "NORMAL"
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "NORMAL"
                PowerManager.THERMAL_STATUS_LIGHT -> "WARM"
                PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                PowerManager.THERMAL_STATUS_SEVERE -> "HIGH"
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "THROTTLED"
                else -> "NORMAL"
            }
        }.getOrDefault("NORMAL")
    }

    fun applyProfile(
        settings: GameTurboSettings,
        supportedRates: List<Int>,
        activity: Activity? = null,
        isInGameLaunch: Boolean = false,
        batteryPercent: Int = 80,
        isCharging: Boolean = false
    ): PerformanceEngineStatus {
        val gpuInfo = queryGpuPipelineInfo()
        val effectiveTargetFps = computeEffectiveTargetFps(settings, supportedRates)
        val scrollMult = computeScrollMultiplier(settings)
        val frictionFactor = computeFrictionFactor(settings)
        val unbuffered = settings.enhanceTouchSensitivity || settings.reduceTouchLatency
        unbufferedTouchEnabled = unbuffered
        threeFingerGuardEnabled = settings.disableThreeFingerScreenshot

        // 1. Apply System Pointer Speed when WRITE_SETTINGS is granted
        val targetPointerSpeed = (settings.scrollingSensitivity + if (settings.enhanceTouchSensitivity) 2 else 0)
            .coerceIn(-7, 7)
        val pointerApplied = applySystemPointerSpeed(targetPointerSpeed)

        // 2. Boost calling thread priority for low touch latency / CPU perf / high FPS / Frame pacing
        val priorityBoosted = applyThreadPriority(
            urgent = settings.reduceTouchLatency ||
                settings.increaseCpuPerformance ||
                settings.highPerfFpsControl ||
                settings.optimizeFramePacing
        )

        // 3. Configure ADPF PerformanceHintManager session (API 31+, respects explicit enableAdpfHints toggle)
        val adpfSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isRobolectric()
        val (adpfActive, targetDurationMs) = updateAdpfHintSession(settings, effectiveTargetFps)

        // 4. Signal Android GameManager / GameState (API 33+)
        val gameStateActive = updateSystemGameState(
            boostActive = isInGameLaunch ||
                settings.increaseCpuPerformance ||
                settings.increaseGpuPerformance ||
                settings.highPerfFpsControl ||
                settings.optimizeFramePacing
        )

        // 5. Inspect Power, Background Activity, Display Modes & Sustained Support
        val (batterySaverActive, ignoringBatteryOpt) = inspector.detectBatterySaverState()
        val sustainedSupported = inspector.isSustainedPerformanceSupported()
        val (bgCount, bgSamples) = if (settings.smartBackgroundControl) {
            inspector.detectBackgroundActivitySummary(settings.addedGamePackages)
        } else {
            0 to emptyList()
        }
        val (resolutionLabel, supportedModes, densityDpi) = inspector.detectDisplayResolutionInfo(activity)
        val thermalLabel = readThermalStatusLabel()
        val autoDecision = if (settings.autoPerformanceOptimization) {
            inspector.evaluateAutoPerformance(
                thermalLabel = thermalLabel,
                batterySaverActive = batterySaverActive,
                batteryPercent = batteryPercent,
                isCharging = isCharging,
                sustainedSupported = sustainedSupported
            ).reasonLabel
        } else {
            "Manual Control"
        }

        // 6. Apply Activity Window flags, Display Mode / Resolution Optimizer, Frame Pacing, Sustained Mode & Unbuffered Touch
        var appliedHz = effectiveTargetFps
        var sustainedActive = settings.sustainedPerformanceMode && sustainedSupported
        var minimalPostProc = false
        var appliedModeId = settings.preferredDisplayModeId
        if (activity != null) {
            val windowResult = applyToActivityWindow(activity, settings, effectiveTargetFps, gpuInfo)
            appliedHz = windowResult.appliedHz
            sustainedActive = windowResult.sustainedActive
            minimalPostProc = windowResult.minimalPostProcessing
            appliedModeId = windowResult.appliedModeId
        }

        val gpuLabel = when {
            settings.enableSkiaVulkan && gpuInfo.vulkanSupported -> "Skia Vulkan (${gpuInfo.vulkanVersionLabel})"
            settings.forceGpuOpenGl -> "OpenGL ES ${gpuInfo.glEsVersion} Hardware"
            settings.increaseGpuPerformance -> "GPU Boost (${gpuInfo.glEsVersion})"
            else -> "System Auto (${gpuInfo.systemHwuiRenderer})"
        }

        val nextStatus = PerformanceEngineStatus(
            effectiveTargetFps = effectiveTargetFps,
            appliedDisplayModeHz = appliedHz,
            adpfSupported = adpfSupported,
            adpfHintActive = adpfActive,
            adpfTargetDurationMs = targetDurationMs,
            framePacingActive = settings.optimizeFramePacing,
            framePacingJitterMs = smoothedJitterMs,
            minimalPostProcessingActive = minimalPostProc,
            disablePowerSavingActive = settings.disablePowerSavingInGame,
            batterySaverActive = batterySaverActive,
            ignoringBatteryOptimizations = ignoringBatteryOpt,
            smartBackgroundActive = settings.smartBackgroundControl,
            backgroundProcessCount = bgCount,
            backgroundProcessSampleNames = bgSamples,
            displayResolutionOptActive = settings.displayResolutionOpt,
            currentResolutionLabel = resolutionLabel,
            supportedResolutionModes = supportedModes,
            appliedDisplayModeId = appliedModeId,
            densityDpi = densityDpi,
            sustainedPerformanceSupported = sustainedSupported,
            sustainedPerformanceActive = sustainedActive,
            autoPerfActive = settings.autoPerformanceOptimization,
            autoProfileRecommendation = autoDecision,
            gameStateActive = gameStateActive,
            unbufferedTouchActive = unbuffered,
            systemPointerSpeedApplied = pointerApplied,
            appliedPointerSpeed = targetPointerSpeed,
            scrollMultiplier = scrollMult,
            frictionFactor = frictionFactor,
            activeGpuPipelineLabel = gpuLabel,
            thermalStatusLabel = thermalLabel,
            threadPriorityBoosted = priorityBoosted,
            gpuInfo = gpuInfo
        )
        _status.value = nextStatus
        return nextStatus
    }

    fun evaluateAutoPerformance(
        batteryPercent: Int,
        isCharging: Boolean
    ): AutoPerfDecision {
        val thermal = readThermalStatusLabel()
        val (batterySaver, _) = inspector.detectBatterySaverState()
        val sustainedSupported = inspector.isSustainedPerformanceSupported()
        return inspector.evaluateAutoPerformance(
            thermalLabel = thermal,
            batterySaverActive = batterySaver,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            sustainedSupported = sustainedSupported
        )
    }

    fun isBatterySaverActive(): Boolean = inspector.detectBatterySaverState().first

    fun detectBatterySaverState(): Pair<Boolean, Boolean> = inspector.detectBatterySaverState()

    fun isSustainedPerformanceSupported(): Boolean = inspector.isSustainedPerformanceSupported()

    fun evaluateMemoryPressure(usedPercent: Int): Pair<String, String> = inspector.evaluateMemoryPressure(usedPercent)

    fun openPowerSaverSettings(): Boolean = inspector.openPowerSaverSettings()

    fun openAppManagementSettings(): Boolean = inspector.openAppManagementSettings()

    fun openDisplaySettings(): Boolean = inspector.openDisplaySettings()

    private fun applySystemPointerSpeed(pointerSpeed: Int): Boolean {
        if (!Settings.System.canWrite(context)) return false
        return runCatching {
            Settings.System.putInt(context.contentResolver, "pointer_speed", pointerSpeed)
            true
        }.getOrDefault(false)
    }

    fun boostCurrentThread(urgentDisplay: Boolean): Boolean {
        return applyThreadPriority(urgentDisplay)
    }

    private fun applyThreadPriority(urgent: Boolean): Boolean {
        if (isRobolectric()) return urgent
        return runCatching {
            Process.setThreadPriority(
                if (urgent) Process.THREAD_PRIORITY_URGENT_DISPLAY
                else Process.THREAD_PRIORITY_DEFAULT
            )
            true
        }.getOrDefault(false)
    }

    private fun updateAdpfHintSession(
        settings: GameTurboSettings,
        effectiveTargetFps: Int
    ): Pair<Boolean, Float> {
        val baseBudgetNs = (1_000_000_000L / effectiveTargetFps.coerceIn(60, 144))
        val perfBoosted = settings.increaseCpuPerformance ||
            settings.increaseGpuPerformance ||
            settings.highPerfFpsControl ||
            settings.cpuMode == CpuMode.ACCELERATION

        val targetNs = when {
            perfBoosted -> (baseBudgetNs * 0.80f).toLong().coerceAtLeast(3_000_000L)
            settings.cpuMode == CpuMode.ENERGY_SAVING -> (baseBudgetNs * 1.15f).toLong()
            else -> baseBudgetNs
        }
        val targetMs = targetNs / 1_000_000f

        if (!settings.enableAdpfHints) {
            closeSession()
            return false to targetMs
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || isRobolectric()) {
            return false to targetMs
        }

        val active = runCatching {
            val phm = context.getSystemService(PerformanceHintManager::class.java) ?: return false to targetMs
            var session = adpfSession as? PerformanceHintManager.Session
            if (session == null) {
                session = phm.createHintSession(intArrayOf(Process.myTid()), targetNs)
                adpfSession = session
                lastAdpfTargetNs = targetNs
            } else if (lastAdpfTargetNs != targetNs) {
                session.updateTargetWorkDuration(targetNs)
                lastAdpfTargetNs = targetNs
            }
            if (session != null && Build.VERSION.SDK_INT >= 35) {
                val preferPowerEfficiency = settings.cpuMode == CpuMode.ENERGY_SAVING && !perfBoosted
                session.setPreferPowerEfficiency(preferPowerEfficiency)
            }
            session != null
        }.getOrDefault(false)

        return active to targetMs
    }

    fun reportFrameDurationNanos(workDurationNanos: Long, settings: GameTurboSettings) {
        if (previousDeltaNs > 0L) {
            val jitterMs = abs(workDurationNanos - previousDeltaNs) / 1_000_000f
            smoothedJitterMs = (smoothedJitterMs * 0.85f + jitterMs * 0.15f).coerceIn(0f, 50f)
        }
        previousDeltaNs = workDurationNanos

        if (!settings.enableAdpfHints || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || isRobolectric()) return
        val session = adpfSession as? PerformanceHintManager.Session ?: return
        val nowMs = android.os.SystemClock.elapsedRealtime()
        if (nowMs - lastAdpfReportMs < 350L) return
        lastAdpfReportMs = nowMs

        val boostActive = settings.increaseCpuPerformance ||
            settings.increaseGpuPerformance ||
            settings.highPerfFpsControl ||
            settings.optimizeFramePacing
        val reportedNs = if (boostActive) {
            // Report slight headroom demand when performance boost is engaged
            maxOf(workDurationNanos, (lastAdpfTargetNs * 1.05f).toLong()).coerceIn(1_000_000L, 50_000_000L)
        } else {
            workDurationNanos.coerceIn(1_000_000L, 50_000_000L)
        }
        runCatching { session.reportActualWorkDuration(reportedNs) }
    }

    private fun updateSystemGameState(boostActive: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || isRobolectric()) return false
        return runCatching {
            val gm = context.getSystemService(GameManager::class.java) ?: return false
            val mode = if (boostActive) {
                GameState.MODE_GAMEPLAY_UNINTERRUPTIBLE
            } else {
                GameState.MODE_NONE
            }
            gm.setGameState(GameState(false, mode))
            boostActive
        }.getOrDefault(false)
    }

    private data class WindowApplyResult(
        val appliedHz: Int,
        val sustainedActive: Boolean,
        val minimalPostProcessing: Boolean,
        val appliedModeId: Int
    )

    private fun applyToActivityWindow(
        activity: Activity,
        settings: GameTurboSettings,
        targetHz: Int,
        gpuInfo: GpuPipelineInfo
    ): WindowApplyResult {
        var appliedHz = targetHz
        var sustainedApplied = false
        var minimalPostProc = false
        var appliedModeId = settings.preferredDisplayModeId

        runCatching {
            val window = activity.window
            // 1. Hardware acceleration flag for OpenGL / Skia Vulkan GPU UI rendering
            val forceHwAccel = settings.forceGpuOpenGl ||
                settings.enableSkiaVulkan ||
                settings.increaseGpuPerformance ||
                settings.highPerfFpsControl ||
                settings.optimizeFramePacing
            if (forceHwAccel) {
                window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            }

            // 2. Display mode resolution & preferred refresh rate (Frame Pacing + Display Resolution Optimizer)
            val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay
            }
            val params = window.attributes
            var attrsChanged = false
            if (display != null) {
                val explicitMode = if (settings.displayResolutionOpt && settings.preferredDisplayModeId > 0) {
                    display.supportedModes.firstOrNull { it.modeId == settings.preferredDisplayModeId }
                } else {
                    null
                }
                val bestMode = explicitMode ?: display.supportedModes.minByOrNull { abs(it.refreshRate - targetHz.toFloat()) }
                if (bestMode != null) {
                    appliedHz = bestMode.refreshRate.toInt()
                    appliedModeId = bestMode.modeId
                    if (params.preferredDisplayModeId != bestMode.modeId) {
                        params.preferredDisplayModeId = bestMode.modeId
                        attrsChanged = true
                    }
                }
            }
            val resolvedRefresh = if (settings.displayResolutionOpt && settings.preferredDisplayModeId > 0) {
                appliedHz.toFloat()
            } else {
                targetHz.toFloat()
            }
            if (abs(params.preferredRefreshRate - resolvedRefresh) > 0.5f) {
                params.preferredRefreshRate = resolvedRefresh
                attrsChanged = true
            }
            if (attrsChanged) {
                window.attributes = params
            }

            // 3. Minimal Post-Processing for low-jitter Frame Pacing (API 30+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching {
                    window.setPreferMinimalPostProcessing(settings.optimizeFramePacing)
                    minimalPostProc = settings.optimizeFramePacing
                }
            }

            // 4. Wide Color Gamut when GPU performance boost is enabled (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val wideGamutSupported = activity.resources.configuration.isScreenWideColorGamut
                val desiredColorMode = if (settings.increaseGpuPerformance && wideGamutSupported) {
                    ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
                } else {
                    ActivityInfo.COLOR_MODE_DEFAULT
                }
                if (window.colorMode != desiredColorMode) {
                    window.colorMode = desiredColorMode
                }
            }

            // 5. Dedicated Android Sustained Performance Mode (API 24+)
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && pm.isSustainedPerformanceModeSupported) {
                val useSustained = settings.sustainedPerformanceMode
                window.setSustainedPerformanceMode(useSustained)
                sustainedApplied = useSustained
            }

            // 6. Unbuffered touch dispatch on Activity Window
            installOrUpdateTouchInterceptor(activity, unbufferedTouchEnabled)

            // 7. Layer acceleration hint for Skia OpenGL / Vulkan UI pipeline
            val targetLayer = if (settings.forceGpuOpenGl || (settings.enableSkiaVulkan && gpuInfo.vulkanSupported)) {
                View.LAYER_TYPE_NONE // Window already FLAG_HARDWARE_ACCELERATED -> full GPU HWUI display list
            } else {
                View.LAYER_TYPE_NONE
            }
            if (window.decorView.layerType != targetLayer) {
                window.decorView.setLayerType(targetLayer, null)
            }
        }

        return WindowApplyResult(
            appliedHz = appliedHz,
            sustainedActive = sustainedApplied,
            minimalPostProcessing = minimalPostProc,
            appliedModeId = appliedModeId
        )
    }

    fun installOrUpdateTouchInterceptor(activity: Activity, enabled: Boolean) {
        unbufferedTouchEnabled = enabled
        val window = activity.window ?: return
        val currentCallback = window.callback
        if (currentCallback != null && currentCallback !is UnbufferedTouchWindowCallback) {
            window.callback = UnbufferedTouchWindowCallback(
                original = currentCallback,
                decorView = window.decorView,
                isEnabled = { unbufferedTouchEnabled },
                isThreeFingerBlocked = { threeFingerGuardEnabled }
            )
        }
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                window.decorView.requestUnbufferedDispatch(InputDevice.SOURCE_ANY)
            }
        }
    }

    fun applyToOverlayWindowParams(
        params: WindowManager.LayoutParams,
        settings: GameTurboSettings,
        supportedRates: List<Int>,
        targetView: View? = null
    ) {
        val targetHz = computeEffectiveTargetFps(settings, supportedRates)
        params.preferredRefreshRate = targetHz.toFloat()
        if (settings.forceGpuOpenGl ||
            settings.enableSkiaVulkan ||
            settings.increaseGpuPerformance ||
            settings.highPerfFpsControl ||
            settings.optimizeFramePacing
        ) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { context.display }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            wm?.defaultDisplay
        }
        val explicitMode = if (settings.displayResolutionOpt && settings.preferredDisplayModeId > 0) {
            display?.supportedModes?.firstOrNull { it.modeId == settings.preferredDisplayModeId }
        } else null
        val bestMode = explicitMode ?: display?.supportedModes?.minByOrNull { abs(it.refreshRate - targetHz.toFloat()) }
        if (bestMode != null) {
            params.preferredDisplayModeId = bestMode.modeId
            params.preferredRefreshRate = bestMode.refreshRate
        }
        if (targetView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            (settings.enhanceTouchSensitivity || settings.reduceTouchLatency)
        ) {
            runCatching {
                targetView.requestUnbufferedDispatch(InputDevice.SOURCE_ANY)
            }
        }
    }

    fun openDeveloperRenderingOptions(): Boolean {
        val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(devIntent)
            true
        }.getOrElse {
            val fallback = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                context.startActivity(fallback)
                false
            }.getOrDefault(false)
        }
    }

    fun closeSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { (adpfSession as? PerformanceHintManager.Session)?.close() }
        }
        adpfSession = null
        lastAdpfTargetNs = 0L
    }

    private class UnbufferedTouchWindowCallback(
        private val original: Window.Callback,
        private val decorView: View,
        private val isEnabled: () -> Boolean,
        private val isThreeFingerBlocked: () -> Boolean
    ) : Window.Callback by original {
        override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
            if (event != null) {
                if (isThreeFingerBlocked() && event.pointerCount >= 3) {
                    return true
                }
                if (isEnabled()) {
                    runCatching { decorView.requestUnbufferedDispatch(event) }
                }
            }
            return original.dispatchTouchEvent(event)
        }
    }
}
