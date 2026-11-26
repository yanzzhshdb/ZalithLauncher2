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

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.setting.MusicSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "MusicPlayer"
private const val POSITION_TICK_MS = 500L

/**
 * 全局音乐播放器，播放队列在启动器界面与前台服务之间共享
 */
object MusicPlayer {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var player: ExoPlayer? = null
    private var queue: List<MusicTrack> = emptyList()
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(MusicPlaybackState())
    val state: StateFlow<MusicPlaybackState> = _state.asStateFlow()

    fun play(tracks: List<MusicTrack>, track: MusicTrack) {
        if (tracks.isEmpty()) return
        val index = tracks.indexOfFirst { it.id == track.id }
        playQueue(tracks, if (index >= 0) index else 0)
    }

    fun playQueue(tracks: List<MusicTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val exoPlayer = ensurePlayer()
        queue = tracks
        exoPlayer.setMediaItems(
            tracks.map { it.toMediaItem() },
            startIndex.coerceIn(0, tracks.lastIndex),
            0L
        )
        exoPlayer.prepare()
        exoPlayer.play()
        MusicPlaybackService.start(GlobalContext)
        updateState()
    }

    fun togglePlayPause() {
        val exoPlayer = player ?: return
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0L)
            exoPlayer.play()
        }
    }

    fun next() {
        player?.seekToNextMediaItem()
    }

    fun previous() {
        player?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun stop() {
        stopTicker()
        player?.run {
            stop()
            clearMediaItems()
        }
        queue = emptyList()
        _state.value = MusicPlaybackState(
            repeatMode = MusicSettings.repeatMode.getValue(),
            shuffleEnabled = MusicSettings.shuffle.getValue(),
            volume = MusicSettings.volume.getValue()
        )
        MusicPlaybackService.stop(GlobalContext)
    }

    fun setVolume(percent: Int) {
        val volume = percent.coerceIn(0, 100)
        MusicSettings.volume.save(volume)
        player?.volume = volume / 100f
        _state.update { it.copy(volume = volume) }
    }

    fun cycleRepeatMode(): MusicRepeatMode {
        val mode = MusicSettings.repeatMode.getValue().next()
        MusicSettings.repeatMode.save(mode)
        player?.repeatMode = mode.toPlayerRepeatMode()
        _state.update { it.copy(repeatMode = mode) }
        return mode
    }

    fun toggleShuffle(): Boolean {
        val enabled = !MusicSettings.shuffle.getValue()
        MusicSettings.shuffle.save(enabled)
        player?.shuffleModeEnabled = enabled
        _state.update { it.copy(shuffleEnabled = enabled) }
        return enabled
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }

        val volume = MusicSettings.volume.getValue()
        val repeatMode = MusicSettings.repeatMode.getValue()
        val shuffle = MusicSettings.shuffle.getValue()

        return ExoPlayer.Builder(GlobalContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
            this.volume = volume / 100f
            this.repeatMode = repeatMode.toPlayerRepeatMode()
            this.shuffleModeEnabled = shuffle
            addListener(listener)
        }.also { exoPlayer ->
            player = exoPlayer
            _state.update {
                it.copy(volume = volume, repeatMode = repeatMode, shuffleEnabled = shuffle)
            }
        }
    }

    private fun updateState() {
        val exoPlayer = player
        if (exoPlayer == null) {
            stopTicker()
            return
        }

        val current = queue.firstOrNull { it.id.toString() == exoPlayer.currentMediaItem?.mediaId }
        val duration = exoPlayer.duration.takeIf { it > 0L } ?: current?.durationMs ?: 0L

        _state.update { state ->
            state.copy(
                trackId = current?.id,
                title = current?.title,
                artist = current?.artist,
                isPlaying = exoPlayer.isPlaying,
                isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                hasPrevious = exoPlayer.hasPreviousMediaItem(),
                hasNext = exoPlayer.hasNextMediaItem()
            )
        }

        if (exoPlayer.isPlaying) startTicker() else stopTicker()
    }

    private fun startTicker() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                val exoPlayer = player ?: break
                if (!exoPlayer.isPlaying) break

                _state.update {
                    it.copy(
                        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = exoPlayer.duration.takeIf { duration -> duration > 0L } ?: it.durationMs
                    )
                }
                delay(POSITION_TICK_MS)
            }
            positionJob = null
        }
    }

    private fun stopTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun MusicTrack.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.fromFile(MusicRepository.fileOf(this)))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
    }

    private fun MusicRepeatMode.toPlayerRepeatMode(): Int = when (this) {
        MusicRepeatMode.OFF -> Player.REPEAT_MODE_OFF
        MusicRepeatMode.ALL -> Player.REPEAT_MODE_ALL
        MusicRepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState()
        }

        override fun onPlayerError(error: PlaybackException) {
            Logger.error(TAG, "An error occurred while playing music", error)
            updateState()
        }
    }
}
