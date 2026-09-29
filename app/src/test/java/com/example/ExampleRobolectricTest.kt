package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Reciter
import com.example.model.Surah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals(if (BuildConfig.DEBUG) "MP3 Quran Preview" else "MP3 Quran", appName)
    }

    @Test
    fun `verify surah catalogue contains 114 surahs`() {
        assertEquals(114, Surah.ALL_SURAHS.size)
        val fatihah = Surah.getByNumber(1)
        assertNotNull(fatihah)
        assertEquals("Al-Fatihah", fatihah?.nameEnglish)
        assertEquals("001", fatihah?.formattedNumber)

        val nas = Surah.getByNumber(114)
        assertNotNull(nas)
        assertEquals("An-Nas", nas?.nameEnglish)
        assertEquals("114", nas?.formattedNumber)
    }

    @Test
    fun `verify reciter audio url generation`() {
        val alafasy = Reciter.getById("afs")
        assertEquals("Mishary Rashid Alafasy", alafasy.nameEnglish)
        val url = alafasy.getAudioUrl(1)
        assertTrue(url.endsWith("001.mp3"))
        assertTrue(url.startsWith("https://"))
    }

    @Test
    fun `verify sleep timer formatting and state`() {
        val stateWithTime = com.example.player.PlayerState(
            sleepTimerSecondsLeft = 90
        )
        assertTrue(stateWithTime.isSleepTimerActive)
        assertEquals("01:30", stateWithTime.formattedSleepTimer)
        assertEquals(2, stateWithTime.sleepTimerMinutesLeft)

        val stateEndOfSurah = com.example.player.PlayerState(
            sleepTimerEndOfSurah = true
        )
        assertTrue(stateEndOfSurah.isSleepTimerActive)
        assertEquals("End of Surah", stateEndOfSurah.formattedSleepTimer)

        val inactiveState = com.example.player.PlayerState()
        org.junit.Assert.assertFalse(inactiveState.isSleepTimerActive)
        assertEquals("", inactiveState.formattedSleepTimer)
    }

    @Test
    fun `verify sleep timer manager configuration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.player.AudioPlayerManager.getInstance(context)

        manager.setSleepTimerDuration(600, fadeOut = true)
        assertTrue(manager.playerState.value.isSleepTimerActive)
        assertEquals(600, manager.playerState.value.sleepTimerSecondsLeft)
        assertTrue(manager.playerState.value.sleepTimerFadeOut)

        manager.addSleepTimerSeconds(300)
        assertEquals(900, manager.playerState.value.sleepTimerSecondsLeft)

        manager.setSleepTimerEndOfSurah(true)
        assertTrue(manager.playerState.value.sleepTimerEndOfSurah)
        assertEquals(0, manager.playerState.value.sleepTimerSecondsLeft)

        manager.cancelSleepTimer()
        org.junit.Assert.assertFalse(manager.playerState.value.isSleepTimerActive)
    }

    @Test
    fun `verify download entity and room operations`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val dao = db.quranDao()

        val download = com.example.data.entity.DownloadedSurahEntity(
            surahNumber = 1,
            reciterId = "afs",
            localFilePath = "/data/user/0/com.example/files/downloads/afs/001.mp3",
            fileSizeBytes = 1048576L // 1 MB
        )
        dao.insertDownload(download)

        val retrieved = dao.getDownload(1, "afs")
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.surahNumber)
        assertEquals("afs", retrieved?.reciterId)
        assertEquals(1048576L, retrieved?.fileSizeBytes)

        dao.deleteDownload(1, "afs")
        val afterDelete = dao.getDownload(1, "afs")
        org.junit.Assert.assertNull(afterDelete)
    }

    @Test
    fun `verify playback speed toggle and adjustment`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.player.AudioPlayerManager.getInstance(context)

        manager.setPlaybackSpeed(1.0f)
        assertEquals(1.0f, manager.playerState.value.playbackSpeed, 0.01f)

        // Toggle from 1.0 -> 1.25 -> 1.5 -> 2.0 -> 0.75 -> 1.0
        val s1 = manager.togglePlaybackSpeed()
        assertEquals(1.25f, s1, 0.01f)
        assertEquals(1.25f, manager.playerState.value.playbackSpeed, 0.01f)

        val s2 = manager.togglePlaybackSpeed()
        assertEquals(1.5f, s2, 0.01f)

        val s3 = manager.togglePlaybackSpeed()
        assertEquals(2.0f, s3, 0.01f)

        val s4 = manager.togglePlaybackSpeed()
        assertEquals(0.75f, s4, 0.01f)

        val s5 = manager.togglePlaybackSpeed()
        assertEquals(1.0f, s5, 0.01f)
    }

    @Test
    fun `verify ayah bookmark room operations`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val dao = db.quranDao()

        val bookmark = com.example.data.entity.BookmarkedAyahEntity(
            surahNumber = 2,
            ayahNumber = 255,
            note = "Ayat al-Kursi",
            timestampMs = 15000L
        )
        val insertedId = dao.insertAyahBookmark(bookmark)
        assertTrue(insertedId > 0)

        val flow = dao.getAllBookmarkedAyahs()
        val list = flow.first()
        assertTrue(list.any { it.surahNumber == 2 && it.ayahNumber == 255 })

        dao.deleteAyahBookmark(insertedId)
        val afterDelete = dao.getAllBookmarkedAyahs().first()
        org.junit.Assert.assertFalse(afterDelete.any { it.id == insertedId })
    }
}
