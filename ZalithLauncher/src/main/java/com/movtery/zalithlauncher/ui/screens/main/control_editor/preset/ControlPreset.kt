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

package com.movtery.zalithlauncher.ui.screens.main.control_editor.preset

import androidx.annotation.StringRes
import com.movtery.inputmap.keycodes.ControlEventKeyName
import com.movtery.inputmap.keycodes.ControlEventKeycode
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_MENU

/**
 * 预设控件的分类
 */
enum class PresetCategory(@StringRes val labelRes: Int) {
    Movement(R.string.control_preset_category_movement),
    Action(R.string.control_preset_category_action),
    Hotbar(R.string.control_preset_category_hotbar),
    View(R.string.control_preset_category_view),
    Launcher(R.string.control_preset_category_launcher)
}

/**
 * 一个可以直接添加到控制布局的预设控件
 * @param id 唯一标识，用于列表 key
 * @param labelRes 显示文本
 * @param labelArg 文本中的格式化参数（如快捷栏槽位号）
 * @param category 所属分类
 * @param clickEvents 已绑定好的点击事件组
 * @param toggleable 是否以开关形式切换按下状态（适合潜行、疾跑这类长按键）
 * @param sizeDp 新建按钮的目标尺寸(dp)
 */
data class ControlPreset(
    val id: String,
    @StringRes val labelRes: Int,
    val category: PresetCategory,
    val clickEvents: List<ClickEvent>,
    val labelArg: Int? = null,
    val toggleable: Boolean = false,
    val sizeDp: Float = 50f
) {
    /**
     * 单个按键预设显示其按键名，方便玩家确认绑定
     */
    val keyLabel: String? = clickEvents.singleOrNull()
        ?.takeIf { it.type == ClickEvent.Type.Key }
        ?.let { ControlEventKeyName.getNameByKey(it.key) }
}

private fun keyEvent(keycode: String) = ClickEvent(ClickEvent.Type.Key, keycode)

private fun launcherEvent(event: String) = ClickEvent(ClickEvent.Type.LauncherEvent, event)

private val movementPresets = listOf(
    ControlPreset(
        id = "forward",
        labelRes = R.string.control_preset_forward,
        category = PresetCategory.Movement,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_W))
    ),
    ControlPreset(
        id = "left",
        labelRes = R.string.control_preset_left,
        category = PresetCategory.Movement,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_A))
    ),
    ControlPreset(
        id = "back",
        labelRes = R.string.control_preset_back,
        category = PresetCategory.Movement,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_S))
    ),
    ControlPreset(
        id = "right",
        labelRes = R.string.control_preset_right,
        category = PresetCategory.Movement,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_D))
    ),
    ControlPreset(
        id = "jump",
        labelRes = R.string.control_preset_jump,
        category = PresetCategory.Movement,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_SPACE))
    ),
    ControlPreset(
        id = "sneak",
        labelRes = R.string.control_preset_sneak,
        category = PresetCategory.Movement,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_LEFT_SHIFT)),
        toggleable = true
    ),
    ControlPreset(
        id = "sprint",
        labelRes = R.string.control_preset_sprint,
        category = PresetCategory.Movement,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_LEFT_CONTROL)),
        toggleable = true
    )
)

private val actionPresets = listOf(
    ControlPreset(
        id = "attack",
        labelRes = R.string.control_preset_attack,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT))
    ),
    ControlPreset(
        id = "use",
        labelRes = R.string.control_preset_use,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT))
    ),
    ControlPreset(
        id = "inventory",
        labelRes = R.string.control_preset_inventory,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_E))
    ),
    ControlPreset(
        id = "drop",
        labelRes = R.string.control_preset_drop,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_Q))
    ),
    ControlPreset(
        id = "swap_hand",
        labelRes = R.string.control_preset_swap_hand,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_F))
    ),
    ControlPreset(
        id = "chat",
        labelRes = R.string.control_preset_chat,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_T))
    ),
    ControlPreset(
        id = "command",
        labelRes = R.string.control_preset_command,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_SLASH))
    ),
    ControlPreset(
        id = "player_list",
        labelRes = R.string.control_preset_player_list,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_TAB))
    ),
    ControlPreset(
        id = "pause",
        labelRes = R.string.control_preset_pause,
        category = PresetCategory.Action,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_ESCAPE))
    )
)

private val hotbarPresets = (1..9).map { slot ->
    val keycode = when (slot) {
        1 -> ControlEventKeycode.GLFW_KEY_1
        2 -> ControlEventKeycode.GLFW_KEY_2
        3 -> ControlEventKeycode.GLFW_KEY_3
        4 -> ControlEventKeycode.GLFW_KEY_4
        5 -> ControlEventKeycode.GLFW_KEY_5
        6 -> ControlEventKeycode.GLFW_KEY_6
        7 -> ControlEventKeycode.GLFW_KEY_7
        8 -> ControlEventKeycode.GLFW_KEY_8
        else -> ControlEventKeycode.GLFW_KEY_9
    }
    ControlPreset(
        id = "hotbar_$slot",
        labelRes = R.string.control_preset_hotbar,
        category = PresetCategory.Hotbar,
        clickEvents = listOf(keyEvent(keycode)),
        labelArg = slot,
        sizeDp = 42f
    )
}

private val viewPresets = listOf(
    ControlPreset(
        id = "hide_hud",
        labelRes = R.string.control_preset_hide_hud,
        category = PresetCategory.View,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_F1))
    ),
    ControlPreset(
        id = "debug",
        labelRes = R.string.control_preset_debug,
        category = PresetCategory.View,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_F3))
    ),
    ControlPreset(
        id = "perspective",
        labelRes = R.string.control_preset_perspective,
        category = PresetCategory.View,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_F5))
    ),
    ControlPreset(
        id = "fullscreen",
        labelRes = R.string.control_preset_fullscreen,
        category = PresetCategory.View,
        clickEvents = listOf(keyEvent(ControlEventKeycode.GLFW_KEY_F11))
    )
)

private val launcherPresets = listOf(
    ControlPreset(
        id = "switch_ime",
        labelRes = R.string.control_preset_switch_ime,
        category = PresetCategory.Launcher,
        clickEvents = listOf(launcherEvent(LAUNCHER_EVENT_SWITCH_IME))
    ),
    ControlPreset(
        id = "switch_menu",
        labelRes = R.string.control_preset_switch_menu,
        category = PresetCategory.Launcher,
        clickEvents = listOf(launcherEvent(LAUNCHER_EVENT_SWITCH_MENU))
    ),
    ControlPreset(
        id = "scroll_up",
        labelRes = R.string.control_preset_scroll_up,
        category = PresetCategory.Launcher,
        clickEvents = listOf(launcherEvent(LAUNCHER_EVENT_SCROLL_UP_SINGLE))
    ),
    ControlPreset(
        id = "scroll_down",
        labelRes = R.string.control_preset_scroll_down,
        category = PresetCategory.Launcher,
        clickEvents = listOf(launcherEvent(LAUNCHER_EVENT_SCROLL_DOWN_SINGLE))
    ),
    ControlPreset(
        id = "mouse_middle",
        labelRes = R.string.control_preset_mouse_middle,
        category = PresetCategory.Launcher,
        clickEvents = listOf(launcherEvent(ControlEventKeycode.GLFW_MOUSE_BUTTON_MIDDLE))
    )
)

/**
 * 一键铺开的快捷按钮及其推荐位置
 * @param presetId [ControlPreset.id]
 * @param x 参考坐标 0~10000
 * @param y 参考坐标 0~10000
 */
data class PresetPlacement(
    val presetId: String,
    val x: Int,
    val y: Int
)

/**
 * 快速战斗包：拇指区一套常用的移动与战斗按钮
 */
val StarterPackPlacements: List<PresetPlacement> = listOf(
    PresetPlacement("attack", x = 8700, y = 7800),
    PresetPlacement("use", x = 7900, y = 8700),
    PresetPlacement("jump", x = 9250, y = 8900),
    PresetPlacement("sneak", x = 6900, y = 8900),
    PresetPlacement("inventory", x = 9200, y = 900)
)

fun findPreset(id: String): ControlPreset? = ControlPresets.firstOrNull { it.id == id }

/**
 * 控件预设目录，按分类顺序排列
 */
val ControlPresets: List<ControlPreset> = buildList {
    addAll(movementPresets)
    addAll(actionPresets)
    addAll(hotbarPresets)
    addAll(viewPresets)
    addAll(launcherPresets)
}
