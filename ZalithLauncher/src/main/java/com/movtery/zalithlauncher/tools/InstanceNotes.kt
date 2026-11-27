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

package com.movtery.zalithlauncher.tools

import com.movtery.zalithlauncher.setting.launcherMMKV

/**
 * 实例备注：给每个版本写一句自己的说明，保存在启动器本地
 */
object InstanceNotes {
    private const val PREFIX = "norx_instance_note_"

    fun noteOf(versionName: String): String =
        launcherMMKV().getString(PREFIX + versionName, "").orEmpty()

    fun save(versionName: String, note: String) {
        val trimmed = note.trim()
        if (trimmed.isEmpty()) {
            launcherMMKV().remove(PREFIX + versionName)
        } else {
            launcherMMKV().putString(PREFIX + versionName, trimmed).apply()
        }
    }

    fun hasNote(versionName: String): Boolean = noteOf(versionName).isNotBlank()
}
