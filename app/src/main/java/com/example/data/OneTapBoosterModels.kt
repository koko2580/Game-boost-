package com.example.data

enum class BoostStepId(
    val title: String,
    val description: String
) {
    DEVICE_SCAN(
        title = "Device Performance Scan",
        description = "Inspecting CPU clocks, RAM load, battery & thermals"
    ),
    MEMORY_OPTIMIZATION(
        title = "Memory Optimization",
        description = "Analyzing memory pressure & reclaiming background RAM"
    ),
    THERMAL_PROTECTION(
        title = "Thermal Protection",
        description = "Evaluating device temperature & thermal throttling risk"
    ),
    POWER_OPTIMIZATION(
        title = "Power Optimization",
        description = "Checking Battery Saver & unrestricted power status"
    ),
    GAMING_PROFILE(
        title = "Gaming Profile",
        description = "Selecting optimal CPU, ADPF & frame pacing profile"
    ),
    FINAL_REPORT(
        title = "Final Performance Report",
        description = "Compiling optimization results & readiness score"
    )
}

enum class BoostStepStatus {
    PENDING,
    RUNNING,
    DONE,
    WARNING
}

data class OneTapStepResult(
    val id: BoostStepId,
    val status: BoostStepStatus = BoostStepStatus.PENDING,
    val metricBadge: String = "Queued",
    val detail: String = id.description
)

data class OneTapBoosterState(
    val isVisible: Boolean = false,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val currentStepIndex: Int = 0,
    val progress: Float = 0f,
    val steps: List<OneTapStepResult> = defaultSteps(),
    val scanCpuMHz: Int = 0,
    val scanCpuCores: Int = 0,
    val ramUsedBeforePercent: Int = 0,
    val ramUsedAfterPercent: Int = 0,
    val memoryPressureLabel: String = "LOW",
    val memoryAdvice: String = "",
    val freedMb: Int = 0,
    val trimmedApps: Int = 0,
    val clearedCacheKb: Long = 0L,
    val thermalLabel: String = "NORMAL",
    val batteryTempC: Float = 32f,
    val thermalAdvice: String = "",
    val batteryPercent: Int = 80,
    val isCharging: Boolean = false,
    val batterySaverActive: Boolean = false,
    val ignoringBatteryOpt: Boolean = false,
    val powerAdvice: String = "",
    val selectedCpuMode: CpuMode = CpuMode.ACCELERATION,
    val sustainedEnabled: Boolean = false,
    val appliedProfileLabel: String = "",
    val finalScore: Int = 0,
    val finalVerdict: String = ""
) {
    companion object {
        fun defaultSteps(): List<OneTapStepResult> =
            BoostStepId.entries.map { OneTapStepResult(id = it) }
    }
}
