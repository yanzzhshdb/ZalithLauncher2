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

import com.movtery.zalithlauncher.setting.SettingsRegistry
import com.movtery.zalithlauncher.setting.launcherMMKV

/**
 * 内置 AI 助手的设置
 */
object AiSettings : SettingsRegistry() {
    /** 旧版本把共享密钥存在这里，升级时清掉，客户端不再保留任何密钥 */
    private const val LEGACY_API_KEY = "aiApiKey"

    /** 发送问题时附带当前实例的版本信息与模组列表 */
    val includeMods = boolSetting("aiIncludeMods", true)

    /** 发送问题时附带日志（崩溃日志或 latest.log 摘要） */
    val includeLog = boolSetting("aiIncludeLog", true)

    /** 游戏崩溃后自动打开 AI 分析 */
    val autoAnalyzeCrash = boolSetting("aiAutoAnalyzeCrash", true)

    /** 分析崩溃时默认使用的版本名，空表示自动选择最近游玩的版本 */
    val targetVersion = stringSetting("aiTargetVersion", "")

    fun clearLegacySecrets() {
        launcherMMKV().remove(LEGACY_API_KEY)
    }
}
