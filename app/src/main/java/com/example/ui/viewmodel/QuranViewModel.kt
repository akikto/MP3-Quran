package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DownloadedSurahItem
import com.example.data.HistoryItem
import com.example.data.QuranRepository
import com.example.data.entity.PlaylistEntity
import com.example.model.AyahBookmark
import com.example.model.Reciter
import com.example.model.Surah
import com.example.player.AudioPlayerManager
import com.example.player.PlayerState
import com.example.player.PlayerStatus
import com.example.player.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SurahFilter(val label: String) {
    ALL("All 114"),
    MECCAN("Meccan (86)"),
    MEDINAN("Medinan (28)"),
    POPULAR("Popular"),
    FAVORITES("Bookmarked")
}

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = QuranRepository(database.quranDao(), application)
    private val playerManager = AudioPlayerManager.getInstance(application)

    val playerState: StateFlow<PlayerState> = playerManager.playerState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SurahFilter.ALL)
    val selectedFilter: StateFlow<SurahFilter> = _selectedFilter.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _showReciterPicker = MutableStateFlow(false)
    val showReciterPicker: StateFlow<Boolean> = _showReciterPicker.asStateFlow()

    private val _showSleepTimerPicker = MutableStateFlow(false)
    val showSleepTimerPicker: StateFlow<Boolean> = _showSleepTimerPicker.asStateFlow()

    private val _showSpeedPicker = MutableStateFlow(false)
    val showSpeedPicker: StateFlow<Boolean> = _showSpeedPicker.asStateFlow()

    private val _showPlaylistDialog = MutableStateFlow<Surah?>(null)
    val showPlaylistDialog: StateFlow<Surah?> = _showPlaylistDialog.asStateFlow()

    val favorites: StateFlow<List<Surah>> = repository.favoriteSurahs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedAyahs: StateFlow<List<AyahBookmark>> = repository.bookmarkedAyahs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAyahBookmarkDialog = MutableStateFlow<Pair<Surah, Long>?>(null)
    val showAyahBookmarkDialog: StateFlow<Pair<Surah, Long>?> = _showAyahBookmarkDialog.asStateFlow()

    val downloads: StateFlow<List<DownloadedSurahItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgressMap: StateFlow<Map<String, Int>> = repository.downloadProgressMap

    val currentReciter: StateFlow<Reciter> = MutableStateFlow(
        Reciter.getById(repository.getSavedReciterId())
    )

    val filteredSurahs: StateFlow<List<Surah>> = combine(
        _searchQuery,
        _selectedFilter,
        favorites
    ) { query, filter, favs ->
        val baseList = when (filter) {
            SurahFilter.ALL -> Surah.ALL_SURAHS
            SurahFilter.MECCAN -> Surah.ALL_SURAHS.filter { it.revelationType == Surah.RevelationType.MECCAN }
            SurahFilter.MEDINAN -> Surah.ALL_SURAHS.filter { it.revelationType == Surah.RevelationType.MEDINAN }
            SurahFilter.POPULAR -> Surah.ALL_SURAHS.filter { it.number in Surah.POPULAR_SURAHS }
            SurahFilter.FAVORITES -> favs
        }

        if (query.isBlank()) {
            baseList
        } else {
            val q = query.trim().lowercase()
            baseList.filter { surah ->
                surah.nameEnglish.lowercase().contains(q) ||
                surah.englishTranslation.lowercase().contains(q) ||
                surah.nameArabic.contains(q) ||
                surah.number.toString() == q
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Surah.ALL_SURAHS)

    init {
        // Wire history persistence callback
        playerManager.onPlaybackHistoryUpdate = { surah, reciter, pos, dur, completed ->
            viewModelScope.launch {
                repository.recordHistory(
                    surahNumber = surah.number,
                    reciterId = reciter.id,
                    lastPositionMs = pos,
                    durationMs = dur,
                    completed = completed
                )
            }
        }
        // Set initial reciter from saved pref
        val savedReciter = Reciter.getById(repository.getSavedReciterId())
        playerManager.setReciter(savedReciter)

        // Wire offline cached audio resolver
        playerManager.localAudioFileResolver = { surahNum, reciterId ->
            repository.getLocalAudioFile(surahNum, reciterId)
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: SurahFilter) {
        _selectedFilter.value = filter
    }

    fun playSurah(surah: Surah, playlist: List<Surah> = Surah.ALL_SURAHS, startPositionMs: Long = 0L) {
        playerManager.playSurah(
            surah = surah,
            reciter = playerState.value.currentReciter,
            playlist = playlist,
            startPositionMs = startPositionMs
        )
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun rewind10Seconds() {
        playerManager.rewind10Seconds()
    }

    fun forward10Seconds() {
        playerManager.forward10Seconds()
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
        _showSpeedPicker.value = false
    }

    fun togglePlaybackSpeed() {
        playerManager.togglePlaybackSpeed()
    }

    fun toggleRepeatMode() {
        playerManager.toggleRepeatMode()
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
        _showSleepTimerPicker.value = false
    }

    fun setSleepTimerDuration(seconds: Int, fadeOut: Boolean = true) {
        playerManager.setSleepTimerDuration(seconds, fadeOut)
        _showSleepTimerPicker.value = false
    }

    fun setSleepTimerEndOfSurah(enabled: Boolean) {
        playerManager.setSleepTimerEndOfSurah(enabled)
        _showSleepTimerPicker.value = false
    }

    fun addSleepTimerMinutes(minutes: Int) {
        playerManager.addSleepTimerSeconds(minutes * 60)
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
        _showSleepTimerPicker.value = false
    }

    fun selectReciter(reciter: Reciter) {
        repository.saveSelectedReciterId(reciter.id)
        playerManager.setReciter(reciter)
        _showReciterPicker.value = false
    }

    fun toggleFavorite(surahNumber: Int) {
        viewModelScope.launch {
            val isFav = favorites.value.any { it.number == surahNumber }
            repository.toggleFavorite(surahNumber, isFav)
        }
    }

    fun isSurahFavorite(surahNumber: Int): Boolean {
        return favorites.value.any { it.number == surahNumber }
    }

    fun openPlayer() {
        _isPlayerExpanded.value = true
    }

    fun collapsePlayer() {
        _isPlayerExpanded.value = false
    }

    fun showReciterPickerSheet(show: Boolean) {
        _showReciterPicker.value = show
    }

    fun showSleepTimerSheet(show: Boolean) {
        _showSleepTimerPicker.value = show
    }

    fun showSpeedPickerSheet(show: Boolean) {
        _showSpeedPicker.value = show
    }

    fun openAddToPlaylist(surah: Surah?) {
        _showPlaylistDialog.value = surah
    }

    fun createPlaylist(name: String, description: String) {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSurahToPlaylist(playlistId: Long, surahNumber: Int) {
        viewModelScope.launch {
            repository.addSurahToPlaylist(playlistId, surahNumber, 0)
            _showPlaylistDialog.value = null
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Download Management
    fun downloadSurah(surah: Surah, reciter: Reciter = playerState.value.currentReciter) {
        repository.startDownload(surah, reciter)
    }

    fun cancelDownload(surahNumber: Int, reciterId: String) {
        repository.cancelDownload(surahNumber, reciterId)
    }

    fun deleteDownload(surahNumber: Int, reciterId: String) {
        viewModelScope.launch {
            repository.deleteDownload(surahNumber, reciterId)
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            repository.deleteAllDownloads()
        }
    }

    fun isSurahDownloaded(surahNumber: Int, reciterId: String): Boolean {
        return downloads.value.any { it.surah.number == surahNumber && it.reciter.id == reciterId }
    }

    fun getDownloadProgress(surahNumber: Int, reciterId: String): Int? {
        val key = "$surahNumber-$reciterId"
        return downloadProgressMap.value[key]
    }

    // Ayah Bookmarking
    fun openAddAyahBookmarkDialog(surah: Surah, timestampMs: Long = 0L) {
        _showAyahBookmarkDialog.value = Pair(surah, timestampMs)
    }

    fun closeAddAyahBookmarkDialog() {
        _showAyahBookmarkDialog.value = null
    }

    fun addAyahBookmark(surahNumber: Int, ayahNumber: Int, note: String = "", timestampMs: Long = 0L) {
        viewModelScope.launch {
            repository.addAyahBookmark(surahNumber, ayahNumber, note, timestampMs)
            _showAyahBookmarkDialog.value = null
        }
    }

    fun removeAyahBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeAyahBookmark(id)
        }
    }
}
