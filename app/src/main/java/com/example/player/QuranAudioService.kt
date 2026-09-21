package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AudioTrack

/**
 * Lightweight Android Foreground Service for persistent background Quran audio playback.
 * Ensures uninterrupted audio when the user navigates to the TV Home screen or System Settings.
 * Optimized specifically for Android 9+ and 1GB RAM Android TV environments.
 */
class QuranAudioService : Service() {

    private var currentTrackTitle: String = "القرآن الكريم"
    private var currentSubtitle: String = "مشغل القرآن للشاشات الذكية"
    private var isPlaying: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START_OR_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: currentTrackTitle
                val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: currentSubtitle
                val playing = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)

                currentTrackTitle = title
                currentSubtitle = subtitle
                isPlaying = playing

                val notification = buildNotification(title, subtitle, playing)
                startInForeground(notification)
            }
            ACTION_PLAY_PAUSE -> {
                AudioServiceBridge.onPlayPause?.invoke()
            }
            ACTION_NEXT -> {
                AudioServiceBridge.onNext?.invoke()
            }
            ACTION_PREV -> {
                AudioServiceBridge.onPrev?.invoke()
            }
            ACTION_STOP -> {
                AudioServiceBridge.onStop?.invoke()
                stopForegroundSafely()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startInForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}")
        }
    }

    private fun stopForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping foreground: ${e.message}")
        }
    }

    private fun buildNotification(title: String, subtitle: String, playing: Boolean): Notification {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Open MainActivity when notification is clicked
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, pendingIntentFlags)

        // Remote/Notification Action intents
        val playPauseIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(this, 1, playPauseIntent, pendingIntentFlags)

        val prevIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(this, 2, prevIntent, pendingIntentFlags)

        val nextIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, pendingIntentFlags)

        val stopIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, pendingIntentFlags)

        val largeIcon = try {
            BitmapFactory.decodeResource(resources, R.drawable.ic_quran_logo)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText("تشغيل مستمر")
            .setSmallIcon(R.drawable.ic_quran_logo)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(playing)
            .addAction(
                android.R.drawable.ic_media_previous,
                "السابق",
                prevPendingIntent
            )
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "إيقاف مؤقت" else "تشغيل",
                playPausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "التالي",
                nextPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "إغلاق",
                stopPendingIntent
            )

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تشغيل القرآن الكريم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "الخدمة الصوتية المستمرة لتشغيل القرآن الكريم في الخلفية والشاشة الذكية"
                setShowBadge(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundSafely()
    }

    companion object {
        private const val TAG = "QuranAudioService"
        const val CHANNEL_ID = "quran_tv_audio_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_OR_UPDATE = "com.example.action.START_OR_UPDATE"
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_STOP = "com.example.action.STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        fun updateService(context: Context, track: AudioTrack?, isPlaying: Boolean) {
            val intent = Intent(context, QuranAudioService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_TITLE, track?.title ?: "القرآن الكريم")
                putExtra(EXTRA_SUBTITLE, track?.reciterOrSubtitle ?: "مشغل القرآن للشاشات الذكية")
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start/update audio service: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, QuranAudioService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop audio service: ${e.message}")
            }
        }
    }
}
