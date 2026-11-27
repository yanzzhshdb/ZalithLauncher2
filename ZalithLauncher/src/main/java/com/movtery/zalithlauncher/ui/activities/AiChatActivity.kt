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

package com.movtery.zalithlauncher.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ai.AiChatSession
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity
import com.movtery.zalithlauncher.ui.screens.main.ai.AiChatPanel
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor
import dagger.hilt.android.AndroidEntryPoint

private const val EXTRA_VERSION_NAME = "EXTRA_VERSION_NAME"
private const val EXTRA_CRASH_LOG = "EXTRA_CRASH_LOG"
private const val EXTRA_CRASH_MESSAGE = "EXTRA_CRASH_MESSAGE"
private const val EXTRA_INITIAL_PROMPT = "EXTRA_INITIAL_PROMPT"
private const val EXTRA_AUTO_ANALYZE = "EXTRA_AUTO_ANALYZE"

/**
 * 独立的 AI 聊天页面，崩溃时也用它来直接分析日志
 */
@AndroidEntryPoint
class AiChatActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME)
        val crashLog = intent.getStringExtra(EXTRA_CRASH_LOG)
        val crashMessage = intent.getStringExtra(EXTRA_CRASH_MESSAGE)
        val initialPrompt = intent.getStringExtra(EXTRA_INITIAL_PROMPT)
        val autoAnalyze = intent.getBooleanExtra(EXTRA_AUTO_ANALYZE, false)

        setContent {
            ZalithLauncherTheme {
                var busy by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = backgroundColor(),
                    contentColor = onBackgroundColor(),
                    topBar = {
                        ChatTopBar(
                            busy = busy,
                            onShare = { shareLastAnswer(this@AiChatActivity) },
                            onClose = { finish() }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .fillMaxSize()
                    ) {
                        AiChatPanel(
                            modifier = Modifier.fillMaxSize(),
                            versionName = versionName,
                            crashLogPath = crashLog,
                            crashMessage = crashMessage,
                            initialPrompt = initialPrompt,
                            autoAnalyze = autoAnalyze,
                            onBusyChanged = { busy = it }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(busy: Boolean, onShare: () -> Unit, onClose: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Text(
                text = stringResource(R.string.ai_assistant_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_close)
                )
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(
                    painter = painterResource(R.drawable.ic_share_filled),
                    contentDescription = stringResource(R.string.ai_share_answer)
                )
            }
            IconButton(
                onClick = { if (!busy) AiChatSession.clear() },
                enabled = !busy
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = stringResource(R.string.ai_clear_chat)
                )
            }
        }
    )
}

private fun shareLastAnswer(context: Context) {
    val answer = AiChatSession.messages.value
        .lastOrNull { it.role == com.movtery.zalithlauncher.ai.AiMessage.Role.ASSISTANT && !it.isError }
        ?.text
    if (answer.isNullOrBlank()) {
        Toast.makeText(context, context.getString(R.string.ai_nothing_to_share), Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, answer)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.ai_share_answer)))
}

/**
 * 打开 AI 助手
 */
fun showAiChat(
    context: Context,
    versionName: String? = null,
    crashLogPath: String? = null,
    crashMessage: String? = null,
    initialPrompt: String? = null,
    autoAnalyze: Boolean = false
) {
    val intent = Intent(context, AiChatActivity::class.java).apply {
        putExtra(EXTRA_VERSION_NAME, versionName)
        putExtra(EXTRA_CRASH_LOG, crashLogPath)
        putExtra(EXTRA_CRASH_MESSAGE, crashMessage)
        putExtra(EXTRA_INITIAL_PROMPT, initialPrompt)
        putExtra(EXTRA_AUTO_ANALYZE, autoAnalyze)
    }
    context.startActivity(intent)
}
