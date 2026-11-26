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

import com.movtery.zalithlauncher.setting.enums.RecorderFrameRate
import com.movtery.zalithlauncher.setting.enums.RecorderQuality

/**
 * 内置录屏设置
 */
object RecorderSettings : SettingsRegistry() {
    /**
     * 录屏画质
     */
    val quality = enumSetting("recorderQuality", RecorderQuality.BALANCED)

    /**
     * 录屏帧率
     */
    val frameRate = enumSetting("recorderFrameRate", RecorderFrameRate.FPS_30)

    /**
     * 同时录制麦克风声音
     */
    val audio = boolSetting("recorderAudio", false)
}
