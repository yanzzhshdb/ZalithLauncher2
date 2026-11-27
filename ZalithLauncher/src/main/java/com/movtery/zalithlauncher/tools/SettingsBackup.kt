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

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.loadAllSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "SettingsBackup"
private val GSON = GsonBuilder().setPrettyPrinting().create()

/**
 * 启动器设置的导出与导入（只涉及设置文件，不含任何账号或密钥）
 */
object SettingsBackup {
    /**
     * 导出当前设置到指定位置，返回写入的字节数
     */
    suspend fun exportTo(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        val content = PathManager.FILE_SETTINGS.takeIf { it.isFile }?.readText() ?: "{}"
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(content.toByteArray())
            output.flush()
        } ?: throw IllegalStateException("Unable to open $uri")
        content.toByteArray().size.toLong()
    }

    /**
     * 从指定位置导入设置，返回导入的条目数量
     */
    suspend fun importFrom(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val content = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("Unable to open $uri")

        val json = runCatching { GSON.fromJson(content, JsonObject::class.java) }
            .getOrElse { throwable ->
                Logger.warning(TAG, "Invalid settings file", throwable)
                throw IllegalArgumentException("Invalid settings file")
            } ?: throw IllegalArgumentException("Invalid settings file")

        PathManager.FILE_SETTINGS.parentFile?.mkdirs()
        PathManager.FILE_SETTINGS.writeText(GSON.toJson(json))
        loadAllSettings(context, reloadAll = true)
        json.entrySet().size
    }
}
