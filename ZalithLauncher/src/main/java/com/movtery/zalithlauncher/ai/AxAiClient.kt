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

import com.google.gson.Gson
import com.movtery.zalithlauncher.path.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * AI 客户端
 *
 * 密钥只允许保存在服务端代理里，客户端不内置、不读取、也不记录任何密钥。
 * 代理地址为空时代表后端还没准备好，调用会直接返回维护提示。
 */
object AxAiClient {
    private const val TAG = "AxAiClient"
    private const val PROXY_URL = ""

    /**
     * 提示词上限。接口走 URL 查询参数，太长会被网关拒绝（一般限制 8KB 请求行）
     */
    const val MAX_PROMPT_CHARS = 2400

    private val gson = Gson()

    sealed interface AiResult {
        data class Success(val text: String, val model: String) : AiResult
        data class Failure(val message: String) : AiResult
        data object Maintenance : AiResult
    }

    private data class ApiResponse(
        val statusCode: Int?,
        val status: Boolean?,
        val msg: String?,
        val result: ApiPayload?
    )

    private data class ApiPayload(
        val model: String?,
        val text: String?
    )

    suspend fun ask(prompt: String): AiResult = withContext(Dispatchers.IO) {
        if (!AiAssistant.isAvailable() || PROXY_URL.isBlank()) return@withContext AiResult.Maintenance

        val trimmed = prompt.trim().take(MAX_PROMPT_CHARS)
        if (trimmed.isBlank()) return@withContext AiResult.Failure("Pertanyaan masih kosong")

        runCatching {
            val body = GLOBAL_CLIENT.get(PROXY_URL) {
                parameter("prompt", trimmed)
                timeout { requestTimeoutMillis = 120_000 }
            }.bodyAsText()
            parse(body)
        }.getOrElse { throwable ->
            //只记录异常类型，避免把提示词或地址写进日志
            Logger.error(TAG, "Failed to ask AI: ${throwable.javaClass.simpleName}")
            AiResult.Failure(describeFailure(throwable))
        }
    }

    private fun parse(body: String): AiResult {
        val parsed = runCatching { gson.fromJson(body, ApiResponse::class.java) }.getOrNull()
            ?: return AiResult.Failure("Jawaban AI tidak bisa dibaca")
        if (parsed.status == false) {
            return AiResult.Failure(parsed.msg ?: "AI gagal menjawab")
        }
        val text = parsed.result?.text?.trim().orEmpty()
        return if (text.isBlank()) {
            AiResult.Failure(parsed.msg ?: "AI mengembalikan jawaban kosong")
        } else {
            AiResult.Success(text, parsed.result?.model ?: "GPT-5 Nano")
        }
    }

    private fun describeFailure(throwable: Throwable): String = when {
        throwable is ClientRequestException -> when (throwable.response.status.value) {
            401, 403 -> "Server AI menolak permintaan, coba lagi nanti"
            404 -> "Endpoint AI tidak ditemukan"
            414 -> "Pertanyaan terlalu panjang, coba lebih singkat"
            else -> "Server AI menolak permintaan (HTTP ${throwable.response.status.value})"
        }
        throwable is SocketTimeoutException -> "AI terlalu lama menjawab, coba lagi"
        throwable is UnknownHostException -> "Tidak ada koneksi internet"
        else -> throwable.message ?: "Gagal menghubungi AI"
    }
}
