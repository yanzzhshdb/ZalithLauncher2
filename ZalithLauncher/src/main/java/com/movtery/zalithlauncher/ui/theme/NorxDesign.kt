/*
 * NorxLauncher
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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * NorxLauncher 设计令牌。
 *
 * 与旧版（大圆角 + 液态玻璃 + 柔和渐变）刻意区分：
 * 硬朗的中等圆角、1dp 描边、纯色面板、霓虹强调线。
 */
object NorxTokens {
    /** 普通卡片圆角 */
    val CardCorner = 16.dp
    /** 小控件（标签、徽章）圆角 */
    val ChipCorner = 10.dp
    /** 设置分组卡片圆角 */
    val GroupCorner = 18.dp
    /** 分组内部卡片圆角 */
    val GroupInner = 3.dp
    /** 描边宽度 */
    val Hairline = 1.dp
    /** 强调线高度 */
    val AccentHeight = 2.dp
}

/** 面板描边颜色，暗色下更明显一点 */
@Composable
fun norxHairlineColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return scheme.outlineVariant.copy(alpha = if (isLauncherInDarkTheme()) 0.45f else 0.7f)
}

@Composable
fun norxCardBorder(width: Dp = NorxTokens.Hairline): BorderStroke {
    return BorderStroke(width, norxHairlineColor())
}

/** 品牌强调渐变：主色 → 第三色 */
@Composable
fun norxAccentBrush(): Brush = Brush.horizontalGradient(
    listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    )
)

/** 面板底色：不透明、平铺，去掉玻璃的朦胧感 */
@Composable
fun norxPanelColor(elevated: Boolean = false): Color {
    val scheme = MaterialTheme.colorScheme
    return if (elevated) scheme.surfaceContainerHigh else scheme.surfaceContainerLow
}

/** 面板上的次级块（条目、输入框底）颜色 */
@Composable
fun norxInsetColor(): Color = MaterialTheme.colorScheme.surfaceContainerHighest

@Composable
fun norxCardShape(corner: Dp = NorxTokens.CardCorner) =
    RoundedCornerShape(corner)

@Composable
fun norxChipShape() = RoundedCornerShape(NorxTokens.ChipCorner)
