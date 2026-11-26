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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.LauncherSettings

/**
 * 更新说明弹窗，每个版本只展示一次
 */
@Composable
fun WhatsNewDialog() {
    val seenVersion = LauncherSettings.launcherWhatsNewSeen.state
    var dismissed by remember { mutableStateOf(false) }

    if (dismissed || seenVersion >= BuildConfig.VERSION_CODE) return

    fun close() {
        LauncherSettings.launcherWhatsNewSeen.save(BuildConfig.VERSION_CODE)
        dismissed = true
    }

    AlertDialog(
        onDismissRequest = { close() },
        confirmButton = {
            TextButton(onClick = { close() }) {
                Text(text = stringResource(R.string.whats_new_confirm))
            }
        },
        title = {
            Text(text = stringResource(R.string.whats_new_title))
        },
        text = {
            Text(
                text = stringResource(R.string.whats_new_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    )
}
