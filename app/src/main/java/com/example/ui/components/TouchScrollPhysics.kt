package com.example.ui.components

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

data class TurboTouchConfig(
    val scrollMultiplier: Float = 1.0f,
    val frictionFactor: Float = 1.0f,
    val unbufferedTouch: Boolean = false
) {
    val combinedGlideScale: Float
        get() = (scrollMultiplier * frictionFactor).coerceIn(0.45f, 2.8f)
}

val LocalTurboTouchConfig = compositionLocalOf { TurboTouchConfig() }

@Composable
fun rememberTurboFlingBehavior(): FlingBehavior {
    val base = ScrollableDefaults.flingBehavior()
    val scale = LocalTurboTouchConfig.current.combinedGlideScale
    return remember(base, scale) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val scaledVelocity = initialVelocity * scale
                return with(base) { performFling(scaledVelocity) }
            }
        }
    }
}
