package com.example.data

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class GameAudioEnhancer(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var toneGenerator: ToneGenerator? = null
    private var savedNotificationVolume: Int? = null

    fun applyEqPreset(enabled: Boolean, preset: EqPreset): Boolean {
        releaseAudioFxOnly()
        if (!enabled || preset == EqPreset.OFF) return true
        return runCatching {
            val eq = Equalizer(0, 0).apply { this.enabled = true }
            val bands = eq.numberOfBands.toInt().coerceAtLeast(1)
            val minLevel = eq.bandLevelRange[0].toInt()
            val maxLevel = eq.bandLevelRange[1].toInt()
            val mid = (minLevel + maxLevel) / 2

            for (band in 0 until bands) {
                val targetLevel = when (preset) {
                    EqPreset.OFF -> mid
                    EqPreset.FPS_FOOTSTEPS -> {
                        if (band >= bands / 2) (maxLevel * 0.75f).toInt() else (minLevel * 0.2f).toInt()
                    }
                    EqPreset.MOBA_CLARITY -> {
                        if (band in 1 until (bands - 1)) (maxLevel * 0.65f).toInt() else mid
                    }
                    EqPreset.BASS_BOOST -> {
                        if (band <= 1) (maxLevel * 0.85f).toInt() else (maxLevel * 0.25f).toInt()
                    }
                }.coerceIn(minLevel, maxLevel)
                eq.setBandLevel(band.toShort(), targetLevel.toShort())
            }
            equalizer = eq

            if (preset == EqPreset.BASS_BOOST) {
                val bb = BassBoost(0, 0).apply {
                    if (strengthSupported) setStrength(700)
                    this.enabled = true
                }
                bassBoost = bb
            }
            true
        }.getOrDefault(false)
    }

    fun playTacticalTone() {
        runCatching {
            val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 35).also {
                toneGenerator = it
            }
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 70)
        }
    }

    fun pulseHaptic(strong: Boolean = false) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        if (strong) 32L else 16L,
                        if (strong) 180 else 90
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (strong) 32L else 16L)
            }
        }
    }

    fun setNotificationMuted(mute: Boolean): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return runCatching {
            if (mute) {
                if (savedNotificationVolume == null) {
                    savedNotificationVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                }
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            } else {
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                savedNotificationVolume?.let { prev ->
                    am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, prev, 0)
                    savedNotificationVolume = null
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun releaseAudioFxOnly() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }

    fun releaseEffects() {
        releaseAudioFxOnly()
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }
}
