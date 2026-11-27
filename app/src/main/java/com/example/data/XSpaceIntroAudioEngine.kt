package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Procedural 16-bit PCM cosmic audio synthesizer synchronized to the
 * 3.35-second X Space Black Hole intro phases:
 *  1. Ambient cosmic pad (0.00s - 3.35s)
 *  2. Logo appearance whoosh & metallic shimmer (0.08s - 0.78s)
 *  3. Singularity energy charging sweep (0.85s - 1.90s)
 *  4. Deep black hole sub-bass rumble (0.95s - 2.92s)
 *  5. Suction / vortex Doppler swirl (2.08s - 2.95s)
 *  6. Final warp transition impact & crystal tail (2.92s - 3.35s)
 *
 * Uses zero external asset dependencies, runs synthesis off the UI thread,
 * honors user mute preference and device STREAM_MUSIC volume, and cleans up safely.
 */
class XSpaceIntroAudioEngine(private val context: Context) {

    companion object {
        const val SAMPLE_RATE_HZ = 22050
        const val DURATION_MS = 3350
        private val TOTAL_SAMPLES = (SAMPLE_RATE_HZ * DURATION_MS) / 1000

        /**
         * Pure-Kotlin PCM synthesis helper (exposed for deterministic unit testing).
         */
        fun synthesizeSoundscapePcm(
            sampleRate: Int = SAMPLE_RATE_HZ,
            durationMs: Int = DURATION_MS
        ): ShortArray {
            val totalSamples = ((sampleRate.toLong() * durationMs) / 1000L).toInt().coerceAtLeast(1)
            val pcm = ShortArray(totalSamples)
            val twoPi = 2.0 * PI

            // Deterministic LCG + 1-pole filter state for smooth noise sweeps
            var seed = 0x5A17C0DE
            var lpNoise = 0.0
            var lpSuction = 0.0

            // Phase accumulators for frequency-swept oscillators
            var chargePhase = 0.0
            var suctionPhase = 0.0
            var impactPhase = 0.0

            val dt = 1.0 / sampleRate.toDouble()

            for (i in 0 until totalSamples) {
                val t = i * dt
                val norm = i.toDouble() / totalSamples.toDouble()

                // Deterministic white noise in [-1, 1]
                seed = seed * 1664525 + 1013904223
                val white = ((seed shr 16) and 0xFFFF) / 32768.0 - 1.0

                // 1. Ambient cosmic drone (A1 55Hz + E2 82.41Hz + slow nebula shimmer 164.81Hz)
                val ambientEnv = smoothWindow(t, start = 0.0, fadeIn = 0.25, fadeOutStart = 2.95, end = 3.35)
                val lfo = 0.78 + 0.22 * sin(twoPi * 1.15 * t)
                val ambient = ambientEnv * lfo * (
                    0.22 * sin(twoPi * 55.0 * t) +
                        0.15 * sin(twoPi * 82.41 * t) +
                        0.08 * sin(twoPi * 164.81 * t + 0.4 * sin(twoPi * 0.7 * t))
                    )

                // 2. Logo appearance whoosh + crystalline shimmer (0.06s -> 0.80s)
                val whooshEnv = bellEnvelope(t, start = 0.05, peak = 0.34, end = 0.82)
                val whooshAlpha = (0.03 + 0.18 * ((t - 0.05) / 0.77).coerceIn(0.0, 1.0))
                lpNoise += whooshAlpha * (white - lpNoise)
                val shimmerEnv = bellEnvelope(t, start = 0.22, peak = 0.38, end = 0.88)
                val logoCue = (whooshEnv * lpNoise * 0.38) +
                    (shimmerEnv * 0.14 * sin(twoPi * 659.25 * t)) +
                    (shimmerEnv * 0.09 * sin(twoPi * 987.77 * t))

                // 3. Energy charging harmonic sweep (0.85s -> 1.95s)
                val chargeEnv = bellEnvelope(t, start = 0.85, peak = 1.60, end = 1.98)
                val chargeProgress = ((t - 0.85) / 1.13).coerceIn(0.0, 1.0)
                val chargeFreq = 110.0 + 360.0 * chargeProgress * chargeProgress
                chargePhase += twoPi * chargeFreq * dt
                val chargeCue = chargeEnv * (
                    0.22 * sin(chargePhase) +
                        0.10 * sin(chargePhase * 2.0)
                    )

                // 4. Deep black hole rumble (0.95s -> 2.95s)
                val rumbleEnv = smoothWindow(t, start = 0.92, fadeIn = 0.35, fadeOutStart = 2.65, end = 2.96)
                val rumbleTremor = 0.75 + 0.25 * sin(twoPi * 7.5 * t)
                val rumble = rumbleEnv * rumbleTremor * (
                    0.34 * sin(twoPi * 36.0 * t) +
                        0.22 * sin(twoPi * 48.0 * t + 0.8 * sin(twoPi * 3.2 * t))
                    )

                // 5. Suction / vortex acceleration (2.05s -> 2.94s)
                val suctionEnv = bellEnvelope(t, start = 2.05, peak = 2.84, end = 2.96)
                val suctionProg = ((t - 2.05) / 0.91).coerceIn(0.0, 1.0)
                val suctionAlpha = 0.04 + 0.34 * suctionProg
                lpSuction += suctionAlpha * (white - lpSuction)
                val suctionFreq = 140.0 + 620.0 * suctionProg * suctionProg * suctionProg
                suctionPhase += twoPi * suctionFreq * dt
                val suctionCue = suctionEnv * (
                    0.28 * lpSuction +
                        0.20 * sin(suctionPhase) * (0.7 + 0.3 * sin(twoPi * 14.0 * t))
                    )

                // 6. Final warp transition impact + bright release tail (2.90s -> 3.35s)
                val impactTime = (t - 2.90).coerceAtLeast(0.0)
                val impactActive = if (t >= 2.90) 1.0 else 0.0
                val impactEnv = impactActive * exp(-impactTime * 7.5)
                val impactFreq = 88.0 * exp(-impactTime * 4.5) + 32.0
                impactPhase += twoPi * impactFreq * dt
                val impactTailEnv = impactActive * exp(-impactTime * 6.0) * smoothWindow(t, 2.90, 0.02, 3.18, 3.35)
                val impactCue = (impactEnv * 0.60 * sin(impactPhase)) +
                    (impactTailEnv * 0.16 * sin(twoPi * 523.25 * t)) +
                    (impactTailEnv * 0.11 * sin(twoPi * 783.99 * t))

                val masterFadeOut = if (norm > 0.94) ((1.0 - norm) / 0.06).coerceIn(0.0, 1.0) else 1.0
                val mix = (ambient + logoCue + chargeCue + rumble + suctionCue + impactCue) * masterFadeOut
                val saturated = tanh(mix * 1.15) * 0.88
                pcm[i] = (saturated * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return pcm
        }

        private fun smoothWindow(
            t: Double,
            start: Double,
            fadeIn: Double,
            fadeOutStart: Double,
            end: Double
        ): Double {
            if (t < start || t > end) return 0.0
            val inGain = if (fadeIn > 0.0) ((t - start) / fadeIn).coerceIn(0.0, 1.0) else 1.0
            val outLen = (end - fadeOutStart).coerceAtLeast(0.001)
            val outGain = if (t > fadeOutStart) ((end - t) / outLen).coerceIn(0.0, 1.0) else 1.0
            return inGain * outGain
        }

        private fun bellEnvelope(t: Double, start: Double, peak: Double, end: Double): Double {
            if (t < start || t > end) return 0.0
            return if (t <= peak) {
                val p = ((t - start) / (peak - start).coerceAtLeast(0.001)).coerceIn(0.0, 1.0)
                p * p
            } else {
                val p = ((end - t) / (end - peak).coerceAtLeast(0.001)).coerceIn(0.0, 1.0)
                p
            }
        }
    }

    private var audioTrack: AudioTrack? = null
    private var prepJob: Job? = null
    private var prepared = false
    private var isMuted = false
    private var isPaused = false
    private var isReleased = false
    private var lastProgress = 0f

    fun start(
        scope: CoroutineScope,
        initialMuted: Boolean,
        startProgress: Float = 0f
    ) {
        isReleased = false
        isMuted = initialMuted
        lastProgress = startProgress.coerceIn(0f, 1f)
        if (isMuted || !hasAudibleDeviceVolume()) {
            return
        }
        ensurePreparedAndPlay(scope, lastProgress)
    }

    fun setMuted(muted: Boolean, scope: CoroutineScope, currentProgress: Float) {
        isMuted = muted
        lastProgress = currentProgress.coerceIn(0f, 1f)
        if (muted) {
            runCatching {
                audioTrack?.apply {
                    setVolume(0f)
                    if (playState == AudioTrack.PLAYSTATE_PLAYING) pause()
                }
            }
        } else if (!isPaused && !isReleased && hasAudibleDeviceVolume()) {
            ensurePreparedAndPlay(scope, lastProgress)
        }
    }

    fun pause() {
        isPaused = true
        runCatching {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) pause()
            }
        }
    }

    fun resume(scope: CoroutineScope, currentProgress: Float) {
        isPaused = false
        lastProgress = currentProgress.coerceIn(0f, 1f)
        if (!isMuted && !isReleased && hasAudibleDeviceVolume()) {
            ensurePreparedAndPlay(scope, lastProgress)
        }
    }

    fun release() {
        isReleased = true
        prepJob?.cancel()
        prepJob = null
        runCatching {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) stop()
                flush()
                release()
            }
        }
        audioTrack = null
        prepared = false
    }

    private fun hasAudibleDeviceVolume(): Boolean {
        return runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
            am.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
        }.getOrDefault(true)
    }

    private fun ensurePreparedAndPlay(scope: CoroutineScope, progress: Float) {
        val existing = audioTrack
        if (prepared && existing != null) {
            playTrackFromProgress(existing, progress)
            return
        }
        if (prepJob?.isActive == true) return

        prepJob = scope.launch {
            val pcm = withContext(Dispatchers.Default) {
                synthesizeSoundscapePcm()
            }
            if (isReleased || isMuted || isPaused) return@launch
            runCatching {
                val byteCount = pcm.size * 2
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(byteCount)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                if (isReleased) {
                    track.release()
                    return@runCatching
                }
                audioTrack = track
                prepared = true
                if (!isMuted && !isPaused && hasAudibleDeviceVolume()) {
                    playTrackFromProgress(track, lastProgress)
                }
            }
        }
    }

    private fun playTrackFromProgress(track: AudioTrack, progress: Float) {
        runCatching {
            val frame = (progress.coerceIn(0f, 0.98f) * TOTAL_SAMPLES).toInt().coerceIn(0, TOTAL_SAMPLES - 1)
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
            }
            track.setPlaybackHeadPosition(frame)
            track.setVolume(0.92f)
            track.play()
        }
    }
}
