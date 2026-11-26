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

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.session.PlaytimeFormat
import com.movtery.zalithlauncher.game.session.PlaytimeTracker
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 游玩统计页面：总时长、最近七天柱状图与各版本的游玩时长排行
 */
@Composable
fun PlaytimeScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Playtime, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current
        var snapshot by remember { mutableStateOf(PlaytimeTracker.snapshot()) }
        var showResetDialog by remember { mutableStateOf(false) }

        AnimatedLazyColumn(
            modifier = Modifier.fillMaxSize(),
            isVisible = isVisible,
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            animatedItem { ctx ->
                StatsCard(
                    modifier = Modifier.animated(ctx),
                    title = stringResource(R.string.playtime_overview_title)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBlock(
                            modifier = Modifier.weight(1f),
                            icon = R.drawable.ic_schedule_outlined,
                            value = PlaytimeFormat.duration(context, snapshot.totalMillis),
                            label = stringResource(R.string.playtime_total_label)
                        )
                        StatBlock(
                            modifier = Modifier.weight(1f),
                            icon = R.drawable.ic_play_arrow_filled,
                            value = snapshot.sessions.toString(),
                            label = stringResource(R.string.playtime_sessions_label)
                        )
                        StatBlock(
                            modifier = Modifier.weight(1f),
                            icon = R.drawable.ic_sports_esports_outlined,
                            value = snapshot.versions.size.toString(),
                            label = stringResource(R.string.playtime_versions_label)
                        )
                    }

                    if (snapshot.isEmpty) {
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(R.string.playtime_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            animatedItem { ctx ->
                StatsCard(
                    modifier = Modifier.animated(ctx),
                    title = stringResource(R.string.playtime_week_title)
                ) {
                    WeeklyBars(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        days = snapshot.recentDays
                    )
                }
            }

            if (snapshot.versions.isNotEmpty()) {
                animatedItem { ctx ->
                    StatsCard(
                        modifier = Modifier.animated(ctx),
                        title = stringResource(R.string.playtime_versions_title)
                    ) {
                        val maxTotal = snapshot.versions.maxOf { it.totalMillis }.coerceAtLeast(1L)
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            snapshot.versions.forEach { version ->
                                VersionPlaytimeRow(
                                    name = version.versionName,
                                    totalText = PlaytimeFormat.duration(context, version.totalMillis),
                                    detail = stringResource(
                                        R.string.playtime_version_detail,
                                        version.sessions,
                                        PlaytimeFormat.relative(context, version.lastPlayed)
                                    ),
                                    ratio = (version.totalMillis.toFloat() / maxTotal).coerceIn(0f, 1f)
                                )
                            }
                        }
                    }
                }
            }

            if (!snapshot.isEmpty) {
                animatedItem { ctx ->
                    StatsCard(
                        modifier = Modifier.animated(ctx),
                        title = stringResource(R.string.playtime_reset_title)
                    ) {
                        Button(
                            onClick = { showResetDialog = true }
                        ) {
                            Text(text = stringResource(R.string.playtime_reset))
                        }
                    }
                }
            }
        }

        if (showResetDialog) {
            SimpleAlertDialog(
                title = stringResource(R.string.playtime_reset_title),
                text = stringResource(R.string.playtime_reset_message),
                onConfirm = {
                    PlaytimeTracker.reset()
                    snapshot = PlaytimeTracker.snapshot()
                    showResetDialog = false
                    Toast.makeText(
                        context,
                        context.getString(R.string.playtime_reset_done),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = { showResetDialog = false }
            )
        }
    }
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable () -> Unit
) {
    SettingsCard(
        modifier = modifier,
        position = CardPosition.Single
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CardTitleLayout {
                Text(
                    modifier = Modifier.padding(all = 16.dp),
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun StatBlock(
    modifier: Modifier = Modifier,
    icon: Int,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * 最近七天的游玩时长柱状图
 */
@Composable
private fun WeeklyBars(
    modifier: Modifier = Modifier,
    days: List<PlaytimeTracker.DailyPlaytime>
) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("EE", Locale.getDefault()) }
    val maxMillis = days.maxOf { it.millis }.coerceAtLeast(1L)
    val barMaxHeight = 88.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val ratio = (day.millis.toFloat() / maxMillis).coerceIn(0f, 1f)
            val barHeight = if (day.millis <= 0L) 4.dp else (barMaxHeight * ratio).coerceAtLeast(8.dp)
            val label = remember(day.epochDay) {
                formatter.format(Date(TimeUnit.DAYS.toMillis(day.epochDay)))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = PlaytimeFormat.compact(context, day.millis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            if (day.millis > 0L) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VersionPlaytimeRow(
    name: String,
    totalText: String,
    detail: String,
    ratio: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Text(
                text = totalText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            progress = { ratio }
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
