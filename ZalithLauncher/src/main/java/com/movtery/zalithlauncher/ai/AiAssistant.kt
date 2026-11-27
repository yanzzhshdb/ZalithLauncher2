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

/**
 * 内置 AI 助手的开关
 *
 * 维护期间客户端不发送任何请求，也不保存任何密钥：真正的密钥只能放在服务端代理里。
 */
object AiAssistant {
    /** 维护开关，服务端准备就绪后再打开 */
    private const val MAINTENANCE = true

    /** 供非 UI 层复用的提示文本 */
    const val MAINTENANCE_MESSAGE =
        "Asisten AI sedang maintenance. Coba lagi nanti."

    fun isAvailable(): Boolean = !MAINTENANCE
}
