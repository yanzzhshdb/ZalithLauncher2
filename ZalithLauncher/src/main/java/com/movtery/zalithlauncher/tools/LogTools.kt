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

import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "LogTools"

/**
 * 日志文件管理：收集、统计体积、删除较旧的日志
 */
object LogTools {
    data class LogEntry(
        val file: File,
        val versionName: String?
    )

    /**
     * 所有日志：启动器日志 + 每个实例的游戏日志
     */
    fun collect(): List<LogEntry> {
        val entries = mutableListOf<LogEntry>()

        PathManager.DIR_LAUNCHER_LOGS.listFiles()
            ?.filter { it.isFile && it.length() > 0L }
            ?.forEach { entries += LogEntry(it, null) }

        VersionsManager.versions.value.forEach { version ->
            val log = version.getLatestLog()
            if (log.isFile && log.length() > 0L) {
                entries += LogEntry(log, version.getVersionName())
            }
        }

        return entries.sortedByDescending { it.file.lastModified() }
    }

    fun totalSize(): Long = collect().sumOf { it.file.length() }

    /**
     * 仅保留最新的 [keep] 个启动器日志，返回 (删除数量, 释放字节数)
     */
    fun deleteOldLauncherLogs(keep: Int): Pair<Int, Long> {
        val logs = PathManager.DIR_LAUNCHER_LOGS.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: return 0 to 0L

        var count = 0
        var freed = 0L
        logs.drop(keep.coerceAtLeast(1)).forEach { file ->
            val length = file.length()
            if (ToolsStorage.deleteFile(file)) {
                count++
                freed += length
            } else {
                Logger.warning(TAG, "Failed to delete log ${file.absolutePath}")
            }
        }
        return count to freed
    }
}
