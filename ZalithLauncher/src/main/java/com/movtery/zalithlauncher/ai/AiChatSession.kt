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

package com.movtery.zalithlauncher.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * 一条聊天消息
 */
data class AiMessage(
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
) {
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
}

/**
 * 内置 AI 会话，状态保存在单例中，这样切换页面不会丢失聊天记录
 */
object AiChatSession {
    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun clear() {
        _messages.value = emptyList()
    }

    fun addSystem(text: String) {
        _messages.update { it + AiMessage(AiMessage.Role.SYSTEM, text) }
    }

    /**
     * 发送一条用户消息，并把 AI 的回答追加到会话里
     */
    suspend fun send(
        userText: String,
        versionName: String? = null,
        includeContext: Boolean = AiSettings.includeMods.getValue(),
        historyForPrompt: Boolean = true
    ) {
        val prompt = userText.trim()
        if (prompt.isEmpty() || _busy.value) return

        _messages.update { it + AiMessage(AiMessage.Role.USER, prompt) }
        _busy.value = true
        try {
            val result = withContext(Dispatchers.IO) {
                val version = AiContextBuilder.resolveVersion(versionName)
                val fullPrompt = AiContextBuilder.chatPrompt(
                    userText = prompt,
                    version = version,
                    history = if (historyForPrompt) _messages.value else emptyList(),
                    includeContext = includeContext
                )
                AxAiClient.ask(fullPrompt)
            }
            when (result) {
                is AxAiClient.AiResult.Success ->
                    _messages.update { it + AiMessage(AiMessage.Role.ASSISTANT, result.text) }
                is AxAiClient.AiResult.Failure ->
                    _messages.update {
                        it + AiMessage(AiMessage.Role.ASSISTANT, result.message, isError = true)
                    }
            }
        } finally {
            _busy.value = false
        }
    }

    /**
     * 崩溃分析：直接把整理好的上下文发给 AI
     */
    suspend fun analyzeCrash(
        versionName: String? = null,
        logFile: java.io.File? = null,
        crashMessage: String? = null
    ) {
        if (_busy.value) return

        _messages.update {
            it + AiMessage(
                AiMessage.Role.USER,
                crashMessage?.takeIf { message -> message.isNotBlank() }
                    ?: "Game saya crash. Tolong analisis log terakhir dan beri solusi."
            )
        }
        _busy.value = true
        try {
            val result = withContext(Dispatchers.IO) {
                val version = AiContextBuilder.resolveVersion(versionName)
                val prompt = AiContextBuilder.crashPrompt(version, logFile, crashMessage)
                AxAiClient.ask(prompt)
            }
            when (result) {
                is AxAiClient.AiResult.Success ->
                    _messages.update { it + AiMessage(AiMessage.Role.ASSISTANT, result.text) }
                is AxAiClient.AiResult.Failure ->
                    _messages.update {
                        it + AiMessage(AiMessage.Role.ASSISTANT, result.message, isError = true)
                    }
            }
        } finally {
            _busy.value = false
        }
    }
}
