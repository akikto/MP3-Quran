package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.QuranDao
import com.example.data.entity.BookmarkedAyahEntity
import com.example.data.entity.DownloadedSurahEntity
import com.example.data.entity.FavoriteSurahEntity
import com.example.data.entity.PlayHistoryEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity

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
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        // Room invokes onCreate while opening the new database. Keep seeding on that connection
        // so no DAO operation (including a restore) can run before the starter rows are committed.
        internal val STARTER_COLLECTIONS_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                val presets = listOf(
                    Triple(1L, "Friday Sunnah", "Surah Al-Kahf recommended for Friday recitation and listening."),
                    Triple(2L, "Serenity & Mercy", "Soothing Surahs for inner peace, healing, and contemplation."),
                    Triple(3L, "Protection & Short Surahs", "Surah Al-Ikhlas, Al-Falaq, An-Nas, and short Juz Amma gems.")
                )
                presets.forEach { (id, name, description) ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO playlists (id, name, description, isSystemPreset, createdAt) VALUES (?, ?, ?, 1, ?)",
                        arrayOf<Any>(id, name, description, now)
                    )
                }
                val items = listOf(
                    1L to listOf(18, 62),
                    2L to listOf(36, 55, 56, 67),
                    3L to listOf(1, 108, 109, 110, 111, 112, 113, 114)
                )
                items.forEach { (id, surahs) ->
                    // If a custom row already owns a preset ID, do not add preset items to it.
                    val isPreset = db.query(
                        "SELECT isSystemPreset FROM playlists WHERE id = ?", arrayOf(id)
                    ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 1 }
                    if (isPreset) surahs.forEachIndexed { index, surah ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO playlist_items (playlistId, surahNumber, orderIndex) VALUES (?, ?, ?)",
                            arrayOf<Any>(id, surah, index)
                        )
                    }
                }
                listOf(1, 18, 55, 67).forEach { surah ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO favorites (surahNumber, addedAt) VALUES (?, ?)",
                        arrayOf<Any>(surah, now)
                    )
                }
                listOf(
                    Triple(2, 255, "Ayat al-Kursi (The Greatest Verse)"),
                    Triple(2, 286, "Last verse of Surah Al-Baqarah"),
                    Triple(20, 25, "Rabbi-shrah li sadri (Dua for ease and speech)")
                ).forEach { (surah, ayah, note) ->
                    db.execSQL(
                        """INSERT OR IGNORE INTO bookmarked_ayahs
                            (surahNumber, ayahNumber, note, timestampMs, bookmarkedAt)
                            SELECT ?, ?, ?, 0, ? WHERE NOT EXISTS
                            (SELECT 1 FROM bookmarked_ayahs WHERE surahNumber = ? AND ayahNumber = ?)""".trimIndent(),
                        arrayOf<Any>(surah, ayah, note, now, surah, ayah)
                    )
                }
            }
        }

        // For the version 2 layout with the five existing tables, add ayah bookmarks.
        // Leave favorites, history, playlists, and downloads untouched.
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `bookmarked_ayahs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `surahNumber` INTEGER NOT NULL,
                        `ayahNumber` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `timestampMs` INTEGER NOT NULL,
                        `bookmarkedAt` INTEGER NOT NULL
                    )""".trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quran_audio_database.db"
                ).addMigrations(MIGRATION_2_3)
                .addCallback(STARTER_COLLECTIONS_CALLBACK).build()
                INSTANCE = instance
                instance
            }
        }

    }
}
