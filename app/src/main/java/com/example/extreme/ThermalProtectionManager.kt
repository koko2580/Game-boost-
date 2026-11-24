package com.example.extreme

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

interface ThermalTelemetryReader {
    fun readRaw(): RawThermalReading
}

data class RawThermalReading(
    val batteryTempCelsius: Float,
    val thermalStatusCode: Int,
    val batteryPercent: Int,
    val isCharging: Boolean
)

class AndroidThermalTelemetryReader(private val context: Context) : ThermalTelemetryReader {
    override fun readRaw(): RawThermalReading {
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 80
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320
        val tempC = (tempTenths / 10f).coerceIn(0f, 95f)
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val thermalCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            }.getOrDefault(0)
        } else {
            0
        }
        return RawThermalReading(
            batteryTempCelsius = tempC,
            thermalStatusCode = thermalCode,
            batteryPercent = pct,
            isCharging = charging
        )
    }
}

class ThermalProtectionManager(
    context: Context,
    private val reader: ThermalTelemetryReader = AndroidThermalTelemetryReader(context)
) {
    companion object {
        const val WARM_REDUCE_TEMP_C = 39.5f
        const val CRITICAL_PROTECT_TEMP_C = 43.0f
    }

    fun evaluate(): ThermalSnapshot {
        val raw = reader.readRaw()
        return classify(raw)
    }

    fun classify(raw: RawThermalReading): ThermalSnapshot {
        val thermalStatusLabel = statusCodeToLabel(raw.thermalStatusCode)
        val level = when {
            raw.batteryTempCelsius >= CRITICAL_PROTECT_TEMP_C ||
                raw.thermalStatusCode >= PowerManager.THERMAL_STATUS_SEVERE ->
                ThermalProtectionLevel.PROTECTED
            raw.batteryTempCelsius >= WARM_REDUCE_TEMP_C ||
                raw.thermalStatusCode == PowerManager.THERMAL_STATUS_MODERATE ||
                raw.thermalStatusCode == PowerManager.THERMAL_STATUS_LIGHT ->
                ThermalProtectionLevel.REDUCED
            else -> ThermalProtectionLevel.NORMAL
        }

        val notice = when (level) {
            ThermalProtectionLevel.NORMAL ->
                "Thermals normal (${raw.batteryTempCelsius}°C • $thermalStatusLabel)"
            ThermalProtectionLevel.REDUCED ->
                "Device warm (${raw.batteryTempCelsius}°C • $thermalStatusLabel) — optimization aggressiveness reduced"
            ThermalProtectionLevel.PROTECTED ->
                "Critical thermal load (${raw.batteryTempCelsius}°C • $thermalStatusLabel) — aggressive optimizations stopped for safety"
        }

        return ThermalSnapshot(
            batteryTempCelsius = raw.batteryTempCelsius,
            thermalStatusCode = raw.thermalStatusCode,
            thermalLabel = thermalStatusLabel,
            batteryPercent = raw.batteryPercent,
            isCharging = raw.isCharging,
            protectionLevel = level,
            notice = notice
        )
    }

    fun recommendedAggressiveness(snapshot: ThermalSnapshot): OptimizationAggressiveness {
        return when (snapshot.protectionLevel) {
            ThermalProtectionLevel.NORMAL -> OptimizationAggressiveness.FULL
            ThermalProtectionLevel.REDUCED -> OptimizationAggressiveness.REDUCED
            ThermalProtectionLevel.PROTECTED -> OptimizationAggressiveness.STOPPED
        }
    }

    private fun statusCodeToLabel(code: Int): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "NORMAL"
        return when (code) {
            PowerManager.THERMAL_STATUS_NONE -> "NORMAL"
            PowerManager.THERMAL_STATUS_LIGHT -> "WARM"
            PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
            PowerManager.THERMAL_STATUS_SEVERE -> "HIGH"
            PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
            else -> "NORMAL"
        }
    }
}
