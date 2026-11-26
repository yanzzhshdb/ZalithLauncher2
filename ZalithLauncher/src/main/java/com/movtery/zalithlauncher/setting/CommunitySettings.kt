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

package com.movtery.zalithlauncher.setting

/**
 * 社区（群聊 / 语音）相关设置
 *
 * 使用自己的 Firebase 项目：应用启动时以编程方式初始化 Firebase，
 * 因此无需在打包时内置 google-services.json，用户可在应用内填写配置。
 */
object CommunitySettings : SettingsRegistry() {
    /** 是否启用社区功能 */
    val enabled = boolSetting("communityEnabled", false)
    /** 聊天中显示的昵称，留空则使用设备名 */
    val displayName = stringSetting("communityDisplayName", "")
    /** Firebase Web API Key */
    val apiKey = stringSetting("communityApiKey", "")
    /** Firebase Application ID (mobilesdk_app_id) */
    val appId = stringSetting("communityAppId", "")
    /** Firebase Project ID */
    val projectId = stringSetting("communityProjectId", "")
    /** Firebase Sender ID（可留空） */
    val senderId = stringSetting("communitySenderId", "")
    /** Firebase Storage Bucket（语音消息需要，可留空） */
    val storageBucket = stringSetting("communityStorageBucket", "")
    /** STUN 服务器 */
    val stunUrl = stringSetting("communityStunUrl", "stun:stun.l.google.com:19302")
    /** TURN 服务器（对称 NAT 下语音需要） */
    val turnUrl = stringSetting("communityTurnUrl", "")
    /** TURN 用户名 */
    val turnUsername = stringSetting("communityTurnUsername", "")
    /** TURN 密码 */
    val turnPassword = stringSetting("communityTurnPassword", "")
}
