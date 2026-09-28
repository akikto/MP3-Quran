package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerState
import com.example.player.PlayerStatus
import com.example.player.RepeatMode
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayerSheet(
    playerState: PlayerState,
    isFavorite: Boolean,
    isDownloaded: Boolean = false,
    downloadProgress: Int? = null,
    onDownloadClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onToggleRepeat: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onBookmarkAyahClick: (() -> Unit)? = null,
    onOpenReciterPicker: () -> Unit,
    onOpenSpeedPicker: () -> Unit,
    onSelectSpeed: (Float) -> Unit = {},
    onOpenSleepTimerPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surah = playerState.currentSurah ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("player_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent,
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Juz ${surah.startingJuz}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDownloadClick != null) {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.testTag("player_download_button")
                        ) {
                            if (downloadProgress != null && downloadProgress in 0..99) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = GoldAccent
                                )
                            } else if (isDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Downloaded for offline",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = "Download for offline listening",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    if (onBookmarkAyahClick != null) {
                        IconButton(
                            onClick = onBookmarkAyahClick,
                            modifier = Modifier.testTag("player_bookmark_ayah_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Bookmark Ayah",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.testTag("player_favorite_toggle")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isFavorite) GoldAccent else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Islamic Geometric Disc / Mandala Visual
            val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 35000, easing = LinearEasing),
                    repeatMode = AnimRepeatMode.Restart
                ),
                label = "rotation"
            )

            val isPlaying = playerState.status == PlayerStatus.PLAYING
            val currentRotation = if (isPlaying) rotation else 0f

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EmeraldPrimary.copy(alpha = 0.6f),
                                DarkSurface,
                                DarkBackground
                            )
                        )
                    )
                    .border(2.5.dp, GoldAccent.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Rotating geometric motif
                Canvas(
                    modifier = Modifier
                        .size(232.dp)
                        .rotate(currentRotation)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Draw Islamic 12-pointed geometric ring
                    for (i in 0 until 12) {
                        val angle = (i * 30 * PI / 180).toFloat()
                        val starX = center.x + (radius * 0.92f) * cos(angle)
                        val starY = center.y + (radius * 0.92f) * sin(angle)
                        drawCircle(
                            color = GoldLight.copy(alpha = 0.5f),
                            radius = 3.dp.toPx(),
                            center = Offset(starX, starY)
                        )
                    }

                    drawCircle(
                        color = GoldAccent.copy(alpha = 0.4f),
                        radius = radius * 0.96f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Rotating Disc with Center MP3 Quran Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "MP3 Quran Player Disc",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .rotate(currentRotation)
                        .border(2.dp, GoldAccent.copy(alpha = 0.8f), CircleShape)
                        .testTag("player_disc_logo")
                )
            }


            Spacer(modifier = Modifier.height(24.dp))

            // Surah Arabic Calligraphy & English Name
            Text(
                text = surah.nameArabic,
                fontFamily = FontFamily.Serif,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Surah ${surah.nameEnglish}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${surah.englishTranslation} • ${surah.revelationType.english}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reciter Badge & Offline status
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenReciterPicker)
                        .testTag("player_reciter_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Reciter",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = playerState.currentReciter.nameEnglish,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (playerState.isPlayingFromOfflineCache) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = com.example.ui.theme.EmeraldPrimary.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.EmeraldPrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline Cached",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Audio Equalizer Visualizer Bars
            AudioWaveIndicator(
                isPlaying = isPlaying,
                barCount = 9,
                barWidth = 4.dp,
                maxHeight = 22.dp,
                minHeight = 4.dp,
                color = GoldAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slider & Time row
            var isUserSeeking by remember { mutableStateOf(false) }
            var sliderPosition by remember { mutableFloatStateOf(0f) }

            val actualProgress = if (playerState.durationMs > 0) {
                (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val currentSliderVal = if (isUserSeeking) sliderPosition else actualProgress

            Slider(
                value = currentSliderVal,
                onValueChange = { newVal ->
                    isUserSeeking = true
                    sliderPosition = newVal
                },
                onValueChangeFinished = {
                    isUserSeeking = false
                    val seekTarget = (sliderPosition * playerState.durationMs).toLong()
                    onSeek(seekTarget)
                },
                colors = SliderDefaults.colors(
                    thumbColor = GoldAccent,
                    activeTrackColor = GoldAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_time_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentMs = if (isUserSeeking) {
                    (sliderPosition * playerState.durationMs).toLong()
                } else {
                    playerState.currentPositionMs
                }
                Text(
                    text = formatTimeMs(currentMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = formatTimeMs(playerState.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat Mode Toggle
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.testTag("player_repeat_toggle")
                ) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            RepeatMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                            RepeatMode.REPEAT_ALL -> Icons.Filled.Repeat
                            RepeatMode.OFF -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (playerState.repeatMode == RepeatMode.OFF) Color.White.copy(alpha = 0.4f) else GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Surah
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.testTag("player_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous Surah",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Rewind 10s
                IconButton(
                    onClick = onRewind10,
                    modifier = Modifier.testTag("player_rewind_10")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Main Play / Pause FAB
                Surface(
                    shape = CircleShape,
                    color = GoldAccent,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayPauseClick)
                        .testTag("player_main_play_pause")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (playerState.status == PlayerStatus.BUFFERING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = DarkBackground
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkBackground,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Forward 10s
                IconButton(
                    onClick = onForward10,
                    modifier = Modifier.testTag("player_forward_10")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Next Surah
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next Surah",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Speed Control Toggle Strip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_speed_toggle_group")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Playback Speed",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = if (playerState.playbackSpeed == 1.0f) "1.0x (Normal)" else "${playerState.playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed Toggle Buttons: 0.75x, 1x, 1.25x, 1.5x, 2x
                    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        speedOptions.forEach { speed ->
                            val isSelected = kotlin.math.abs(playerState.playbackSpeed - speed) < 0.05f
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) GoldAccent else Color.White.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) GoldAccent else Color.White.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectSpeed(speed) }
                                    .testTag("speed_toggle_${speed}x")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (speed == 1.0f) "1x" else "${speed}x",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Utilities: Speed, Sleep Timer, Reciter Quick Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Selector Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSpeedPicker)
                        .testTag("speed_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Playback Speed",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${playerState.playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sleep Timer Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (playerState.isSleepTimerActive) EmeraldPrimary else DarkSurface,
                    border = if (playerState.isSleepTimerActive) androidx.compose.foundation.BorderStroke(1.dp, GoldAccent) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSleepTimerPicker)
                        .testTag("sleep_timer_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (playerState.isSleepTimerActive) GoldLight else GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playerState.isSleepTimerActive) {
                                playerState.formattedSleepTimer
                            } else {
                                "Sleep Timer"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun formatTimeMs(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
