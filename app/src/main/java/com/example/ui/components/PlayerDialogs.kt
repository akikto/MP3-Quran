package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldAccent

@Composable
fun SleepTimerDialog(
    currentMinutesLeft: Int,
    onSetTimer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to "Turn Off Timer",
        15 to "15 Minutes",
        30 to "30 Minutes",
        45 to "45 Minutes",
        60 to "60 Minutes (1 Hour)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = GoldAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sleep Timer")
            }
        },
        text = {
            Column {
                Text(
                    text = "Playback will automatically pause when the timer expires.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                options.forEach { (mins, label) ->
                    val isSelected = (mins == 0 && currentMinutesLeft == 0) || (mins > 0 && currentMinutesLeft > 0 && (currentMinutesLeft in (mins - 2)..(mins + 2)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimer(mins) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSetTimer(mins) },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_sleep_timer")) {
                Text("Close", color = GoldAccent)
            }
        }
    )
}

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = GoldAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed")
            }
        },
        text = {
            Column {
                speeds.forEach { speed ->
                    val isSelected = currentSpeed == speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSpeed(speed) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectSpeed(speed) },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_speed_dialog")) {
                Text("Close", color = GoldAccent)
            }
        }
    )
}
