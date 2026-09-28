package com.example.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.player.AudioPlayerManager

class QuranAudioService : Service() {

    private lateinit var playerManager: AudioPlayerManager

    override fun onCreate() {
        super.onCreate()
        playerManager = AudioPlayerManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            AudioPlayerManager.ACTION_PLAY -> playerManager.resume()
            AudioPlayerManager.ACTION_PAUSE -> playerManager.pause()
            AudioPlayerManager.ACTION_TOGGLE -> playerManager.togglePlayPause()
            AudioPlayerManager.ACTION_NEXT -> playerManager.playNext()
            AudioPlayerManager.ACTION_PREVIOUS -> playerManager.playPrevious()
            AudioPlayerManager.ACTION_FORWARD -> playerManager.forward10Seconds()
            AudioPlayerManager.ACTION_REWIND -> playerManager.rewind10Seconds()
            AudioPlayerManager.ACTION_STOP -> {
                playerManager.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val notification = playerManager.buildNotification()
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                AudioPlayerManager.NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } catch (e: Exception) {
            // Fallback for older devices or edge cases
            startForeground(AudioPlayerManager.NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }
}
