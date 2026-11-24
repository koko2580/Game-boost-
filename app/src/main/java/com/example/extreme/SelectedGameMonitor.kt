package com.example.extreme

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ForegroundAppInspector {
    fun hasUsageAccess(): Boolean
    fun currentForegroundPackage(): String?
}

class AndroidForegroundAppInspector(private val context: Context) : ForegroundAppInspector {
    override fun hasUsageAccess(): Boolean {
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

    override fun currentForegroundPackage(): String? {
        if (!hasUsageAccess()) return null
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val end = System.currentTimeMillis()
            val start = end - 12_000L
            val events = usm.queryEvents(start, end)
            val event = UsageEvents.Event()
            var latestPkg: String? = null
            var latestTs = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp >= latestTs) {
                    latestTs = event.timeStamp
                    latestPkg = event.packageName
                }
            }
            latestPkg
        }.getOrNull()
    }
}

data class ForegroundEvaluationResult(
    val decision: ForegroundDecision,
    val foregroundPackage: String?,
    val selectedGamePackage: String?,
    val usageAccessGranted: Boolean
)

class SelectedGameMonitor(
    private val context: Context,
    private val inspector: ForegroundAppInspector = AndroidForegroundAppInspector(context)
) {
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _lastForegroundPackage = MutableStateFlow<String?>(null)
    val lastForegroundPackage: StateFlow<String?> = _lastForegroundPackage.asStateFlow()

    fun hasUsageAccess(): Boolean = inspector.hasUsageAccess()

    fun startMonitoring() {
        _isMonitoring.value = true
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
    }

    fun pollAndEvaluate(
        selectedGamePackage: String?,
        isSessionCurrentlyActive: Boolean,
        allowTurboXForegroundWhileManual: Boolean = false
    ): ForegroundEvaluationResult {
        val accessGranted = inspector.hasUsageAccess()
        if (selectedGamePackage.isNullOrBlank()) {
            return ForegroundEvaluationResult(
                decision = if (isSessionCurrentlyActive) ForegroundDecision.EXIT_STOP else ForegroundDecision.NO_SELECTED_GAME,
                foregroundPackage = null,
                selectedGamePackage = null,
                usageAccessGranted = accessGranted
            )
        }
        val fgPkg = inspector.currentForegroundPackage()
        _lastForegroundPackage.value = fgPkg
        if (!accessGranted && fgPkg == null) {
            return ForegroundEvaluationResult(
                decision = ForegroundDecision.PERMISSION_MISSING,
                foregroundPackage = null,
                selectedGamePackage = selectedGamePackage,
                usageAccessGranted = false
            )
        }
        return evaluatePackageChange(
            foregroundPackage = fgPkg,
            selectedGamePackage = selectedGamePackage,
            isSessionCurrentlyActive = isSessionCurrentlyActive,
            allowTurboXForegroundWhileManual = allowTurboXForegroundWhileManual,
            usageAccessGranted = accessGranted
        )
    }

    fun evaluatePackageChange(
        foregroundPackage: String?,
        selectedGamePackage: String?,
        isSessionCurrentlyActive: Boolean,
        allowTurboXForegroundWhileManual: Boolean = false,
        usageAccessGranted: Boolean = inspector.hasUsageAccess()
    ): ForegroundEvaluationResult {
        _lastForegroundPackage.value = foregroundPackage
        if (selectedGamePackage.isNullOrBlank()) {
            return ForegroundEvaluationResult(
                decision = if (isSessionCurrentlyActive) ForegroundDecision.EXIT_STOP else ForegroundDecision.NO_SELECTED_GAME,
                foregroundPackage = foregroundPackage,
                selectedGamePackage = null,
                usageAccessGranted = usageAccessGranted
            )
        }

        if (foregroundPackage == selectedGamePackage) {
            return ForegroundEvaluationResult(
                decision = ForegroundDecision.ACTIVATE_SELECTED,
                foregroundPackage = foregroundPackage,
                selectedGamePackage = selectedGamePackage,
                usageAccessGranted = usageAccessGranted
            )
        }

        if (allowTurboXForegroundWhileManual && foregroundPackage == context.packageName && isSessionCurrentlyActive) {
            return ForegroundEvaluationResult(
                decision = ForegroundDecision.ACTIVATE_SELECTED,
                foregroundPackage = foregroundPackage,
                selectedGamePackage = selectedGamePackage,
                usageAccessGranted = usageAccessGranted
            )
        }

        val decision = if (isSessionCurrentlyActive && !foregroundPackage.isNullOrBlank()) {
            ForegroundDecision.EXIT_STOP
        } else {
            ForegroundDecision.IGNORE_NON_SELECTED
        }

        return ForegroundEvaluationResult(
            decision = decision,
            foregroundPackage = foregroundPackage,
            selectedGamePackage = selectedGamePackage,
            usageAccessGranted = usageAccessGranted
        )
    }
}
