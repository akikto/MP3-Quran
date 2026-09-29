package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.BookmarkedAyahEntity
import com.example.data.entity.DownloadedSurahEntity
import com.example.data.entity.FavoriteSurahEntity
import com.example.data.entity.PlayHistoryEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteSurahEntity>>

    @Query("SELECT * FROM favorites")
    suspend fun snapshotFavorites(): List<FavoriteSurahEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importFavorite(favorite: FavoriteSurahEntity): Long

    @Query("UPDATE favorites SET addedAt = :addedAt, isStarter = 0 WHERE surahNumber = :surahNumber AND isStarter = 1")
    suspend fun replaceStarterFavorite(surahNumber: Int, addedAt: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE surahNumber = :surahNumber)")
    fun isFavorite(surahNumber: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteSurahEntity)

    @Query("DELETE FROM favorites WHERE surahNumber = :surahNumber")
    suspend fun removeFavorite(surahNumber: Int)

    // History
    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordHistory(history: PlayHistoryEntity)

    @Query("SELECT * FROM play_history WHERE surahNumber = :surahNumber LIMIT 1")
    suspend fun getHistoryForSurah(surahNumber: Int): PlayHistoryEntity?

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY isSystemPreset DESC, createdAt ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isSystemPreset = 0")
    suspend fun snapshotCustomPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC, surahNumber ASC")
    suspend fun snapshotPlaylistItems(playlistId: Long): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importPlaylistItem(item: PlaylistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId AND isSystemPreset = 0")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getItemsForPlaylist(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND surahNumber = :surahNumber")
    suspend fun removePlaylistItem(playlistId: Long, surahNumber: Int)

    // Downloads
    @Query("SELECT * FROM downloaded_surahs ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadedSurahEntity>>

    @Query("SELECT * FROM downloaded_surahs WHERE reciterId = :reciterId ORDER BY surahNumber ASC")
    fun getDownloadsForReciter(reciterId: String): Flow<List<DownloadedSurahEntity>>

    @Query("SELECT * FROM downloaded_surahs WHERE surahNumber = :surahNumber AND reciterId = :reciterId LIMIT 1")
    suspend fun getDownload(surahNumber: Int, reciterId: String): DownloadedSurahEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_surahs WHERE surahNumber = :surahNumber AND reciterId = :reciterId)")
    fun isDownloaded(surahNumber: Int, reciterId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadedSurahEntity)

    @Query("DELETE FROM downloaded_surahs WHERE surahNumber = :surahNumber AND reciterId = :reciterId")
    suspend fun deleteDownload(surahNumber: Int, reciterId: String)

    @Query("DELETE FROM downloaded_surahs")
    suspend fun clearAllDownloads()

    // Ayah Bookmarks
    @Query("SELECT * FROM bookmarked_ayahs ORDER BY bookmarkedAt DESC")
    fun getAllBookmarkedAyahs(): Flow<List<BookmarkedAyahEntity>>

    @Query("SELECT * FROM bookmarked_ayahs")
    suspend fun snapshotBookmarks(): List<BookmarkedAyahEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importBookmark(bookmark: BookmarkedAyahEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_ayahs WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber)")
    fun isAyahBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahBookmark(bookmark: BookmarkedAyahEntity): Long

    @Query("DELETE FROM bookmarked_ayahs WHERE id = :id")
    suspend fun deleteAyahBookmark(id: Long)

    @Query("DELETE FROM bookmarked_ayahs WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun deleteAyahBookmarkBySurahAndAyah(surahNumber: Int, ayahNumber: Int)
}
