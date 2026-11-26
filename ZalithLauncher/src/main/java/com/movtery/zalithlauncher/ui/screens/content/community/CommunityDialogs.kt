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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.community.CommunityController
import com.movtery.zalithlauncher.setting.CommunitySettings
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog

/**
 * Firebase 与网络配置对话框
 */
@Composable
fun CommunityConfigDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf(CommunitySettings.displayName.getValue()) }
    var apiKey by remember { mutableStateOf(CommunitySettings.apiKey.getValue()) }
    var appId by remember { mutableStateOf(CommunitySettings.appId.getValue()) }
    var projectId by remember { mutableStateOf(CommunitySettings.projectId.getValue()) }
    var senderId by remember { mutableStateOf(CommunitySettings.senderId.getValue()) }
    var storageBucket by remember { mutableStateOf(CommunitySettings.storageBucket.getValue()) }
    var stunUrl by remember { mutableStateOf(CommunitySettings.stunUrl.getValue()) }
    var turnUrl by remember { mutableStateOf(CommunitySettings.turnUrl.getValue()) }
    var turnUsername by remember { mutableStateOf(CommunitySettings.turnUsername.getValue()) }
    var turnPassword by remember { mutableStateOf(CommunitySettings.turnPassword.getValue()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.community_config_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.community_config_summary),
                    style = MaterialTheme.typography.bodySmall
                )
                ConfigField(
                    label = stringResource(R.string.community_config_display_name),
                    value = displayName,
                    onValueChange = { displayName = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_api_key),
                    value = apiKey,
                    onValueChange = { apiKey = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_app_id),
                    value = appId,
                    onValueChange = { appId = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_project_id),
                    value = projectId,
                    onValueChange = { projectId = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_sender_id),
                    value = senderId,
                    onValueChange = { senderId = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_storage_bucket),
                    value = storageBucket,
                    onValueChange = { storageBucket = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_stun),
                    value = stunUrl,
                    onValueChange = { stunUrl = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_turn),
                    value = turnUrl,
                    onValueChange = { turnUrl = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_turn_user),
                    value = turnUsername,
                    onValueChange = { turnUsername = it }
                )
                ConfigField(
                    label = stringResource(R.string.community_config_turn_password),
                    value = turnPassword,
                    onValueChange = { turnPassword = it }
                )
                Text(
                    text = stringResource(R.string.community_config_rules),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    CommunitySettings.displayName.save(displayName.trim())
                    CommunitySettings.apiKey.save(apiKey.trim())
                    CommunitySettings.appId.save(appId.trim())
                    CommunitySettings.projectId.save(projectId.trim())
                    CommunitySettings.senderId.save(senderId.trim())
                    CommunitySettings.storageBucket.save(storageBucket.trim())
                    CommunitySettings.stunUrl.save(stunUrl.trim())
                    CommunitySettings.turnUrl.save(turnUrl.trim())
                    CommunitySettings.turnUsername.save(turnUsername.trim())
                    CommunitySettings.turnPassword.save(turnPassword.trim())
                    CommunityController.reload(context)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.generic_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.generic_cancel))
            }
        }
    )
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true
    )
}

/**
 * 创建群组
 */
@Composable
fun CommunityCreateGroupDialog(onDismiss: () -> Unit, onCreated: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    SimpleEditDialog(
        title = stringResource(R.string.community_create_group),
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(R.string.community_group_name)) },
        singleLine = true,
        onDismissRequest = onDismiss,
        onConfirm = {
            if (name.isNotBlank()) {
                CommunityController.createGroup(name) { groupId -> onCreated(groupId) }
                onDismiss()
            }
        }
    )
}

/**
 * 通过邀请码加入群组
 */
@Composable
fun CommunityJoinGroupDialog(onDismiss: () -> Unit, onJoined: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    SimpleEditDialog(
        title = stringResource(R.string.community_join_group),
        value = code,
        onValueChange = { code = it.uppercase() },
        label = { Text(stringResource(R.string.community_invite_code)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        onDismissRequest = onDismiss,
        onConfirm = {
            if (code.isNotBlank()) {
                CommunityController.joinGroup(code) { groupId -> onJoined(groupId) }
                onDismiss()
            }
        }
    )
}
