package com.stavros.graham

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.max

private const val BAR_COUNT = 20

// Per-bar multipliers give each bar a distinct height so the waveform looks
// natural rather than all bars moving in lockstep.
private val BAR_MULTIPLIERS = floatArrayOf(
    0.4f, 0.7f, 0.55f, 0.9f, 0.65f,
    1.0f, 0.75f, 0.85f, 0.5f, 0.95f,
    0.6f, 0.80f, 0.45f, 0.70f, 1.0f,
    0.55f, 0.90f, 0.65f, 0.75f, 0.40f,
)

// RMS dB values below this threshold are treated as silence.
private const val RMS_SILENCE_THRESHOLD = 0f

// RMS dB value at which bars reach full height. Values above this are clamped.
private const val RMS_FULL_SCALE = 10f

// Fraction of bar height shown at silence so bars are always visible.
private const val MIN_BAR_FRACTION = 0.08f

@Composable
fun WaveformIndicator(rmsLevel: Float) {
    // Normalise rmsLevel to [0, 1], clamped at both ends.
    val normalised = if (rmsLevel <= RMS_SILENCE_THRESHOLD) {
        0f
    } else {
        (rmsLevel / RMS_FULL_SCALE).coerceIn(0f, 1f)
    }

    val barColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(BAR_COUNT) { index ->
            val multiplier = remember { BAR_MULTIPLIERS[index] }
            val targetFraction = max(MIN_BAR_FRACTION, normalised * multiplier)
            val animatedFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 100),
                label = "bar_$index",
            )

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(animatedFraction)
                    .background(
                        color = barColor,
                        shape = RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}
