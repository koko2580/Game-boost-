package com.example.data

import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.LruCache
import android.Manifest
import android.view.Display
import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

class SystemHardwareController(private val context: Context) {

    companion object {
        fun selectPackagesToTrim(
            allKnownPackages: List<String>,
            runningAppPackages: List<String>,
            protectedPackages: Set<String>,
            ownPackage: String,
            backgroundLimitIndex: Int
        ): List<String> {
            val candidates = (runningAppPackages + allKnownPackages)
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() && it != ownPackage && it !in protectedPackages }
                .distinct()
                .toList()
            val retainCount = when (backgroundLimitIndex.coerceIn(0, 4)) {
                1 -> 0 // No background processes retained
                2 -> 1 // At most 1
                3 -> 2 // At most 2
                4 -> 3 // At most 3
                else -> 0 // Default: trim background non-protected apps
            }
            return if (retainCount <= 0) candidates else candidates.drop(retainCount)
        }

        fun evaluateForegroundGameTrigger(
            foregroundPkg: String?,
            lastDetectedPkg: String?,
            settings: GameTurboSettings,
            ownPackage: String
        ): String? {
            if (!settings.autoDetectGameLaunch) return null
            if (foregroundPkg.isNullOrBlank()) return null
            if (foregroundPkg == ownPackage) return null
            if (foregroundPkg == lastDetectedPkg) return null
            return if (foregroundPkg in settings.addedGamePackages) foregroundPkg else null
        }
    }

    private val activityManager by lazy(LazyThreadSafetyMode.NONE) {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }
    private val isLowRamDevice: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        activityManager?.isLowRamDevice == true
    }
    private val iconCache: LruCache<String, ImageBitmap> by lazy(LazyThreadSafetyMode.NONE) {
        LruCache(if (isLowRamDevice) 32 else 64)
    }
    private val cachedDeviceModel: String by lazy(LazyThreadSafetyMode.NONE) {
        "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }
    private val cachedAndroidVersion: String = Build.VERSION.RELEASE ?: "14"

    val audioEnhancer = GameAudioEnhancer(context)
    val performanceEngine = PerformanceTuningEngine(context)

    fun checkPermissions(): PermissionStatus {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val dndGranted = nm?.isNotificationPolicyAccessGranted == true
        val overlayGranted = Settings.canDrawOverlays(context)
        val writeSettingsGranted = Settings.System.canWrite(context)
        val usageStatsGranted = checkUsageStatsPermission()
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return PermissionStatus(
            dndGranted = dndGranted,
            overlayGranted = overlayGranted,
            writeSettingsGranted = writeSettingsGranted,
            usageStatsGranted = usageStatsGranted,
            notificationsGranted = notificationsGranted
        )
    }

    private fun checkUsageStatsPermission(): Boolean {
        return runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    fun openDndPermissionSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openWriteSettingsPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openWifiSettingsPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun getForegroundPackageName(): String? {
        if (!checkUsageStatsPermission()) return null
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val end = System.currentTimeMillis()
            val start = end - 10_000L
            val events = usm.queryEvents(start, end)
            val event = UsageEvents.Event()
            var lastPkg: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastPkg = event.packageName
                }
            }
            lastPkg
        }.getOrNull()
    }

    suspend fun loadLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val seen = HashSet<String>()
        resolveInfos.mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName || !seen.add(pkg)) return@mapNotNull null
            val appInfo = info.activityInfo.applicationInfo
            val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            val version = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).versionName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0).versionName
                }
            }.getOrNull().orEmpty().ifEmpty { "1.0" }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            InstalledAppInfo(
                packageName = pkg,
                appName = label,
                versionName = version,
                isSystemApp = isSystem
            )
        }.sortedWith(compareBy<InstalledAppInfo> { it.isSystemApp }.thenBy { it.appName.lowercase() })
    }

    fun peekCachedAppIcon(packageName: String): ImageBitmap? = synchronized(iconCache) {
        iconCache.get(packageName)
    }

    suspend fun loadAppIconAsync(packageName: String): ImageBitmap? {
        peekCachedAppIcon(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            getAppIcon(packageName)
        }
    }

    fun getAppIcon(packageName: String): ImageBitmap? {
        peekCachedAppIcon(packageName)?.let { return it }
        return runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable).asImageBitmap()
            synchronized(iconCache) {
                iconCache.put(packageName, bitmap)
            }
            bitmap
        }.getOrNull()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val targetMaxPx = if (isLowRamDevice) 72 else 96
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= targetMaxPx && src.height <= targetMaxPx) {
                return src
            }
            val scale = minOf(
                targetMaxPx.toFloat() / src.width.coerceAtLeast(1),
                targetMaxPx.toFloat() / src.height.coerceAtLeast(1)
            )
            val w = (src.width * scale).toInt().coerceAtLeast(1)
            val h = (src.height * scale).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(src, w, h, true)
        }
        val width = drawable.intrinsicWidth.coerceIn(48, targetMaxPx)
        val height = drawable.intrinsicHeight.coerceIn(48, targetMaxPx)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun readHardwareTelemetry(
        cpuMode: CpuMode,
        lastPingMs: Int?,
        lastJitterMs: Int?,
        actualFps: Int,
        frameTimeMs: Float
    ): HardwareTelemetry {
        val coreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val coreFreqs = readCpuCoreFrequenciesMHz(coreCount)
        val activeFreq = coreFreqs.maxOrNull()?.takeIf { it > 0 } ?: when (cpuMode) {
            CpuMode.ENERGY_SAVING -> 550
            CpuMode.BALANCED -> 1150
            CpuMode.ACCELERATION -> 1850
        }

        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedPercent = (((totalMb - availMb).toDouble() / totalMb.toDouble()) * 100.0)
            .toInt()
            .coerceIn(1, 99)

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 80
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 325) ?: 325
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3950) ?: 3950
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val (refreshRate, supportedRates) = readDisplayRefreshInfo()
        val (netType, linkMbps, rssiDbm) = readNetworkSummary()

        val gpuInfo = performanceEngine.queryGpuPipelineInfo()
        val gpuSummary = "${gpuInfo.rendererName} • GLES ${gpuInfo.glEsVersion} • ${gpuInfo.vulkanVersionLabel}"

        return HardwareTelemetry(
            cpuFreqMHz = activeFreq,
            cpuCoreCount = coreCount,
            cpuCoreFreqsMHz = if (coreFreqs.isNotEmpty()) coreFreqs else List(coreCount) { activeFreq },
            ramUsedPercent = usedPercent,
            ramAvailableMb = availMb,
            ramTotalMb = totalMb,
            batteryPercent = batteryPct,
            batteryTempCelsius = tempTenths / 10f,
            batteryVoltageMv = voltageMv,
            isCharging = isCharging,
            displayRefreshRateHz = refreshRate,
            supportedRefreshRates = supportedRates,
            actualFps = actualFps,
            frameTimeMs = frameTimeMs,
            gpuRenderer = gpuSummary,
            deviceModel = cachedDeviceModel,
            androidVersion = cachedAndroidVersion,
            pingMs = lastPingMs,
            pingJitterMs = lastJitterMs,
            networkType = netType,
            wifiLinkMbps = linkMbps,
            wifiRssiDbm = rssiDbm
        )
    }

    private fun readCpuCoreFrequenciesMHz(coreCount: Int): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until coreCount) {
            val curFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            val maxFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            val khz = runCatching {
                when {
                    curFile.canRead() -> curFile.readText().trim().toLongOrNull()
                    maxFile.canRead() -> maxFile.readText().trim().toLongOrNull()
                    else -> null
                }
            }.getOrNull()
            if (khz != null && khz > 0L) {
                result.add((khz / 1000L).toInt())
            }
        }
        return result
    }

    fun readDisplayRefreshInfo(): Pair<Int, List<Int>> {
        return runCatching {
            val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                wm?.defaultDisplay
            }
            val current = display?.mode?.refreshRate?.toInt() ?: 60
            val supported = display?.supportedModes
                ?.map { it.refreshRate.toInt() }
                ?.distinct()
                ?.sorted()
                ?.takeIf { it.isNotEmpty() } ?: listOf(current)
            current to supported
        }.getOrDefault(60 to listOf(60))
    }

    fun applyPreferredRefreshRate(activity: Activity, targetHz: Int) {
        runCatching {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay
            } ?: return
            val modes = display.supportedModes
            val bestMode = modes.minByOrNull { abs(it.refreshRate - targetHz.toFloat()) } ?: return
            val params = activity.window.attributes
            if (params.preferredDisplayModeId != bestMode.modeId) {
                params.preferredDisplayModeId = bestMode.modeId
                activity.window.attributes = params
            }
        }
    }

    private fun readNetworkSummary(): Triple<String, Int?, Int?> {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return Triple("Unknown", null, null)
            val network = cm.activeNetwork ?: return Triple("Offline", null, null)
            val caps = cm.getNetworkCapabilities(network) ?: return Triple("Offline", null, null)
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    @Suppress("DEPRECATION")
                    val info = wm?.connectionInfo
                    val linkSpeed = info?.linkSpeed?.takeIf { it > 0 }
                    val rssi = info?.rssi?.takeIf { it > -120 && it < 0 }
                    Triple("Wi-Fi", linkSpeed, rssi)
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Triple("Cellular", null, null)
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Triple("Ethernet", null, null)
                else -> Triple("Connected", null, null)
            }
        }.getOrDefault(Triple("Offline", null, null))
    }

    suspend fun measurePingWithJitter(): Pair<Int?, Int?> = withContext(Dispatchers.IO) {
        val samples = mutableListOf<Int>()
        val targets = listOf("1.1.1.1" to 53, "8.8.8.8" to 53, "1.1.1.1" to 443)
        for ((host, port) in targets) {
            runCatching {
                val start = System.nanoTime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 1400)
                }
                val ms = ((System.nanoTime() - start) / 1_000_000L).toInt().coerceAtLeast(1)
                samples.add(ms)
            }
        }
        if (samples.isEmpty()) return@withContext null to null
        val avg = samples.average().toInt()
        val jitter = if (samples.size >= 2) {
            (samples.max() - samples.min())
        } else 0
        avg to jitter
    }

    suspend fun performRealMemoryBoost(
        protectedPackages: Set<String>,
        allKnownPackages: List<String>,
        backgroundLimitIndex: Int = 0,
        cacheLimitIndex: Int = 0
    ): BoostResult = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val before = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(before)

        val runningPkgs = runCatching {
            am?.runningAppProcesses
                ?.flatMap { proc -> proc.pkgList?.toList() ?: listOf(proc.processName) }
                ?: emptyList()
        }.getOrDefault(emptyList())

        val toTrim = selectPackagesToTrim(
            allKnownPackages = allKnownPackages,
            runningAppPackages = runningPkgs,
            protectedPackages = protectedPackages,
            ownPackage = context.packageName,
            backgroundLimitIndex = backgroundLimitIndex
        )

        var trimmedCount = 0
        if (am != null) {
            for (pkg in toTrim) {
                runCatching {
                    am.killBackgroundProcesses(pkg)
                    trimmedCount++
                }
            }
        }

        val freedCacheBytes = clearCachesInternal(cacheLimitIndex)

        val after = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(after)
        val deltaMb = ((after.availMem - before.availMem) / (1024L * 1024L)).toInt()
        val cacheKb = freedCacheBytes / 1024L
        val totalFreedMb = (deltaMb + (cacheKb / 1024L).toInt()).coerceAtLeast(16)

        BoostResult(
            freedMb = totalFreedMb,
            trimmedPackages = trimmedCount,
            clearedCacheKb = cacheKb
        )
    }

    suspend fun clearAppCaches(cacheLimitIndex: Int = 0): Long = withContext(Dispatchers.IO) {
        clearCachesInternal(cacheLimitIndex) / 1024L
    }

    private fun clearCachesInternal(cacheLimitIndex: Int): Long {
        var freed = clearDirectoryBytes(context.cacheDir) + clearDirectoryBytes(context.codeCacheDir)
        val tier = cacheLimitIndex.coerceIn(0, 2)
        if (tier >= 1) {
            val extCache = runCatching { context.externalCacheDir }.getOrNull()
            freed += clearDirectoryBytes(extCache)
            synchronized(iconCache) {
                if (tier == 1) {
                    iconCache.trimToSize(if (isLowRamDevice) 12 else 24)
                } else {
                    iconCache.evictAll()
                }
            }
        }
        return freed
    }

    private fun clearDirectoryBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                freed += clearDirectoryBytes(child)
                child.delete()
            } else {
                val len = child.length()
                if (child.delete()) freed += len
            }
        }
        return freed
    }

    fun getBrightnessFraction(): Float {
        return runCatching {
            val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            (raw / 255f).coerceIn(0.05f, 1f)
        }.getOrDefault(0.5f)
    }

    fun setBrightnessFraction(fraction: Float): Boolean {
        if (!Settings.System.canWrite(context)) return false
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val value = (fraction.coerceIn(0.05f, 1f) * 255f).toInt().coerceIn(10, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            true
        }.getOrDefault(false)
    }

    fun isAutoBrightnessEnabled(): Boolean {
        return runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        }.getOrDefault(false)
    }

    fun setAutoBrightness(enabled: Boolean): Boolean {
        if (!Settings.System.canWrite(context)) return false
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            true
        }.getOrDefault(false)
    }

    fun toggleAutoBrightness(): Boolean? {
        if (!Settings.System.canWrite(context)) return null
        val next = !isAutoBrightnessEnabled()
        return if (setAutoBrightness(next)) next else null
    }

    fun isAutoRotateEnabled(): Boolean {
        return runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        }.getOrDefault(true)
    }

    fun setAutoRotate(enabled: Boolean): Boolean {
        if (!Settings.System.canWrite(context)) return false
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (enabled) 1 else 0
            )
            true
        }.getOrDefault(false)
    }

    fun toggleAutoRotate(): Boolean? {
        if (!Settings.System.canWrite(context)) return null
        val next = !isAutoRotateEnabled()
        return if (setAutoRotate(next)) next else null
    }

    fun getVolumeFraction(): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.5f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }

    fun setVolumeFraction(fraction: Float) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (fraction.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
    }

    fun setDoNotDisturb(enabled: Boolean): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        if (!nm.isNotificationPolicyAccessGranted) return false
        return runCatching {
            nm.setInterruptionFilter(
                if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            true
        }.getOrDefault(false)
    }

    fun launchPackage(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun launchQuickShortcut(packages: List<String>, fallbackUrl: String) {
        for (pkg in packages) {
            if (launchPackage(pkg)) return
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(webIntent) }
    }
}
