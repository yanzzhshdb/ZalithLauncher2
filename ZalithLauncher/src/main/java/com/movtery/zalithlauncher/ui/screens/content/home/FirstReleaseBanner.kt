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

package com.movtery.zalithlauncher.ui.screens.content.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.event.FirstReleaseEvent
import com.movtery.zalithlauncher.ui.activities.showFirstRelease
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext

/**
 * 主页的 First Release 活动横幅
 */
@Composable
fun FirstReleaseBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by produceState(initialValue = FirstReleaseEvent.state()) {
        while (true) {
            value = FirstReleaseEvent.state()
            delay(5_000L)
        }
    }

    val transition = rememberInfiniteTransition(label = "eventBanner")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eventBannerShift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF6D28D9), Color(0xFFA855F7), Color(0xFF38BDF8)),
                    start = Offset(0f, 0f),
                    end = Offset(700f * (0.5f + shift), 400f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            )
            .clickable { showFirstRelease(context) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        text = stringResource(R.string.event_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(modifier = Modifier.weight(1f))
                Text(
                    text = if (state.permanent) {
                        stringResource(R.string.event_banner_permanent)
                    } else {
                        stringResource(R.string.event_banner_time_left, formatLeft(state.timeLeftMillis))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (state.canClaim) {
                        stringResource(R.string.event_banner_claim)
                    } else if (state.permanent) {
                        stringResource(R.string.event_banner_unlocked)
                    } else {
                        stringResource(R.string.event_banner_progress, formatDuration(state.progressMillis))
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    progress = { state.ratio },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun formatLeft(millis: Long): String {
    val days = millis / 86_400_000L
    val hours = (millis / 3_600_000L) % 24L
    return if (days > 0) "${days}d ${hours}h" else "${hours}h"
}
