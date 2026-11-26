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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.contract.MediaPickerContract
import com.movtery.zalithlauncher.music.MusicPlaybackState
import com.movtery.zalithlauncher.music.MusicRepeatMode
import com.movtery.zalithlauncher.music.MusicTrack
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.viewmodel.MusicViewModel
import kotlin.math.roundToInt

/**
 * 音乐库与音乐播放器
 */
@Composable
fun MusicScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    viewModel: MusicViewModel = viewModel()
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Music, settingsScreenKey, false)
    ) { isVisible ->
        MusicContent(isVisible = isVisible, viewModel = viewModel)
    }
}

@Composable
private fun MusicContent(isVisible: Boolean, viewModel: MusicViewModel) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()

    var pendingDelete by remember { mutableStateOf<MusicTrack?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(allowTypes = arrayOf("audio/*"), allowMultiple = true)
    ) { uris ->
        if (uris.isNullOrEmpty()) {
            Toast.makeText(context, context.getString(R.string.music_import_empty), Toast.LENGTH_SHORT).show()
        } else {
            viewModel.import(uris)
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MusicViewModel.MusicEvent.NothingSelected -> {
                    Toast.makeText(context, context.getString(R.string.music_import_empty), Toast.LENGTH_SHORT).show()
                }
                is MusicViewModel.MusicEvent.Imported -> {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.music_import_result,
                            event.result.imported,
                            event.result.failed
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    AnimatedLazyColumn(
        modifier = Modifier.fillMaxSize(),
        isVisible = isVisible,
        contentPadding = PaddingValues(all = 12.dp)
    ) {
        animatedItem { ctx ->
            NowPlayingCard(
                modifier = Modifier.animated(ctx),
                playback = playback,
                onToggle = { if (playback.hasTrack) viewModel.playPause() },
                onPrevious = { viewModel.previous() },
                onNext = { viewModel.next() },
                onSeek = { viewModel.seekTo(it) },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onSleepTimer = { viewModel.setSleepTimer(it) },
                onVolumeChange = { viewModel.setVolume(it) },
                onStop = { viewModel.stop() }
            )
        }

        animatedItem { ctx ->
            LibraryHeader(
                modifier = Modifier.animated(ctx),
                trackCount = tracks.size,
                importing = importing,
                importedCount = importProgress.first,
                totalCount = importProgress.second,
                onAdd = { picker.launch(Unit) }
            )
        }

        if (tracks.isEmpty()) {
            animatedItem { ctx ->
                EmptyLibraryCard(
                    modifier = Modifier.animated(ctx),
                    onAdd = { picker.launch(Unit) }
                )
            }
        } else {
            animatedItems(items = tracks, key = { it.id }) { _, track, ctx ->
                TrackCard(
                    modifier = Modifier.animated(ctx),
                    track = track,
                    isCurrent = playback.trackId == track.id,
                    isPlaying = playback.isPlaying,
                    onClick = {
                        requestNotificationPermission()
                        viewModel.play(track)
                    },
                    onDelete = { pendingDelete = track }
                )
            }
        }
    }

    pendingDelete?.let { track ->
        SimpleAlertDialog(
            title = stringResource(R.string.music_delete_title),
            text = stringResource(R.string.music_delete_message, track.title),
            onConfirm = {
                viewModel.delete(track)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun NowPlayingCard(
    playback: MusicPlaybackState,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSleepTimer: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    val duration = playback.durationMs.coerceAtLeast(0L)
    val sliderMax = duration.toFloat().coerceAtLeast(1f)
    val position = dragPosition ?: playback.positionMs.toFloat()

    BackgroundCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note_filled),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                TitleAndSummary(
                    modifier = Modifier.weight(1f),
                    title = playback.title ?: stringResource(R.string.music_nothing_playing),
                    summary = if (!playback.hasTrack) {
                        stringResource(R.string.music_nothing_playing_summary)
                    } else {
                        playback.artist ?: stringResource(R.string.music_unknown_artist)
                    },
                    titleStyle = MaterialTheme.typography.titleMedium
                )
                if (playback.hasTrack) {
                    IconButton(onClick = onStop) {
                        Icon(
                            painter = painterResource(R.drawable.ic_stop_filled),
                            contentDescription = stringResource(R.string.music_action_stop)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    modifier = Modifier.width(52.dp),
                    text = formatMusicDuration(position.toLong()),
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    modifier = Modifier.weight(1f),
                    value = position.coerceIn(0f, sliderMax),
                    onValueChange = { dragPosition = it },
                    onValueChangeFinished = {
                        dragPosition?.let { onSeek(it.toLong()) }
                        dragPosition = null
                    },
                    valueRange = 0f..sliderMax,
                    enabled = playback.hasTrack && duration > 0L
                )
                Text(
                    modifier = Modifier.width(52.dp),
                    text = formatMusicDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle, enabled = playback.hasTrack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shuffle_filled),
                        contentDescription = stringResource(R.string.music_action_shuffle),
                        tint = if (playback.shuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                var timerMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { timerMenuExpanded = true },
                        enabled = playback.hasTrack
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_schedule_outlined),
                            contentDescription = stringResource(R.string.music_action_sleep_timer),
                            tint = if (playback.sleepTimerEndsAt > 0L) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = timerMenuExpanded,
                        onDismissRequest = { timerMenuExpanded = false }
                    ) {
                        listOf(0, 5, 15, 30, 60).forEach { minutes ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (minutes == 0) {
                                            stringResource(R.string.music_sleep_timer_off)
                                        } else {
                                            stringResource(R.string.music_sleep_timer_minutes, minutes)
                                        }
                                    )
                                },
                                onClick = {
                                    timerMenuExpanded = false
                                    onSleepTimer(minutes)
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = onPrevious, enabled = playback.hasTrack && playback.hasPrevious) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous_filled),
                        contentDescription = stringResource(R.string.music_action_previous)
                    )
                }
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(56.dp),
                    enabled = playback.hasTrack
                ) {
                    Icon(
                        painter = painterResource(
                            if (playback.isPlaying) R.drawable.ic_pause_filled
                            else R.drawable.ic_play_arrow_filled
                        ),
                        contentDescription = stringResource(
                            if (playback.isPlaying) R.string.music_action_pause
                            else R.string.music_action_play
                        ),
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onNext, enabled = playback.hasTrack && playback.hasNext) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next_filled),
                        contentDescription = stringResource(R.string.music_action_next)
                    )
                }
                IconButton(onClick = onCycleRepeat, enabled = playback.hasTrack) {
                    Icon(
                        painter = painterResource(
                            if (playback.repeatMode == MusicRepeatMode.ONE) R.drawable.ic_repeat_one_filled
                            else R.drawable.ic_repeat_filled
                        ),
                        contentDescription = stringResource(R.string.music_action_repeat),
                        tint = if (playback.repeatMode == MusicRepeatMode.OFF) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            if (playback.sleepTimerEndsAt > 0L) {
                val remainingMinutes = (
                        (playback.sleepTimerEndsAt - System.currentTimeMillis()) / 60_000L
                        ).coerceAtLeast(0L).toInt()
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.music_sleep_timer_remaining, remainingMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_volume_up_filled),
                    contentDescription = stringResource(R.string.music_volume),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    modifier = Modifier.weight(1f),
                    value = playback.volume.toFloat(),
                    onValueChange = { onVolumeChange(it.roundToInt()) },
                    valueRange = 0f..100f
                )
                Text(
                    modifier = Modifier.width(44.dp),
                    text = stringResource(R.string.music_volume_value, playback.volume),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    trackCount: Int,
    importing: Boolean,
    importedCount: Int,
    totalCount: Int,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.music_library_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = pluralStringResource(R.plurals.music_library_count, trackCount, trackCount),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconTextButton(
                onClick = onAdd,
                painter = painterResource(R.drawable.ic_add),
                text = stringResource(R.string.music_add),
                enabled = !importing
            )
        }

        if (importing) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.music_importing, importedCount, totalCount),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.alpha(0.7f)
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    BackgroundCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note_filled),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.music_empty_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.music_empty_summary),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.7f)
            )
            IconTextButton(
                onClick = onAdd,
                painter = painterResource(R.drawable.ic_add),
                text = stringResource(R.string.music_add)
            )
        }
    }
}

@Composable
private fun TrackCard(
    track: MusicTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackgroundCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isCurrent && isPlaying) R.drawable.ic_graphic_eq_filled
                    else R.drawable.ic_music_note_filled
                ),
                contentDescription = null,
                tint = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            TitleAndSummary(
                modifier = Modifier.weight(1f),
                title = track.title,
                summary = listOfNotNull(
                    track.artist,
                    formatMusicDuration(track.durationMs)
                ).joinToString(" · ")
            )
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = stringResource(R.string.music_action_delete)
                )
            }
        }
    }
}

private fun formatMusicDuration(milliseconds: Long): String {
    if (milliseconds <= 0L) return "0:00"

    val totalSeconds = milliseconds / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3600

    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
