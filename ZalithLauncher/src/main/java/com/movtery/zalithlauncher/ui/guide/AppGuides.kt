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

package com.movtery.zalithlauncher.ui.guide

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.guide.GuideController
import com.movtery.guide.GuideScope
import com.movtery.guide.NextTip
import com.movtery.guide.NodeClickMode
import com.movtery.guide.rememberGuide
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.guide.overlays.MainVersionGuideOverlay
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.viewmodel.EventViewModel

private const val TAG = "AppGuides"

/**
 * @param mainScreen 启动器主界面的引导
 * @param editorScreen 控制布局编辑器的引导
 */
class AppGuides(
    val mainScreen: GuideController,
    val editorScreen: GuideController,
    val joystickEvents: GuideController,
) {
    /**
     * 启动指定组的引导流，启动成功后记录进度
     * @return 是否成功启动（流为空或已有其他流激活时失败）
     */
    fun start(group: GuideKeys.Keys): Boolean {
        val controller = when (group) {
            GuideKeys.Main -> mainScreen
            GuideKeys.Editor -> editorScreen
            GuideKeys.JoystickEvents -> joystickEvents
        }
        val started = controller.start()
        if (started) {
            GuideProgress.markPlayed(group, group.version)
            Logger.info(TAG, "Guide started: ${GuideProgress.keyOf(group)} v${group.version}")
        }
        return started
    }

    /**
     * 启动指定组的引导流，持久化存储播放版本，已播放过当前版本时忽略
     * @return 是否成功启动（流为空或已有其他流激活时失败，或已播放过当前版本）
     */
    fun startOnce(group: GuideKeys.Keys): Boolean {
        return !GuideProgress.isPlayed(group) && start(group)
    }
}

@Composable
fun rememberAppGuides(
    eventViewModel: EventViewModel? = null
): AppGuides {
    val mainScreen = rememberMainGuides()
    val editorScreen = rememberEditorGuides()
    val joystickEvents = rememberJoystickEventsGuides()

    val guides = remember(mainScreen, editorScreen, joystickEvents) {
        AppGuides(mainScreen, editorScreen, joystickEvents)
    }

    eventViewModel?.let { viewModel ->
        LaunchedEffect(guides) {
            viewModel.events.collect { event ->
                when (event) {
                    is EventViewModel.Event.Guide.StartGuide -> guides.start(event.group)
                    is EventViewModel.Event.Guide.StartGuideOnce -> guides.startOnce(event.group)
                    else -> {}
                }
            }
        }
    }

    return guides
}

@Composable
private fun rememberMainGuides() = rememberGuide(holeShape = MaterialTheme.shapes.extraLarge) {
    intro { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_main_welcome_title, BuildKeys.LAUNCHER_NAME),
            body = stringResource(R.string.guide_main_welcome_text)
        )
    }
    entry(GuideKeys.Main.Step.Account) { scope ->
        GuideCard(
            scope = scope,
            body = stringResource(R.string.guide_main_account)
        )
    }
    entry(
        key = GuideKeys.Main.Step.VersionList,
        overlay = { scope ->
            MainVersionGuideOverlay(scope)
        },
        content = {}
    )
    entry(GuideKeys.Main.Step.CardDrag) { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_main_card_drag_title),
            body = stringResource(R.string.guide_main_card_drag_text)
        )
    }
    entry(GuideKeys.Main.Step.CardTip) { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_main_card_tip_title),
            body = stringResource(
                R.string.guide_main_card_tip_text,
                stringResource(R.string.home_add_version_card)
            )
        )
    }
}

@Composable
private fun rememberEditorGuides() = rememberGuide {
    intro { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_editor_welcome_title),
            body = stringResource(R.string.guide_editor_welcome_text)
        )
    }
    entry(
        key = GuideKeys.Editor.Step.MenuBall,
        nodeClick = NodeClickMode.PassThrough,
        advanceOnScrimClick = false
    ) { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_editor_menu_ball_title),
            body = stringResource(R.string.guide_editor_menu_ball_text)
        )
    }
    entry(GuideKeys.Editor.Step.LayerList) { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_editor_layer_list_title),
            body = stringResource(R.string.guide_editor_layer_list_text),
            note = stringResource(R.string.guide_editor_layer_list_note)
        )
    }
    entry(GuideKeys.Editor.Step.CreateLayer) { scope ->
        GuideCard(
            scope = scope,
            body = stringResource(R.string.guide_editor_create_layer)
        )
    }
    entry(GuideKeys.Editor.Step.AddButtons) { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_editor_add_widgets_title),
            body = stringResource(R.string.guide_editor_add_widgets_text)
        )
    }
    entry(GuideKeys.Editor.Step.AddStyles) { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_editor_styles_title),
            body = stringResource(R.string.guide_editor_styles_text)
        )
    }
    entry(GuideKeys.Editor.Step.StyleCabinet) { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_editor_style_cabinet_title),
            body = stringResource(R.string.guide_editor_style_cabinet_text),
            note = stringResource(R.string.guide_editor_style_cabinet_note)
        )
    }
    entry(GuideKeys.Editor.Step.Preview) { scope ->
        GuideCard(
            scope = scope,
            body = stringResource(R.string.guide_editor_preview)
        )
    }
    entry(GuideKeys.Editor.Step.Save) { scope ->
        GuideCard(
            scope = scope,
            body = stringResource(R.string.guide_editor_save)
        )
    }
}

@Composable
private fun rememberJoystickEventsGuides() = rememberGuide {
    intro { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_joystick_events_welcome_title),
            body = stringResource(R.string.guide_joystick_events_welcome_text)
        )
    }
    entry(GuideKeys.JoystickEvents.Step.Direction) { scope ->
        GuideCard(
            scope = scope,
            title = stringResource(R.string.guide_joystick_events_direction_title),
            body = stringResource(R.string.guide_joystick_events_direction_text)
        )
    }
    entry(
        key = GuideKeys.JoystickEvents.Step.Lock,
        nodeClick = NodeClickMode.PassThrough,
        advanceOnScrimClick = false
    ) { scope ->
        GuideCard(
            scope = scope,
            body = listOf(
                stringResource(R.string.guide_joystick_events_lock_text1),
                stringResource(R.string.guide_joystick_events_lock_text2),
                stringResource(R.string.guide_joystick_events_lock_text3)
            ).joinToString("\n\n"),
            note = stringResource(R.string.guide_joystick_events_lock_text4)
        )
    }
    entry(GuideKeys.JoystickEvents.Step.Events) { scope ->
        GuideCard(
            scope = scope,
            body = listOf(
                stringResource(R.string.guide_joystick_events_events_text1),
                stringResource(R.string.guide_joystick_events_events_text2)
            ).joinToString("\n\n")
        )
    }
}

/**
 * 发布启动引导事件
 */
fun EventViewModel.sendStartGuide(group: GuideKeys.Keys) {
    sendEvent(EventViewModel.Event.Guide.StartGuide(group))
}

/**
 * 发布启动引导事件，已播放过的引导将被忽略
 */
fun EventViewModel.sendStartGuideOnce(group: GuideKeys.Keys) {
    sendEvent(EventViewModel.Event.Guide.StartGuideOnce(group))
}

/**
 * 引导卡片的步骤进度条
 */
@Composable
private fun GuideStepBar(
    fraction: Float,
    stepIndex: Int,
    stepCount: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.guide_step_progress,
                    stepIndex + 1,
                    stepCount
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.guide_step_tap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * 引导的文本卡片
 */
@Composable
private fun GuideCard(
    scope: GuideScope,
    modifier: Modifier = Modifier,
    title: String? = null,
    body: String? = null,
    note: String? = null,
) {
    Surface(
        modifier = modifier.widthIn(max = 360.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(all = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (scope.stepCount > 1) {
                GuideStepBar(
                    fraction = ((scope.stepIndex + 1).toFloat() / scope.stepCount).coerceIn(0f, 1f),
                    stepIndex = scope.stepIndex,
                    stepCount = scope.stepCount
                )
            }

            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (note != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NextTipLabel(tip: NextTip) {
    val transition = rememberInfiniteTransition(label = "GuideNextTip")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GuideNextTipPulse"
    )

    Surface(
        modifier = Modifier.widthIn(max = 360.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = pulse }
                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
            )
            Text(
                text = when (tip) {
                    NextTip.TapBlank -> stringResource(R.string.guide_tip_blank)
                    NextTip.Finish -> stringResource(R.string.guide_tip_finish)
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
