/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.RendererSettings
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn

private enum class PerformancePreset(
    val resolutionPercent: Int,
    val sustainedPerformance: Boolean,
    val titleRes: Int,
    val descriptionRes: Int
) {
    BATTERY_SAVER(50, false, R.string.nlauncher_preset_battery, R.string.nlauncher_preset_battery_summary),
    BALANCED(75, true, R.string.nlauncher_preset_balanced, R.string.nlauncher_preset_balanced_summary),
    VISUAL_QUALITY(100, true, R.string.nlauncher_preset_quality, R.string.nlauncher_preset_quality_summary)
}

@Composable
fun PerformancePresets(modifier: Modifier = Modifier) {
    val resolution = RendererSettings.resolutionRatio.state
    val sustained = RendererSettings.sustainedPerformance.state
    val selectedPreset = remember(resolution, sustained) {
        PerformancePreset.entries.firstOrNull {
            it.resolutionPercent == resolution && it.sustainedPerformance == sustained
        }
    }

    SettingsCardColumn(modifier = modifier) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            text = stringResource(R.string.nlauncher_performance_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            text = stringResource(R.string.nlauncher_performance_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PerformancePreset.entries.forEachIndexed { index, preset ->
                SettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = when {
                        PerformancePreset.entries.size == 1 -> CardPosition.Single
                        index == 0 -> CardPosition.Top
                        index == PerformancePreset.entries.lastIndex -> CardPosition.Bottom
                        else -> CardPosition.Middle
                    },
                    title = stringResource(preset.titleRes),
                    summary = stringResource(preset.descriptionRes),
                    onClick = {
                        RendererSettings.resolutionRatio.save(preset.resolutionPercent)
                        RendererSettings.sustainedPerformance.save(preset.sustainedPerformance)
                    }
                ) {
                    if (selectedPreset == preset) {
                        Text(
                            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                            text = stringResource(R.string.nlauncher_preset_selected),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
