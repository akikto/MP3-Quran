package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @Before
    fun prepare() {
        context.deleteDatabase(name)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @Test
    fun version2To3PreservesSavedDataAndCreatesBookmarks() = runBlocking {
        // A version 2 database containing each category of saved data.
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            old.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`surahNumber` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`surahNumber`))")
            old.execSQL("CREATE TABLE IF NOT EXISTS `play_history` (`surahNumber` INTEGER NOT NULL, `reciterId` TEXT NOT NULL, `lastPositionMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `playedAt` INTEGER NOT NULL, PRIMARY KEY(`surahNumber`))")
            old.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `isSystemPreset` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE IF NOT EXISTS `playlist_items` (`playlistId` INTEGER NOT NULL, `surahNumber` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `surahNumber`))")
            old.execSQL("CREATE TABLE IF NOT EXISTS `downloaded_surahs` (`surahNumber` INTEGER NOT NULL, `reciterId` TEXT NOT NULL, `localFilePath` TEXT NOT NULL, `fileSizeBytes` INTEGER NOT NULL, `downloadedAt` INTEGER NOT NULL, PRIMARY KEY(`surahNumber`, `reciterId`))")
            old.execSQL("INSERT INTO favorites VALUES (18, 100)")
            old.execSQL("INSERT INTO play_history VALUES (18, 'reciter', 1234, 5000, 0, 101)")
            old.execSQL("INSERT INTO playlists VALUES (42, 'My playlist', 'Personal notes', 0, 102)")
            old.execSQL("INSERT INTO playlist_items VALUES (42, 18, 0)")
            old.execSQL("INSERT INTO downloaded_surahs VALUES (18, 'reciter', '/saved/audio.mp3', 4096, 103)")
            old.version = 2
        } finally {
            old.close()
        }

        // Open through Room so its schema validation and the production migration both run.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()
        try {
            val dao = db.quranDao()
            assertEquals(100L, dao.getAllFavorites().first().single().addedAt)
            assertEquals(false, dao.getAllFavorites().first().single().isStarter)
            assertEquals(1234L, dao.getHistoryForSurah(18)?.lastPositionMs)
            assertEquals("My playlist", dao.getAllPlaylists().first().single().name)
            assertEquals("Personal notes", dao.getAllPlaylists().first().single().description)
            assertEquals(18, dao.getItemsForPlaylist(42).first().single().surahNumber)
            assertEquals("/saved/audio.mp3", dao.getDownload(18, "reciter")?.localFilePath)
            assertTrue(dao.getAllBookmarkedAyahs().first().isEmpty())
            val id = dao.insertAyahBookmark(
                com.example.data.entity.BookmarkedAyahEntity(
                    surahNumber = 18, ayahNumber = 10, note = "Keep", timestampMs = 2500
                )
            )
            assertTrue(id > 0)
            assertNotNull(dao.getAllBookmarkedAyahs().first().find { it.id == id })
        } finally {
            db.close()
        }
        // Reopening the upgraded database should keep both the old records and new bookmarks.
        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()
        try {
            assertEquals(42L, reopened.quranDao().getAllPlaylists().first().single().id)
            assertEquals("Keep", reopened.quranDao().getAllBookmarkedAyahs().first().single().note)
        } finally {
            reopened.close()
        }
    }

    @Test
    fun unsupportedVersionDoesNotEraseSavedData() {
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            old.execSQL("CREATE TABLE saved_note (text TEXT NOT NULL)")
            old.execSQL("INSERT INTO saved_note VALUES ('do not delete')")
            old.version = 1
        } finally {
            old.close()
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        try {
            db.openHelper.writableDatabase
            fail("Opening an unsupported schema should fail rather than erase user data")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("migration", ignoreCase = true))
        } finally {
            db.close()
        }

        val saved = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            saved.rawQuery("SELECT text FROM saved_note", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("do not delete", cursor.getString(0))
            }
        } finally {
            saved.close()
        }
    }
}