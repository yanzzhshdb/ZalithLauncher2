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

package com.movtery.zalithlauncher.upgrade

import com.movtery.zalithlauncher.path.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.safeBodyAsJson
import com.movtery.zalithlauncher.utils.network.withRetry
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "LauncherReleaseChecker"

/**
 * 版本更新通知的检查来源：NLauncher 仓库的正式 Release
 */
object LauncherReleaseChecker {
    private const val REPOSITORY = "yanzzhshdb/ZalithLauncher2"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$REPOSITORY/releases/latest"

    /** 未能获取到 Release 链接时的兜底地址 */
    const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases"

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String = "",
        @SerialName("name") val name: String? = null,
        @SerialName("body") val body: String? = null,
        @SerialName("html_url") val htmlUrl: String = "",
        @SerialName("published_at") val publishedAt: String? = null
    )

    /**
     * @param tag 原始标签，例如 v1.2.0
     * @param version 去除前缀后的版本名，例如 1.2.0
     * @param notes 更新日志
     * @param url Release 页面
     */
    data class LatestRelease(
        val tag: String,
        val version: String,
        val notes: String?,
        val url: String
    )

    /**
     * 拉取最新的 Release，网络异常或无 Release 时返回 null
     */
    suspend fun fetchLatest(): LatestRelease? = withContext(Dispatchers.IO) {
        runCatching {
            withRetry(logTag = TAG, maxRetries = 2) {
                val release = GLOBAL_CLIENT.get(LATEST_RELEASE_API).safeBodyAsJson<GitHubRelease>()
                val version = normalizeVersion(release.tagName)
                if (version.isBlank()) {
                    null
                } else {
                    LatestRelease(
                        tag = release.tagName,
                        version = version,
                        notes = release.body?.takeIf { it.isNotBlank() },
                        url = release.htmlUrl.ifBlank { RELEASES_PAGE }
                    )
                }
            }
        }.getOrElse { e ->
            Logger.info(TAG, "No release information available: ${e.message}")
            null
        }
    }

    /**
     * 判断远端版本是否比当前版本更新
     */
    fun isNewer(latest: String, current: String): Boolean {
        val remote = parseVersion(latest) ?: return false
        val local = parseVersion(current) ?: return false
        for (index in 0 until maxOf(remote.size, local.size)) {
            val remotePart = remote.getOrElse(index) { 0 }
            val localPart = local.getOrElse(index) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }
        return false
    }

    private fun parseVersion(raw: String): List<Int>? {
        val version = normalizeVersion(raw)
        if (version.isBlank()) return null
        val parts = version.split('.')
        val numbers = parts.map { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull() ?: return null
        }
        return numbers.takeIf { it.isNotEmpty() }
    }

    private fun normalizeVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").trim()
}
