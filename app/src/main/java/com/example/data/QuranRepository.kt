package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.dao.QuranDao
import com.example.data.entity.FavoriteSurahEntity
import com.example.data.entity.BookmarkedAyahEntity
import com.example.data.entity.PlayHistoryEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import com.example.downloader.QuranAudioDownloader
import com.example.model.AyahBookmark
import com.example.model.Reciter
import com.example.model.Surah
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.io.File

data class HistoryItem(
    val surah: Surah,
    val reciter: Reciter,
    val lastPositionMs: Long,
    val durationMs: Long,
    val playedAt: Long
)

data class DownloadedSurahItem(
    val surah: Surah,
    val reciter: Reciter,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long
) {
    val formattedFileSize: String
        get() {
            val mb = fileSizeBytes / (1024f * 1024f)
            return "%.1f MB".format(mb)
        }
}

class QuranRepository(
    private val dao: QuranDao,
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("quran_audio_prefs", Context.MODE_PRIVATE)

    val downloader = QuranAudioDownloader(context, dao)

    // Reciter preferences
    fun getSavedReciterId(): String {
        return prefs.getString("selected_reciter_id", Reciter.DEFAULT_RECITER.id)
            ?: Reciter.DEFAULT_RECITER.id
    }

    fun saveSelectedReciterId(reciterId: String) {
        prefs.edit().putString("selected_reciter_id", reciterId).apply()
    }

    // Favorites
    val favoriteSurahs: Flow<List<Surah>> = dao.getAllFavorites().map { list ->
        list.mapNotNull { fav -> Surah.getByNumber(fav.surahNumber) }
    }

    fun isFavorite(surahNumber: Int): Flow<Boolean> = dao.isFavorite(surahNumber)

    suspend fun toggleFavorite(surahNumber: Int, isCurrentFav: Boolean) {
        if (isCurrentFav) {
            dao.removeFavorite(surahNumber)
        } else {
            dao.addFavorite(FavoriteSurahEntity(surahNumber))
        }
    }

    // History
    val recentHistory: Flow<List<HistoryItem>> = dao.getRecentHistory().map { list ->
        list.mapNotNull { item ->
            val surah = Surah.getByNumber(item.surahNumber) ?: return@mapNotNull null
            val reciter = Reciter.getById(item.reciterId)
            HistoryItem(
                surah = surah,
                reciter = reciter,
                lastPositionMs = item.lastPositionMs,
                durationMs = item.durationMs,
                playedAt = item.playedAt
            )
        }
    }

    suspend fun recordHistory(
        surahNumber: Int,
        reciterId: String,
        lastPositionMs: Long,
        durationMs: Long,
        completed: Boolean = false
    ) {
        dao.recordHistory(
            PlayHistoryEntity(
                surahNumber = surahNumber,
                reciterId = reciterId,
                lastPositionMs = lastPositionMs,
                durationMs = durationMs,
                completed = completed
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    // Playlists
    val playlists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    fun getSurahsForPlaylist(playlistId: Long): Flow<List<Surah>> {
        return dao.getItemsForPlaylist(playlistId).map { items ->
            items.mapNotNull { Surah.getByNumber(it.surahNumber) }
        }
    }

    suspend fun createPlaylist(name: String, description: String): Long {
        return dao.insertPlaylist(
            PlaylistEntity(
                name = name,
                description = description,
                isSystemPreset = false
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSurahToPlaylist(playlistId: Long, surahNumber: Int, currentCount: Int) {
        dao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                surahNumber = surahNumber,
                orderIndex = currentCount
            )
        )
    }

    suspend fun removeSurahFromPlaylist(playlistId: Long, surahNumber: Int) {
        dao.removePlaylistItem(playlistId, surahNumber)
    }

    // Downloads
    val allDownloads: Flow<List<DownloadedSurahItem>> = dao.getAllDownloads().map { list ->
        list.mapNotNull { record ->
            val surah = Surah.getByNumber(record.surahNumber) ?: return@mapNotNull null
            val reciter = Reciter.getById(record.reciterId)
            DownloadedSurahItem(
                surah = surah,
                reciter = reciter,
                localFilePath = record.localFilePath,
                fileSizeBytes = record.fileSizeBytes,
                downloadedAt = record.downloadedAt
            )
        }
    }

    fun isDownloaded(surahNumber: Int, reciterId: String): Flow<Boolean> =
        dao.isDownloaded(surahNumber, reciterId)

    val downloadProgressMap: StateFlow<Map<String, Int>> = downloader.downloadProgressMap

    fun startDownload(surah: Surah, reciter: Reciter) {
        downloader.startDownload(surah, reciter)
    }

    fun cancelDownload(surahNumber: Int, reciterId: String) {
        downloader.cancelDownload(surahNumber, reciterId)
    }

    suspend fun deleteDownload(surahNumber: Int, reciterId: String) {
        downloader.deleteDownload(surahNumber, reciterId)
    }

    suspend fun deleteAllDownloads() {
        downloader.deleteAllDownloads()
    }

    fun getLocalAudioFile(surahNumber: Int, reciterId: String): File? {
        return downloader.getLocalDownloadedFile(surahNumber, reciterId)
    }

    // Ayah Bookmarks
    val bookmarkedAyahs: Flow<List<AyahBookmark>> = dao.getAllBookmarkedAyahs().map { list ->
        list.mapNotNull { item ->
            val surah = Surah.getByNumber(item.surahNumber) ?: return@mapNotNull null
            AyahBookmark(
                id = item.id,
                surah = surah,
                ayahNumber = item.ayahNumber,
                note = item.note,
                timestampMs = item.timestampMs,
                bookmarkedAt = item.bookmarkedAt
            )
        }
    }

    fun isAyahBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean> =
        dao.isAyahBookmarked(surahNumber, ayahNumber)

    suspend fun addAyahBookmark(surahNumber: Int, ayahNumber: Int, note: String = "", timestampMs: Long = 0L): Long {
        return dao.insertAyahBookmark(
            BookmarkedAyahEntity(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                note = note,
                timestampMs = timestampMs
            )
        )
    }

    suspend fun removeAyahBookmark(id: Long) {
        dao.deleteAyahBookmark(id)
    }

    suspend fun removeAyahBookmarkBySurahAndAyah(surahNumber: Int, ayahNumber: Int) {
        dao.deleteAyahBookmarkBySurahAndAyah(surahNumber, ayahNumber)
    }
}
