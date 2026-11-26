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

package com.movtery.zalithlauncher.ui.screens.content.community

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.community.CommunityController
import com.movtery.zalithlauncher.community.model.CommunityMessage
import com.movtery.zalithlauncher.community.model.MessageRef
import com.movtery.zalithlauncher.community.model.VoiceParticipant
import com.movtery.zalithlauncher.community.voice.VoiceNotePlayer
import com.movtery.zalithlauncher.community.voice.VoiceNoteRecorder
import com.movtery.zalithlauncher.community.voice.VoiceRoomController
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.theme.glass.LiquidGlassSurface
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * 群聊屏幕：实时消息、语音消息与语音房间
 */
@Composable
fun GroupChatScreen(
    key: NormalNavKey.Community.Chat,
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel
) {
    val context = LocalContext.current
    val groups by CommunityController.groups.collectAsStateWithLifecycle()
    val messages by CommunityController.messages.collectAsStateWithLifecycle()
    val members by CommunityController.members.collectAsStateWithLifecycle()
    val myUid by CommunityController.myUid.collectAsStateWithLifecycle()
    val busy by CommunityController.busy.collectAsStateWithLifecycle()

    val voiceActive by VoiceRoomController.active.collectAsStateWithLifecycle()
    val voiceJoining by VoiceRoomController.joining.collectAsStateWithLifecycle()
    val voiceMuted by VoiceRoomController.muted.collectAsStateWithLifecycle()
    val voiceParticipants by VoiceRoomController.participants.collectAsStateWithLifecycle()
    val playingUrl by VoiceNotePlayer.playingUrl.collectAsStateWithLifecycle()

    val group = groups.firstOrNull { it.id == key.groupId }
    LaunchedEffect(key.groupId, group?.id) {
        group?.let { CommunityController.openGroup(it) }
    }

    var input by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    val recorder = remember { VoiceNoteRecorder(context) }
    val listState = rememberLazyListState()

    DisposableEffect(key.groupId) {
        onDispose {
            recorder.cancel()
            CommunityController.closeGroup()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            CommunityController.consumeError()?.let { eventViewModel.sendToast(androidText(it)) }
            VoiceRoomController.consumeError()?.let { eventViewModel.sendToast(androidText(it)) }
            delay(1_500)
        }
    }

    val voicePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            CommunityController.joinVoice(context, key.groupId)
        } else {
            eventViewModel.sendToast(
                androidText(context.getString(R.string.community_voice_needs_permission))
            )
        }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (recorder.start()) recording = true
        } else {
            eventViewModel.sendToast(
                androidText(context.getString(R.string.community_mic_permission))
            )
        }
    }

    val hasMicPermission: () -> Boolean = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    val toggleRecording: () -> Unit = {
        if (recording) {
            val result = recorder.stop()
            recording = false
            if (result != null) {
                CommunityController.sendVoiceNote(result.file, result.durationMs)
            }
        } else if (hasMicPermission()) {
            if (recorder.start()) recording = true
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val toggleVoice: () -> Unit = {
        if (voiceActive) {
            CommunityController.leaveVoice()
        } else if (hasMicPermission()) {
            CommunityController.joinVoice(context, key.groupId)
        } else {
            voicePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val now = System.currentTimeMillis()
    val typingNames = members
        .filter { it.first != myUid && it.second.typingUntil > now }
        .map { it.second.name }
    val onlineCount = members.count { now - it.second.lastSeen < 60_000L }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LiquidGlassSurface(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group?.group?.name ?: key.groupName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.community_online, onlineCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = toggleVoice) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volume_up_filled),
                        contentDescription = stringResource(R.string.community_voice_join),
                        tint = if (voiceActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (group != null && group.group.inviteCode.isNotBlank()) {
                    IconButton(
                        onClick = {
                            copyInviteCode(context, group.group.inviteCode)
                            eventViewModel.sendToast(
                                androidText(context.getString(R.string.community_invite_copied))
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_content_copy_filled),
                            contentDescription = stringResource(R.string.community_copy_invite)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        CommunityController.leaveGroup(key.groupId)
                        backStackViewModel.communityScreen.backStack.removeLastOrNull()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_exit_to_app),
                        contentDescription = stringResource(R.string.community_leave_group)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = messages, key = { it.id }) { message ->
                MessageBubble(
                    modifier = Modifier.animateItem(),
                    message = message,
                    mine = message.message.senderId == myUid,
                    playingUrl = playingUrl,
                    onPlay = { url -> VoiceNotePlayer.toggle(url) }
                )
            }
        }

        AnimatedVisibility(
            visible = typingNames.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = stringResource(R.string.community_typing, typingNames.joinToString(", ")),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = voiceActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            VoiceBar(
                participants = voiceParticipants,
                muted = voiceMuted,
                onMute = { VoiceRoomController.toggleMute() },
                onLeave = { CommunityController.leaveVoice() }
            )
        }

        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            backdropBlur = false
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        CommunityController.notifyTyping()
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.community_message_hint)) },
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp)
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            CommunityController.sendText(input)
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send_filled),
                        contentDescription = stringResource(R.string.community_send)
                    )
                }
                IconButton(onClick = toggleRecording) {
                    Icon(
                        painter = painterResource(
                            if (recording) R.drawable.ic_close else R.drawable.ic_mic_filled
                        ),
                        contentDescription = if (recording) {
                            stringResource(R.string.community_recording)
                        } else {
                            stringResource(R.string.community_record)
                        },
                        tint = if (recording) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = recording || busy,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = if (recording) {
                    stringResource(R.string.community_recording)
                } else {
                    stringResource(R.string.community_connecting)
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MessageBubble(
    modifier: Modifier = Modifier,
    message: MessageRef,
    mine: Boolean,
    playingUrl: String?,
    onPlay: (String) -> Unit
) {
    val container = if (mine) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = container,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (mine) 20.dp else 6.dp,
                bottomEnd = if (mine) 6.dp else 20.dp
            ),
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                if (!mine) {
                    Text(
                        text = message.message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (message.message.type == CommunityMessage.TYPE_VOICE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = { onPlay(message.message.audioUrl) }) {
                            Icon(
                                painter = painterResource(
                                    if (playingUrl == message.message.audioUrl) {
                                        R.drawable.ic_pause_filled
                                    } else {
                                        R.drawable.ic_play_arrow_filled
                                    }
                                ),
                                contentDescription = stringResource(R.string.community_voice_message)
                            )
                        }
                        Text(
                            text = formatDuration(message.message.audioDurationMs),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = message.message.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = formatTime(message.message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VoiceBar(
    participants: List<Pair<String, VoiceParticipant>>,
    muted: Boolean,
    onMute: () -> Unit,
    onLeave: () -> Unit
) {
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        backdropBlur = false
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.community_voice_in_room, participants.size),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = participants.joinToString(", ") { it.second.name },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onMute) {
                Icon(
                    painter = painterResource(
                        if (muted) R.drawable.ic_mic_off_filled else R.drawable.ic_mic_filled
                    ),
                    contentDescription = if (muted) {
                        stringResource(R.string.community_voice_unmute)
                    } else {
                        stringResource(R.string.community_voice_mute)
                    }
                )
            }
            IconButton(onClick = onLeave) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.community_voice_leave)
                )
            }
        }
    }
}

private fun copyInviteCode(context: Context, code: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    manager?.setPrimaryClip(ClipData.newPlainText("invite", code))
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).toInt().coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
