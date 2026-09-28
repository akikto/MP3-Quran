package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteSurahEntity(
    @PrimaryKey val surahNumber: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey val surahNumber: Int,
    val reciterId: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val completed: Boolean = false,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val isSystemPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "surahNumber"])
data class PlaylistItemEntity(
    val playlistId: Long,
    val surahNumber: Int,
    val orderIndex: Int
)

@Entity(tableName = "downloaded_surahs", primaryKeys = ["surahNumber", "reciterId"])
data class DownloadedSurahEntity(
    val surahNumber: Int,
    val reciterId: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarked_ayahs")
data class BookmarkedAyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val note: String = "",
    val timestampMs: Long = 0L,
    val bookmarkedAt: Long = System.currentTimeMillis()
)
