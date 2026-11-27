package com.example.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.graphics.RuntimeShader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.GameTurboSettings
import com.example.data.XSpaceIntroAudioEngine
import com.example.ui.components.TurboXLogoMark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val CosmicDeepSpace = Color(0xFF020309)
private val CosmicNebulaNavy = Color(0xFF070D1F)
private val CosmicViolet = Color(0xFF8B5CF6)
private val CosmicBlue = Color(0xFF2979FF)
private val CosmicCyan = Color(0xFF00E5FF)
private val CosmicSilver = Color(0xFFE6EEFF)

/**
 * AGSL shader (API 33+ high/mid devices) rendering a subtle dark cosmic lensing
 * aurora & radial warp wave behind the Compose Canvas particles and accretion rings.
 */
private const val BLACK_HOLE_COSMIC_AGSL = """
    uniform float2 uResolution;
    uniform float uTime;
    uniform float uSingularity;
    uniform float uWarp;

    half4 main(float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * uResolution) / min(uResolution.x, uResolution.y);
        float dist = length(uv);
        float angle = atan(uv.y, uv.x);

        float swirl = angle + (1.4 * uSingularity) / (dist + 0.14) - uTime * 2.2;
        float wave = 0.5 + 0.5 * sin(swirl * 3.0 + dist * 9.0 - uTime * 3.5);
        float diskBand = exp(-abs(dist - (0.20 + 0.06 * sin(uTime))) * 9.5) * uSingularity;

        float3 blueCol = float3(0.16, 0.47, 1.0);
        float3 violetCol = float3(0.55, 0.36, 0.96);
        float3 cyanCol = float3(0.0, 0.90, 1.0);

        float3 color = mix(blueCol, violetCol, wave) * diskBand * 0.42;
        color += cyanCol * exp(-dist * 5.2) * uSingularity * 0.24;

        float warpRing = exp(-abs(dist - (uWarp * 1.15)) * 12.0) * uWarp;
        color += mix(violetCol, cyanCol, 0.55) * warpRing * 0.52;

        float vignette = smoothstep(0.04, 0.16 + uWarp * 0.45, dist);
        color *= mix(1.0, vignette, uSingularity * 0.85);

        return half4(color, clamp(diskBand * 0.7 + warpRing * 0.65, 0.0, 0.72));
    }
"""

internal data class XSpacePerformanceProfile(
    val particleCount: Int,
    val useShaderBackdrop: Boolean,
    val useSimplifiedFallback: Boolean
)

internal fun resolveXSpacePerformanceProfile(
    context: Context,
    settings: GameTurboSettings,
    sdkInt: Int = Build.VERSION.SDK_INT
): XSpacePerformanceProfile {
    val am = runCatching {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }.getOrNull()
    val isLowRam = am?.isLowRamDevice == true
    val cores = Runtime.getRuntime().availableProcessors()
    val constrained = isLowRam || cores <= 4 || settings.lowRamOverlayMode
    val fallbackOnly = !settings.openingAnimation

    return when {
        fallbackOnly -> XSpacePerformanceProfile(
            particleCount = 12,
            useShaderBackdrop = false,
            useSimplifiedFallback = true
        )
        constrained -> XSpacePerformanceProfile(
            particleCount = 18,
            useShaderBackdrop = false,
            useSimplifiedFallback = false
        )
        else -> XSpacePerformanceProfile(
            particleCount = 48,
            useShaderBackdrop = sdkInt >= Build.VERSION_CODES.TIRAMISU,
            useSimplifiedFallback = false
        )
    }
}

private data class CosmicParticleSeed(
    val baseAngleRad: Float,
    val initialRadiusFraction: Float,
    val angularVelocity: Float,
    val radiusDp: Float,
    val colorIndex: Int,
    val trailArcRad: Float
)

private fun buildParticlePool(count: Int): Array<CosmicParticleSeed> {
    var seed = 133742
    return Array(count) { idx ->
        seed = seed * 1103515245 + 12345
        val r1 = ((seed ushr 16) and 0x7FFF) / 32767f
        seed = seed * 1103515245 + 12345
        val r2 = ((seed ushr 16) and 0x7FFF) / 32767f
        seed = seed * 1103515245 + 12345
        val r3 = ((seed ushr 16) and 0x7FFF) / 32767f
        CosmicParticleSeed(
            baseAngleRad = (idx.toFloat() / count.toFloat()) * (2f * PI.toFloat()) + r1 * 0.35f,
            initialRadiusFraction = 0.24f + r2 * 0.68f,
            angularVelocity = 1.6f + r3 * 2.8f,
            radiusDp = 1.2f + r1 * 2.1f,
            colorIndex = idx % 3,
            trailArcRad = 0.08f + r2 * 0.16f
        )
    }
}

@Composable
fun XSpaceBlackHoleIntroScreen(
    settings: GameTurboSettings,
    onToggleMute: (Boolean) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val profile = remember(settings.lowRamOverlayMode, settings.openingAnimation) {
        resolveXSpacePerformanceProfile(context, settings)
    }
    val particles = remember(profile.particleCount) {
        buildParticlePool(profile.particleCount)
    }

    val runtimeShader = remember(profile.useShaderBackdrop) {
        if (profile.useShaderBackdrop && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(BLACK_HOLE_COSMIC_AGSL) }.getOrNull()
        } else {
            null
        }
    }

    val audioEngine = remember(context) { XSpaceIntroAudioEngine(context) }

    var savedProgress by rememberSaveable { mutableFloatStateOf(0f) }
    val introProgress = remember { Animatable(savedProgress.coerceIn(0f, 1f)) }
    var transitionDispatched by rememberSaveable { mutableStateOf(false) }
    var isLifecycleResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var localMuted by rememberSaveable { mutableStateOf(!settings.xSpaceIntroSoundEnabled) }
    var controlsVisible by remember { mutableStateOf(false) }

    fun completeIntro() {
        if (transitionDispatched) return
        transitionDispatched = true
        savedProgress = 1f
        audioEngine.release()
        onFinished()
    }

    BackHandler(enabled = !transitionDispatched) {
        completeIntro()
    }

    LaunchedEffect(settings.xSpaceIntroSoundEnabled) {
        val nextMuted = !settings.xSpaceIntroSoundEnabled
        localMuted = nextMuted
        audioEngine.setMuted(nextMuted, scope, introProgress.value)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleResumed = true
                    audioEngine.resume(scope, introProgress.value)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isLifecycleResumed = false
                    savedProgress = introProgress.value
                    audioEngine.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (!profile.useSimplifiedFallback && !transitionDispatched) {
            audioEngine.start(
                scope = scope,
                initialMuted = localMuted,
                startProgress = introProgress.value
            )
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioEngine.release()
        }
    }

    LaunchedEffect(Unit) {
        controlsVisible = true
    }

    LaunchedEffect(isLifecycleResumed, profile.useSimplifiedFallback, transitionDispatched) {
        if (!isLifecycleResumed || transitionDispatched) return@LaunchedEffect
        val totalMs = if (profile.useSimplifiedFallback) 340 else XSpaceIntroAudioEngine.DURATION_MS
        val remainingFraction = (1f - introProgress.value).coerceIn(0f, 1f)
        val remainingMs = (totalMs * remainingFraction).toInt().coerceAtLeast(16)
        introProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = remainingMs, easing = LinearEasing)
        )
        savedProgress = 1f
        completeIntro()
    }

    val p = introProgress.value
    savedProgress = p

    // Phase progress factors
    val logoFadeIn = ((p - 0.02f) / 0.22f).coerceIn(0f, 1f)
    val logoScaleInitial = 0.62f + 0.38f * easeOutCubic(((p - 0.02f) / 0.24f).coerceIn(0f, 1f))
    val sweepProgress = ((p - 0.10f) / 0.22f).coerceIn(0f, 1f)

    val singularityStrength = easeInOutCubic(((p - 0.26f) / 0.28f).coerceIn(0f, 1f))
    val suctionProgress = easeInCubic(((p - 0.66f) / 0.22f).coerceIn(0f, 1f))
    val warpProgress = easeOutCubic(((p - 0.86f) / 0.14f).coerceIn(0f, 1f))

    // Logo absorption transform (pulled & twisted into portal during 0.66 -> 0.88)
    val logoScale = (logoScaleInitial * (1f - 0.92f * suctionProgress)).coerceAtLeast(0.05f)
    val logoScaleX = logoScale * (1f + 0.24f * sin(suctionProgress * PI.toFloat()))
    val logoScaleY = logoScale * (1f - 0.28f * suctionProgress)
    val logoRotation = 115f * suctionProgress * suctionProgress
    val logoAlpha = (logoFadeIn * (1f - easeInCubic(((p - 0.72f) / 0.16f).coerceIn(0f, 1f)))).coerceIn(0f, 1f)

    // Controlled cinematic light burst peak at warp onset (0.87 -> 0.95 -> 1.00)
    val burstPhase = ((p - 0.87f) / 0.13f).coerceIn(0f, 1f)
    val burstAlpha = if (burstPhase <= 0.42f) {
        (burstPhase / 0.42f) * 0.46f
    } else {
        ((1f - burstPhase) / 0.58f) * 0.46f
    }.coerceIn(0f, 0.46f)

    val statusText = when {
        p < 0.28f -> stringResource(R.string.xspace_intro_phase_cosmic)
        p < 0.66f -> stringResource(R.string.xspace_intro_phase_horizon)
        p < 0.87f -> stringResource(R.string.xspace_intro_phase_vortex)
        else -> stringResource(R.string.xspace_intro_phase_warp)
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag("xspace_black_hole_intro_root")
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(CosmicNebulaNavy, CosmicDeepSpace, Color.Black)
                )
            )
            // Block stray taps during the intro so user doesn't double-trigger navigation
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        val isCompactWidth = maxWidth < 380.dp
        val isLandscape = maxWidth > maxHeight
        val baseLogoSize = if (isLandscape) {
            (maxHeight * 0.36f).coerceIn(96.dp, 148.dp)
        } else {
            (maxWidth * 0.34f).coerceIn(108.dp, 156.dp)
        }

        // Layer 1: Cosmic field, optional API 33+ AGSL shader, rotating accretion disk, spiraling particles, warp flash
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = if (isLandscape) h * 0.48f else h * 0.46f
            val minDim = minOf(w, h)
            val diagonal = sqrt(w * w + h * h)

            // Ambient deep blue / violet nebula glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CosmicBlue.copy(alpha = 0.16f + 0.14f * singularityStrength),
                        CosmicViolet.copy(alpha = 0.12f + 0.10f * singularityStrength),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = minDim * (0.48f + 0.20f * singularityStrength)
                ),
                radius = minDim * (0.48f + 0.20f * singularityStrength),
                center = Offset(cx, cy)
            )

            // Optional AGSL RuntimeShader backdrop on supported devices (safe fallback if null)
            if (runtimeShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    runtimeShader.setFloatUniform("uResolution", w, h)
                    runtimeShader.setFloatUniform("uTime", p * 3.35f)
                    runtimeShader.setFloatUniform("uSingularity", singularityStrength)
                    runtimeShader.setFloatUniform("uWarp", warpProgress)
                    drawRect(brush = ShaderBrush(runtimeShader))
                }
            }

            // Pre-allocated floating & inward-spiraling cosmic particles
            val spiralPull = easeInCubic(((p - 0.24f) / 0.64f).coerceIn(0f, 1f))
            val warpStretch = warpProgress * 1.4f
            for (i in particles.indices) {
                val pt = particles[i]
                val pColor = when (pt.colorIndex) {
                    0 -> CosmicCyan
                    1 -> CosmicBlue
                    else -> CosmicViolet
                }
                val radial = minDim * pt.initialRadiusFraction * (1f - 0.88f * spiralPull) +
                    diagonal * 0.38f * warpStretch * pt.initialRadiusFraction
                val angle = pt.baseAngleRad + p * pt.angularVelocity * (1f + 2.4f * spiralPull)
                val px = cx + cos(angle) * radial
                val py = cy + sin(angle) * radial * (0.94f - 0.12f * singularityStrength)
                val particleAlpha = (0.35f + 0.55f * (1f - warpProgress * 0.6f)).coerceIn(0f, 0.9f)

                if (spiralPull > 0.08f || warpProgress > 0.05f) {
                    val prevAngle = angle - pt.trailArcRad * (0.6f + spiralPull * 1.6f)
                    val prevRadius = if (warpProgress > 0.05f) {
                        (radial * 0.76f).coerceAtLeast(4f)
                    } else {
                        radial * 1.07f
                    }
                    val tx = cx + cos(prevAngle) * prevRadius
                    val ty = cy + sin(prevAngle) * prevRadius * (0.94f - 0.12f * singularityStrength)
                    drawLine(
                        color = pColor.copy(alpha = particleAlpha * 0.62f),
                        start = Offset(tx, ty),
                        end = Offset(px, py),
                        strokeWidth = pt.radiusDp.dp.toPx() * 0.85f,
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    color = pColor.copy(alpha = particleAlpha),
                    radius = pt.radiusDp.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Rotating tilted accretion disk + photon sphere + dark singularity core
            if (singularityStrength > 0.01f) {
                val baseDiskRadius = minDim * (0.21f + 0.07f * singularityStrength) * (1f - 0.14f * suctionProgress) +
                    diagonal * 0.68f * warpProgress
                val ringTiltY = 0.42f + 0.58f * warpProgress

                // Outer violet-blue accretion swirl
                rotate(degrees = p * 540f, pivot = Offset(cx, cy)) {
                    drawOval(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                CosmicBlue.copy(alpha = 0.08f),
                                CosmicCyan.copy(alpha = 0.52f * singularityStrength),
                                CosmicViolet.copy(alpha = 0.64f * singularityStrength),
                                CosmicBlue.copy(alpha = 0.35f * singularityStrength),
                                CosmicBlue.copy(alpha = 0.08f)
                            ),
                            center = Offset(cx, cy)
                        ),
                        topLeft = Offset(cx - baseDiskRadius * 1.55f, cy - baseDiskRadius * 1.55f * ringTiltY),
                        size = Size(baseDiskRadius * 3.10f, baseDiskRadius * 3.10f * ringTiltY),
                        style = Stroke(width = (10.dp.toPx() * (0.5f + 0.5f * singularityStrength)))
                    )
                }

                // Counter-rotating inner cyan-violet photon ring
                rotate(degrees = -p * 760f, pivot = Offset(cx, cy)) {
                    drawOval(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                CosmicViolet.copy(alpha = 0.12f),
                                CosmicCyan.copy(alpha = 0.78f * singularityStrength),
                                Color.White.copy(alpha = 0.65f * singularityStrength),
                                CosmicBlue.copy(alpha = 0.50f * singularityStrength),
                                CosmicViolet.copy(alpha = 0.12f)
                            ),
                            center = Offset(cx, cy)
                        ),
                        topLeft = Offset(cx - baseDiskRadius * 1.10f, cy - baseDiskRadius * 1.10f * ringTiltY),
                        size = Size(baseDiskRadius * 2.20f, baseDiskRadius * 2.20f * ringTiltY),
                        style = Stroke(width = 4.5.dp.toPx())
                    )
                }

                // Event horizon black core expanding during final warp transition
                val coreRadius = (minDim * 0.11f * singularityStrength * (1f + 0.35f * suctionProgress)) +
                    (diagonal * 0.62f * warpProgress.pow(1.4f))

                // Soft horizon rim glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black,
                            CosmicBlue.copy(alpha = 0.70f * singularityStrength),
                            CosmicViolet.copy(alpha = 0.38f * singularityStrength),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = (coreRadius * 1.55f).coerceAtLeast(1f)
                    ),
                    radius = (coreRadius * 1.55f).coerceAtLeast(1f),
                    center = Offset(cx, cy)
                )

                // Pure black singularity void
                drawCircle(
                    color = Color.Black,
                    radius = coreRadius.coerceAtLeast(1f),
                    center = Offset(cx, cy)
                )

                // Crisp event-horizon ring
                drawCircle(
                    color = CosmicCyan.copy(alpha = (0.72f * singularityStrength * (1f - warpProgress * 0.75f)).coerceIn(0f, 1f)),
                    radius = coreRadius.coerceAtLeast(1f),
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }

            // Controlled cinematic warp light burst (never blinding)
            if (burstAlpha > 0.005f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CosmicCyan.copy(alpha = burstAlpha),
                            CosmicBlue.copy(alpha = burstAlpha * 0.75f),
                            CosmicViolet.copy(alpha = burstAlpha * 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = diagonal * (0.28f + 0.55f * warpProgress)
                    ),
                    radius = diagonal * (0.28f + 0.55f * warpProgress),
                    center = Offset(cx, cy)
                )
            }
        }

        // Layer 2: Centered Metallic Turbo X Logo + Neon Glow + Light Sweep + Absorption Motion
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(bottom = if (isLandscape) 12.dp else 28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(baseLogoSize * 1.45f)
                        .graphicsLayer {
                            alpha = logoAlpha
                            scaleX = logoScaleX
                            scaleY = logoScaleY
                            rotationZ = logoRotation
                        }
                ) {
                    // Neon blue/purple emblem aura
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension * 0.48f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CosmicBlue.copy(alpha = 0.45f * logoFadeIn),
                                    CosmicViolet.copy(alpha = 0.25f * logoFadeIn),
                                    Color.Transparent
                                )
                            ),
                            radius = r
                        )
                    }

                    // Existing original Turbo X emblem mark
                    TurboXLogoMark(
                        accentColor = CosmicCyan,
                        modifier = Modifier.size(baseLogoSize)
                    )

                    // Diagonal metallic light sweep across the logo
                    if (sweepProgress in 0.01f..0.99f) {
                        Canvas(
                            modifier = Modifier
                                .size(baseLogoSize)
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            val w = size.width
                            val h = size.height
                            val bandX = -w * 0.5f + w * 2.0f * sweepProgress
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        CosmicSilver.copy(alpha = 0.58f),
                                        CosmicCyan.copy(alpha = 0.72f),
                                        Color.Transparent
                                    ),
                                    start = Offset(bandX - w * 0.22f, 0f),
                                    end = Offset(bandX + w * 0.22f, h)
                                ),
                                start = Offset(bandX - w * 0.28f, h * 0.95f),
                                end = Offset(bandX + w * 0.28f, h * 0.05f),
                                strokeWidth = w * 0.24f
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

                Text(
                    text = stringResource(R.string.xspace_intro_brand),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompactWidth || isLandscape) 22.sp else 28.sp,
                        letterSpacing = 5.sp
                    ),
                    color = CosmicSilver,
                    modifier = Modifier.alpha((logoAlpha * (1f - warpProgress)).coerceIn(0f, 1f))
                )
                Text(
                    text = stringResource(R.string.xspace_intro_subtitle),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.2.sp
                    ),
                    color = CosmicCyan,
                    modifier = Modifier.alpha((logoAlpha * 0.9f * (1f - warpProgress)).coerceIn(0f, 1f))
                )
            }
        }

        // Layer 3: Safe-area Top Controls (Mute / Skip Intro) & Bottom Phase Indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(240)),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val muteDesc = stringResource(
                        if (localMuted) R.string.xspace_intro_unmute_cd else R.string.xspace_intro_mute_cd
                    )
                    Box(
                        modifier = Modifier
                            .testTag("xspace_intro_mute_btn")
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xA60B1224))
                            .border(1.dp, CosmicBlue.copy(alpha = 0.65f), CircleShape)
                            .clickable {
                                val nextMuted = !localMuted
                                localMuted = nextMuted
                                audioEngine.setMuted(nextMuted, scope, introProgress.value)
                                onToggleMute(!nextMuted)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (localMuted) {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            contentDescription = muteDesc,
                            tint = if (localMuted) TextMuted else CosmicCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    val skipShape = CutCornerShape(topStart = 9.dp, bottomEnd = 9.dp, topEnd = 3.dp, bottomStart = 3.dp)
                    Row(
                        modifier = Modifier
                            .testTag("xspace_intro_skip_btn")
                            .minimumInteractiveComponentSize()
                            .clip(skipShape)
                            .background(Color(0xB80B1224))
                            .border(1.dp, CosmicViolet.copy(alpha = 0.72f), skipShape)
                            .clickable(enabled = !transitionDispatched) { completeIntro() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.xspace_intro_skip),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = CosmicSilver
                        )
                        Icon(
                            imageVector = Icons.Rounded.DoubleArrow,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = if (isLandscape) 6.dp else 14.dp)
                    .alpha((1f - warpProgress * 0.9f).coerceIn(0f, 1f))
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(if (isCompactWidth) 140.dp else 180.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x402979FF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(p.coerceIn(0.03f, 1f))
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CosmicBlue, CosmicViolet, CosmicCyan)
                                )
                            )
                    )
                }
            }
        }
    }
}

private fun easeOutCubic(x: Float): Float {
    val inv = 1f - x.coerceIn(0f, 1f)
    return 1f - inv * inv * inv
}

private fun easeInCubic(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * c
}

private fun easeInOutCubic(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return if (c < 0.5f) 4f * c * c * c else 1f - (-2f * c + 2f).let { it * it * it } / 2f
}
