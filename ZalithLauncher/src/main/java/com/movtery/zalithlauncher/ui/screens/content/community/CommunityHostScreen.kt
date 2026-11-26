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

package com.movtery.zalithlauncher.ui.screens.content.community

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.entry
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/**
 * 社区嵌套导航容器
 */
@Composable
fun CommunityHostScreen(
    key: NestedNavKey.Community,
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel
) {
    NestedNavDisplay(
        ownerKey = key,
        backStack = key.backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = { onBack(key.backStack) },
        transitionSpec = rememberTransitionSpec(),
        popTransitionSpec = rememberTransitionSpec(),
        entryProvider = entryProvider {
            entry<NormalNavKey.Community.Home> {
                CommunityScreen(
                    backStackViewModel = backStackViewModel,
                    eventViewModel = eventViewModel
                )
            }
            entry<NormalNavKey.Community.Chat> { chatKey ->
                GroupChatScreen(
                    key = chatKey,
                    backStackViewModel = backStackViewModel,
                    eventViewModel = eventViewModel
                )
            }
        }
    )
}
