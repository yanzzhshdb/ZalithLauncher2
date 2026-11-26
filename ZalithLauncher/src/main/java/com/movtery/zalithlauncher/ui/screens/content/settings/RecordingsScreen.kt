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

package com.movtery.zalithlauncher.ui.screens.content.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.recorder.ScreenRecorder
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 录屏管理页面：查看、播放、分享与删除内置录屏文件
 */
@Composable
fun RecordingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Recordings, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current
        val dir = remember(context) { ScreenRecorder.recordingsDir(context) }

        var files by remember { mutableStateOf<List<File>>(emptyList()) }
        var refreshKey by remember { mutableIntStateOf(0) }
        var pendingDelete by remember { mutableStateOf<File?>(null) }

        LaunchedEffect(refreshKey) {
            files = withContext(Dispatchers.IO) {
                dir.listFiles()
                    ?.filter { it.isFile && it.length() > 0L }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
            }
        }

        AnimatedLazyColumn(
            modifier = Modifier.fillMaxSize(),
            isVisible = isVisible,
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            animatedItem { ctx ->
                SettingsCard(
                    modifier = Modifier.animated(ctx),
                    position = CardPosition.Single
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_tab_recordings),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.record_list_hint, dir.absolutePath),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (files.isEmpty()) {
                animatedItem { ctx ->
                    SettingsCard(
                        modifier = Modifier.animated(ctx),
                        position = CardPosition.Single
                    ) {
                        Text(
                            modifier = Modifier.padding(all = 16.dp),
                            text = stringResource(R.string.record_list_empty),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                files.forEach { file ->
                    animatedItem(key = file.absolutePath) { ctx ->
                        RecordingRow(
                            modifier = Modifier.animated(ctx),
                            file = file,
                            onPlay = { playRecording(context, file) },
                            onShare = { shareRecording(context, file) },
                            onDelete = { pendingDelete = file }
                        )
                    }
                }
            }
        }

        pendingDelete?.let { file ->
            SimpleAlertDialog(
                title = stringResource(R.string.record_delete_title),
                text = stringResource(R.string.record_delete_message),
                onConfirm = {
                    val deleted = file.delete()
                    pendingDelete = null
                    refreshKey++
                    Toast.makeText(
                        context,
                        context.getString(
                            if (deleted) R.string.record_deleted else R.string.record_delete_failed
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}

@Composable
private fun RecordingRow(
    file: File,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        modifier = modifier.fillMaxWidth(),
        position = CardPosition.Single
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(
                        R.string.record_list_detail,
                        formatSize(file.length()),
                        formatDate(file.lastModified())
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onPlay) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = stringResource(R.string.record_action_play)
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    painter = painterResource(R.drawable.ic_share_filled),
                    contentDescription = stringResource(R.string.recorder_action_share)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = stringResource(R.string.generic_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun playRecording(context: android.content.Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "video/mp4")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.record_play_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun shareRecording(context: android.content.Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("video/mp4")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.recorder_share_title))
        )
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.record_share_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L ->
        String.format(Locale.US, "%.2f GB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L ->
        String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024.0 * 1024.0))
    else ->
        String.format(Locale.US, "%.0f KB", bytes.toDouble() / 1024.0)
}
