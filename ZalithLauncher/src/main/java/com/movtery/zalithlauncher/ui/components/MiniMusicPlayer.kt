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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.music.MusicPlayer
import com.movtery.zalithlauncher.ui.theme.glass.GlassLayer
import com.movtery.zalithlauncher.ui.theme.glass.LiquidGlassSurface
import com.movtery.zalithlauncher.ui.theme.glass.rememberGlassSpec

/**
 * 悬浮迷你播放器：有歌曲时显示在屏幕底部，点击信息区域可跳转到音乐页面
 */
@Composable
fun MiniMusicPlayer(
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    val playback by MusicPlayer.state.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = visible && playback.hasTrack,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(200)) +
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it }
            ) +
            scaleIn(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.92f
            ),
        exit = fadeOut(animationSpec = tween(150)) +
            slideOutVertically(animationSpec = tween(180), targetOffsetY = { it / 2 })
    ) {
        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            spec = rememberGlassSpec(GlassLayer.Regular),
            contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_graphic_eq_filled),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenPlayer)
                ) {
                    Text(
                        text = playback.title.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modifier = Modifier.alpha(0.7f),
                        text = playback.artist ?: stringResource(R.string.music_unknown_artist),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { MusicPlayer.togglePlayPause() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (playback.isPlaying) R.drawable.ic_pause_filled
                            else R.drawable.ic_play_arrow_filled
                        ),
                        contentDescription = stringResource(
                            if (playback.isPlaying) R.string.music_action_pause
                            else R.string.music_action_play
                        )
                    )
                }

                IconButton(
                    onClick = { MusicPlayer.next() },
                    modifier = Modifier.size(40.dp),
                    enabled = playback.hasNext
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next_filled),
                        contentDescription = stringResource(R.string.music_action_next)
                    )
                }
            }

            if (playback.durationMs > 0L) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                (playback.positionMs.toFloat() / playback.durationMs)
                                    .coerceIn(0f, 1f)
                            )
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
