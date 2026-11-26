/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.activities.MainActivity
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "MusicPlaybackService"
private const val CHANNEL_ID = "music_playback"
private const val NOTIFICATION_ID = 0x4D55

/**
 * 后台音乐播放服务，保证切换到其他应用或启动游戏后音乐仍可继续播放
 */
class MusicPlaybackService : Service() {
    companion object {
        private const val ACTION_TOGGLE = "com.movtery.zalithlauncher.music.action.TOGGLE"
        private const val ACTION_PREVIOUS = "com.movtery.zalithlauncher.music.action.PREVIOUS"
        private const val ACTION_NEXT = "com.movtery.zalithlauncher.music.action.NEXT"
        private const val ACTION_STOP = "com.movtery.zalithlauncher.music.action.STOP"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, MusicPlaybackService::class.java)
                )
            }.onFailure {
                Logger.warning(TAG, "Failed to start the music playback service", it)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, MusicPlaybackService::class.java))
            }.onFailure {
                Logger.warning(TAG, "Failed to stop the music playback service", it)
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var isForeground = false
    private var lastNotificationSignature: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        scope.launch {
            MusicPlayer.state.collect { state ->
                when {
                    !isForeground -> startForegroundCompat(state)
                    state.hasTrack -> notifyState(state)
                    else -> stopPlayback()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> MusicPlayer.togglePlayPause()
            ACTION_PREVIOUS -> MusicPlayer.previous()
            ACTION_NEXT -> MusicPlayer.next()
            ACTION_STOP -> {
                MusicPlayer.stop()
                return START_NOT_STICKY
            }
        }

        if (!isForeground) startForegroundCompat(MusicPlayer.state.value)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat(state: MusicPlaybackState) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(state), type)
        lastNotificationSignature = signatureOf(state)
        isForeground = true
    }

    private fun notifyState(state: MusicPlaybackState) {
        //播放进度会持续刷新，只在通知内容真正变化时才重建通知
        val signature = signatureOf(state)
        if (signature == lastNotificationSignature) return
        lastNotificationSignature = signature

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(state))
        }.onFailure {
            Logger.warning(TAG, "Failed to update the music notification", it)
        }
    }

    private fun signatureOf(state: MusicPlaybackState): String {
        return "${state.trackId}|${state.title}|${state.artist}|${state.isPlaying}"
    }

    private fun stopPlayback() {
        isForeground = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(state: MusicPlaybackState): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (state.isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled
        val playPauseText = if (state.isPlaying) R.string.music_action_pause else R.string.music_action_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note_filled)
            .setContentTitle(state.title ?: getString(R.string.music_now_playing))
            .setContentText(state.artist ?: getString(R.string.music_unknown_artist))
            .setContentIntent(openAppIntent)
            .setOngoing(state.isPlaying)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_skip_previous_filled, getString(R.string.music_action_previous), actionIntent(ACTION_PREVIOUS))
            .addAction(playPauseIcon, getString(playPauseText), actionIntent(ACTION_TOGGLE))
            .addAction(R.drawable.ic_skip_next_filled, getString(R.string.music_action_next), actionIntent(ACTION_NEXT))
            .addAction(R.drawable.ic_stop_filled, getString(R.string.music_action_stop), actionIntent(ACTION_STOP))
            .build()
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.music_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.music_notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
