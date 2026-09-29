package com.example

import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.example.data.AppDatabase
import com.example.data.entity.BookmarkedAyahEntity
import com.example.data.entity.FavoriteSurahEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import com.example.model.Surah
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * Runs against the real system DocumentsUI provider, not a mocked activity result.
 * Run on an emulator/device with a local Downloads document provider.
 */
@RunWith(AndroidJUnit4::class)
class CollectionsDocumentPickerTest {
    @get:org.junit.Rule val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val database by lazy { AppDatabase.getInstance(instrumentation.targetContext) }
    private val dao get() = database.quranDao()

    @Test
    fun exportResetAndImportThroughSystemPicker() {
        // This test clears Room data; never run it against someone's device or saved preview data.
        assumeTrue("Use a disposable emulator: this test resets local collections",
            Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")
        // Use a unique playlist so this works both on a fresh install and a reused device.
        val name = "Picker restore ${System.currentTimeMillis()}"
        val filename = "picker-collections-${System.currentTimeMillis()}.json"
        val note = "Saved from picker"
        runBlocking {
            dao.addFavorite(FavoriteSurahEntity(112))
            dao.insertAyahBookmark(BookmarkedAyahEntity(
                surahNumber = 113, ayahNumber = 2, note = note, timestampMs = 1200
            ))
            dao.insertPlaylist(PlaylistEntity(name = name, description = "Ordered listening")).also {
                dao.insertPlaylistItem(PlaylistItemEntity(it, 114, 0))
                dao.insertPlaylistItem(PlaylistItemEntity(it, 112, 1))
            }
        }
        compose.onNodeWithText("Playlists").performClick()
        compose.onNodeWithText(name).assertExists()

        try {
            compose.onNodeWithTag("export_collections").performClick()
            openDownloads()
            requireNotNull(device.findObject(
                By.res("android:id/title").clazz("android.widget.EditText")
            )) {
                "System save picker has no filename field: ${pickerTree()}"
            }.text = filename
            clickPickerAction("Save", "action_menu_save")
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText("Collections exported", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Room data reset while keeping the exported document in external storage. A
            // real pm clear kills the instrumentation process and cannot be done mid-test.
            database.clearAllTables()
            assertTrue(runBlocking { dao.snapshotFavorites().isEmpty() })
            assertTrue(runBlocking { dao.snapshotBookmarks().isEmpty() })
            assertTrue(runBlocking { dao.snapshotCustomPlaylists().isEmpty() })

            compose.onNodeWithTag("import_collections").performClick()
            openDownloads()
            clickPickerText(filename)
            compose.waitUntil(20_000) {
                runBlocking { dao.snapshotCustomPlaylists().any { it.name == name } }
            }

            runBlocking {
                assertTrue(dao.snapshotFavorites().any { it.surahNumber == 112 })
                val bookmark = dao.snapshotBookmarks().single { it.note == note }
                assertEquals(113, bookmark.surahNumber)
                assertEquals(2, bookmark.ayahNumber)
                assertEquals(note, bookmark.note)
                val playlist = dao.snapshotCustomPlaylists().single { it.name == name }
                assertEquals(name, playlist.name)
                assertEquals("Ordered listening", playlist.description)
                assertEquals(listOf(114, 112),
                    dao.snapshotPlaylistItems(playlist.id).map { it.surahNumber })
            }
            compose.onNodeWithText(name).assertExists()
            compose.onNodeWithText("Ordered listening").assertExists()
            val restoredId = runBlocking { dao.snapshotCustomPlaylists().single { it.name == name }.id }
            compose.onNodeWithTag("playlist_card_$restoredId").performScrollTo()
            // The database order is authoritative; the screen also renders both items.
            compose.onNodeWithText(requireNotNull(Surah.getByNumber(114)).nameEnglish,
                useUnmergedTree = true).assertExists()
            compose.onNodeWithText(requireNotNull(Surah.getByNumber(112)).nameEnglish,
                useUnmergedTree = true).assertExists()
            compose.onNodeWithText("Saved").performClick()
            compose.onNodeWithText("Surahs (", substring = true).assertExists()
            compose.onNodeWithTag("saved_tab_1").performClick()
            compose.onNodeWithText(note).assertExists()
        } finally {
            database.clearAllTables()
            device.executeShellCommand("rm -f /sdcard/Download/$filename")
        }
    }

    @Test
    fun cancellingPickerLeavesCollectionsAlone() {
        compose.onNodeWithText("Playlists").performClick()
        val before = runBlocking { dao.snapshotFavorites() }
        compose.onNodeWithTag("import_collections").performClick()
        device.waitForIdle()
        device.pressBack()
        compose.onNodeWithTag("import_collections").assertExists()
        assertEquals(before, runBlocking { dao.snapshotFavorites() })
        compose.onNodeWithText("Collections restored. Existing saved items were kept.").assertDoesNotExist()
    }

    @Test
    fun unreadableDocumentReportsFailureWithoutChangingCollections() {
        compose.onNodeWithText("Playlists").performClick()
        val before = runBlocking { dao.snapshotFavorites() }
        // An empty JSON file is unreadable as a collections backup.
        val filename = "invalid-collections-${System.currentTimeMillis()}.json"
        val path = "/sdcard/Download/$filename"
        device.executeShellCommand("touch $path")
        assertTrue("Could not create test document", device.executeShellCommand("ls -l $path")
            .contains(filename))
        try {
            compose.onNodeWithTag("import_collections").performClick()
            openDownloads()
            clickPickerText(filename)
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText("Import failed:", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(before, runBlocking { dao.snapshotFavorites() })
            compose.onNodeWithText("Import failed:", substring = true).assertExists()
        } finally {
            device.executeShellCommand("rm -f $path")
        }
    }

    private fun openDownloads() {
        device.waitForIdle()
        // DocumentsUI may reopen in Downloads or in Recents. Select Downloads explicitly.
        if (device.findObject(By.text("Downloads")) == null) {
            (device.findObject(By.desc("Show roots")) ?:
                device.findObject(By.desc("Navigate up")) ?:
                device.findObject(By.res("com.android.documentsui", "drawer_button")))
                ?.click()
        }
        device.waitForIdle()
        device.findObject(By.text("Downloads"))?.click()
        device.waitForIdle()
    }

    private fun clickPickerAction(label: String, resource: String) {
        val button = device.wait(
            androidx.test.uiautomator.Until.findObject(By.text(label.uppercase())), 10_000
        ) ?: device.findObject(By.text(label)) ?:
            device.findObject(By.res("com.android.documentsui", resource))
        requireNotNull(button) { "System document picker did not show $label: ${pickerTree()}" }.click()
    }

    private fun clickPickerText(label: String) {
        val row = device.wait(
            androidx.test.uiautomator.Until.findObject(By.text(label)), 10_000
        )
        requireNotNull(row) { "System document picker did not show $label in Downloads: ${pickerTree()}" }.click()
    }

    private fun pickerTree(): String = ByteArrayOutputStream().use {
        device.dumpWindowHierarchy(it)
        it.toString("UTF-8")
    }
}