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

package com.movtery.zalithlauncher.ui.screens.game.elements

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.recorder.ScreenRecorder
import com.movtery.zalithlauncher.setting.RecorderSettings
import com.movtery.zalithlauncher.setting.enums.RecorderFrameRate
import com.movtery.zalithlauncher.setting.enums.RecorderQuality
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.MenuListLayout
import com.movtery.zalithlauncher.ui.components.MenuSwitchButton
import com.movtery.zalithlauncher.ui.components.MenuTextButton
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import kotlinx.coroutines.delay

/**
 * 游戏内的内置录屏控制
 *
 * 点击开关时向系统申请录屏权限，授权后由 [com.movtery.zalithlauncher.recorder.ScreenRecorderService] 采集屏幕。
 */
@Composable
fun RecordMenuContent(
    modifier: Modifier = Modifier,
    onShowToast: (AndroidStringText, Int) -> Unit,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor()
) {
    val context = LocalContext.current
    val recording by ScreenRecorder.recording.collectAsStateWithLifecycle()
    val startedAt by ScreenRecorder.startedAt.collectAsStateWithLifecycle()

    val projectionManager = remember(context) {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            ScreenRecorder.start(context, result.resultCode, data)
        } else {
            onShowToast(androidText(R.string.recorder_permission_denied), Toast.LENGTH_SHORT)
        }
    }

    fun requestProjection() {
        val manager = projectionManager
        if (manager == null) {
            onShowToast(androidText(R.string.recorder_start_failed), Toast.LENGTH_SHORT)
            return
        }
        runCatching { projectionLauncher.launch(manager.createScreenCaptureIntent()) }
            .onFailure {
                onShowToast(androidText(R.string.recorder_start_failed), Toast.LENGTH_SHORT)
            }
    }

    var pendingProjection by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingProjection) {
                pendingProjection = false
                requestProjection()
            }
        } else {
            pendingProjection = false
            RecorderSettings.audio.save(false)
            onShowToast(androidText(R.string.recorder_audio_permission_denied), Toast.LENGTH_SHORT)
        }
    }

    fun startRecording() {
        val needsAudio = RecorderSettings.audio.getValue()
        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (needsAudio && !audioGranted) {
            pendingProjection = true
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            requestProjection()
        }
    }

    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(recording, startedAt) {
        while (recording && startedAt > 0L) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(500L)
        }
        if (!recording) elapsed = 0L
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuSwitchButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.recorder_switch_title),
            switch = recording,
            onSwitch = { enable ->
                if (enable) startRecording() else ScreenRecorder.stop(context)
            },
            color = color,
            contentColor = contentColor
        )

        if (recording) {
            MenuTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.recorder_recording_info, formatElapsed(elapsed)),
                onClick = { ScreenRecorder.stop(context) },
                color = color,
                contentColor = MaterialTheme.colorScheme.error
            )
        }

        MenuListLayout(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.recorder_quality_title),
            items = RecorderQuality.entries,
            currentItem = RecorderSettings.quality.state,
            onItemChange = { RecorderSettings.quality.save(it) },
            getItemText = { stringResource(it.nameRes) },
            color = color,
            contentColor = contentColor,
            enabled = !recording
        )

        MenuListLayout(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.recorder_frame_rate_title),
            items = RecorderFrameRate.entries,
            currentItem = RecorderSettings.frameRate.state,
            onItemChange = { RecorderSettings.frameRate.save(it) },
            getItemText = { stringResource(it.nameRes) },
            color = color,
            contentColor = contentColor,
            enabled = !recording
        )

        MenuSwitchButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.recorder_audio_title),
            switch = RecorderSettings.audio.state,
            onSwitch = { RecorderSettings.audio.save(it) },
            color = color,
            contentColor = contentColor,
            enabled = !recording
        )

        Text(
            modifier = Modifier.padding(horizontal = 6.dp),
            text = stringResource(R.string.recorder_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 把毫秒格式化为 mm:ss 或 h:mm:ss
 */
private fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = totalSeconds % 3600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
