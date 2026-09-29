package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Surah
import com.example.player.PlayerStatus
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

@Composable
fun SurahCard(
    surah: Surah,
    isCurrentSurah: Boolean,
    playerStatus: PlayerStatus,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    isDownloaded: Boolean = false,
    downloadProgress: Int? = null,
    onDownloadClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPlaying = isCurrentSurah && playerStatus == PlayerStatus.PLAYING
    val isBuffering = isCurrentSurah && playerStatus == PlayerStatus.BUFFERING
    val isDownloading = downloadProgress != null && downloadProgress in 0..99

    val containerColor = if (isCurrentSurah) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderStroke = if (isCurrentSurah) {
        BorderStroke(1.2.dp, GoldAccent)
    } else {
        BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onCardClick)
            .testTag("surah_card_${surah.number}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentSurah) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Star badge with Surah Number
            IslamicStarBadge(
                number = surah.number,
                isActive = isCurrentSurah,
                size = 36.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Surah English Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = surah.nameEnglish,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentSurah) GoldAccent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )

                    // Compact Revelation tag badge
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (surah.revelationType == Surah.RevelationType.MECCAN) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            GoldAccent.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = surah.revelationType.english,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (surah.revelationType == Surah.RevelationType.MECCAN) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                GoldAccent
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp
                        )
                    }

                    // Downloaded offline badge
                    if (isDownloaded) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflinePin,
                                    contentDescription = "Downloaded",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(9.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "${surah.englishTranslation} • ${surah.totalVerses} Verses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            // Arabic Name & Audio Wave (Compact)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = surah.nameArabic,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentSurah) GoldAccent else MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp
                )

                if (isCurrentSurah) {
                    AudioWaveIndicator(
                        isPlaying = isPlaying,
                        modifier = Modifier.padding(top = 1.dp),
                        barCount = 3,
                        color = GoldAccent
                    )
                }
            }

            // Compact Action Buttons: Download, Add to playlist, Favorite, Play/Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Download button
                if (onDownloadClick != null) {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("download_surah_${surah.number}")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { (downloadProgress ?: 0) / 100f },
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = GoldAccent
                            )
                        } else if (isDownloaded) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = "Download",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Add to Playlist button
                if (onAddToPlaylistClick != null) {
                    IconButton(
                        onClick = onAddToPlaylistClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("add_to_playlist_${surah.number}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlaylistAdd,
                            contentDescription = "Add to playlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Bookmark button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("bookmark_surah_${surah.number}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isFavorite) "Bookmarked" else "Bookmark",
                        tint = if (isFavorite) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Play / Pause Circle (Compact & Elegant)
                Surface(
                    shape = CircleShape,
                    color = if (isCurrentSurah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayClick)
                        .testTag("play_button_${surah.number}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 1.5.dp,
                                color = GoldAccent
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (isCurrentSurah) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
