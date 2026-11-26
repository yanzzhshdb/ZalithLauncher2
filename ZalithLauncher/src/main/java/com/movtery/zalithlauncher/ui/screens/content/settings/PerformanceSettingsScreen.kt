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

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.performance.PerformanceOptimizer
import com.movtery.zalithlauncher.game.performance.PerformancePreset
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.PerformanceSettings
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 性能优化页面：一键把游戏内画质选项与启动器渲染设置调整到预设档位
 */
@Composable
fun PerformanceSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Performance, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val versions by VersionsManager.versions.collectAsStateWithLifecycle()

        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                SettingsCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx),
                    position = CardPosition.Single
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_tab_performance),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.performance_intro),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.performance_launcher_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (versions.isEmpty()) {
                AnimatedItem { ctx ->
                    SettingsCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animated(ctx),
                        position = CardPosition.Single
                    ) {
                        Text(
                            modifier = Modifier.padding(all = 16.dp),
                            text = stringResource(R.string.performance_no_version),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                AnimatedItem { ctx ->
                    SettingsCardColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animated(ctx)
                    ) {
                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top,
                            unit = PerformanceSettings.targetVersion,
                            items = versions,
                            title = stringResource(R.string.performance_target_version_title),
                            summary = stringResource(R.string.performance_target_version_summary),
                            getItemText = { it.getVersionName() },
                            getItemId = { it.getVersionName() }
                        )

                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Bottom,
                            unit = PerformanceSettings.preset,
                            items = PerformancePreset.entries,
                            title = stringResource(R.string.performance_preset_title),
                            summary = stringResource(PerformanceSettings.preset.state.summaryRes),
                            getItemText = { stringResource(it.nameRes) },
                            getItemSummary = {
                                Text(
                                    text = stringResource(it.summaryRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                AnimatedItem { ctx ->
                    SettingsCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animated(ctx),
                        position = CardPosition.Single
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { applyPreset(context, versions, scope) }
                            ) {
                                Text(text = stringResource(R.string.performance_apply))
                            }

                            OutlinedButton(
                                onClick = { restoreBackup(context, versions, scope) }
                            ) {
                                Text(text = stringResource(R.string.performance_restore))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findTargetVersion(versions: List<Version>): Version? {
    val targetName = PerformanceSettings.targetVersion.getValue()
    return versions.firstOrNull { it.getVersionName() == targetName }
}

private fun applyPreset(context: Context, versions: List<Version>, scope: CoroutineScope) {
    val version = findTargetVersion(versions) ?: return
    val preset = PerformanceSettings.preset.getValue()
    scope.launch {
        val result = withContext(Dispatchers.IO) {
            PerformanceOptimizer.apply(version, preset)
        }
        val message = when {
            result.optionsMissing -> context.getString(R.string.performance_no_options)
            result.applied -> context.getString(R.string.performance_applied, result.changedKeys)
            else -> context.getString(R.string.performance_apply_failed)
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

private fun restoreBackup(context: Context, versions: List<Version>, scope: CoroutineScope) {
    val version = findTargetVersion(versions) ?: return
    scope.launch {
        val restored = withContext(Dispatchers.IO) {
            PerformanceOptimizer.restore(version)
        }
        val message = if (restored) {
            context.getString(R.string.performance_restored)
        } else {
            context.getString(R.string.performance_no_backup)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
