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
import android.media.AudioManager
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * 语音引擎回调
 */
interface VoiceSignaling {
    fun onLocalDescription(peerUid: String, description: SessionDescription)
    fun onLocalCandidate(peerUid: String, candidate: IceCandidate)
    fun onPeerConnected(peerUid: String, connected: Boolean)
    fun onEngineError(message: String)
}

/**
 * 纯音频 WebRTC 引擎，采用网状（mesh）结构：每个参与者与其他参与者各建立一条连接
 */
class VoiceEngine(
    private val context: Context,
    private val signaling: VoiceSignaling
) {
    private var factory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localTrack: AudioTrack? = null
    private var localStream: MediaStream? = null
    private val peers = mutableMapOf<String, PeerConnection>()
    private var iceServers: List<PeerConnection.IceServer> = emptyList()
    private var audioManager: AudioManager? = null
    private var previousMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeaker: Boolean = false

    fun start(iceServers: List<PeerConnection.IceServer>) {
        if (factory != null) return
        this.iceServers = iceServers

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        val created = PeerConnectionFactory.builder().createPeerConnectionFactory()
        factory = created

        val source = created.createAudioSource(MediaConstraints())
        audioSource = source
        val track = created.createAudioTrack("nl_voice", source)
        localTrack = track
        val stream = created.createLocalMediaStream("nl_voice_stream")
        stream.addTrack(track)
        localStream = stream

        routeAudio(true)
    }

    fun createPeer(peerUid: String): PeerConnection? {
        peers[peerUid]?.let { return it }
        val factory = factory ?: return null

        val configuration = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val connection = factory.createPeerConnection(configuration, observerFor(peerUid)) ?: return null
        localTrack?.let { track -> connection.addTrack(track, listOf(STREAM_ID)) }
        peers[peerUid] = connection
        return connection
    }

    fun createOffer(peerUid: String) {
        val connection = createPeer(peerUid) ?: return
        connection.createOffer(sdpObserver(peerUid), MediaConstraints())
    }

    fun handleRemoteDescription(peerUid: String, description: SessionDescription) {
        val connection = createPeer(peerUid) ?: return
        connection.setRemoteDescription(sdpObserver(peerUid), description)
        if (description.type == SessionDescription.Type.OFFER) {
            connection.createAnswer(sdpObserver(peerUid), MediaConstraints())
        }
    }

    fun handleRemoteCandidate(peerUid: String, candidate: IceCandidate) {
        createPeer(peerUid)?.addIceCandidate(candidate)
    }

    fun setMuted(muted: Boolean) {
        localTrack?.setEnabled(!muted)
    }

    fun closePeer(peerUid: String) {
        peers.remove(peerUid)?.let { connection ->
            runCatching { connection.close() }
            runCatching { connection.dispose() }
        }
    }

    fun stop() {
        peers.keys.toList().forEach { closePeer(it) }
        peers.clear()
        runCatching { localStream?.dispose() }
        runCatching { localTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { factory?.dispose() }
        localStream = null
        localTrack = null
        audioSource = null
        factory = null
        routeAudio(false)
    }

    private fun routeAudio(enable: Boolean) {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = manager
        if (enable) {
            previousMode = manager.mode
            previousSpeaker = manager.isSpeakerphoneOn
            manager.mode = AudioManager.MODE_IN_COMMUNICATION
            manager.isSpeakerphoneOn = true
        } else {
            manager.isSpeakerphoneOn = previousSpeaker
            manager.mode = previousMode
        }
    }

    private fun sdpObserver(peerUid: String) = object : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription) {
            peers[peerUid]?.setLocalDescription(this, description)
            signaling.onLocalDescription(peerUid, description)
        }

        override fun onSetSuccess() {}

        override fun onCreateFailure(error: String) {
            signaling.onEngineError(error)
        }

        override fun onSetFailure(error: String) {
            signaling.onEngineError(error)
        }
    }

    private fun observerFor(peerUid: String) = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            val connected = state == PeerConnection.IceConnectionState.CONNECTED ||
                state == PeerConnection.IceConnectionState.COMPLETED
            signaling.onPeerConnected(peerUid, connected)
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {}

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let { signaling.onLocalCandidate(peerUid, it) }
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

        override fun onAddStream(stream: MediaStream?) {}

        override fun onRemoveStream(stream: MediaStream?) {}

        override fun onDataChannel(channel: DataChannel?) {}

        override fun onRenegotiationNeeded() {}

        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
    }

    companion object {
        private const val STREAM_ID = "nl_voice_stream"
    }
}
