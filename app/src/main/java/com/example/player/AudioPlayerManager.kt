package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.CountDownTimer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.Reciter
import com.example.model.Surah
import com.example.service.QuranAudioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class PlayerStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

enum class RepeatMode {
    OFF,
    REPEAT_ALL,
    REPEAT_ONE
}

data class PlayerState(
    val status: PlayerStatus = PlayerStatus.IDLE,
    val currentSurah: Surah? = null,
    val currentReciter: Reciter = Reciter.DEFAULT_RECITER,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.REPEAT_ALL,
    val sleepTimerSecondsLeft: Int = 0,
    val sleepTimerEndOfSurah: Boolean = false,
    val sleepTimerFadeOut: Boolean = true,
    val isPlayingFromOfflineCache: Boolean = false,
    val errorMessage: String? = null
) {
    val isSleepTimerActive: Boolean
        get() = sleepTimerSecondsLeft > 0 || sleepTimerEndOfSurah

    val sleepTimerMinutesLeft: Int
        get() = if (sleepTimerSecondsLeft > 0) (sleepTimerSecondsLeft + 59) / 60 else 0

    val formattedSleepTimer: String
        get() = when {
            sleepTimerEndOfSurah -> "End of Surah"
            sleepTimerSecondsLeft > 0 -> {
                val mins = sleepTimerSecondsLeft / 60
                val secs = sleepTimerSecondsLeft % 60
                "%02d:%02d".format(mins, secs)
            }
            else -> ""
        }
}

class AudioPlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var resumeOnFocusGain = false
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        handleAudioFocusChange(change)
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var currentQueue: List<Surah> = Surah.ALL_SURAHS
    private var queueIndex: Int = 0

    private var progressTrackingJob: Job? = null
    private var sleepTimer: CountDownTimer? = null

    var onPlaybackHistoryUpdate: ((surah: Surah, reciter: Reciter, pos: Long, dur: Long, completed: Boolean) -> Unit)? = null
    var localAudioFileResolver: ((surahNumber: Int, reciterId: String) -> File?)? = null

    init {
        createNotificationChannel()
    }

    companion object {
        const val CHANNEL_ID = "quran_audio_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_TOGGLE = "com.example.action.TOGGLE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.action.PREVIOUS"
        const val ACTION_REWIND = "com.example.action.REWIND"
        const val ACTION_FORWARD = "com.example.action.FORWARD"
        const val ACTION_STOP = "com.example.action.STOP"

        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun playSurah(
        surah: Surah,
        reciter: Reciter = _playerState.value.currentReciter,
        playlist: List<Surah> = Surah.ALL_SURAHS,
        startPositionMs: Long = 0L
    ) {
        currentQueue = playlist
        queueIndex = currentQueue.indexOfFirst { it.number == surah.number }.let { if (it == -1) 0 else it }

        _playerState.value = _playerState.value.copy(
            status = PlayerStatus.BUFFERING,
            currentSurah = surah,
            currentReciter = reciter,
            currentPositionMs = startPositionMs,
            durationMs = 0L,
            errorMessage = null
        )

        // Start Foreground Service
        val serviceIntent = Intent(context, QuranAudioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        prepareAndPlay(surah, reciter, startPositionMs)
    }

    fun setReciter(reciter: Reciter) {
        val currentSurah = _playerState.value.currentSurah
        val currentPos = _playerState.value.currentPositionMs
        val isCurrentlyPlaying = _playerState.value.status == PlayerStatus.PLAYING

        _playerState.value = _playerState.value.copy(currentReciter = reciter)

        if (currentSurah != null && (isCurrentlyPlaying || _playerState.value.status == PlayerStatus.PAUSED)) {
            playSurah(
                surah = currentSurah,
                reciter = reciter,
                playlist = currentQueue,
                startPositionMs = currentPos
            )
        }
    }

    private fun prepareAndPlay(surah: Surah, reciter: Reciter, startPositionMs: Long) {
        releasePlayer()

        if (!requestAudioFocus()) {
            _playerState.value = _playerState.value.copy(
                status = PlayerStatus.ERROR,
                errorMessage = "Could not gain audio focus."
            )
            return
        }

        val audioUrl = reciter.getAudioUrl(surah.number)
        val localFile = localAudioFileResolver?.invoke(surah.number, reciter.id)
        val isOffline = localFile != null && localFile.exists() && localFile.length() > 0

        _playerState.value = _playerState.value.copy(
            isPlayingFromOfflineCache = isOffline
        )

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (isOffline) {
                    setDataSource(localFile!!.absolutePath)
                } else {
                    setDataSource(audioUrl)
                }
                setOnPreparedListener { mp ->
                    val duration = mp.duration.toLong().coerceAtLeast(0L)
                    _playerState.value = _playerState.value.copy(
                        status = PlayerStatus.PLAYING,
                        durationMs = duration
                    )
                    applyPlaybackSpeed(_playerState.value.playbackSpeed)
                    if (startPositionMs > 0 && startPositionMs < duration) {
                        mp.seekTo(startPositionMs.toInt())
                    }
                    mp.start()
                    startProgressTracker()
                    updateNotification()
                }

                setOnCompletionListener {
                    onPlaybackCompleted()
                }

                setOnErrorListener { _, what, extra ->
                    stopProgressTracker()
                    _playerState.value = _playerState.value.copy(
                        status = PlayerStatus.ERROR,
                        errorMessage = "Audio playback error ($what, $extra). Please check your internet connection."
                    )
                    true
                }

                setOnBufferingUpdateListener { _, percent ->
                    // Buffering update if needed
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            _playerState.value = _playerState.value.copy(
                status = PlayerStatus.ERROR,
                errorMessage = "Error preparing audio: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    fun togglePlayPause() {
        when (_playerState.value.status) {
            PlayerStatus.PLAYING -> pause()
            PlayerStatus.PAUSED -> resume()
            PlayerStatus.IDLE, PlayerStatus.ERROR -> {
                val surah = _playerState.value.currentSurah ?: Surah.ALL_SURAHS.first()
                playSurah(surah, _playerState.value.currentReciter)
            }
            PlayerStatus.BUFFERING -> { /* waiting */ }
        }
    }

    fun pause() {
        resumeOnFocusGain = false
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                stopProgressTracker()
                _playerState.value = _playerState.value.copy(status = PlayerStatus.PAUSED)
                updateNotification()
                recordHistoryCheckpoint(completed = false)
            }
        }
    }

    fun resume() {
        if (!requestAudioFocus()) {
            _playerState.value = _playerState.value.copy(
                errorMessage = "Could not gain audio focus. Please try again."
            )
            return
        }
        resumeOnFocusGain = false
        val position = _playerState.value.currentPositionMs
        val duration = _playerState.value.durationMs
        if (duration > 0 && position >= duration) {
            _playerState.value.currentSurah?.let {
                playSurah(it, _playerState.value.currentReciter, currentQueue)
            }
            return
        }
        mediaPlayer?.let {
            try {
                it.start()
                startProgressTracker()
                _playerState.value = _playerState.value.copy(
                    status = PlayerStatus.PLAYING,
                    errorMessage = null
                )
                updateNotification()
            } catch (e: IllegalStateException) {
                // A paused network stream can become unusable; prepare it again at the saved position.
                _playerState.value.currentSurah?.let { surah ->
                    playSurah(surah, _playerState.value.currentReciter, currentQueue, position)
                }
            }
        } ?: run {
            _playerState.value.currentSurah?.let {
                playSurah(it, _playerState.value.currentReciter, currentQueue, _playerState.value.currentPositionMs)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val clamped = positionMs.coerceIn(0L, _playerState.value.durationMs)
            it.seekTo(clamped.toInt())
            _playerState.value = _playerState.value.copy(currentPositionMs = clamped)
        }
    }

    fun rewind10Seconds() {
        val current = _playerState.value.currentPositionMs
        seekTo((current - 10000L).coerceAtLeast(0L))
    }

    fun forward10Seconds() {
        val current = _playerState.value.currentPositionMs
        seekTo((current + 10000L).coerceAtMost(_playerState.value.durationMs))
    }

    fun playNext() {
        if (currentQueue.isEmpty()) return
        if (queueIndex < currentQueue.size - 1) {
            queueIndex++
        } else {
            queueIndex = 0 // loop back to first
        }
        playSurah(currentQueue[queueIndex], _playerState.value.currentReciter, currentQueue)
    }

    fun playPrevious() {
        if (currentQueue.isEmpty()) return
        // If played more than 4 seconds, restart current surah
        if (_playerState.value.currentPositionMs > 4000) {
            seekTo(0)
            return
        }
        if (queueIndex > 0) {
            queueIndex--
        } else {
            queueIndex = currentQueue.size - 1
        }
        playSurah(currentQueue[queueIndex], _playerState.value.currentReciter, currentQueue)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        applyPlaybackSpeed(speed)
    }

    fun togglePlaybackSpeed(): Float {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val current = _playerState.value.playbackSpeed
        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - current) < 0.05f }
        val nextIndex = if (currentIndex == -1 || currentIndex == speeds.lastIndex) 0 else currentIndex + 1
        val nextSpeed = speeds[nextIndex]
        setPlaybackSpeed(nextSpeed)
        return nextSpeed
    }

    private fun applyPlaybackSpeed(speed: Float) {
        mediaPlayer?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val params = it.playbackParams
                    params.speed = speed
                    it.playbackParams = params
                } catch (e: Exception) {
                    // Ignore on unsupported devices
                }
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        _playerState.value = _playerState.value.copy(repeatMode = mode)
    }

    fun toggleRepeatMode() {
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    fun setSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
        } else {
            setSleepTimerDuration(minutes * 60, fadeOut = true)
        }
    }

    fun setSleepTimerDuration(seconds: Int, fadeOut: Boolean = true) {
        sleepTimer?.cancel()
        restoreVolume()

        if (seconds <= 0) {
            cancelSleepTimer()
            return
        }

        _playerState.value = _playerState.value.copy(
            sleepTimerSecondsLeft = seconds,
            sleepTimerEndOfSurah = false,
            sleepTimerFadeOut = fadeOut
        )

        val millis = seconds * 1000L

        sleepTimer = object : CountDownTimer(millis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secLeft = (millisUntilFinished / 1000L).toInt()
                _playerState.value = _playerState.value.copy(sleepTimerSecondsLeft = secLeft)

                // Smooth fade-out in last 30 seconds
                if (_playerState.value.sleepTimerFadeOut && secLeft in 1..30) {
                    val fadeFraction = (secLeft / 30f).coerceIn(0.05f, 1.0f)
                    mediaPlayer?.setVolume(fadeFraction, fadeFraction)
                }
            }

            override fun onFinish() {
                _playerState.value = _playerState.value.copy(
                    sleepTimerSecondsLeft = 0,
                    sleepTimerEndOfSurah = false
                )
                pause()
                restoreVolume()
            }
        }.start()
    }

    fun setSleepTimerEndOfSurah(enabled: Boolean) {
        sleepTimer?.cancel()
        restoreVolume()
        _playerState.value = _playerState.value.copy(
            sleepTimerSecondsLeft = 0,
            sleepTimerEndOfSurah = enabled
        )
    }

    fun addSleepTimerSeconds(additionalSeconds: Int) {
        val current = _playerState.value.sleepTimerSecondsLeft
        val newDuration = (current + additionalSeconds).coerceAtLeast(60)
        setSleepTimerDuration(newDuration, _playerState.value.sleepTimerFadeOut)
    }

    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        restoreVolume()
        _playerState.value = _playerState.value.copy(
            sleepTimerSecondsLeft = 0,
            sleepTimerEndOfSurah = false
        )
    }

    private fun restoreVolume() {
        mediaPlayer?.let {
            try {
                it.setVolume(1.0f, 1.0f)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun onPlaybackCompleted() {
        recordHistoryCheckpoint(completed = true)

        // Check if sleep timer was set for end of current Surah
        if (_playerState.value.sleepTimerEndOfSurah) {
            _playerState.value = _playerState.value.copy(
                sleepTimerEndOfSurah = false,
                status = PlayerStatus.PAUSED,
                currentPositionMs = _playerState.value.durationMs
            )
            stopProgressTracker()
            updateNotification()
            return
        }

        when (_playerState.value.repeatMode) {
            RepeatMode.REPEAT_ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                startProgressTracker()
            }
            RepeatMode.REPEAT_ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                stopProgressTracker()
                _playerState.value = _playerState.value.copy(
                    status = PlayerStatus.PAUSED,
                    currentPositionMs = _playerState.value.durationMs
                )
                updateNotification()
            }
        }
    }

    private fun recordHistoryCheckpoint(completed: Boolean) {
        val surah = _playerState.value.currentSurah ?: return
        val reciter = _playerState.value.currentReciter
        val pos = _playerState.value.currentPositionMs
        val dur = _playerState.value.durationMs
        onPlaybackHistoryUpdate?.invoke(surah, reciter, pos, dur, completed)
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackingJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val current = mp.currentPosition.toLong().coerceAtLeast(0L)
                        val duration = mp.duration.toLong().coerceAtLeast(0L)
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = current,
                            durationMs = duration
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    fun stop() {
        sleepTimer?.cancel()
        stopProgressTracker()
        recordHistoryCheckpoint(completed = false)
        releasePlayer()
        abandonAudioFocus()
        _playerState.value = _playerState.value.copy(
            status = PlayerStatus.IDLE,
            currentPositionMs = 0L,
            durationMs = 0L
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun releasePlayer() {
        stopProgressTracker()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (e: Exception) {
                // Ignore
            }
            mediaPlayer = null
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            hasAudioFocus = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (hasAudioFocus) audioFocusRequest = request
        } else {
            @Suppress("DEPRECATION")
            hasAudioFocus = audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
        hasAudioFocus = false
        resumeOnFocusGain = false
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val wasPlaying = _playerState.value.status == PlayerStatus.PLAYING
                hasAudioFocus = false
                pause()
                resumeOnFocusGain = wasPlaying
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                mediaPlayer?.setVolume(1.0f, 1.0f)
                if (resumeOnFocusGain && _playerState.value.status == PlayerStatus.PAUSED) {
                    resume()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quran Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and information for currently playing Quran recitation"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(): Notification {
        val state = _playerState.value
        val surah = state.currentSurah
        val reciter = state.currentReciter
        val isPlaying = state.status == PlayerStatus.PLAYING

        val title = surah?.let { "Surah ${it.nameEnglish} (${it.nameArabic})" } ?: "Audio Quran"
        val subtitle = "${reciter.nameEnglish} • ${reciter.style}"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, pendingIntentFlags)

        // Actions
        val prevPendingIntent = PendingIntent.getService(
            context, 1,
            Intent(context, QuranAudioService::class.java).apply { action = ACTION_PREVIOUS },
            pendingIntentFlags
        )

        val togglePendingIntent = PendingIntent.getService(
            context, 2,
            Intent(context, QuranAudioService::class.java).apply { action = ACTION_TOGGLE },
            pendingIntentFlags
        )

        val nextPendingIntent = PendingIntent.getService(
            context, 3,
            Intent(context, QuranAudioService::class.java).apply { action = ACTION_NEXT },
            pendingIntentFlags
        )

        val stopPendingIntent = PendingIntent.getService(
            context, 4,
            Intent(context, QuranAudioService::class.java).apply { action = ACTION_STOP },
            pendingIntentFlags
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText("Recitation")
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$subtitle\n${surah?.englishTranslation ?: ""}")
            )

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // ignore if permissions not yet granted
        }
    }
}
