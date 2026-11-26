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

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.community.CommunityController
import com.movtery.zalithlauncher.community.FirebaseBootstrap
import com.movtery.zalithlauncher.community.model.GroupRef
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.theme.glass.LiquidGlassCard
import com.movtery.zalithlauncher.ui.theme.glass.LiquidGlassSurface
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast

/**
 * 社区主页：群组列表、创建与加入
 */
@Composable
fun CommunityScreen(
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel
) {
    val context = LocalContext.current
    val statusState by CommunityController.status.collectAsStateWithLifecycle()
    val groups by CommunityController.groups.collectAsStateWithLifecycle()
    val unread by CommunityController.unreadGroups.collectAsStateWithLifecycle()
    val busy by CommunityController.busy.collectAsStateWithLifecycle()

    var showConfig by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { CommunityController.attach(context) }

    LaunchedEffect(Unit) {
        while (true) {
            CommunityController.consumeError()?.let { eventViewModel.sendToast(androidText(it)) }
            kotlinx.coroutines.delay(1_500)
        }
    }

    val status = statusState
    val configured = status !is FirebaseBootstrap.Status.NotConfigured

    val openChat: (GroupRef) -> Unit = { group ->
        backStackViewModel.communityScreen.backStack.navigateTo(
            NormalNavKey.Community.Chat(groupId = group.id, groupName = group.group.name)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.community_title),
                    style = MaterialTheme.typography.titleLarge
                )
                val subtitle = when (status) {
                    is FirebaseBootstrap.Status.Ready -> stringResource(R.string.community_connected)
                    is FirebaseBootstrap.Status.Initializing -> stringResource(R.string.community_connecting)
                    is FirebaseBootstrap.Status.Failed -> stringResource(R.string.community_failed)
                    else -> stringResource(R.string.community_setup_title)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = { showConfig = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_filled),
                    contentDescription = stringResource(R.string.community_config_title)
                )
            }
        }

        if (!configured) {
            SetupCard(
                message = (status as? FirebaseBootstrap.Status.Failed)?.message,
                onConfigure = { showConfig = true }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = { showCreate = true }) {
                    Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.community_create_group))
                }
                FilledTonalButton(onClick = { showJoin = true }) {
                    Icon(painter = painterResource(R.drawable.ic_link), contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.community_join_group))
                }
            }

            if (groups.isEmpty()) {
                EmptyCard()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = groups, key = { it.id }) { group ->
                        GroupRow(
                            modifier = Modifier.animateItem(),
                            group = group,
                            unread = unread.contains(group.id),
                            onClick = { openChat(group) }
                        )
                    }
                }
            }
        }
    }

    if (showConfig) {
        CommunityConfigDialog(onDismiss = { showConfig = false })
    }
    if (showCreate) {
        CommunityCreateGroupDialog(
            onDismiss = { showCreate = false },
            onCreated = { groupId ->
                groups.firstOrNull { it.id == groupId }?.let(openChat)
            }
        )
    }
    if (showJoin) {
        CommunityJoinGroupDialog(
            onDismiss = { showJoin = false },
            onJoined = { groupId ->
                groups.firstOrNull { it.id == groupId }?.let(openChat)
            }
        )
    }
}

@Composable
private fun SetupCard(message: String?, onConfigure: () -> Unit) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.community_setup_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.community_setup_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            FilledTonalButton(onClick = onConfigure) {
                Text(stringResource(R.string.community_setup_action))
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    LiquidGlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.community_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.community_empty_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupRow(
    modifier: Modifier = Modifier,
    group: GroupRef,
    unread: Boolean,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_forum_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.group.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = group.group.lastMessage.ifBlank {
                        stringResource(R.string.community_empty_summary)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
