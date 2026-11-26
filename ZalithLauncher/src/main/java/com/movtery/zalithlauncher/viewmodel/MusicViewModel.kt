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

package com.movtery.zalithlauncher.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.music.MusicImportResult
import com.movtery.zalithlauncher.music.MusicPlaybackState
import com.movtery.zalithlauncher.music.MusicPlayer
import com.movtery.zalithlauncher.music.MusicRepeatMode
import com.movtery.zalithlauncher.music.MusicRepository
import com.movtery.zalithlauncher.music.MusicTrack
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    val tracks: StateFlow<List<MusicTrack>> = MusicRepository.tracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playback: StateFlow<MusicPlaybackState> = MusicPlayer.state

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importProgress = MutableStateFlow(0 to 0)
    val importProgress: StateFlow<Pair<Int, Int>> = _importProgress.asStateFlow()

    private val _events = MutableSharedFlow<MusicEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) {
            _events.tryEmit(MusicEvent.NothingSelected)
            return
        }
        if (_importing.value) return

        viewModelScope.launch {
            _importing.value = true
            _importProgress.value = 0 to uris.size

            val result = MusicRepository.import(uris) { done, total ->
                _importProgress.value = done to total
            }

            _importing.value = false
            _events.tryEmit(MusicEvent.Imported(result))
        }
    }

    fun play(track: MusicTrack) {
        MusicPlayer.play(tracks.value, track)
    }

    fun playPause() = MusicPlayer.togglePlayPause()

    fun next() = MusicPlayer.next()

    fun previous() = MusicPlayer.previous()

    fun seekTo(positionMs: Long) = MusicPlayer.seekTo(positionMs)

    fun stop() = MusicPlayer.stop()

    fun setVolume(percent: Int) = MusicPlayer.setVolume(percent)

    fun cycleRepeatMode(): MusicRepeatMode = MusicPlayer.cycleRepeatMode()

    fun toggleShuffle(): Boolean = MusicPlayer.toggleShuffle()

    fun setSleepTimer(minutes: Int) = MusicPlayer.startSleepTimer(minutes)

    fun delete(track: MusicTrack) {
        viewModelScope.launch {
            if (MusicPlayer.state.value.trackId == track.id) MusicPlayer.stop()
            MusicRepository.delete(track)
        }
    }

    sealed interface MusicEvent {
        data object NothingSelected : MusicEvent
        data class Imported(val result: MusicImportResult) : MusicEvent
    }
}
