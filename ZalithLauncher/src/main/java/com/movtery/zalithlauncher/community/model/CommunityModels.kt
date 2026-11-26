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

package com.movtery.zalithlauncher.community.model

/**
 * 群组（Firestore: groups/{groupId}）
 */
data class CommunityGroup(
    val name: String = "",
    val ownerId: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastSenderName: String = ""
)

/**
 * 群聊消息（Firestore: groups/{groupId}/messages/{messageId}）
 */
data class CommunityMessage(
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val type: String = TYPE_TEXT,
    val audioUrl: String = "",
    val audioDurationMs: Long = 0L,
    val createdAt: Long = 0L
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_VOICE = "voice"
    }
}

/**
 * 群成员（Firestore: groups/{groupId}/members/{uid}）
 */
data class CommunityMember(
    val name: String = "",
    val joinedAt: Long = 0L,
    val lastSeen: Long = 0L,
    val typingUntil: Long = 0L
)

/**
 * 语音房间参与者（Firestore: groups/{groupId}/voice/{uid}）
 */
data class VoiceParticipant(
    val name: String = "",
    val muted: Boolean = false,
    val joinedAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * WebRTC 信令（Firestore: groups/{groupId}/voice/{uid}/signals/{signalId}）
 */
data class VoiceSignal(
    val from: String = "",
    val kind: String = "",
    val sdp: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val createdAt: Long = 0L
) {
    companion object {
        const val KIND_OFFER = "offer"
        const val KIND_ANSWER = "answer"
        const val KIND_CANDIDATE = "candidate"
    }
}

/**
 * 带文档 id 的模型包装
 */
data class GroupRef(val id: String, val group: CommunityGroup)

data class MessageRef(val id: String, val message: CommunityMessage)
