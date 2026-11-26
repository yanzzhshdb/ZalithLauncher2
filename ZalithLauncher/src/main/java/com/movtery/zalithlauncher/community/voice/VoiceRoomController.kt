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

package com.movtery.zalithlauncher.community.voice

import android.content.Context
import com.movtery.zalithlauncher.community.CommunityConfig
import com.movtery.zalithlauncher.community.CommunityRepository
import com.movtery.zalithlauncher.community.model.VoiceParticipant
import com.movtery.zalithlauncher.community.model.VoiceSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

/**
 * 语音房间控制器：负责 WebRTC 连接与 Firestore 信令的编排
 *
 * 连接策略：uid 较小的一方主动发起 offer，避免双方同时发起造成冲突
 */
object VoiceRoomController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val active = MutableStateFlow(false)
    val joining = MutableStateFlow(false)
    val muted = MutableStateFlow(false)
    val participants = MutableStateFlow<List<Pair<String, VoiceParticipant>>>(emptyList())
    val connectedPeers = MutableStateFlow<Set<String>>(emptySet())
    val error = MutableStateFlow<String?>(null)
    val currentGroupId = MutableStateFlow<String?>(null)

    private var repository: CommunityRepository? = null
    private var engine: VoiceEngine? = null
    private var myUid: String = ""
    private var participantsJob: Job? = null
    private var signalsJob: Job? = null
    private var heartbeatJob: Job? = null
    private val offered = mutableSetOf<String>()

    fun join(context: Context, repository: CommunityRepository, groupId: String) {
        if (active.value || joining.value) return
        this.repository = repository
        this.myUid = repository.uid
        currentGroupId.value = groupId
        joining.value = true

        val engine = VoiceEngine(context.applicationContext, signaling)
        this.engine = engine
        runCatching { engine.start(buildIceServers()) }
            .onFailure {
                error.value = it.message ?: "WebRTC initialization failed"
                joining.value = false
                return
            }

        scope.launch {
            runCatching { repository.joinVoice(groupId) }
                .onFailure {
                    error.value = it.message
                    joining.value = false
                    leave()
                    return@launch
                }

            joining.value = false
            active.value = true

            participantsJob = scope.launch {
                repository.voiceParticipantsFlow(groupId)
                    .catch { error.value = it.message }
                    .collect { list ->
                        participants.value = list
                        reconcile(list)
                    }
            }
            signalsJob = scope.launch {
                repository.signalsFlow(groupId)
                    .catch { error.value = it.message }
                    .collect { signals -> signals.forEach { consumeSignal(it.first, it.second) } }
            }
            heartbeatJob = scope.launch {
                while (isActive) {
                    runCatching { repository.heartbeat(groupId) }
                    delay(HEARTBEAT_INTERVAL)
                }
            }
        }
    }

    fun toggleMute() {
        val next = !muted.value
        muted.value = next
        engine?.setMuted(next)
        val repository = repository ?: return
        val groupId = currentGroupId.value ?: return
        scope.launch { runCatching { repository.setVoiceMuted(groupId, next) } }
    }

    fun leave() {
        participantsJob?.cancel()
        signalsJob?.cancel()
        heartbeatJob?.cancel()
        participantsJob = null
        signalsJob = null
        heartbeatJob = null

        engine?.stop()
        engine = null

        val repository = repository
        val groupId = currentGroupId.value
        if (repository != null && groupId != null) {
            scope.launch { runCatching { repository.leaveVoice(groupId) } }
        }

        offered.clear()
        connectedPeers.value = emptySet()
        participants.value = emptyList()
        muted.value = false
        active.value = false
        joining.value = false
        currentGroupId.value = null
        repository = null
    }

    fun consumeError(): String? = error.value?.also { error.value = null }

    /**
     * 根据参与者列表建立/关闭连接
     */
    private fun reconcile(list: List<Pair<String, VoiceParticipant>>) {
        val engine = engine ?: return
        val remoteUids = list.map { it.first }.filter { it != myUid }.toSet()

        connectedPeers.value = connectedPeers.value.intersect(remoteUids)
        offered.retainAll(remoteUids)

        remoteUids.forEach { peerUid ->
            if (myUid < peerUid && offered.add(peerUid)) {
                engine.createOffer(peerUid)
            }
        }

        participants.value.map { it.first }
            .filter { it != myUid && it !in remoteUids }
            .forEach { engine.closePeer(it) }
    }

    private fun consumeSignal(signalId: String, signal: VoiceSignal) {
        val engine = engine ?: return
        val repository = repository ?: return
        val groupId = currentGroupId.value ?: return

        runCatching {
            when (signal.kind) {
                VoiceSignal.KIND_OFFER -> {
                    offered.add(signal.from)
                    engine.handleRemoteDescription(
                        signal.from,
                        SessionDescription(SessionDescription.Type.OFFER, signal.sdp)
                    )
                }
                VoiceSignal.KIND_ANSWER -> engine.handleRemoteDescription(
                    signal.from,
                    SessionDescription(SessionDescription.Type.ANSWER, signal.sdp)
                )
                VoiceSignal.KIND_CANDIDATE -> engine.handleRemoteCandidate(
                    signal.from,
                    IceCandidate(signal.sdpMid, signal.sdpMLineIndex, signal.sdp)
                )
            }
        }.onFailure { error.value = it.message }

        scope.launch { runCatching { repository.deleteSignal(groupId, signalId) } }
    }

    private val signaling = object : VoiceSignaling {
        override fun onLocalDescription(peerUid: String, description: SessionDescription) {
            val repository = repository ?: return
            val groupId = currentGroupId.value ?: return
            val kind = if (description.type == SessionDescription.Type.OFFER) {
                VoiceSignal.KIND_OFFER
            } else {
                VoiceSignal.KIND_ANSWER
            }
            scope.launch {
                runCatching {
                    repository.sendSignal(
                        groupId,
                        peerUid,
                        VoiceSignal(
                            from = myUid,
                            kind = kind,
                            sdp = description.description,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }.onFailure { error.value = it.message }
            }
        }

        override fun onLocalCandidate(peerUid: String, candidate: IceCandidate) {
            val repository = repository ?: return
            val groupId = currentGroupId.value ?: return
            scope.launch {
                runCatching {
                    repository.sendSignal(
                        groupId,
                        peerUid,
                        VoiceSignal(
                            from = myUid,
                            kind = VoiceSignal.KIND_CANDIDATE,
                            sdp = candidate.sdp,
                            sdpMid = candidate.sdpMid ?: "",
                            sdpMLineIndex = candidate.sdpMLineIndex,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        override fun onPeerConnected(peerUid: String, connected: Boolean) {
            connectedPeers.value = if (connected) {
                connectedPeers.value + peerUid
            } else {
                connectedPeers.value - peerUid
            }
        }

        override fun onEngineError(message: String) {
            error.value = message
        }
    }

    private fun buildIceServers(): List<PeerConnection.IceServer> {
        val config = CommunityConfig.fromSettings()
        val servers = mutableListOf<PeerConnection.IceServer>()
        config.stunUrl.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { servers.add(PeerConnection.IceServer(it)) }
        if (config.turnUrl.isNotBlank()) {
            servers.add(
                PeerConnection.IceServer(config.turnUrl, config.turnUsername, config.turnPassword)
            )
        }
        if (servers.isEmpty()) {
            servers.add(PeerConnection.IceServer("stun:stun.l.google.com:19302"))
        }
        return servers
    }

    private const val HEARTBEAT_INTERVAL = 15_000L
}
