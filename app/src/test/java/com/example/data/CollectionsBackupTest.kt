package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.BookmarkedAyahEntity
import com.example.data.entity.FavoriteSurahEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class CollectionsBackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databases = mutableListOf<AppDatabase>()

    private fun database(): AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .build().also { databases.add(it) }

    private fun freshDatabase(vararg callbacks: RoomDatabase.Callback): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .apply { callbacks.forEach { addCallback(it) } }
            .addCallback(AppDatabase.STARTER_COLLECTIONS_CALLBACK)
            .build().also { databases.add(it) }

    @After fun close() = databases.forEach { it.close() }

    @Test fun freshDatabaseSeedsBeforeImmediateImport() = runBlocking {
        val db = freshDatabase()
        val json = """{"version":1,"favorites":[{"surahNumber":18,"addedAt":123},{"surahNumber":42,"addedAt":456}],"bookmarks":[{"surahNumber":18,"ayahNumber":10,"note":"Saved","timestampMs":50,"bookmarkedAt":789}],"playlists":[{"name":"Personal","description":"Restored","createdAt":123,"items":[18,67]}]}"""
        CollectionsBackup(db).import(ByteArrayInputStream(json.toByteArray()))
        val dao = db.quranDao()
        assertEquals(4, dao.snapshotFavorites().count { it.surahNumber in listOf(1, 18, 55, 67) })
        assertEquals(1, dao.snapshotFavorites().count { it.surahNumber == 18 })
        assertEquals(456L, dao.snapshotFavorites().single { it.surahNumber == 42 }.addedAt)
        assertEquals(4, dao.snapshotBookmarks().size)
        assertEquals("Saved", dao.snapshotBookmarks().single { it.surahNumber == 18 }.note)
        val custom = dao.snapshotCustomPlaylists().single()
        assertTrue(custom.id > 3)
        assertEquals(listOf(18, 67), dao.snapshotPlaylistItems(custom.id).map { it.surahNumber })
        assertEquals(2, dao.snapshotPlaylistItems(1).size)
        assertEquals(3, dao.getAllPlaylists().first().count { it.isSystemPreset })
    }

    @Test fun starterInsertsDoNotOverwriteExistingRowsOrAttachItemsToCustomPlaylist() = runBlocking {
        // Simulate rows already present when the creation callback runs. The production callback
        // must not replace them, even though it normally runs before anyone can write to Room.
        val db = freshDatabase(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO playlists VALUES (1, 'Mine', 'Keep', 0, 101)")
                db.execSQL("INSERT INTO playlist_items VALUES (1, 42, 0)")
                db.execSQL("INSERT INTO favorites VALUES (18, 222)")
                db.execSQL("INSERT INTO bookmarked_ayahs VALUES (1, 2, 255, 'My note', 99, 333)")
            }
        })
        val dao = db.quranDao()
        assertEquals("Mine", dao.snapshotCustomPlaylists().single().name)
        assertEquals(listOf(42), dao.snapshotPlaylistItems(1).map { it.surahNumber })
        assertEquals(222L, dao.snapshotFavorites().single { it.surahNumber == 18 }.addedAt)
        assertEquals("My note", dao.snapshotBookmarks().single { it.ayahNumber == 255 }.note)
        assertEquals(3, dao.snapshotBookmarks().size)
    }

    @Test fun roundTripPreservesCollectionsAndRepeatedImportDoesNotDuplicate() = runBlocking {
        val source = database()
        val sourceDao = source.quranDao()
        sourceDao.addFavorite(FavoriteSurahEntity(18, 123))
        sourceDao.insertAyahBookmark(BookmarkedAyahEntity(
            surahNumber = 18, ayahNumber = 10, note = "Reflection / العربية",
            timestampMs = 2450, bookmarkedAt = 456
        ))
        val id = sourceDao.insertPlaylist(PlaylistEntity(
            name = "My Friday", description = "Quiet listening", createdAt = 789
        ))
        sourceDao.insertPlaylistItem(PlaylistItemEntity(id, 67, 0))
        sourceDao.insertPlaylistItem(PlaylistItemEntity(id, 18, 1))
        sourceDao.insertPlaylist(PlaylistEntity(
            id = 100, name = "Built-in", description = "Not personal", isSystemPreset = true
        ))
        val output = ByteArrayOutputStream()
        CollectionsBackup(source).export(output)
        val backup = output.toByteArray()

        val target = database()
        val dao = target.quranDao()
        dao.addFavorite(FavoriteSurahEntity(1, 999))
        dao.addFavorite(FavoriteSurahEntity(18, 222))
        dao.insertAyahBookmark(BookmarkedAyahEntity(
            surahNumber = 1, ayahNumber = 1, note = "Other note"
        ))
        dao.insertAyahBookmark(BookmarkedAyahEntity(
            surahNumber = 18, ayahNumber = 10, note = "My different reflection", timestampMs = 2450
        ))
        val otherId = dao.insertPlaylist(PlaylistEntity(name = "Unrelated", description = "Keep"))
        val restore = CollectionsBackup(target)
        repeat(2) { restore.import(ByteArrayInputStream(backup)) }

        assertEquals(setOf(1, 18), dao.snapshotFavorites().map { it.surahNumber }.toSet())
        assertEquals(222L, dao.snapshotFavorites().single { it.surahNumber == 18 }.addedAt)
        assertEquals(3, dao.snapshotBookmarks().size)
        assertEquals("Reflection / العربية", dao.snapshotBookmarks().single { it.note == "Reflection / العربية" }.note)
        assertEquals(2450L, dao.snapshotBookmarks().single { it.note == "Reflection / العربية" }.timestampMs)
        assertEquals(2, dao.snapshotCustomPlaylists().size)
        assertEquals("Keep", dao.snapshotCustomPlaylists().single { it.id == otherId }.description)
        val restored = dao.snapshotCustomPlaylists().single { it.name == "My Friday" }
        assertEquals("Quiet listening", restored.description)
        assertEquals(789L, restored.createdAt)
        assertEquals(listOf(67, 18), dao.snapshotPlaylistItems(restored.id).map { it.surahNumber })
    }

    @Test fun importMergesItemsWithoutReorderingExistingPlaylist() = runBlocking {
        val db = database()
        val dao = db.quranDao()
        val id = dao.insertPlaylist(PlaylistEntity(name = "Study", description = "Notes"))
        dao.insertPlaylistItem(PlaylistItemEntity(id, 55, 0))
        val json = """{"version":1,"favorites":[],"bookmarks":[],"playlists":[{"name":"Study","description":"Notes","createdAt":10,"items":[18,55,67]}]}"""
        CollectionsBackup(db).import(ByteArrayInputStream(json.toByteArray()))
        assertEquals(listOf(55, 18, 67), dao.snapshotPlaylistItems(id).map { it.surahNumber })
        assertEquals(1, dao.snapshotCustomPlaylists().size)
    }

    @Test fun sameNamedPlaylistsRemainDistinctAfterRepeatedRestore() = runBlocking {
        val source = database()
        val dao = source.quranDao()
        // A listener can create two playlists within the same millisecond.
        val first = dao.insertPlaylist(PlaylistEntity(name = "Listen", description = "Today", createdAt = 42))
        val second = dao.insertPlaylist(PlaylistEntity(name = "Listen", description = "Today", createdAt = 42))
        dao.insertPlaylistItem(PlaylistItemEntity(first, 18, 0))
        dao.insertPlaylistItem(PlaylistItemEntity(second, 67, 0))
        dao.insertPlaylistItem(PlaylistItemEntity(second, 55, 1))
        val output = ByteArrayOutputStream()
        CollectionsBackup(source).export(output)

        val target = database()
        val restore = CollectionsBackup(target)
        repeat(2) { restore.import(ByteArrayInputStream(output.toByteArray())) }
        val restored = target.quranDao().snapshotCustomPlaylists()
        assertEquals(2, restored.size)
        assertEquals(listOf(listOf(18), listOf(67, 55)),
            restored.map { target.quranDao().snapshotPlaylistItems(it.id).map { item -> item.surahNumber } })
    }

    @Test fun malformedOrUnsupportedFileNeverPartiallyImports() = runBlocking {
        val db = database()
        val backup = CollectionsBackup(db)
        val badFiles = listOf(
            "not json",
            """{"version":2,"favorites":[],"bookmarks":[],"playlists":[]}""",
            """{"version":1,"favorites":[{"surahNumber":18,"addedAt":1}],"bookmarks":[],"playlists":[{"name":"Bad","description":"","createdAt":1,"items":[999]}]}""",
            """{"version":1,"favorites":[],"bookmarks":[],"playlists":[{"name":"Bad","description":"","createdAt":1,"items":[18,18]}]}""",
            """{"version":1,"favorites":[{"surahNumber":"18","addedAt":1}],"bookmarks":[],"playlists":[]}"""
        )
        for (file in badFiles) {
            try {
                backup.import(ByteArrayInputStream(file.toByteArray()))
                fail("Expected rejection for $file")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().isNotBlank())
            }
        }
        assertTrue(db.quranDao().snapshotFavorites().isEmpty())
        assertTrue(db.quranDao().snapshotCustomPlaylists().isEmpty())
        assertTrue(db.quranDao().snapshotBookmarks().isEmpty())
    }
}