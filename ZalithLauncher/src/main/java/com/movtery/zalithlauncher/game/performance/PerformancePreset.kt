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

import com.movtery.zalithlauncher.R

/**
 * 性能预设
 *
 * 每个预设同时控制游戏内的 options.txt 关键项与启动器侧的渲染设置，
 * 目标是在保持画面观感的前提下减少卡顿。
 */
enum class PerformancePreset(
    val nameRes: Int,
    val summaryRes: Int,
    /** 启动器渲染分辨率百分比 */
    val resolutionPercent: Int,
    /** 是否开启系统的持续性能模式 */
    val sustainedPerformance: Boolean,
    /** 写入 options.txt 的键值 */
    val options: Map<String, String>
) {
    /** 平衡：图形基本不动，只削减开销 */
    BALANCED(
        nameRes = R.string.performance_preset_balanced,
        summaryRes = R.string.performance_preset_balanced_summary,
        resolutionPercent = 85,
        sustainedPerformance = true,
        options = mapOf(
            "renderDistance" to "8",
            "simulationDistance" to "6",
            "entityDistanceScaling" to "0.75",
            "maxFps" to "60",
            "enableVsync" to "false",
            "particles" to "decreased",
            "clouds" to "fast",
            "renderClouds" to "true",
            "entityShadows" to "false",
            "ao" to "true",
            "mipmapLevels" to "3",
            "biomeBlendRadius" to "2",
            "graphicsMode" to "fancy",
            "fancyGraphics" to "true"
        )
    ),

    /** 性能：优先帧率，画面仍保持可看 */
    PERFORMANCE(
        nameRes = R.string.performance_preset_performance,
        summaryRes = R.string.performance_preset_performance_summary,
        resolutionPercent = 75,
        sustainedPerformance = true,
        options = mapOf(
            "renderDistance" to "6",
            "simulationDistance" to "5",
            "entityDistanceScaling" to "0.5",
            "maxFps" to "60",
            "enableVsync" to "false",
            "particles" to "minimal",
            "clouds" to "off",
            "renderClouds" to "false",
            "entityShadows" to "false",
            "ao" to "false",
            "mipmapLevels" to "2",
            "biomeBlendRadius" to "2",
            "graphicsMode" to "fast",
            "fancyGraphics" to "false"
        )
    ),

    /** 画质：堆满细节，交给高性能设备 */
    QUALITY(
        nameRes = R.string.performance_preset_quality,
        summaryRes = R.string.performance_preset_quality_summary,
        resolutionPercent = 100,
        sustainedPerformance = false,
        options = mapOf(
            "renderDistance" to "12",
            "simulationDistance" to "10",
            "entityDistanceScaling" to "1.0",
            "maxFps" to "120",
            "enableVsync" to "false",
            "particles" to "all",
            "clouds" to "fancy",
            "renderClouds" to "true",
            "entityShadows" to "true",
            "ao" to "true",
            "mipmapLevels" to "4",
            "biomeBlendRadius" to "5",
            "graphicsMode" to "fancy",
            "fancyGraphics" to "true"
        )
    )
}
