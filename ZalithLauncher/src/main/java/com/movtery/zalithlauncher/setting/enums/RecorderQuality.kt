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

package com.movtery.zalithlauncher.setting.enums

import com.movtery.zalithlauncher.R

/**
 * 录屏画质档位
 * @param nameRes 名称资源
 * @param scale 相对于屏幕分辨率的长边缩放
 * @param bitsPerPixel 每像素每帧的编码比特数，越大画质越好
 */
enum class RecorderQuality(
    val nameRes: Int,
    val scale: Float,
    val bitsPerPixel: Float
) {
    /** 文件最小，适合长时间录制 */
    COMPACT(R.string.recorder_quality_compact, 0.5f, 0.06f),
    /** 清晰度与体积的平衡 */
    BALANCED(R.string.recorder_quality_balanced, 0.75f, 0.08f),
    /** 原生分辨率，画面最清晰 */
    SHARP(R.string.recorder_quality_sharp, 1f, 0.12f)
}
