package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    playerState: PlayerState,
    onSetDurationSeconds: (seconds: Int, fadeOut: Boolean) -> Unit,
    onSetEndOfSurah: () -> Unit,
    onAddMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fadeOutEnabled by remember { mutableStateOf(playerState.sleepTimerFadeOut) }
    var customSliderMinutes by remember { mutableFloatStateOf(20f) }
    var showCustomSlider by remember { mutableStateOf(false) }

    val presetMinutes = listOf(5, 10, 15, 30, 45, 60, 90, 120)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Stop audio recitation automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sleep_timer_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Active Timer Banner (if currently running)
            if (playerState.isSleepTimerActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_timer_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.5.dp, GoldAccent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DarkSurface,
                                        EmeraldPrimary.copy(alpha = 0.4f)
                                    )
                                )
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE TIMER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (playerState.sleepTimerEndOfSurah) {
                                "Pausing at End of Surah"
                            } else {
                                playerState.formattedSleepTimer
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        if (playerState.sleepTimerEndOfSurah && playerState.currentSurah != null) {
                            Text(
                                text = "Surah ${playerState.currentSurah.nameEnglish} (${playerState.currentSurah.nameArabic})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GoldLight
                            )
                        } else if (playerState.sleepTimerSecondsLeft > 0) {
                            Text(
                                text = "Remaining until playback stops",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick buttons: +5m, +15m, Cancel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!playerState.sleepTimerEndOfSurah) {
                                OutlinedButton(
                                    onClick = { onAddMinutes(5) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_5_minutes_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+5m", color = Color.White, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onAddMinutes(15) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_15_minutes_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+15m", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = onCancelTimer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("cancel_sleep_timer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TimerOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Option 1: "End of Current Surah" Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSetEndOfSurah() }
                    .testTag("option_end_of_surah"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (playerState.sleepTimerEndOfSurah) EmeraldPrimary else DarkSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (playerState.sleepTimerEndOfSurah) GoldAccent else Color.White.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (playerState.sleepTimerEndOfSurah) GoldAccent else EmeraldPrimary.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = if (playerState.sleepTimerEndOfSurah) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "End of Current Surah",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldAccent.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "POPULAR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (playerState.currentSurah != null) {
                                "Stops when Surah ${playerState.currentSurah.nameEnglish} finishes"
                            } else {
                                "Stops as soon as current recitation ends"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Option 2: Duration Presets
            Text(
                text = "Preset Durations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 4x2 Grid of Presets
            val rows = presetMinutes.chunked(4)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { mins ->
                        val isSelected = !playerState.sleepTimerEndOfSurah &&
                                playerState.sleepTimerSecondsLeft > 0 &&
                                (playerState.sleepTimerSecondsLeft in (mins * 60 - 30)..(mins * 60 + 30))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoldAccent else DarkSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GoldAccent else Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSetDurationSeconds(mins * 60, fadeOutEnabled)
                                }
                                .testTag("sleep_preset_${mins}m")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                                Text(
                                    text = if (mins >= 60) "${mins / 60}h" else "min",
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 3: Custom Duration Accordion
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustomSlider = !showCustomSlider },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Duration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = if (showCustomSlider) "Hide" else "${customSliderMinutes.toInt()} mins",
                            color = GoldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(visible = showCustomSlider) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${customSliderMinutes.toInt()} Minutes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldLight
                                )
                                Button(
                                    onClick = {
                                        onSetDurationSeconds(customSliderMinutes.toInt() * 60, fadeOutEnabled)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("apply_custom_timer")
                                ) {
                                    Text("Start Timer", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Slider(
                                value = customSliderMinutes,
                                onValueChange = { customSliderMinutes = it },
                                valueRange = 5f..180f,
                                steps = 34, // 5 min increments
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("custom_timer_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("5m", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text("60m", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text("120m", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text("180m", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smooth Fade-out Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gentle Fade Out",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Softly lowers volume in the final 30s",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Switch(
                        checked = fadeOutEnabled,
                        onCheckedChange = { fadeOutEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldAccent,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("fade_out_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
