package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Surah
import com.example.ui.components.AddAyahBookmarkDialog
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.FullScreenPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PlaybackSpeedDialog
import com.example.ui.components.ReciterPickerSheet
import com.example.ui.components.SleepTimerSheet
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FavoritesAndHistoryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.RecitersScreen
import com.example.ui.screens.SurahListScreen
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.QuranViewModel
import kotlinx.coroutines.launch

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SURAHS("Surahs", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    DOWNLOADS("Offline", Icons.Filled.CloudDone, Icons.Outlined.CloudDone),
    RECITERS("Qaris", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
    SAVED("Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    PLAYLISTS("Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic)
}

@Composable
fun QuranApp(
    viewModel: QuranViewModel = viewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val filteredSurahs by viewModel.filteredSurahs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val bookmarkedAyahs by viewModel.bookmarkedAyahs.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsStateWithLifecycle()
    val currentReciter by viewModel.currentReciter.collectAsStateWithLifecycle()

    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsStateWithLifecycle()
    val showReciterPicker by viewModel.showReciterPicker.collectAsStateWithLifecycle()
    val showSleepTimerPicker by viewModel.showSleepTimerPicker.collectAsStateWithLifecycle()
    val showSpeedPicker by viewModel.showSpeedPicker.collectAsStateWithLifecycle()
    val showAyahBookmarkDialog by viewModel.showAyahBookmarkDialog.collectAsStateWithLifecycle()
    val surahForPlaylist by viewModel.showPlaylistDialog.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportCollections(uri) { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importCollections(uri) { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }
    var currentTab by remember { mutableStateOf(NavigationTab.SURAHS) }
    val navigationFontSize = (11f * minOf(1f, 1.15f / LocalDensity.current.fontScale)).sp

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* granted or denied */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Display error message if any
    LaunchedEffect(playerState.errorMessage) {
        playerState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Mini Player floating above bottom navigation
                if (playerState.currentSurah != null) {
                    MiniPlayerBar(
                        playerState = playerState,
                        onExpandClick = { viewModel.openPlayer() },
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.playNext() },
                        onToggleSpeedClick = { viewModel.togglePlaybackSpeed() }
                    )
                }

                // Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = navigationFontSize,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = GoldAccent.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.SURAHS -> {
                    SurahListScreen(
                        surahs = filteredSurahs,
                        playerState = playerState,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        isSurahFavorite = { viewModel.isSurahFavorite(it) },
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onFilterSelected = { viewModel.onFilterSelected(it) },
                        onSurahSelected = { surah ->
                            viewModel.playSurah(surah)
                            viewModel.openPlayer()
                        },
                        onPlaySurah = { surah ->
                            if (playerState.currentSurah?.number == surah.number) {
                                viewModel.togglePlayPause()
                            } else {
                                viewModel.playSurah(surah)
                            }
                        },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onOpenReciterPicker = { viewModel.showReciterPickerSheet(true) },
                        onOpenSleepTimerPicker = { viewModel.showSleepTimerSheet(true) },
                        isSurahDownloaded = { viewModel.isSurahDownloaded(it, playerState.currentReciter.id) },
                        getDownloadProgress = { viewModel.getDownloadProgress(it, playerState.currentReciter.id) },
                        onDownloadClick = { viewModel.downloadSurah(it, playerState.currentReciter) },
                        onAddToPlaylistClick = { viewModel.openAddToPlaylist(it) }
                    )
                }

                NavigationTab.DOWNLOADS -> {
                    DownloadsScreen(
                        downloads = downloads,
                        playerState = playerState,
                        onPlayDownloadedSurah = { item, queue ->
                            viewModel.playSurah(item.surah, queue)
                            viewModel.openPlayer()
                        },
                        onDeleteDownload = { num, reciterId ->
                            viewModel.deleteDownload(num, reciterId)
                        },
                        onDeleteAllDownloads = {
                            viewModel.deleteAllDownloads()
                        },
                        onNavigateToSurahs = {
                            currentTab = NavigationTab.SURAHS
                        }
                    )
                }

                NavigationTab.RECITERS -> {
                    RecitersScreen(
                        currentReciter = playerState.currentReciter,
                        onSelectReciter = { reciter ->
                            viewModel.selectReciter(reciter)
                        }
                    )
                }

                NavigationTab.SAVED -> {
                    FavoritesAndHistoryScreen(
                        favorites = favorites,
                        bookmarkedAyahs = bookmarkedAyahs,
                        history = history,
                        playerState = playerState,
                        onSurahSelected = { surah ->
                            viewModel.playSurah(surah)
                            viewModel.openPlayer()
                        },
                        onPlaySurah = { surah ->
                            if (playerState.currentSurah?.number == surah.number) {
                                viewModel.togglePlayPause()
                            } else {
                                viewModel.playSurah(surah)
                            }
                        },
                        onPlayAyahBookmark = { bookmark ->
                            viewModel.playSurah(
                                surah = bookmark.surah,
                                playlist = Surah.ALL_SURAHS,
                                startPositionMs = bookmark.timestampMs
                            )
                            viewModel.openPlayer()
                        },
                        onDeleteAyahBookmark = { id ->
                            viewModel.removeAyahBookmark(id)
                        },
                        onAddAyahBookmark = { sNum, aNum, note ->
                            viewModel.addAyahBookmark(sNum, aNum, note)
                        },
                        onResumeHistory = { item ->
                            viewModel.playSurah(
                                surah = item.surah,
                                playlist = Surah.ALL_SURAHS,
                                startPositionMs = item.lastPositionMs
                            )
                            viewModel.openPlayer()
                        },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onClearHistory = { viewModel.clearAllHistory() }
                    )
                }

                NavigationTab.PLAYLISTS -> {
                    PlaylistsScreen(
                        playlists = playlists,
                        onCreatePlaylist = { name, desc ->
                            viewModel.createPlaylist(name, desc)
                        },
                        onDeletePlaylist = { id ->
                            viewModel.deletePlaylist(id)
                        },
                        getPlaylistItems = { viewModel.getPlaylistItems(it) },
                        onExport = { exportLauncher.launch("quran-collections.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        onPlayPresetList = { surahNumbers ->
                            val playlistSurahs = surahNumbers.mapNotNull { Surah.getByNumber(it) }
                            if (playlistSurahs.isNotEmpty()) {
                                viewModel.playSurah(playlistSurahs.first(), playlistSurahs)
                                viewModel.openPlayer()
                            }
                        }
                    )
                }
            }
        }
    }

    // Full Screen Player Modal Bottom Sheet
    if (isPlayerExpanded && playerState.currentSurah != null) {
        val currentSurahNum = playerState.currentSurah!!.number
        FullScreenPlayerSheet(
            playerState = playerState,
            isFavorite = viewModel.isSurahFavorite(currentSurahNum),
            isDownloaded = viewModel.isSurahDownloaded(currentSurahNum, playerState.currentReciter.id),
            downloadProgress = viewModel.getDownloadProgress(currentSurahNum, playerState.currentReciter.id),
            onDownloadClick = { viewModel.downloadSurah(playerState.currentSurah!!, playerState.currentReciter) },
            onDismiss = { viewModel.collapsePlayer() },
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onSeek = { viewModel.seekTo(it) },
            onNext = { viewModel.playNext() },
            onPrevious = { viewModel.playPrevious() },
            onRewind10 = { viewModel.rewind10Seconds() },
            onForward10 = { viewModel.forward10Seconds() },
            onToggleRepeat = { viewModel.toggleRepeatMode() },
            onFavoriteToggle = { viewModel.toggleFavorite(currentSurahNum) },
            onBookmarkAyahClick = {
                viewModel.openAddAyahBookmarkDialog(playerState.currentSurah!!, playerState.currentPositionMs)
            },
            onOpenReciterPicker = { viewModel.showReciterPickerSheet(true) },
            onOpenSpeedPicker = { viewModel.showSpeedPickerSheet(true) },
            onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
            onOpenSleepTimerPicker = { viewModel.showSleepTimerSheet(true) }
        )
    }

    // Reciter Picker Sheet
    if (showReciterPicker) {
        ReciterPickerSheet(
            selectedReciter = playerState.currentReciter,
            onSelectReciter = { reciter ->
                viewModel.selectReciter(reciter)
            },
            onDismiss = { viewModel.showReciterPickerSheet(false) }
        )
    }

    // Sleep Timer Sheet
    if (showSleepTimerPicker) {
        SleepTimerSheet(
            playerState = playerState,
            onSetDurationSeconds = { seconds, fadeOut ->
                viewModel.setSleepTimerDuration(seconds, fadeOut)
            },
            onSetEndOfSurah = {
                viewModel.setSleepTimerEndOfSurah(true)
            },
            onAddMinutes = { mins ->
                viewModel.addSleepTimerMinutes(mins)
            },
            onCancelTimer = {
                viewModel.cancelSleepTimer()
            },
            onDismiss = { viewModel.showSleepTimerSheet(false) }
        )
    }

    // Playback Speed Dialog
    if (showSpeedPicker) {
        PlaybackSpeedDialog(
            currentSpeed = playerState.playbackSpeed,
            onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { viewModel.showSpeedPickerSheet(false) }
        )
    }

    // Add to Playlist Dialog
    surahForPlaylist?.let { targetSurah ->
        AddToPlaylistDialog(
            surah = targetSurah,
            playlists = playlists,
            onAddToPlaylist = { pId, sNum ->
                viewModel.addSurahToPlaylist(pId, sNum)
            },
            onCreatePlaylist = { name, desc ->
                viewModel.createPlaylist(name, desc)
            },
            onDismiss = { viewModel.openAddToPlaylist(null) }
        )
    }

    // Add Ayah Bookmark Dialog
    showAyahBookmarkDialog?.let { (surah, timestampMs) ->
        AddAyahBookmarkDialog(
            surah = surah,
            initialTimestampMs = timestampMs,
            onSaveBookmark = { ayahNum, note, ts ->
                viewModel.addAyahBookmark(surah.number, ayahNum, note, ts)
            },
            onDismiss = { viewModel.closeAddAyahBookmarkDialog() }
        )
    }
}
