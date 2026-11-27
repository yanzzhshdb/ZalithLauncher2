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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R

/**
 * NorxLauncher 品牌字体：几何无衬线，标题与标签全部使用，
 * 与旧版默认 Roboto 的观感彻底区分
 */
val NorxFontFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

private val base = Typography()

/**
 * 紧凑、偏技术感的字号体系：标题收字距，标签全大写放宽字距
 */
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = base.displayMedium.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp
    ),
    displaySmall = base.displaySmall.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = base.headlineLarge.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = base.headlineMedium.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = base.headlineSmall.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    titleLarge = base.titleLarge.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp
    ),
    titleMedium = base.titleMedium.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp
    ),
    titleSmall = base.titleSmall.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp
    ),
    labelLarge = base.labelLarge.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp
    ),
    labelMedium = base.labelMedium.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp
    ),
    labelSmall = base.labelSmall.copy(
        fontFamily = NorxFontFamily,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.9.sp
    ),
    bodyLarge = base.bodyLarge.copy(
        fontFamily = NorxFontFamily,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = base.bodyMedium.copy(
        fontFamily = NorxFontFamily,
        letterSpacing = 0.2.sp
    ),
    bodySmall = base.bodySmall.copy(
        fontFamily = NorxFontFamily,
        letterSpacing = 0.25.sp
    )
)
