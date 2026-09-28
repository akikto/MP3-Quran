package com.example.downloader

import android.content.Context
import com.example.data.dao.QuranDao
import com.example.data.entity.DownloadedSurahEntity
import com.example.model.Reciter
import com.example.model.Surah
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class DownloadProgress(
    val surahNumber: Int,
    val reciterId: String,
    val progressPercent: Int, // 0..100, or -1 for error
    val isDownloading: Boolean = true
)

class QuranAudioDownloader(
    private val context: Context,
    private val dao: QuranDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Key is "$surahNumber-$reciterId"
    private val _downloadProgressMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Int>> = _downloadProgressMap.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()

    private fun getDownloadKey(surahNumber: Int, reciterId: String): String = "$surahNumber-$reciterId"

    fun isCurrentlyDownloading(surahNumber: Int, reciterId: String): Boolean {
        val key = getDownloadKey(surahNumber, reciterId)
        val progress = _downloadProgressMap.value[key] ?: return false
        return progress in 0..99
    }

    fun getDownloadProgress(surahNumber: Int, reciterId: String): Int? {
        val key = getDownloadKey(surahNumber, reciterId)
        return _downloadProgressMap.value[key]
    }

    fun startDownload(surah: Surah, reciter: Reciter, onComplete: (() -> Unit)? = null) {
        val key = getDownloadKey(surah.number, reciter.id)
        if (isCurrentlyDownloading(surah.number, reciter.id)) return

        val job = scope.launch {
            _downloadProgressMap.value = _downloadProgressMap.value + (key to 0)

            val audioUrl = reciter.getAudioUrl(surah.number)
            val reciterDir = File(context.filesDir, "downloads/${reciter.id}")
            if (!reciterDir.exists()) reciterDir.mkdirs()

            val targetFile = File(reciterDir, "${surah.formattedNumber}.mp3")
            val tempFile = File(reciterDir, "${surah.formattedNumber}.mp3.tmp")

            var connection: HttpURLConnection? = null
            try {
                val url = URL(audioUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    connect()
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("Server returned HTTP $responseCode")
                }

                val fileLength = connection.contentLength.toLong()
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastReportedPercent = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (fileLength > 0) {
                        val currentPercent = ((totalBytesRead * 100) / fileLength).toInt().coerceIn(0, 100)
                        if (currentPercent != lastReportedPercent) {
                            lastReportedPercent = currentPercent
                            _downloadProgressMap.value = _downloadProgressMap.value + (key to currentPercent)
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.renameTo(targetFile)) {
                    val finalSize = targetFile.length()
                    dao.insertDownload(
                        DownloadedSurahEntity(
                            surahNumber = surah.number,
                            reciterId = reciter.id,
                            localFilePath = targetFile.absolutePath,
                            fileSizeBytes = finalSize,
                            downloadedAt = System.currentTimeMillis()
                        )
                    )
                    _downloadProgressMap.value = _downloadProgressMap.value - key
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke()
                    }
                } else {
                    throw IllegalStateException("Failed to move temporary download file")
                }
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                _downloadProgressMap.value = _downloadProgressMap.value + (key to -1)
            } finally {
                connection?.disconnect()
                activeJobs.remove(key)
            }
        }
        activeJobs[key] = job
    }

    fun cancelDownload(surahNumber: Int, reciterId: String) {
        val key = getDownloadKey(surahNumber, reciterId)
        activeJobs[key]?.cancel()
        activeJobs.remove(key)
        _downloadProgressMap.value = _downloadProgressMap.value - key

        val reciterDir = File(context.filesDir, "downloads/$reciterId")
        val tempFile = File(reciterDir, "%03d.mp3.tmp".format(surahNumber))
        if (tempFile.exists()) tempFile.delete()
    }

    suspend fun deleteDownload(surahNumber: Int, reciterId: String) {
        cancelDownload(surahNumber, reciterId)
        withContext(Dispatchers.IO) {
            val record = dao.getDownload(surahNumber, reciterId)
            record?.let {
                val file = File(it.localFilePath)
                if (file.exists()) file.delete()
            }
            dao.deleteDownload(surahNumber, reciterId)
        }
    }

    suspend fun deleteAllDownloads() {
        withContext(Dispatchers.IO) {
            val all = dao.getAllDownloads()
            dao.clearAllDownloads()
            val downloadsDir = File(context.filesDir, "downloads")
            if (downloadsDir.exists()) {
                downloadsDir.deleteRecursively()
            }
        }
    }

    fun getLocalDownloadedFile(surahNumber: Int, reciterId: String): File? {
        val formatted = "%03d".format(surahNumber)
        val file = File(context.filesDir, "downloads/$reciterId/$formatted.mp3")
        return if (file.exists() && file.length() > 0) file else null
    }
}
