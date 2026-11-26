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

package com.movtery.zalithlauncher.community

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 以编程方式初始化 Firebase。
 *
 * 使用独立的具名 FirebaseApp，避免与内置配置冲突；
 * 因此应用无需内置 google-services.json，用户填写自己的项目信息即可。
 */
object FirebaseBootstrap {
    private const val APP_NAME = "nlauncher-community"

    sealed interface Status {
        data object NotConfigured : Status
        data object Initializing : Status
        data class Ready(val uid: String) : Status
        data class Failed(val message: String) : Status
    }

    private val _status = MutableStateFlow<Status>(Status.NotConfigured)
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile
    private var firebaseApp: FirebaseApp? = null

    @Volatile
    private var currentConfig: CommunityConfig? = null

    @Volatile
    private var started = false

    fun app(): FirebaseApp? = firebaseApp

    fun config(): CommunityConfig? = currentConfig

    /**
     * 按当前设置初始化（幂等）
     */
    fun ensureStarted(context: Context) {
        val target = CommunityConfig.fromSettings()
        if (!target.isConfigured) {
            _status.value = Status.NotConfigured
            return
        }
        if (started && firebaseApp != null && currentConfig == target) return

        currentConfig = target
        started = true
        _status.value = Status.Initializing
        runCatching { start(context.applicationContext, target) }
            .onFailure { _status.value = Status.Failed(it.message ?: it.javaClass.simpleName) }
    }

    /**
     * 配置变更后重新初始化
     */
    fun restart(context: Context) {
        started = false
        firebaseApp = null
        currentConfig = null
        _status.value = Status.NotConfigured
        ensureStarted(context)
    }

    private fun start(context: Context, config: CommunityConfig) {
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(context, buildOptions(config), APP_NAME)
            ?: error("FirebaseApp initialization failed")
        firebaseApp = app

        val auth = FirebaseAuth.getInstance(app)
        val existing = auth.currentUser
        if (existing != null) {
            _status.value = Status.Ready(existing.uid)
            return
        }

        auth.signInAnonymously().addOnCompleteListener { task ->
            val user = task.result?.user
            _status.value = if (task.isSuccessful && user != null) {
                Status.Ready(user.uid)
            } else {
                Status.Failed(task.exception?.message ?: "anonymous sign-in failed")
            }
        }
    }

    private fun buildOptions(config: CommunityConfig): FirebaseOptions {
        val builder = FirebaseOptions.Builder()
            .setApiKey(config.apiKey)
            .setApplicationId(config.appId)
            .setProjectId(config.projectId)
        if (config.senderId.isNotBlank()) builder.setGcmSenderId(config.senderId)
        if (config.storageBucket.isNotBlank()) builder.setStorageBucket(config.storageBucket)
        return builder.build()
    }
}
