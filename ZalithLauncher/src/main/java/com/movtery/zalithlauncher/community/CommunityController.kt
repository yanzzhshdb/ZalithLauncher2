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
import android.os.Build
import com.movtery.zalithlauncher.community.model.CommunityMember
import com.movtery.zalithlauncher.community.model.CommunityMessage
import com.movtery.zalithlauncher.community.model.GroupRef
import com.movtery.zalithlauncher.community.model.MessageRef
import com.movtery.zalithlauncher.community.voice.VoiceRoomController
import com.movtery.zalithlauncher.setting.CommunitySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * 社区功能门面：持有当前会话的数据流与操作入口
 */
object CommunityController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val status = FirebaseBootstrap.status
    val groups = MutableStateFlow<List<GroupRef>>(emptyList())
    val messages = MutableStateFlow<List<MessageRef>>(emptyList())
    val members = MutableStateFlow<List<Pair<String, CommunityMember>>>(emptyList())
    val openedGroup = MutableStateFlow<GroupRef?>(null)
    val unreadGroups = MutableStateFlow<Set<String>>(emptySet())
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val myUid = MutableStateFlow("")

    private var repository: CommunityRepository? = null
    private var attached = false
    private var statusJob: Job? = null
    private var groupsJob: Job? = null
    private var messagesJob: Job? = null
    private var membersJob: Job? = null
    private var heartbeatJob: Job? = null
    private val seenAt = mutableMapOf<String, Long>()
    private var lastTypingAt = 0L

    fun displayName(): String {
        val custom = CommunitySettings.displayName.getValue().trim()
        if (custom.isNotEmpty()) return custom
        val model = Build.MODEL?.trim().orEmpty()
        return if (model.isEmpty()) "Player" else model
    }

    fun repository(): CommunityRepository? = repository

    /**
     * 绑定 Firebase（幂等），配置就绪后自动开始收集群组
     */
    fun attach(context: Context) {
        val appContext = context.applicationContext
        if (attached) {
            FirebaseBootstrap.ensureStarted(appContext)
            return
        }
        attached = true
        FirebaseBootstrap.ensureStarted(appContext)
        statusJob?.cancel()
        statusJob = scope.launch {
            FirebaseBootstrap.status.collect { state ->
                if (state is FirebaseBootstrap.Status.Ready) {
                    bind(state.uid)
                }
            }
        }
    }

    /**
     * 配置变更后重新连接
     */
    fun reload(context: Context) {
        val appContext = context.applicationContext
        detachCollections()
        statusJob?.cancel()
        statusJob = null
        repository = null
        attached = false
        FirebaseBootstrap.restart(appContext)
        attach(appContext)
    }

    private fun bind(uid: String) {
        if (repository?.uid == uid) return
        val app = FirebaseBootstrap.app() ?: return
        detachCollections()
        val repo = CommunityRepository(app, uid, displayName())
        repository = repo
        myUid.value = uid

        groupsJob = scope.launch {
            repo.groupsFlow()
                .catch { error.value = it.message }
                .collect { list ->
                    groups.value = list
                    recomputeUnread(list)
                    openedGroup.value?.let { current ->
                        list.firstOrNull { it.id == current.id }?.let { openedGroup.value = it }
                    }
                }
        }
    }

    private fun detachCollections() {
        groupsJob?.cancel()
        messagesJob?.cancel()
        membersJob?.cancel()
        heartbeatJob?.cancel()
        groupsJob = null
        messagesJob = null
        membersJob = null
        heartbeatJob = null
        groups.value = emptyList()
        messages.value = emptyList()
        members.value = emptyList()
        openedGroup.value = null
        unreadGroups.value = emptySet()
    }

    private fun recomputeUnread(list: List<GroupRef>) {
        val openedId = openedGroup.value?.id
        unreadGroups.value = list
            .filter { it.id != openedId && it.group.lastMessageAt > (seenAt[it.id] ?: 0L) }
            .map { it.id }
            .toSet()
    }

    fun openGroup(group: GroupRef) {
        val repo = repository ?: return
        openedGroup.value = group
        seenAt[group.id] = maxOf(seenAt[group.id] ?: 0L, group.group.lastMessageAt)
        recomputeUnread(groups.value)

        messagesJob?.cancel()
        membersJob?.cancel()
        heartbeatJob?.cancel()
        messages.value = emptyList()
        members.value = emptyList()

        messagesJob = scope.launch {
            repo.messagesFlow(group.id)
                .catch { error.value = it.message }
                .collect { list ->
                    messages.value = list
                    val latest = list.lastOrNull()?.message?.createdAt ?: 0L
                    seenAt[group.id] = maxOf(seenAt[group.id] ?: 0L, latest)
                    recomputeUnread(groups.value)
                }
        }
        membersJob = scope.launch {
            repo.membersFlow(group.id)
                .catch { error.value = it.message }
                .collect { members.value = it }
        }
        heartbeatJob = scope.launch {
            while (isActive) {
                runCatching { repo.heartbeat(group.id) }
                delay(20_000)
            }
        }
    }

    fun closeGroup() {
        messagesJob?.cancel()
        membersJob?.cancel()
        heartbeatJob?.cancel()
        messagesJob = null
        membersJob = null
        heartbeatJob = null
        openedGroup.value = null
        messages.value = emptyList()
        members.value = emptyList()
    }

    fun notifyTyping() {
        val repo = repository ?: return
        val group = openedGroup.value ?: return
        val now = System.currentTimeMillis()
        if (now - lastTypingAt < 2_000) return
        lastTypingAt = now
        scope.launch { runCatching { repo.updateTyping(group.id, now + 4_000) } }
    }

    fun sendText(text: String) {
        val repo = repository ?: return
        val group = openedGroup.value ?: return
        val content = text.trim()
        if (content.isEmpty()) return
        scope.launch {
            runCatching {
                repo.sendMessage(
                    group.id,
                    CommunityMessage(
                        senderId = repo.uid,
                        senderName = repo.displayName,
                        text = content,
                        type = CommunityMessage.TYPE_TEXT,
                        createdAt = System.currentTimeMillis()
                    )
                )
                repo.updateTyping(group.id, 0L)
            }.onFailure { error.value = it.message }
        }
    }

    fun sendVoiceNote(file: File, durationMs: Long) {
        val repo = repository ?: return
        val group = openedGroup.value ?: return
        scope.launch {
            busy.value = true
            runCatching {
                val url = repo.uploadVoiceNote(group.id, file)
                repo.sendMessage(
                    group.id,
                    CommunityMessage(
                        senderId = repo.uid,
                        senderName = repo.displayName,
                        text = "",
                        type = CommunityMessage.TYPE_VOICE,
                        audioUrl = url,
                        audioDurationMs = durationMs,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }.onFailure { error.value = it.message }
            busy.value = false
        }
    }

    fun createGroup(name: String, onCreated: (String) -> Unit = {}) {
        val repo = repository ?: return
        val title = name.trim()
        if (title.isEmpty()) return
        scope.launch {
            busy.value = true
            runCatching { repo.createGroup(title) }
                .onSuccess(onCreated)
                .onFailure { error.value = it.message }
            busy.value = false
        }
    }

    fun joinGroup(inviteCode: String, onJoined: (String) -> Unit = {}) {
        val repo = repository ?: return
        val code = inviteCode.trim()
        if (code.isEmpty()) return
        scope.launch {
            busy.value = true
            runCatching { repo.joinGroup(code) }
                .onSuccess(onJoined)
                .onFailure { error.value = it.message }
            busy.value = false
        }
    }

    fun leaveGroup(groupId: String) {
        val repo = repository ?: return
        if (openedGroup.value?.id == groupId) closeGroup()
        scope.launch {
            runCatching { repo.leaveGroup(groupId) }.onFailure { error.value = it.message }
        }
    }

    fun joinVoice(context: Context, groupId: String) {
        val repo = repository ?: return
        VoiceRoomController.join(context, repo, groupId)
    }

    fun leaveVoice() {
        VoiceRoomController.leave()
    }

    fun consumeError(): String? = error.value?.also { error.value = null }

    /**
     * 正在输入的其他成员
     */
    fun typingMembers(): List<String> {
        val now = System.currentTimeMillis()
        return members.value
            .filter { it.first != myUid.value && it.second.typingUntil > now }
            .map { it.second.name }
    }

    /**
     * 在线成员（最近 60 秒内有心跳）
     */
    fun onlineMembers(): List<String> {
        val now = System.currentTimeMillis()
        return members.value
            .filter { now - it.second.lastSeen < 60_000L }
            .map { it.second.name }
    }
}
