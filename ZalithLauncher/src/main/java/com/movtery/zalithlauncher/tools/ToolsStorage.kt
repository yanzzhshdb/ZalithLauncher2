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

import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import java.util.Locale

private const val TAG = "ToolsStorage"

/**
 * 存储相关的小工具：统计体积、清理缓存目录
 */
object ToolsStorage {
    /**
     * 统计文件或目录占用的字节数
     */
    fun sizeOf(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * 清空指定目录（保留目录本身），返回释放的字节数
     */
    fun clearDirs(dirs: List<File?>): Long {
        var freed = 0L
        dirs.filterNotNull().forEach { dir ->
            if (!dir.exists()) return@forEach
            if (dir.isFile) {
                val length = dir.length()
                if (dir.delete()) freed += length
                return@forEach
            }
            dir.listFiles()?.forEach { child ->
                val length = sizeOf(child)
                if (child.deleteRecursively()) {
                    freed += length
                } else {
                    Logger.warning(TAG, "Failed to delete ${child.absolutePath}")
                }
            }
        }
        return freed
    }

    fun deleteFile(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L ->
            String.format(Locale.US, "%.2f GB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L ->
            String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024.0 * 1024.0))
        bytes >= 1024L ->
            String.format(Locale.US, "%.0f KB", bytes.toDouble() / 1024.0)
        else -> "$bytes B"
    }
}
