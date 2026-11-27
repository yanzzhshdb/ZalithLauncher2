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

package com.movtery.zalithlauncher.ui.screens.main.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ai.AiChatSession
import com.movtery.zalithlauncher.ai.AiContextBuilder
import com.movtery.zalithlauncher.ai.AiMessage
import com.movtery.zalithlauncher.ai.AiSettings
import kotlinx.coroutines.launch
import java.io.File

/**
 * 内置 AI 聊天面板
 *
 * 可直接嵌入设置页，也可以由崩溃页跳转过来自动发起分析
 */
@Composable
fun AiChatPanel(
    modifier: Modifier = Modifier,
    versionName: String? = null,
    crashLogPath: String? = null,
    crashMessage: String? = null,
    initialPrompt: String? = null,
    autoAnalyze: Boolean = false,
    onBusyChanged: (Boolean) -> Unit = {}
) {
    val messages by AiChatSession.messages.collectAsState()
    val busy by AiChatSession.busy.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    var includeContext by remember { mutableStateOf(AiSettings.includeMods.getValue()) }
    var autoStarted by remember { mutableStateOf(false) }

    val resolvedVersion = remember(versionName) { AiContextBuilder.resolveVersion(versionName) }
    val modsCount = remember(resolvedVersion) {
        resolvedVersion?.let { AiContextBuilder.listMods(it).size } ?: 0
    }
    val fpsPrompt = stringResource(R.string.ai_chip_fps)

    LaunchedEffect(busy) { onBusyChanged(busy) }

    LaunchedEffect(Unit) {
        if (autoStarted) return@LaunchedEffect
        autoStarted = true
        when {
            autoAnalyze -> AiChatSession.analyzeCrash(
                versionName = versionName,
                logFile = crashLogPath?.let(::File)
                    ?: resolvedVersion?.getLatestLog(),
                crashMessage = crashMessage
            )
            !initialPrompt.isNullOrBlank() -> AiChatSession.send(
                userText = initialPrompt,
                versionName = versionName,
                includeContext = includeContext
            )
        }
    }

    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || busy) return
        input = ""
        scope.launch {
            AiChatSession.send(
                userText = trimmed,
                versionName = versionName,
                includeContext = includeContext
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    WelcomeCard(
                        versionName = resolvedVersion?.getVersionName(),
                        modsCount = modsCount
                    )
                }
            }

            items(messages) { message ->
                MessageBubble(message)
            }

            if (busy) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.ai_thinking),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = includeContext,
                    onClick = {
                        includeContext = !includeContext
                        AiSettings.includeMods.save(includeContext)
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.ai_context_chip),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        scope.launch {
                            AiChatSession.analyzeCrash(
                                versionName = versionName,
                                logFile = resolvedVersion?.getLatestLog()
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.ai_chip_crash),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                FilterChip(
                    selected = false,
                    onClick = { submit(fpsPrompt) },
                    label = {
                        Text(
                            text = stringResource(R.string.ai_chip_fps_title),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.ai_input_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp)
                )
                IconButton(
                    onClick = { submit(input) },
                    enabled = !busy && input.isNotBlank()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send_filled),
                        contentDescription = stringResource(R.string.ai_send)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(versionName: String?, modsCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(all = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_welcome_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.ai_welcome_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (versionName != null) {
                Text(
                    text = stringResource(R.string.ai_context_info, versionName, modsCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AiMessage) {
    val isUser = message.role == AiMessage.Role.USER
    val isSystem = message.role == AiMessage.Role.SYSTEM

    val container = when {
        isSystem -> MaterialTheme.colorScheme.secondaryContainer
        message.isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = when {
        isSystem -> MaterialTheme.colorScheme.onSecondaryContainer
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 340.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = container,
            contentColor = content
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = message.text,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
