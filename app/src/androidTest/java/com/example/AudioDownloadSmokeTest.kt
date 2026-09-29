package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.AppDatabase
import com.example.downloader.QuranAudioDownloader
import com.example.model.Reciter
import com.example.model.Surah
import com.example.player.AudioPlayerManager
import com.example.player.PlayerStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Device-only smoke test. Uses the live MP3 source, so run it on an emulator or device
 * with internet access: :app:connectedDebugAndroidTest.
 */
@RunWith(AndroidJUnit4::class)
class AudioDownloadSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val player = AudioPlayerManager.getInstance(context)
    private val surah = requireNotNull(Surah.getByNumber(112))
    private val reciter = Reciter.DEFAULT_RECITER

    @After
    fun cleanUp() {
        instrumentation.runOnMainSync {
            player.stop()
            player.localAudioFileResolver = null
        }
    }

    @Test
    fun streamDownloadAndPlaySavedAudio() {
        val dao = AppDatabase.getInstance(context).quranDao()
        val downloader = QuranAudioDownloader(context, dao)
        runBlocking { downloader.deleteDownload(surah.number, reciter.id) }
        val completed = CountDownLatch(1)

        try {
            // First play the actual online stream, not a pre-existing local copy.
            instrumentation.runOnMainSync {
                player.localAudioFileResolver = null
                player.playSurah(surah, reciter, listOf(surah))
            }
            await("online playback") {
                player.playerState.value.status == PlayerStatus.PLAYING &&
                    player.playerState.value.currentPositionMs > 0
            }
            assertFalse(player.playerState.value.isPlayingFromOfflineCache)
            instrumentation.runOnMainSync { player.stop() }

            downloader.startDownload(surah, reciter) { completed.countDown() }
            assertTrue(
                "Download did not finish: ${downloader.getDownloadProgress(surah.number, reciter.id)}",
                completed.await(90, TimeUnit.SECONDS)
            )
            val downloaded = downloader.getLocalDownloadedFile(surah.number, reciter.id)
            assertNotNull("Downloaded MP3 file missing or empty", downloaded)
            assertTrue(downloaded!!.length() > 0)
            assertNotNull(runBlocking { dao.getDownload(surah.number, reciter.id) })

            // An unreachable URL proves that playback cannot silently fall back to the network.
            val offlineReciter = reciter.copy(serverUrl = "https://127.0.0.1:1/")
            instrumentation.runOnMainSync {
                player.localAudioFileResolver = { number, id ->
                    downloader.getLocalDownloadedFile(number, id)
                }
                player.playSurah(surah, offlineReciter, listOf(surah))
            }
            await("offline playback") {
                player.playerState.value.status == PlayerStatus.PLAYING &&
                    player.playerState.value.currentPositionMs > 0
            }
            assertEquals(surah.number, player.playerState.value.currentSurah?.number)
            assertTrue(player.playerState.value.isPlayingFromOfflineCache)
        } finally {
            runBlocking { downloader.deleteDownload(surah.number, reciter.id) }
        }
    }

    private fun await(description: String, ready: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
        while (System.nanoTime() < deadline) {
            val state = player.playerState.value
            if (state.status == PlayerStatus.ERROR) {
                throw AssertionError("$description failed: ${state.errorMessage}")
            }
            if (ready()) return
            Thread.sleep(250)
        }
        throw AssertionError("$description timed out: ${player.playerState.value}")
    }
}