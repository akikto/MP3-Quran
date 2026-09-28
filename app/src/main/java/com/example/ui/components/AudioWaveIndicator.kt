package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldAccent

@Composable
fun AudioWaveIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 18.dp,
    minHeight: Dp = 4.dp,
    color: Color = GoldAccent
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val animatables = remember(barCount) {
            List(barCount) { Animatable(minHeight.value) }
        }

        val targetFractions = remember { listOf(0.9f, 0.4f, 1.0f, 0.6f, 0.8f) }
        val durations = remember { listOf(450, 600, 350, 520, 480) }

        animatables.forEachIndexed { index, animatable ->
            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    val target = minHeight.value + (maxHeight.value - minHeight.value) * targetFractions[index % targetFractions.size]
                    val duration = durations[index % durations.size]
                    animatable.animateTo(
                        targetValue = target,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                } else {
                    animatable.snapTo(minHeight.value + (index % 2) * 3f)
                }
            }

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(animatable.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
