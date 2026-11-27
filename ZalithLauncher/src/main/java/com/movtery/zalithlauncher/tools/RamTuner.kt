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
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.GameSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.platform.bytesToMB
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import com.movtery.zalithlauncher.utils.platform.getTotalMemory
import java.io.File

private const val TAG = "RamTuner"

/**
 * 根据设备内存与模组数量，给出内存分配与 JVM 参数建议
 */
object RamTuner {
    private const val JVM_ARGS =
        "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=16M"

    data class Recommendation(
        val deviceRamMb: Int,
        val maxRamMb: Int,
        val recommendedMb: Int,
        val modsCount: Int,
        val jvmArgs: String
    )

    fun totalModsCount(): Int =
        VersionsManager.versions.value.sumOf { version ->
            File(version.getGameDir(), "mods")
                .listFiles { file -> file.isFile && file.extension.equals("jar", true) }
                ?.size ?: 0
        }

    fun recommend(context: Context): Recommendation {
        val deviceRamMb = getTotalMemory(context).bytesToMB(0).toInt().coerceAtLeast(512)
        val maxRamMb = getMaxMemoryForSettings(context).coerceAtLeast(512)
        val modsCount = totalModsCount()

        var recommended = when {
            deviceRamMb <= 2048 -> 1024
            deviceRamMb <= 3072 -> 1536
            deviceRamMb <= 4096 -> 2048
            deviceRamMb <= 6144 -> 3072
            deviceRamMb <= 8192 -> 4096
            deviceRamMb <= 12288 -> 5120
            else -> 6144
        }

        if (modsCount >= 40) recommended += 512
        if (modsCount >= 100) recommended += 512

        recommended = (recommended / 256) * 256
        recommended = recommended.coerceIn(512, maxRamMb)

        return Recommendation(
            deviceRamMb = deviceRamMb,
            maxRamMb = maxRamMb,
            recommendedMb = recommended,
            modsCount = modsCount,
            jvmArgs = JVM_ARGS
        )
    }

    /**
     * 写入全局设置
     */
    fun applyGlobal(recommendation: Recommendation) {
        GameSettings.ramAllocation.save(recommendation.recommendedMb)
        GameSettings.jvmArgs.save(recommendation.jvmArgs)
    }

    /**
     * 为每个实例写入独立配置，返回成功写入的实例数量
     */
    fun applyToInstances(recommendation: Recommendation): Int {
        var applied = 0
        VersionsManager.versions.value.forEach { version ->
            runCatching {
                val config = VersionConfig.parseConfig(version.getVersionPath())
                config.ramAllocation = recommendation.recommendedMb
                config.jvmArgs = recommendation.jvmArgs
                config.save()
                applied++
            }.onFailure {
                Logger.warning(TAG, "Failed to apply ram tuning to ${version.getVersionName()}", it)
            }
        }
        if (applied > 0) {
            VersionsManager.refresh("RamTuner")
        }
        return applied
    }
}
