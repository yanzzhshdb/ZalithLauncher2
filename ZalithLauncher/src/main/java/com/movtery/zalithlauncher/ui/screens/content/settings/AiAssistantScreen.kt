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

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ai.AiContextBuilder
import com.movtery.zalithlauncher.ai.AiSettings
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.activities.showAiChat
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
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.TextInputSettingsCard

/**
 * AI 助手设置页面：密钥、上下文开关与快捷入口
 */
@Composable
fun AiAssistantScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Ai, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current
        val versions by VersionsManager.versions.collectAsStateWithLifecycle()

        AnimatedColumn(
            modifier = Modifier
                .verticalScrollWithBar(rememberScrollState())
                .fillMaxWidth(),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                SettingsCard(
                    modifier = Modifier.animated(ctx),
                    position = CardPosition.Single
                ) {
                    Text(
                        modifier = Modifier.padding(all = 16.dp),
                        text = stringResource(R.string.ai_settings_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier.animated(ctx)
                ) {
                    TextInputSettingsCard(
                        unit = AiSettings.apiKey,
                        title = stringResource(R.string.ai_api_key_title),
                        summary = stringResource(R.string.ai_api_key_summary),
                        position = CardPosition.Single
                    )

                    SwitchSettingsCard(
                        checked = AiSettings.includeMods.state,
                        onCheckedChange = { AiSettings.includeMods.save(it) },
                        title = stringResource(R.string.ai_setting_context_title),
                        summary = stringResource(R.string.ai_setting_context_summary),
                        position = CardPosition.Single
                    )

                    SwitchSettingsCard(
                        checked = AiSettings.includeLog.state,
                        onCheckedChange = { AiSettings.includeLog.save(it) },
                        title = stringResource(R.string.ai_setting_log_title),
                        summary = stringResource(R.string.ai_setting_log_summary),
                        position = CardPosition.Single
                    )

                    SwitchSettingsCard(
                        checked = AiSettings.autoAnalyzeCrash.state,
                        onCheckedChange = { AiSettings.autoAnalyzeCrash.save(it) },
                        title = stringResource(R.string.ai_setting_auto_crash_title),
                        summary = stringResource(R.string.ai_setting_auto_crash_summary),
                        position = CardPosition.Single
                    )

                    ListSettingsCard(
                        unit = AiSettings.targetVersion,
                        items = listOf("") + versions.map { it.getVersionName() },
                        title = stringResource(R.string.ai_setting_version_title),
                        summary = stringResource(R.string.ai_setting_version_summary),
                        position = CardPosition.Single,
                        getItemText = {
                            if (it.isBlank()) stringResource(R.string.ai_setting_version_auto) else it
                        },
                        getItemId = { it }
                    )
                }
            }

            AnimatedItem { ctx ->
                SettingsCard(
                    modifier = Modifier.animated(ctx),
                    position = CardPosition.Single
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                showAiChat(
                                    context = context,
                                    versionName = AiContextBuilder.resolveVersion()?.getVersionName()
                                )
                            }
                        ) {
                            Text(text = stringResource(R.string.ai_open_chat))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val version = AiContextBuilder.resolveVersion()
                                showAiChat(
                                    context = context,
                                    versionName = version?.getVersionName(),
                                    crashLogPath = version?.getLatestLog()?.absolutePath
                                        ?: AiContextBuilder.launcherCrashReport().absolutePath,
                                    autoAnalyze = true
                                )
                            }
                        ) {
                            Text(text = stringResource(R.string.ai_analyze_crash))
                        }
                    }
                }
            }
        }
    }
}
