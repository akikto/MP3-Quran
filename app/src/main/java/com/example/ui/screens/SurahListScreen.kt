package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Reciter
import com.example.model.Surah
import com.example.player.PlayerState
import com.example.ui.components.SurahCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.viewmodel.SurahFilter

@Composable
fun SurahListScreen(
    surahs: List<Surah>,
    playerState: PlayerState,
    searchQuery: String,
    selectedFilter: SurahFilter,
    isSurahFavorite: (Int) -> Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (SurahFilter) -> Unit,
    onSurahSelected: (Surah) -> Unit,
    onPlaySurah: (Surah) -> Unit,
    onFavoriteToggle: (Int) -> Unit,
    onOpenReciterPicker: () -> Unit,
    onOpenSleepTimerPicker: () -> Unit,
    isSurahDownloaded: (Int) -> Boolean = { false },
    getDownloadProgress: (Int) -> Int? = { null },
    onDownloadClick: ((Surah) -> Unit)? = null,
    onAddToPlaylistClick: (Surah) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary,
                            EmeraldPrimaryContainer
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "MP3 Quran Logo",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GoldAccent, CircleShape)
                                .testTag("banner_app_logo")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MP3 Quran",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "114 Surahs • High Quality Audio",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldLight
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sleep Timer Pill in Banner
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (playerState.isSleepTimerActive) GoldAccent else Color.Black.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (playerState.isSleepTimerActive) GoldAccent else GoldAccent.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(onClick = onOpenSleepTimerPicker)
                                .testTag("banner_sleep_timer_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Sleep Timer",
                                    tint = if (playerState.isSleepTimerActive) Color.Black else GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                if (playerState.isSleepTimerActive) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = playerState.formattedSleepTimer,
                                        color = Color.Black,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Reciter Quick Switch Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(onClick = onOpenReciterPicker)
                                .testTag("hero_reciter_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                    text = playerState.currentReciter.nameEnglish.split(" ").lastOrNull()
                                        ?: playerState.currentReciter.nameEnglish,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bismillah Calligraphy Banner
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    color = GoldLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search by Surah name, number, or Arabic...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("surah_search_input")
        )

        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SurahFilter.values()) { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Surah List
        if (surahs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No Surahs matching \"$searchQuery\"" else "No Surahs in this category",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("surahs_lazy_column"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(surahs, key = { it.number }) { surah ->
                    val isCurrent = playerState.currentSurah?.number == surah.number
                    SurahCard(
                        surah = surah,
                        isCurrentSurah = isCurrent,
                        playerStatus = playerState.status,
                        isFavorite = isSurahFavorite(surah.number),
                        isDownloaded = isSurahDownloaded(surah.number),
                        downloadProgress = getDownloadProgress(surah.number),
                        onDownloadClick = onDownloadClick?.let { { it(surah) } },
                        onCardClick = { onSurahSelected(surah) },
                        onPlayClick = { onPlaySurah(surah) },
                        onFavoriteToggle = { onFavoriteToggle(surah.number) },
                        onAddToPlaylistClick = { onAddToPlaylistClick(surah) }
                    )
                }
            }
        }
    }
}
