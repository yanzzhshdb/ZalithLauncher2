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

package com.movtery.zalithlauncher.ui.theme.glass

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.GlassSettings
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import kotlin.math.roundToInt

/**
 * 玻璃层的厚度，越厚越不透明、高光越强
 */
enum class GlassLayer {
    /** 薄玻璃，用于按钮、标签等小元件 */
    Thin,
    /** 标准玻璃，用于卡片、面板 */
    Regular,
    /** 厚玻璃，用于对话框等需要保证可读性的悬浮层 */
    Thick
}

/**
 * 液态玻璃的完整视觉参数
 * @param tint 玻璃底色
 * @param tintAlpha 底色不透明度
 * @param highlightColor 高光颜色
 * @param highlightAlpha 镜面高光强度
 * @param edgeAlpha 边缘高光圈强度
 * @param innerGlowAlpha 底部内反射强度
 * @param shadowAlpha 投影强度
 * @param shadowElevation 投影高度
 * @param blurRadius 背景采样模糊半径（0 表示不采样背景）
 * @param specular 是否绘制镜面高光
 */
@Immutable
data class GlassSpec(
    val tint: Color,
    val tintAlpha: Float,
    val highlightColor: Color,
    val highlightAlpha: Float,
    val edgeAlpha: Float,
    val innerGlowAlpha: Float,
    val shadowAlpha: Float,
    val shadowElevation: Dp,
    val blurRadius: Int,
    val specular: Boolean
)

/**
 * 根据当前主题与用户设置计算玻璃参数
 */
@Composable
fun rememberGlassSpec(layer: GlassLayer = GlassLayer.Regular): GlassSpec {
    val dark = isLauncherInDarkTheme()
    val intensity = (GlassSettings.intensity.state / 100f).coerceIn(0f, 1f)
    val specular = GlassSettings.specular.state
    val blur = LauncherSettings.backgroundBlur.state

    val context = LocalContext.current
    val powerSaverEnabled = LauncherSettings.launcherPowerSaver.state
    //省电模式（系统或启动器设置）下自动跳过背景采样，避免持续的高斯模糊开销
    val powerSave = remember(context, powerSaverEnabled) {
        powerSaverEnabled || context.powerSaveModeEnabled()
    }

    return remember(dark, intensity, specular, blur, powerSave, layer) {
        buildGlassSpec(
            dark = dark,
            intensity = intensity,
            specular = specular,
            blur = blur,
            powerSave = powerSave,
            layer = layer
        )
    }
}

private fun buildGlassSpec(
    dark: Boolean,
    intensity: Float,
    specular: Boolean,
    blur: Int,
    powerSave: Boolean,
    layer: GlassLayer
): GlassSpec {
    val layerScale = when (layer) {
        GlassLayer.Thin -> 0.62f
        GlassLayer.Regular -> 1f
        GlassLayer.Thick -> 1.5f
    }

    //深浅色主题都用白色提亮，让玻璃面板始终比背景更亮，形成悬浮层次
    val tintBase = if (dark) 0.06f else 0.13f
    val tintRange = if (dark) 0.10f else 0.24f
    val tintAlpha = ((tintBase + tintRange * intensity) * layerScale).coerceIn(0.03f, 0.92f)

    val highlightBase = if (dark) 0.10f else 0.30f
    val highlightAlpha = if (specular) {
        (highlightBase * (0.35f + 0.65f * intensity) * layerScale).coerceIn(0f, 0.95f)
    } else 0f

    return GlassSpec(
        tint = Color.White,
        tintAlpha = tintAlpha,
        highlightColor = Color.White,
        highlightAlpha = highlightAlpha,
        edgeAlpha = (if (dark) 0.34f else 0.62f) * (0.45f + 0.55f * intensity),
        innerGlowAlpha = (if (dark) 0.05f else 0.11f) * intensity,
        shadowAlpha = if (dark) 0.55f else 0.22f,
        shadowElevation = if (dark) 18.dp else 12.dp,
        blurRadius = if (powerSave) 0 else (blur * (0.45f + 0.55f * intensity)).roundToInt(),
        specular = specular
    )
}

private fun Context.powerSaveModeEnabled(): Boolean {
    return (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
}
