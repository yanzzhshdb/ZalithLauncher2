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

package com.movtery.zalithlauncher.game.performance

import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.RendererSettings
import com.movtery.zalithlauncher.setting.enums.ResolutionRule
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "PerformanceOptimizer"
private const val OPTIONS_FILE = "options.txt"
private const val BACKUP_SUFFIX = ".nlbackup"

/**
 * 把 [PerformancePreset] 应用到指定版本
 *
 * 只修改 options.txt 中预设覆盖到的键，并在首次修改前保留一份原始备份。
 */
object PerformanceOptimizer {
    data class Result(
        val applied: Boolean,
        val changedKeys: Int,
        val backupCreated: Boolean,
        val optionsMissing: Boolean = false
    )

    private fun optionsFile(version: Version) = File(version.getGameDir(), OPTIONS_FILE)

    private fun backupFile(version: Version) = File(version.getGameDir(), OPTIONS_FILE + BACKUP_SUFFIX)

    fun hasBackup(version: Version): Boolean = backupFile(version).isFile

    /**
     * 应用预设
     * @param applyLauncherSettings 是否同时调整启动器侧的渲染设置
     */
    fun apply(
        version: Version,
        preset: PerformancePreset,
        applyLauncherSettings: Boolean = true
    ): Result {
        val options = optionsFile(version)
        if (!options.isFile) {
            Logger.warning(TAG, "options.txt not found for ${version.getVersionName()}")
            return Result(applied = false, changedKeys = 0, backupCreated = false, optionsMissing = true)
        }

        val backup = backupFile(version)
        var backupCreated = false
        if (!backup.isFile) {
            runCatching { options.copyTo(backup, overwrite = true) }
                .onSuccess { backupCreated = true }
                .onFailure { Logger.warning(TAG, "Failed to back up options.txt", it) }
        }

        val changed = runCatching { patch(options, preset.options) }.getOrElse {
            Logger.error(TAG, "Failed to patch options.txt", it)
            return Result(applied = false, changedKeys = 0, backupCreated = backupCreated)
        }

        if (applyLauncherSettings) {
            RendererSettings.sustainedPerformance.save(preset.sustainedPerformance)
            RendererSettings.requestMaxRefreshRate.save(true)
            if (RendererSettings.resolutionRule.getValue() == ResolutionRule.PERCENTAGE) {
                RendererSettings.resolutionRatio.save(preset.resolutionPercent)
            }
        }

        return Result(applied = true, changedKeys = changed, backupCreated = backupCreated)
    }

    /**
     * 恢复首次优化前的 options.txt
     */
    fun restore(version: Version): Boolean {
        val options = optionsFile(version)
        val backup = backupFile(version)
        if (!backup.isFile) return false
        return runCatching {
            backup.copyTo(options, overwrite = true)
            true
        }.getOrElse {
            Logger.error(TAG, "Failed to restore options.txt", it)
            false
        }
    }

    private fun patch(options: File, values: Map<String, String>): Int {
        val lines = options.readLines()
        val seenKeys = mutableSetOf<String>()
        var changed = 0

        val patched = lines.map { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@map line
            val key = line.substring(0, separator)
            val newValue = values[key] ?: return@map line
            seenKeys += key
            if (line.substring(separator + 1) == newValue) {
                line
            } else {
                changed++
                "$key:$newValue"
            }
        }.toMutableList()

        values.forEach { (key, value) ->
            if (key !in seenKeys) {
                patched += "$key:$value"
                changed++
            }
        }

        val temp = File(options.parentFile, "$OPTIONS_FILE.tmp")
        temp.writeText(patched.joinToString("\n"))
        if (!temp.renameTo(options)) {
            temp.copyTo(options, overwrite = true)
            temp.delete()
        }
        return changed
    }
}
