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

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.movtery.zalithlauncher.community.model.CommunityGroup
import com.movtery.zalithlauncher.community.model.CommunityMember
import com.movtery.zalithlauncher.community.model.CommunityMessage
import com.movtery.zalithlauncher.community.model.GroupRef
import com.movtery.zalithlauncher.community.model.MessageRef
import com.movtery.zalithlauncher.community.model.VoiceParticipant
import com.movtery.zalithlauncher.community.model.VoiceSignal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * 社区数据源：群组、消息、成员、语音房间与信令
 */
class CommunityRepository(
    app: FirebaseApp,
    val uid: String,
    val displayName: String
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(app)
    private val storage: FirebaseStorage? = runCatching { FirebaseStorage.getInstance(app) }.getOrNull()

    private val groups get() = firestore.collection(COL_GROUPS)

    private fun groupDoc(groupId: String) = groups.document(groupId)

    private fun memberDoc(groupId: String) = groupDoc(groupId).collection(COL_MEMBERS).document(uid)

    private fun voiceDoc(groupId: String) = groupDoc(groupId).collection(COL_VOICE).document(uid)

    // ---------------------------------------------------------------- 群组

    fun groupsFlow(): Flow<List<GroupRef>> = callbackFlow {
        val registration = groups
            .whereArrayContains(FIELD_MEMBER_IDS, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val result = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(CommunityGroup::class.java)?.let { GroupRef(document.id, it) }
                }.orEmpty()
                trySend(result.sortedByDescending { it.group.lastMessageAt })
            }
        awaitClose { registration.remove() }
    }

    suspend fun createGroup(name: String): String {
        val now = System.currentTimeMillis()
        val document = groups.document()
        document.set(
            mapOf(
                "name" to name,
                "ownerId" to uid,
                "inviteCode" to generateInviteCode(),
                "memberIds" to listOf(uid),
                "createdAt" to now,
                "lastMessage" to "",
                "lastMessageAt" to 0L,
                "lastSenderName" to ""
            )
        ).awaitUnit()
        upsertMember(document.id)
        return document.id
    }

    suspend fun joinGroup(inviteCode: String): String {
        val snapshot = groups
            .whereEqualTo("inviteCode", inviteCode.trim().uppercase())
            .limit(1)
            .get()
            .awaitOrThrow()
        val document = snapshot.documents.firstOrNull()
            ?: throw IllegalStateException("Invite code not found")
        document.reference.update(FIELD_MEMBER_IDS, FieldValue.arrayUnion(uid)).awaitUnit()
        upsertMember(document.id)
        return document.id
    }

    suspend fun leaveGroup(groupId: String) {
        groupDoc(groupId).update(FIELD_MEMBER_IDS, FieldValue.arrayRemove(uid)).awaitUnit()
        memberDoc(groupId).delete().awaitUnit()
    }

    // ---------------------------------------------------------------- 消息

    fun messagesFlow(groupId: String): Flow<List<MessageRef>> = callbackFlow {
        val registration = groupDoc(groupId)
            .collection(COL_MESSAGES)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(MESSAGE_PAGE_SIZE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val result = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(CommunityMessage::class.java)?.let { MessageRef(document.id, it) }
                }.orEmpty()
                trySend(result.reversed())
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(groupId: String, message: CommunityMessage) {
        val payload = mapOf(
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "text" to message.text,
            "type" to message.type,
            "audioUrl" to message.audioUrl,
            "audioDurationMs" to message.audioDurationMs,
            "createdAt" to message.createdAt
        )
        groupDoc(groupId).collection(COL_MESSAGES).document().set(payload).awaitUnit()
        groupDoc(groupId).update(
            mapOf(
                "lastMessage" to previewOf(message),
                "lastMessageAt" to message.createdAt,
                "lastSenderName" to message.senderName
            )
        ).awaitUnit()
    }

    /**
     * 上传语音消息并返回下载地址
     */
    suspend fun uploadVoiceNote(groupId: String, file: File): String {
        val bucket = storage ?: throw IllegalStateException("Firebase Storage is not configured")
        val reference = bucket.reference.child("voice/$groupId/$uid/${System.currentTimeMillis()}_${file.name}")
        reference.putFile(Uri.fromFile(file)).awaitUnit()
        return reference.downloadUrl.awaitOrThrow().toString()
    }

    // ---------------------------------------------------------------- 成员

    fun membersFlow(groupId: String): Flow<List<Pair<String, CommunityMember>>> = callbackFlow {
        val registration = groupDoc(groupId)
            .collection(COL_MEMBERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val result = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(CommunityMember::class.java)?.let { document.id to it }
                }.orEmpty()
                trySend(result)
            }
        awaitClose { registration.remove() }
    }

    suspend fun upsertMember(groupId: String) {
        val now = System.currentTimeMillis()
        memberDoc(groupId).set(
            mapOf(
                "name" to displayName,
                "joinedAt" to now,
                "lastSeen" to now,
                "typingUntil" to 0L
            ),
            SetOptions.merge()
        ).awaitUnit()
    }

    suspend fun heartbeat(groupId: String) {
        memberDoc(groupId).set(
            mapOf(
                "name" to displayName,
                "lastSeen" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).awaitUnit()
    }

    suspend fun updateTyping(groupId: String, typingUntil: Long) {
        memberDoc(groupId).set(mapOf("typingUntil" to typingUntil), SetOptions.merge()).awaitUnit()
    }

    // ---------------------------------------------------------------- 语音房间

    fun voiceParticipantsFlow(groupId: String): Flow<List<Pair<String, VoiceParticipant>>> = callbackFlow {
        val registration = groupDoc(groupId)
            .collection(COL_VOICE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val result = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(VoiceParticipant::class.java)?.let { document.id to it }
                }.orEmpty()
                trySend(result.sortedBy { it.second.joinedAt })
            }
        awaitClose { registration.remove() }
    }

    suspend fun joinVoice(groupId: String) {
        val now = System.currentTimeMillis()
        voiceDoc(groupId).set(
            mapOf(
                "name" to displayName,
                "muted" to false,
                "joinedAt" to now,
                "updatedAt" to now
            )
        ).awaitUnit()
    }

    suspend fun leaveVoice(groupId: String) {
        voiceDoc(groupId).delete().awaitUnit()
    }

    suspend fun setVoiceMuted(groupId: String, muted: Boolean) {
        voiceDoc(groupId).set(
            mapOf("muted" to muted, "updatedAt" to System.currentTimeMillis()),
            SetOptions.merge()
        ).awaitUnit()
    }

    /**
     * 监听发送给我的信令
     */
    fun signalsFlow(groupId: String): Flow<List<Pair<String, VoiceSignal>>> = callbackFlow {
        val registration = voiceDoc(groupId)
            .collection(COL_SIGNALS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .limit(SIGNAL_PAGE_SIZE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val result = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(VoiceSignal::class.java)?.let { document.id to it }
                }.orEmpty()
                trySend(result)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendSignal(groupId: String, targetUid: String, signal: VoiceSignal) {
        groupDoc(groupId).collection(COL_VOICE)
            .document(targetUid)
            .collection(COL_SIGNALS)
            .document()
            .set(
                mapOf(
                    "from" to signal.from,
                    "kind" to signal.kind,
                    "sdp" to signal.sdp,
                    "sdpMid" to signal.sdpMid,
                    "sdpMLineIndex" to signal.sdpMLineIndex,
                    "createdAt" to signal.createdAt
                )
            )
            .awaitUnit()
    }

    suspend fun deleteSignal(groupId: String, signalId: String) {
        voiceDoc(groupId).collection(COL_SIGNALS).document(signalId).delete().awaitUnit()
    }

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(6) { append(alphabet[Random.nextInt(alphabet.length)]) }
        }
    }

    private fun previewOf(message: CommunityMessage): String = when (message.type) {
        CommunityMessage.TYPE_VOICE -> "🎤 Voice message"
        else -> message.text
    }

    companion object {
        const val COL_GROUPS = "groups"
        const val COL_MESSAGES = "messages"
        const val COL_MEMBERS = "members"
        const val COL_VOICE = "voice"
        const val COL_SIGNALS = "signals"
        const val FIELD_MEMBER_IDS = "memberIds"
        const val FIELD_CREATED_AT = "createdAt"
        private const val MESSAGE_PAGE_SIZE = 200L
        private const val SIGNAL_PAGE_SIZE = 40L
    }
}

/**
 * 等待任务完成，不关心结果
 */
internal suspend fun <T> Task<T>.awaitUnit() = suspendCancellableCoroutine<Unit> { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) {
            continuation.resumeWithException(error)
        } else {
            continuation.resume(Unit)
        }
    }
}

/**
 * 等待任务完成并返回结果
 */
internal suspend fun <T> Task<T>.awaitOrThrow(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) {
            continuation.resumeWithException(error)
        } else {
            val result = task.result
            if (result == null) {
                continuation.resumeWithException(IllegalStateException("Task returned no result"))
            } else {
                continuation.resume(result)
            }
        }
    }
}
