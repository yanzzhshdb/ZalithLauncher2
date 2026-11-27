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

package com.movtery.zalithlauncher.ui.screens.event

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.event.FirstReleaseEvent
import com.movtery.zalithlauncher.game.session.PlaytimeTracker
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.ui.theme.ColorThemeType
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AuroraViolet = Color(0xFF6D28D9)
private val AuroraLilac = Color(0xFFA855F7)
private val AuroraSky = Color(0xFF38BDF8)

/**
 * First Release 活动页面
 */
@Composable
fun FirstReleaseScreen(
    modifier: Modifier = Modifier,
    onApplyAurora: () -> Unit = {},
    onAskAi: (() -> Unit)? = null
) {
    val state by produceState(initialValue = FirstReleaseEvent.state()) {
        while (true) {
            value = FirstReleaseEvent.state()
            delay(1_000L)
        }
    }
    var justClaimed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScrollWithBar(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeroBanner(state.permanent)

        if (state.permanent) {
            InfoCard(
                title = stringResource(R.string.event_permanent_title),
                body = stringResource(
                    R.string.event_permanent_body,
                    formatDate(state.claimedAt)
                ),
                accent = AuroraLilac
            )
        } else if (state.expired) {
            InfoCard(
                title = stringResource(R.string.event_ended_title),
                body = if (state.goalReached) {
                    stringResource(R.string.event_ended_claimable)
                } else {
                    stringResource(R.string.event_ended_body)
                },
                accent = MaterialTheme.colorScheme.error
            )
        } else {
            CountdownCard(state)
        }

        ProgressCard(state)
        StatsCard()

        if (state.canClaim) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = {
                    if (FirstReleaseEvent.claim()) {
                        justClaimed = true
                        LauncherSettings.launcherColorTheme.save(ColorThemeType.NORX_AURORA)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraViolet,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = stringResource(R.string.event_claim_button),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        if (justClaimed || state.permanent) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.event_aurora_applied),
                style = MaterialTheme.typography.labelMedium,
                color = AuroraLilac,
                textAlign = TextAlign.Center
            )
        }

        RewardsCard(state)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onApplyAurora
            ) {
                Text(text = stringResource(R.string.event_apply_theme))
            }
            if (onAskAi != null) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onAskAi
                ) {
                    Text(text = stringResource(R.string.event_ask_ai))
                }
            }
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            text = stringResource(R.string.event_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HeroBanner(permanent: Boolean) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraShift"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AuroraViolet, AuroraLilac, AuroraSky),
                    start = Offset(0f, 0f),
                    end = Offset(900f * (0.4f + shift), 700f)
                ),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    text = stringResource(R.string.event_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.event_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.event_subtitle, FirstReleaseEvent.VERSION_LABEL),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = if (permanent) {
                        stringResource(R.string.event_status_permanent)
                    } else {
                        stringResource(R.string.event_status_limited)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CountdownCard(state: FirstReleaseEvent.State) {
    val days = state.timeLeftMillis / 86_400_000L
    val hours = (state.timeLeftMillis / 3_600_000L) % 24L
    val minutes = (state.timeLeftMillis / 60_000L) % 60L
    val seconds = (state.timeLeftMillis / 1_000L) % 60L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.event_countdown_title),
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeCell(days, stringResource(R.string.event_unit_day))
                TimeCell(hours, stringResource(R.string.event_unit_hour))
                TimeCell(minutes, stringResource(R.string.event_unit_minute))
                TimeCell(seconds, stringResource(R.string.event_unit_second))
            }
        }
    }
}

@Composable
private fun TimeCell(value: Long, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%02d".format(value),
            style = MaterialTheme.typography.headlineSmall,
            color = AuroraLilac,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressCard(state: FirstReleaseEvent.State) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.event_progress_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${formatDuration(state.progressMillis)} / ${formatDuration(FirstReleaseEvent.GOAL_MILLIS)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AuroraLilac
                )
            }

            if (state.goalReached) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    progress = { 1f },
                    color = AuroraLilac,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    progress = { state.ratio },
                    color = AuroraViolet,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            Text(
                text = if (state.goalReached) {
                    stringResource(R.string.event_progress_done)
                } else {
                    stringResource(R.string.event_progress_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsCard() {
    val snapshot = remember { PlaytimeTracker.snapshot() }
    val streak = remember(snapshot) { computeStreak(snapshot) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatHours(snapshot.totalMillis),
                    style = MaterialTheme.typography.titleMedium,
                    color = AuroraLilac
                )
                Text(
                    text = stringResource(R.string.event_stat_total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.event_stat_streak_value, streak),
                    style = MaterialTheme.typography.titleMedium,
                    color = AuroraLilac
                )
                Text(
                    text = stringResource(R.string.event_stat_streak),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun computeStreak(snapshot: PlaytimeTracker.Snapshot): Int {
    var streak = 0
    for (day in snapshot.recentDays.asReversed()) {
        if (day.millis > 0L) streak++ else break
    }
    return streak
}

private fun formatHours(millis: Long): String {
    val minutes = millis / 60_000L
    val hours = minutes / 60L
    return if (hours > 0L) "${hours}j ${minutes % 60L}m" else "${minutes}m"
}

@Composable
private fun RewardsCard(state: FirstReleaseEvent.State) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.event_rewards_title),
                style = MaterialTheme.typography.titleSmall
            )
            RewardRow(
                title = stringResource(R.string.event_reward_theme),
                body = stringResource(R.string.event_reward_theme_body),
                unlocked = state.permanent || !state.expired
            )
            RewardRow(
                title = stringResource(R.string.event_reward_banner),
                body = stringResource(R.string.event_reward_banner_body),
                unlocked = state.permanent || !state.expired
            )
            RewardRow(
                title = stringResource(R.string.event_reward_permanent),
                body = stringResource(R.string.event_reward_permanent_body),
                unlocked = state.permanent
            )
        }
    }
}

@Composable
private fun RewardRow(title: String, body: String, unlocked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .background(
                    color = if (unlocked) AuroraLilac else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(50)
                )
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = accent
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
