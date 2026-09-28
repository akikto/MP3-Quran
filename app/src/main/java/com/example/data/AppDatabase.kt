package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.QuranDao
import com.example.data.entity.BookmarkedAyahEntity
import com.example.data.entity.DownloadedSurahEntity
import com.example.data.entity.FavoriteSurahEntity
import com.example.data.entity.PlayHistoryEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FavoriteSurahEntity::class,
        PlayHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        DownloadedSurahEntity::class,
        BookmarkedAyahEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quran_audio_database.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed system playlists & initial favorite bookmarks on initial create
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).quranDao()
                            seedDefaultPlaylists(dao)
                            seedDefaultBookmarks(dao)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultBookmarks(dao: QuranDao) {
            // Seed favorite Surahs: Al-Fatihah, Al-Kahf, Ar-Rahman, Al-Mulk
            dao.addFavorite(FavoriteSurahEntity(1))
            dao.addFavorite(FavoriteSurahEntity(18))
            dao.addFavorite(FavoriteSurahEntity(55))
            dao.addFavorite(FavoriteSurahEntity(67))

            // Seed famous Ayah bookmarks
            dao.insertAyahBookmark(
                BookmarkedAyahEntity(
                    surahNumber = 2,
                    ayahNumber = 255,
                    note = "Ayat al-Kursi (The Greatest Verse)",
                    timestampMs = 0L
                )
            )
            dao.insertAyahBookmark(
                BookmarkedAyahEntity(
                    surahNumber = 2,
                    ayahNumber = 286,
                    note = "Last verse of Surah Al-Baqarah",
                    timestampMs = 0L
                )
            )
            dao.insertAyahBookmark(
                BookmarkedAyahEntity(
                    surahNumber = 20,
                    ayahNumber = 25,
                    note = "Rabbi-shrah li sadri (Dua for ease and speech)",
                    timestampMs = 0L
                )
            )
        }

        private suspend fun seedDefaultPlaylists(dao: QuranDao) {
            // Preset 1: Friday Special
            val p1Id = dao.insertPlaylist(
                PlaylistEntity(
                    id = 1,
                    name = "Friday Sunnah",
                    description = "Surah Al-Kahf recommended for Friday recitation and listening.",
                    isSystemPreset = true
                )
            )
            dao.insertPlaylistItem(PlaylistItemEntity(p1Id, 18, 0)) // Al-Kahf
            dao.insertPlaylistItem(PlaylistItemEntity(p1Id, 62, 1)) // Al-Jumu'ah

            // Preset 2: Heart of Quran & Mercy
            val p2Id = dao.insertPlaylist(
                PlaylistEntity(
                    id = 2,
                    name = "Serenity & Mercy",
                    description = "Soothing Surahs for inner peace, healing, and contemplation.",
                    isSystemPreset = true
                )
            )
            dao.insertPlaylistItem(PlaylistItemEntity(p2Id, 36, 0)) // Ya-Sin
            dao.insertPlaylistItem(PlaylistItemEntity(p2Id, 55, 1)) // Ar-Rahman
            dao.insertPlaylistItem(PlaylistItemEntity(p2Id, 56, 2)) // Al-Waqi'ah
            dao.insertPlaylistItem(PlaylistItemEntity(p2Id, 67, 3)) // Al-Mulk

            // Preset 3: Protection & Last 10 Surahs
            val p3Id = dao.insertPlaylist(
                PlaylistEntity(
                    id = 3,
                    name = "Protection & Short Surahs",
                    description = "Surah Al-Ikhlas, Al-Falaq, An-Nas, and short Juz Amma gems.",
                    isSystemPreset = true
                )
            )
            val shortSurahs = listOf(1, 108, 109, 110, 111, 112, 113, 114)
            shortSurahs.forEachIndexed { index, surahNum ->
                dao.insertPlaylistItem(PlaylistItemEntity(p3Id, surahNum, index))
            }
        }
    }
}
