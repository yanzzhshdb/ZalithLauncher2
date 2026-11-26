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

package com.movtery.zalithlauncher.community.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * 语音消息录制
 */
class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    val isRecording: Boolean
        get() = recorder != null

    fun start(): Boolean {
        if (recorder != null) return false
        val directory = File(context.cacheDir, "community_voice").apply { mkdirs() }
        val file = File(directory, "note_${System.currentTimeMillis()}.m4a")
        val created: MediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            createLegacyRecorder()
        }

        return runCatching {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioEncodingBitRate(64_000)
            created.setAudioSamplingRate(44_100)
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
            recorder = created
            outputFile = file
            startedAt = System.currentTimeMillis()
            true
        }.getOrElse {
            runCatching { created.release() }
            false
        }
    }

    fun stop(): Recording? {
        val current = recorder ?: return null
        recorder = null
        val file = outputFile
        outputFile = null
        val duration = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)

        return runCatching {
            current.stop()
            current.release()
            file?.takeIf { it.exists() && it.length() > 0 }?.let { Recording(it, duration) }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun createLegacyRecorder(): MediaRecorder = MediaRecorder()

    fun cancel() {
        val current = recorder ?: return
        recorder = null
        runCatching { current.stop() }
        runCatching { current.release() }
        outputFile?.delete()
        outputFile = null
    }

    data class Recording(val file: File, val durationMs: Long)
}

/**
 * 语音消息播放（同一时间只播放一条）
 */
object VoiceNotePlayer {
    private var player: MediaPlayer? = null
    val playingUrl = MutableStateFlow<String?>(null)

    fun toggle(url: String) {
        if (playingUrl.value == url) {
            stop()
            return
        }
        stop()
        runCatching {
            val created = MediaPlayer()
            created.setDataSource(url)
            created.setOnCompletionListener { stop() }
            created.setOnErrorListener { _, _, _ ->
                stop()
                true
            }
            created.prepareAsync()
            created.setOnPreparedListener { it.start() }
            player = created
            playingUrl.value = url
        }.onFailure { stop() }
    }

    fun stop() {
        player?.let { current ->
            runCatching { if (current.isPlaying) current.stop() }
            runCatching { current.release() }
        }
        player = null
        playingUrl.value = null
    }
}
