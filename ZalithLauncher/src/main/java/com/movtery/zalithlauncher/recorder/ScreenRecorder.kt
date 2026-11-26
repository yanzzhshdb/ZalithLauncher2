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

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.setting.RecorderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 内置录屏控制器
 *
 * 负责与 [ScreenRecorderService] 交互，并把录制状态暴露给界面
 */
object ScreenRecorder {
    private val _recording = MutableStateFlow(false)
    /** 是否正在录制 */
    val recording = _recording.asStateFlow()

    private val _startedAt = MutableStateFlow(0L)
    /** 本次录制开始的时间戳，未录制时为 0 */
    val startedAt = _startedAt.asStateFlow()

    private val _lastFile = MutableStateFlow<File?>(null)
    /** 最近一次保存的视频文件 */
    val lastFile = _lastFile.asStateFlow()

    /**
     * 录屏文件的保存目录（应用专属外部存储，无需额外权限）
     */
    fun recordingsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    internal fun createOutputFile(context: Context): File {
        val name = SimpleDateFormat("NL_yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(recordingsDir(context), "$name.mp4")
    }

    /**
     * 使用系统录屏授权结果开始录制
     */
    fun start(context: Context, resultCode: Int, data: Intent) {
        val intent = Intent(context, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_START
            putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecorderService.EXTRA_RESULT_DATA, data)
            putExtra(ScreenRecorderService.EXTRA_WITH_AUDIO, RecorderSettings.audio.getValue())
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
    }

    internal fun markStarted(file: File) {
        _lastFile.value = file
        _startedAt.value = System.currentTimeMillis()
        _recording.value = true
    }

    internal fun markStopped() {
        _recording.value = false
        _startedAt.value = 0L
    }
}
