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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContentColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.theme.onCardColor

/**
 * 液态玻璃标准圆角，与 iOS 27 的大圆角观感保持一致
 */
val LiquidGlassShape: Shape = RoundedCornerShape(28.dp)

/**
 * 为任意元素铺一层液态玻璃：
 * 投影 -> 边缘高光圈 -> 圆角裁剪 -> 背景采样模糊与底色 -> 镜面高光与内反射
 * @param shape 玻璃的形状
 * @param spec 玻璃视觉参数
 * @param backdropBlur 是否采样启动器背景进行模糊（对话框等独立窗口需关闭）
 * @param shadow 是否绘制投影
 * @param border 是否绘制边缘高光圈
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = LiquidGlassShape,
    spec: GlassSpec = rememberGlassSpec(),
    backdropBlur: Boolean = true,
    shadow: Boolean = true,
    border: Boolean = true
): Modifier {
    var modifier = this

    if (shadow && spec.shadowAlpha > 0f) {
        modifier = modifier.shadow(
            elevation = spec.shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = spec.shadowAlpha * 0.55f),
            spotColor = Color.Black.copy(alpha = spec.shadowAlpha)
        )
    }

    if (border) {
        //边缘高光圈绘制在裁剪之外，保证 1dp 描边不会被裁掉一半
        modifier = modifier.drawWithContent {
            drawContent()
            drawGlassEdge(shape, spec)
        }
    }

    modifier = modifier.clip(shape)

    modifier = if (backdropBlur) {
        modifier.backgroundGlass(spec.blurRadius, spec.tint.copy(alpha = spec.tintAlpha))
    } else {
        modifier.background(spec.tint.copy(alpha = spec.tintAlpha))
    }

    return modifier.drawWithContent {
        drawContent()
        drawGlassSheen(spec)
    }
}

/**
 * 镜面高光与底部内反射，绘制在裁剪区域内部
 */
private fun DrawScope.drawGlassSheen(spec: GlassSpec) {
    if (size.width <= 0f || size.height <= 0f) return

    if (spec.specular && spec.highlightAlpha > 0f) {
        //顶部边缘受光最强，向下迅速衰减，形成玻璃表面的高光带
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    spec.highlightColor.copy(alpha = spec.highlightAlpha),
                    spec.highlightColor.copy(alpha = spec.highlightAlpha * 0.35f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.5f
            )
        )
    }

    if (spec.innerGlowAlpha > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    spec.highlightColor.copy(alpha = spec.innerGlowAlpha)
                ),
                startY = size.height * 0.62f,
                endY = size.height
            )
        )
    }
}

/**
 * 1dp 渐变描边，模拟玻璃边缘的折射高光
 */
private fun DrawScope.drawGlassEdge(shape: Shape, spec: GlassSpec) {
    if (spec.edgeAlpha <= 0f) return

    drawOutline(
        outline = shape.createOutline(size, layoutDirection, this),
        brush = Brush.linearGradient(
            colors = listOf(
                spec.highlightColor.copy(alpha = spec.edgeAlpha),
                spec.highlightColor.copy(alpha = spec.edgeAlpha * 0.12f),
                spec.highlightColor.copy(alpha = spec.edgeAlpha * 0.45f),
                spec.highlightColor.copy(alpha = spec.edgeAlpha)
            )
        ),
        style = Stroke(width = 1.dp.toPx())
    )
}

/**
 * 液态玻璃面板
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassShape,
    spec: GlassSpec = rememberGlassSpec(),
    backdropBlur: Boolean = true,
    shadow: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .liquidGlass(shape, spec, backdropBlur, shadow)
            .padding(contentPadding),
        content = content
    )
}

/**
 * 可点击的液态玻璃卡片，按下时会轻微收缩，模拟玻璃被按压的形变
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassShape,
    spec: GlassSpec = rememberGlassSpec(),
    backdropBlur: Boolean = true,
    shadow: Boolean = true,
    enabled: Boolean = true,
    contentColor: Color = onCardColor(),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.972f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "LiquidGlassPress"
    )

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .liquidGlass(shape, spec, backdropBlur, shadow)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onClick
                        )
                    } else Modifier
                ),
            content = content
        )
    }
}
