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

package com.movtery.zalithlauncher.ai

import com.movtery.zalithlauncher.game.session.PlaytimeTracker
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.RendererSettings
import java.io.File

/**
 * 把本地实例信息、模组列表与日志摘要整理成 AI 能看懂的上下文
 */
object AiContextBuilder {
    private const val MAX_LOG_DIGEST = 900
    private const val MAX_MODS = 20
    private const val MAX_READ_BYTES = 512 * 1024L

    private val INTERESTING_LOG_KEYWORDS = listOf(
        "exception", "error", "caused by", "fatal", "crash", "failed", "denied",
        "unsupported", "mixin", "mod ", "at net.", "at com.", "at java.", "-- "
    )

    /**
     * 找到要分析的版本：优先用设置里选的，其次用最近游玩的版本
     */
    fun resolveVersion(versionName: String? = null): Version? {
        val versions = VersionsManager.versions.value
        val wanted = versionName?.takeIf { it.isNotBlank() }
            ?: AiSettings.targetVersion.getValue().takeIf { it.isNotBlank() }
        wanted?.let { name ->
            versions.firstOrNull { it.getVersionName() == name }?.let { return it }
        }
        val lastPlayed = PlaytimeTracker.snapshot().latest?.versionName
        return lastPlayed?.let { name -> versions.firstOrNull { it.getVersionName() == name } }
            ?: versions.firstOrNull()
    }

    /**
     * 版本信息 + 模组列表，让 AI 知道我们跑的是什么环境
     */
    fun versionSummary(version: Version?): String {
        if (version == null) return "Tidak ada instance terpasang yang bisa dibaca."

        val info = version.getVersionInfo()
        val loader = info.primaryLoader?.let { "${it.loader.name} ${it.version}" } ?: "Vanilla"
        val builder = StringBuilder()
        builder.appendLine("Instance: ${version.getVersionName()}")
        builder.appendLine("Versi Minecraft: ${info.minecraftVersion}")
        builder.appendLine("Loader: $loader")
        builder.appendLine("Renderer: ${version.getRenderer()}")
        builder.appendLine("RAM dialokasikan: ${version.getRamAllocation()} MB")
        builder.appendLine("Java runtime: ${version.getJavaRuntime()}")
        builder.appendLine("Folder game: ${version.getGameDir().absolutePath}")
        builder.appendLine("Mode performa berkelanjutan: ${RendererSettings.sustainedPerformance.getValue()}")
        builder.appendLine("Skala resolusi: ${RendererSettings.resolutionRule.getValue()} ${RendererSettings.resolutionRatio.getValue()}%")

        if (AiSettings.includeMods.getValue()) {
            val mods = listMods(version)
            if (mods.isEmpty()) {
                builder.appendLine("Mod: tidak ada mod terpasang")
            } else {
                builder.appendLine("Mod (${mods.size}):")
                mods.forEach { builder.appendLine("- $it") }
            }
        }
        return builder.toString().trim()
    }

    fun listMods(version: Version): List<String> {
        val dir = File(version.getGameDir(), "mods")
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".jar", true) } ?: return emptyList()
        return files.map { it.name.take(48) }.sorted().take(MAX_MODS)
    }

    /**
     * 从日志里挑出与崩溃相关的行，避免把整份日志塞进 URL
     */
    fun crashDigest(logFile: File?): String {
        val file = logFile?.takeIf { it.exists() && it.isFile } ?: return ""
        val text = runCatching {
            file.bufferedReader().use { reader ->
                val chars = CharArray(MAX_READ_BYTES.toInt())
                val read = reader.read(chars)
                if (read <= 0) "" else String(chars, 0, read)
            }
        }.getOrElse { return "" }

        val lines = text.lines()
        val picked = lines.filter { line ->
            val lower = line.lowercase()
            INTERESTING_LOG_KEYWORDS.any { lower.contains(it) }
        }
        val source = if (picked.size >= 8) picked else lines.takeLast(120)

        val builder = StringBuilder()
        for (line in source.takeLast(60)) {
            if (builder.length + line.length > MAX_LOG_DIGEST) break
            builder.appendLine(line.take(200))
        }
        return builder.toString().trim()
    }

    /**
     * 崩溃分析提示词
     */
    fun crashPrompt(
        version: Version?,
        logFile: File?,
        crashMessage: String? = null,
        extraNote: String? = null
    ): String {
        val builder = StringBuilder()
        builder.appendLine("Kamu adalah teknisi Minecraft Java Edition di Android yang berpengalaman.")
        builder.appendLine("Analisis crash berikut, lalu jawab dalam Bahasa Indonesia dengan format:")
        builder.appendLine("1. Penyebab utama (ringkas)")
        builder.appendLine("2. Mod/komponen yang bermasalah (sebut nama filenya kalau ada)")
        builder.appendLine("3. Langkah perbaikan konkret dan berurutan")
        builder.appendLine("4. Pencegahan supaya tidak terulang")
        builder.appendLine("Jangan mengarang mod yang tidak ada di daftar. Kalau data kurang, sebutkan data apa yang perlu dikirim.")
        builder.appendLine()
        builder.appendLine("== Info instance ==")
        builder.appendLine(versionSummary(version))
        if (!crashMessage.isNullOrBlank()) {
            builder.appendLine()
            builder.appendLine("== Pesan crash ==")
            builder.appendLine(crashMessage.take(400))
        }
        if (!extraNote.isNullOrBlank()) {
            builder.appendLine()
            builder.appendLine("== Catatan pengguna ==")
            builder.appendLine(extraNote.take(400))
        }
        if (AiSettings.includeLog.getValue()) {
            val digest = crashDigest(logFile)
            if (digest.isNotBlank()) {
                builder.appendLine()
                builder.appendLine("== Potongan log ==")
                builder.appendLine(digest)
            }
        }
        return builder.toString()
    }

    /**
     * 普通聊天提示词，可选带上实例上下文与最近几轮对话
     */
    fun chatPrompt(
        userText: String,
        version: Version?,
        history: List<AiMessage> = emptyList(),
        includeContext: Boolean = true
    ): String {
        val builder = StringBuilder()
        builder.appendLine("Kamu adalah asisten di dalam launcher Minecraft Android bernama NLauncher (NorxLauncher).")
        builder.appendLine("Jawab dalam Bahasa Indonesia yang santai tapi jelas, singkat dan langsung ke solusi.")
        builder.appendLine("Kamu paham mod, loader (Forge/NeoForge/Fabric/OptiFine), renderer (Pojav/ANGLE/Vulkan), dan optimasi FPS di Android.")

        if (includeContext) {
            builder.appendLine()
            builder.appendLine("== Info instance ==")
            builder.appendLine(versionSummary(version))
        }

        val recent = history.takeLast(4).filter { it.role != AiMessage.Role.SYSTEM }
        if (recent.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("== Percakapan sebelumnya ==")
            recent.forEach { message ->
                val speaker = if (message.role == AiMessage.Role.USER) "Pengguna" else "AI"
                builder.appendLine("$speaker: ${message.text.take(220)}")
            }
        }

        builder.appendLine()
        builder.appendLine("== Pertanyaan pengguna ==")
        builder.appendLine(userText.take(700))
        return builder.toString()
    }

    /**
     * 启动器自身的崩溃报告路径
     */
    fun launcherCrashReport(): File = PathManager.FILE_CRASH_REPORT
}
