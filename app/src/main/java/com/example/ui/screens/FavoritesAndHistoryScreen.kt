package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.HistoryItem
import com.example.model.AyahBookmark
import com.example.model.FamousAyah
import com.example.model.Surah
import com.example.player.PlayerState
import com.example.ui.components.GlobalAddAyahDialog
import com.example.ui.components.IslamicStarBadge
import com.example.ui.components.SurahCard
import com.example.ui.components.formatTimeMs
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesAndHistoryScreen(
    favorites: List<Surah>,
    bookmarkedAyahs: List<AyahBookmark>,
    history: List<HistoryItem>,
    playerState: PlayerState,
    onSurahSelected: (Surah) -> Unit,
    onPlaySurah: (Surah) -> Unit,
    onPlayAyahBookmark: (AyahBookmark) -> Unit,
    onDeleteAyahBookmark: (Long) -> Unit,
    onAddAyahBookmark: (surahNumber: Int, ayahNumber: Int, note: String) -> Unit,
    onResumeHistory: (HistoryItem) -> Unit,
    onFavoriteToggle: (Int) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddAyahDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Surahs (${favorites.size})",
        "Ayahs (${bookmarkedAyahs.size})",
        "History (${history.size})"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "Saved & Bookmarks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Quick access to your favorite Surahs, Ayahs, and recent listening history",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                val icon = when (index) {
                    0 -> Icons.Default.Bookmark
                    1 -> Icons.Default.AutoAwesome
                    else -> Icons.Default.History
                }
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.testTag("saved_tab_$index")
                )
            }
        }

        when (selectedTabIndex) {
            0 -> {
                // Favorite Surahs Tab
                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = GoldAccent.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No bookmarked Surahs yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap the bookmark icon on any Surah to save it here for quick listening.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("favorites_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favorites, key = { it.number }) { surah ->
                            val isCurrent = playerState.currentSurah?.number == surah.number
                            SurahCard(
                                surah = surah,
                                isCurrentSurah = isCurrent,
                                playerStatus = playerState.status,
                                isFavorite = true,
                                onCardClick = { onSurahSelected(surah) },
                                onPlayClick = { onPlaySurah(surah) },
                                onFavoriteToggle = { onFavoriteToggle(surah.number) },
                                onAddToPlaylistClick = null
                            )
                        }
                    }
                }
            }

            1 -> {
                // Bookmarked Ayahs Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar with "+ Bookmark Ayah" button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${bookmarkedAyahs.size} Bookmarked Verses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { showAddAyahDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_ayah_bookmark_trigger_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bookmark Ayah", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Popular/Famous Ayahs quick-add strip
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Quick Bookmark Famous Verses:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldAccent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(FamousAyah.POPULAR_AYAHS) { famous ->
                                    val isAlreadyBookmarked = bookmarkedAyahs.any {
                                        it.surah.number == famous.surahNumber && it.ayahNumber == famous.ayahNumber
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isAlreadyBookmarked) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isAlreadyBookmarked) EmeraldPrimary else GoldAccent.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (!isAlreadyBookmarked) {
                                                    onAddAyahBookmark(famous.surahNumber, famous.ayahNumber, famous.name)
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isAlreadyBookmarked) Icons.Default.Bookmark else Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (isAlreadyBookmarked) EmeraldPrimary else GoldAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${famous.surahNumber}:${famous.ayahNumber} ${famous.name.split(" ").firstOrNull()}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (bookmarkedAyahs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = GoldAccent.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No bookmarked Ayahs yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Bookmark specific verses and reflections to easily replay them or review anytime.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("bookmarked_ayahs_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(bookmarkedAyahs, key = { it.id }) { bookmark ->
                                val isCurrent = playerState.currentSurah?.number == bookmark.surah.number
                                val famousMatch = FamousAyah.POPULAR_AYAHS.firstOrNull {
                                    it.surahNumber == bookmark.surah.number && it.ayahNumber == bookmark.ayahNumber
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onPlayAyahBookmark(bookmark) }
                                        .testTag("ayah_bookmark_card_${bookmark.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Verse Star Badge
                                        IslamicStarBadge(
                                            number = bookmark.ayahNumber,
                                            isActive = isCurrent,
                                            size = 42.dp
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Information
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${bookmark.surah.nameEnglish} • Ayah ${bookmark.ayahNumber}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) GoldAccent else MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (bookmark.note.isNotBlank()) {
                                                Text(
                                                    text = bookmark.note,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = GoldAccent,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (famousMatch != null) {
                                                Text(
                                                    text = famousMatch.arabicText,
                                                    fontFamily = FontFamily.Serif,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (bookmark.timestampMs > 0) {
                                                Text(
                                                    text = "Position: ${formatTimeMs(bookmark.timestampMs)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Actions: Delete & Play
                                        IconButton(
                                            onClick = { onDeleteAyahBookmark(bookmark.id) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("delete_ayah_bookmark_${bookmark.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Bookmark",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .clickable { onPlayAyahBookmark(bookmark) }
                                                .testTag("play_ayah_bookmark_${bookmark.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // History Tab
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = GoldAccent.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No recitation history",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "As you listen to Surahs, your listening history and progress will appear here so you can pick up where you left off.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Listened (${history.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            IconButton(
                                onClick = onClearHistory,
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("history_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(history, key = { "${it.surah.number}_${it.playedAt}" }) { item ->
                                val progress = if (item.durationMs > 0) {
                                    (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onResumeHistory(item) }
                                        .testTag("history_item_${item.surah.number}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IslamicStarBadge(
                                                number = item.surah.number,
                                                isActive = false,
                                                size = 40.dp
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.surah.nameEnglish,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${item.reciter.nameEnglish} • ${formatTimeMs(item.lastPositionMs)} / ${formatTimeMs(item.durationMs)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = item.surah.nameArabic,
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = GoldAccent
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onResumeHistory(item) }
                                                    .testTag("resume_history_${item.surah.number}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Resume",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (progress > 0f) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = GoldAccent,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAyahDialog) {
        GlobalAddAyahDialog(
            allSurahs = Surah.ALL_SURAHS,
            onSaveBookmark = { sNum, aNum, note ->
                onAddAyahBookmark(sNum, aNum, note)
                showAddAyahDialog = false
            },
            onDismiss = { showAddAyahDialog = false }
        )
    }
}
