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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme

/** 应用整体背景的颜色 */
@Composable
fun backgroundColor(): Color = MaterialTheme.colorScheme.surfaceContainer
@Composable
fun onBackgroundColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * 卡片背景颜色
 * [androidx.compose.material3.Card]
 * [com.movtery.zalithlauncher.ui.components.BackgroundCard]
 * [androidx.compose.ui.window.Dialog]
 * @param influencedByBackground 是否受背景内容影响，更改自身不透明度
 */
@Composable
fun cardColor(
    influencedByBackground: Boolean = true
): Color {
    val color = norxPanelColor()
    return if (influencedByBackground) {
        color
    } else {
        //浮层（对话框、菜单）保留一点点通透感，配合遮罩形成层次
        color.copy(alpha = 0.96f)
    }
}
@Composable
fun onCardColor(): Color = MaterialTheme.colorScheme.onSurface
/**
 * 卡片顶部Title的背景颜色，半透明的surface
 */
@Composable
fun cardTitleColor(
    alpha: Float = 0.5f
): Color = MaterialTheme.colorScheme.surface.copy(alpha = alpha)

/**
 * 卡片上的Item的背景颜色
 * @param influencedByBackground 是否受背景内容影响，更改自身不透明度
 */
@Composable
fun itemColor(
    influencedByBackground: Boolean = true,
    isDark: Boolean = isLauncherInDarkTheme()
): Color {
    return norxInsetColor()
}
@Composable
fun onItemColor() = MaterialTheme.colorScheme.onSurface