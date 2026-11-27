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

import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import java.util.zip.ZipFile

private const val TAG = "InstanceTools"

/**
 * 实例（版本）维度的检查工具：体积统计、健康检查、重复模组、大文件
 */
object InstanceTools {
    data class InstanceSize(
        val versionName: String,
        val path: File,
        val bytes: Long
    )

    enum class IssueType {
        MISSING_VERSION_JSON,
        MISSING_CLIENT_JAR,
        MISSING_ASSETS,
        MISSING_JAVA_RUNTIME,
        INVALID_CONFIG,
        EMPTY_INSTANCE
    }

    data class Issue(
        val type: IssueType,
        val detail: String = ""
    )

    data class HealthReport(
        val versionName: String,
        val issues: List<Issue>
    )

    data class DuplicateGroup(
        val versionName: String,
        val modId: String,
        val files: List<File>
    )

    private fun installedVersions(): List<Version> = VersionsManager.versions.value

    /**
     * 每个实例目录占用的体积，从大到小排序
     */
    fun instanceSizes(): List<InstanceSize> =
        installedVersions()
            .map { version ->
                val path = version.getVersionPath()
                InstanceSize(
                    versionName = version.getVersionName(),
                    path = path,
                    bytes = ToolsStorage.sizeOf(path)
                )
            }
            .sortedByDescending { it.bytes }

    /**
     * 检查每个实例的关键文件是否齐全
     */
    fun healthCheck(): List<HealthReport> =
        installedVersions().map { version ->
            val issues = mutableListOf<Issue>()
            val name = version.getVersionName()
            val versionPath = version.getVersionPath()

            val json = File(versionPath, "$name.json")
            if (!json.isFile) {
                issues += Issue(IssueType.MISSING_VERSION_JSON, "$name.json")
            } else {
                val inheritsFrom = runCatching {
                    Regex("\"inheritsFrom\"\\s*:").containsMatchIn(json.readText())
                }.getOrDefault(false)
                if (!inheritsFrom && !version.getClientJar().isFile) {
                    issues += Issue(IssueType.MISSING_CLIENT_JAR, version.getClientJar().name)
                }
            }

            if (!File(version.getGameHome(), "assets").isDirectory) {
                issues += Issue(IssueType.MISSING_ASSETS, "assets")
            }

            val javaRuntime = version.getJavaRuntime()
            if (javaRuntime.isNotBlank() && !RuntimesManager.isRuntimeUsable(javaRuntime)) {
                issues += Issue(IssueType.MISSING_JAVA_RUNTIME, javaRuntime)
            }

            runCatching { VersionConfig.parseConfig(versionPath) }
                .onFailure { throwable ->
                    Logger.warning(TAG, "Failed to parse config of $name", throwable)
                    issues += Issue(IssueType.INVALID_CONFIG, "version.config")
                }

            if (versionPath.listFiles().isNullOrEmpty()) {
                issues += Issue(IssueType.EMPTY_INSTANCE, versionPath.name)
            }

            HealthReport(name, issues)
        }

    /**
     * 在同一个实例内查找重复的模组（依据模组 ID）
     */
    fun duplicateMods(): List<DuplicateGroup> {
        val result = mutableListOf<DuplicateGroup>()
        installedVersions().forEach { version ->
            val modsDir = File(version.getGameDir(), "mods")
            val jars = modsDir.listFiles { file -> file.isFile && file.extension.equals("jar", true) }
                ?: return@forEach

            val byId = mutableMapOf<String, MutableList<File>>()
            jars.forEach inner@{ jar ->
                val id = readModId(jar) ?: return@inner
                byId.getOrPut(id) { mutableListOf() }.add(jar)
            }

            byId.forEach { (id, files) ->
                if (files.size > 1) {
                    result += DuplicateGroup(
                        versionName = version.getVersionName(),
                        modId = id,
                        files = files.sortedBy { it.name }
                    )
                }
            }
        }
        return result.sortedWith(compareBy({ it.versionName }, { it.modId }))
    }

    /**
     * 读取模组 ID，识别失败时返回 null
     */
    private fun readModId(jar: File): String? = runCatching {
        ZipFile(jar).use { zip ->
            val candidates = listOf(
                "fabric.mod.json",
                "quilt.mod.json",
                "META-INF/neoforge.mods.toml",
                "META-INF/mods.toml"
            )
            var id: String? = null
            for (path in candidates) {
                val entry = zip.getEntry(path) ?: continue
                val text = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                id = if (path.endsWith(".json")) {
                    Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.getOrNull(1)
                } else {
                    Regex("modId\\s*=\\s*\"([^\"]+)\"").find(text)?.groupValues?.getOrNull(1)
                }
                if (id != null) break
            }
            id
        }
    }.getOrNull()

    /**
     * 找出游戏目录下体积最大的文件
     */
    fun bigFiles(minBytes: Long = 50L * 1024L * 1024L, limit: Int = 20): List<File> =
        runCatching {
            PathManager.DIR_GAME
                .walkTopDown()
                .maxDepth(8)
                .filter { it.isFile && it.length() >= minBytes }
                .sortedByDescending { it.length() }
                .take(limit)
                .toList()
        }.getOrElse {
            Logger.warning(TAG, "Failed to scan big files", it)
            emptyList()
        }
}
