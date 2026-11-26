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

package com.movtery.zalithlauncher.recorder

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.notification.NOTIFICATION_ID_RECORDING
import com.movtery.zalithlauncher.notification.NOTIFICATION_ID_RECORDING_SAVED
import com.movtery.zalithlauncher.notification.NotificationChannelData
import com.movtery.zalithlauncher.setting.RecorderSettings
import com.movtery.zalithlauncher.utils.getRealScreenSize
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import kotlin.math.roundToInt

private const val TAG = "ScreenRecorderService"

/**
 * 屏幕录制前台服务
 *
 * 使用 MediaProjection 采集整块屏幕（游戏画面也在其中），MediaRecorder 直接编码为 MP4。
 */
class ScreenRecorderService : Service() {
    companion object {
        const val ACTION_START = "com.movtery.zalithlauncher.recorder.START"
        const val ACTION_STOP = "com.movtery.zalithlauncher.recorder.STOP"

        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        const val EXTRA_WITH_AUDIO = "withAudio"
    }

    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var display: VirtualDisplay? = null
    private var outputFile: File? = null
    private var running = false

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_STOP -> stopRecording()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        if (running) return

        //Android 14 起必须先以 mediaProjection 类型进入前台，才能创建投影
        startForegroundCompat(preparingNotification())

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        @Suppress("DEPRECATION")
        val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            Logger.warning(TAG, "Missing media projection permission result")
            stopSelf()
            return
        }

        val withAudio = intent.getBooleanExtra(EXTRA_WITH_AUDIO, false)

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        val mediaProjection = runCatching {
            manager?.getMediaProjection(resultCode, resultData)
        }.getOrNull()

        if (mediaProjection == null) {
            Logger.error(TAG, "Failed to create media projection")
            finishAndClean(stopService = true)
            return
        }
        projection = mediaProjection
        mediaProjection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    //用户在系统界面停止了录屏
                    stopRecording()
                }
            },
            mainHandler
        )

        val file = ScreenRecorder.createOutputFile(this)
        outputFile = file

        val size = getRealScreenSize(this)
        val quality = RecorderSettings.quality.getValue()
        val frameRate = RecorderSettings.frameRate.getValue().fps
        val width = ((size.width * quality.scale).roundToInt() / 2 * 2).coerceAtLeast(2)
        val height = ((size.height * quality.scale).roundToInt() / 2 * 2).coerceAtLeast(2)
        val bitRate = (width.toLong() * height * frameRate * quality.bitsPerPixel)
            .toLong()
            .coerceIn(2_000_000L, 40_000_000L)
            .toInt()

        try {
            val mediaRecorder = buildRecorder(file, width, height, frameRate, bitRate, withAudio)
            recorder = mediaRecorder
            //surface 只能在 prepare() 之后、start() 之前获取
            val surface = mediaRecorder.surface
            display = mediaProjection.createVirtualDisplay(
                "NLRecorder",
                width,
                height,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )
            mediaRecorder.start()
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to start screen recorder", e)
            outputFile = null
            runCatching { file.delete() }
            finishAndClean(stopService = true)
            return
        }

        running = true
        ScreenRecorder.markStarted(file)
        updateNotification(recordingNotification())
    }

    private fun buildRecorder(
        file: File,
        width: Int,
        height: Int,
        frameRate: Int,
        bitRate: Int,
        withAudio: Boolean
    ): MediaRecorder {
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        if (withAudio) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setOutputFile(file.absolutePath)
        if (withAudio) {
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(44100)
            mediaRecorder.setAudioEncodingBitRate(128_000)
            mediaRecorder.setAudioChannels(1)
        }
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setVideoSize(width, height)
        mediaRecorder.setVideoFrameRate(frameRate)
        mediaRecorder.setVideoEncodingBitRate(bitRate)
        mediaRecorder.prepare()
        return mediaRecorder
    }

    private fun stopRecording() {
        if (!running) {
            finishAndClean(stopService = true)
            return
        }
        running = false

        val file = outputFile
        outputFile = null

        runCatching { display?.release() }
        display = null

        var saved = true
        recorder?.let { rec ->
            runCatching { rec.stop() }.onFailure {
                //录制时间过短时 stop() 会抛异常，此时文件不可用
                Logger.warning(TAG, "Recorder stopped before any frame was written", it)
                saved = false
            }
            runCatching { rec.reset() }
            runCatching { rec.release() }
        }
        recorder = null

        val currentProjection = projection
        projection = null
        runCatching { currentProjection?.stop() }

        if (!saved) runCatching { file?.delete() }

        ScreenRecorder.markStopped()

        if (saved && file != null && file.length() > 0L) {
            notifySaved(file)
        } else {
            notificationManager()?.cancel(NOTIFICATION_ID_RECORDING)
        }

        finishAndClean(stopService = true)
    }

    private fun finishAndClean(stopService: Boolean) {
        runCatching { display?.release() }
        display = null
        runCatching { recorder?.release() }
        recorder = null
        val currentProjection = projection
        projection = null
        runCatching { currentProjection?.stop() }
        ScreenRecorder.markStopped()
        notificationManager()?.cancel(NOTIFICATION_ID_RECORDING)
        if (stopService) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (running) stopRecording()
        super.onDestroy()
    }

    private fun notificationManager(): NotificationManagerCompat? =
        runCatching { NotificationManagerCompat.from(this) }.getOrNull()

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_RECORDING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID_RECORDING, notification)
        }
    }

    private fun updateNotification(notification: Notification) {
        runCatching {
            notificationManager()?.notify(NOTIFICATION_ID_RECORDING, notification)
        }
    }

    private fun preparingNotification(): Notification {
        return NotificationCompat.Builder(this, NotificationChannelData.RECORDER_CHANNEL.channelId)
            .setSmallIcon(R.drawable.ic_movie_filled)
            .setContentTitle(getString(R.string.recorder_notification_preparing))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun recordingNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ScreenRecorderService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationChannelData.RECORDER_CHANNEL.channelId)
            .setSmallIcon(R.drawable.ic_movie_filled)
            .setContentTitle(getString(R.string.recorder_notification_title))
            .setContentText(getString(R.string.recorder_notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis())
            .addAction(0, getString(R.string.recorder_notification_stop), stopIntent)
            .build()
    }

    private fun notifySaved(file: File) {
        val uri = runCatching {
            FileProvider.getUriForFile(this, "$packageName.provider", file)
        }.getOrNull()

        val openIntent = uri?.let {
            PendingIntent.getActivity(
                this,
                1,
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(it, "video/mp4")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val shareIntent = uri?.let {
            PendingIntent.getActivity(
                this,
                2,
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("video/mp4")
                        .putExtra(Intent.EXTRA_STREAM, it)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    getString(R.string.recorder_share_title)
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, NotificationChannelData.RECORDER_CHANNEL.channelId)
            .setSmallIcon(R.drawable.ic_movie_filled)
            .setContentTitle(getString(R.string.recorder_notification_saved_title))
            .setContentText(getString(R.string.recorder_notification_saved_text, file.name))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.recorder_notification_saved_detail, file.absolutePath))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        openIntent?.let { builder.setContentIntent(it) }
        openIntent?.let { builder.addAction(0, getString(R.string.recorder_action_open), it) }
        shareIntent?.let { builder.addAction(0, getString(R.string.recorder_action_share), it) }

        runCatching {
            notificationManager()?.notify(NOTIFICATION_ID_RECORDING_SAVED, builder.build())
        }
    }
}
