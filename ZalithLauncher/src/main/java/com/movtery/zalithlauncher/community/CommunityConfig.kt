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

package com.movtery.zalithlauncher.community

import com.movtery.zalithlauncher.setting.CommunitySettings

/**
 * 用户填写的 Firebase / 网络配置
 */
data class CommunityConfig(
    val apiKey: String = "",
    val appId: String = "",
    val projectId: String = "",
    val senderId: String = "",
    val storageBucket: String = "",
    val stunUrl: String = "",
    val turnUrl: String = "",
    val turnUsername: String = "",
    val turnPassword: String = ""
) {
    /** Firebase 初始化至少需要这三项 */
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && appId.isNotBlank() && projectId.isNotBlank()

    val hasStorage: Boolean
        get() = storageBucket.isNotBlank()

    companion object {
        fun fromSettings(): CommunityConfig = CommunityConfig(
            apiKey = CommunitySettings.apiKey.getValue().trim(),
            appId = CommunitySettings.appId.getValue().trim(),
            projectId = CommunitySettings.projectId.getValue().trim(),
            senderId = CommunitySettings.senderId.getValue().trim(),
            storageBucket = CommunitySettings.storageBucket.getValue().trim(),
            stunUrl = CommunitySettings.stunUrl.getValue().trim(),
            turnUrl = CommunitySettings.turnUrl.getValue().trim(),
            turnUsername = CommunitySettings.turnUsername.getValue().trim(),
            turnPassword = CommunitySettings.turnPassword.getValue().trim()
        )
    }
}
