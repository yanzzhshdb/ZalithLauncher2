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
import android.os.Build
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.zipDirectory
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.platform.bytesToMB
import com.movtery.zalithlauncher.utils.platform.getFreeMemory
import com.movtery.zalithlauncher.utils.platform.getTotalMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ReportBundler"

/**
 * 把设备信息、启动器日志和实例日志打包成一个 zip，方便反馈问题
 */
object ReportBundler {
    private const val MAX_LAUNCHER_LOGS = 3

    suspend fun create(context: Context): File = withContext(Dispatchers.IO) {
        val stage = File(PathManager.DIR_CACHE, "report_stage")
        stage.deleteRecursively()
        if (!stage.mkdirs()) {
            throw IllegalStateException("Failed to create ${stage.absolutePath}")
        }

        try {
            File(stage, "device-info.txt").writeText(buildDeviceInfo(context))

            val logsDir = File(stage, "logs").apply { mkdirs() }
            PathManager.DIR_LAUNCHER_LOGS.listFiles()
                ?.filter { it.isFile && it.length() > 0L }
                ?.sortedByDescending { it.lastModified() }
                ?.take(MAX_LAUNCHER_LOGS)
                ?.forEach { log ->
                    runCatching { log.copyTo(File(logsDir, log.name), overwrite = true) }
                        .onFailure { Logger.warning(TAG, "Failed to copy ${log.absolutePath}", it) }
                }

            VersionsManager.versions.value.forEach { version ->
                val log = version.getLatestLog()
                if (log.isFile && log.length() > 0L) {
                    val targetDir = File(stage, "instances/${version.getVersionName()}").apply { mkdirs() }
                    runCatching { log.copyTo(File(targetDir, log.name), overwrite = true) }
                        .onFailure { Logger.warning(TAG, "Failed to copy ${log.absolutePath}", it) }
                }
            }

            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val zip = File(PathManager.DIR_CACHE, "NorxLauncher-report-$stamp.zip")
            zipDirectory(sourceDir = stage, outputZipFile = zip)
            zip
        } finally {
            stage.deleteRecursively()
        }
    }

    private fun buildDeviceInfo(context: Context): String = buildString {
        appendLine("NorxLauncher report")
        appendLine("generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        appendLine()
        appendLine("launcher version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("launcher arch: ${BuildKeys.BUILD_ARCH}")
        appendLine("android: ${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT})")
        appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("abis: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("cpu cores: ${Runtime.getRuntime().availableProcessors()}")
        appendLine(
            "ram: total ${getTotalMemory(context).bytesToMB(0)} MB / free ${getFreeMemory(context).bytesToMB(0)} MB"
        )
        appendLine(
            "storage: free ${ToolsStorage.formatSize(context.filesDir.usableSpace)} / " +
                "total ${ToolsStorage.formatSize(context.filesDir.totalSpace)}"
        )
        appendLine("game dir: ${PathManager.DIR_GAME.absolutePath}")
        appendLine()
        appendLine("java runtimes:")
        RuntimesManager.getRuntimes().forEach { runtime ->
            appendLine("  - ${runtime.name} (${runtime.versionString ?: "unknown"})")
        }
        appendLine()
        appendLine("instances:")
        InstanceTools.instanceSizes().forEach { size ->
            appendLine("  - ${size.versionName}: ${ToolsStorage.formatSize(size.bytes)}")
        }
    }
}
