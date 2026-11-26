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

package com.movtery.zalithlauncher.ui.screens.main.control_editor.preset

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.rememberDialogMaxHeight
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.ui.theme.onItemColor

/**
 * 控件预设目录弹窗，选中分类或搜索后点击任意预设即可添加到当前控件层
 * @param visible 是否显示
 * @param onAddPreset 添加一个预设控件，同时回传已本地化的控件名称
 * @param onAddStarterPack 一键添加整套快速战斗按钮
 */
@Composable
fun ControlPresetCatalogDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onAddPreset: (preset: ControlPreset, label: String) -> Unit,
    onAddStarterPack: () -> Unit
) {
    if (!visible) return

    /** null 代表展示全部分类 */
    var selectedCategory by remember { mutableStateOf<PresetCategory?>(null) }
    var query by remember { mutableStateOf("") }
    var addedCount by remember { mutableIntStateOf(0) }

    val entries = ControlPresets.map { preset ->
        val arg = preset.labelArg
        val label = if (arg != null) {
            stringResource(preset.labelRes, arg)
        } else {
            stringResource(preset.labelRes)
        }
        preset to label
    }

    val keyword = query.trim()
    val filtered = remember(entries, selectedCategory, keyword) {
        entries.filter { (preset, label) ->
            (selectedCategory == null || preset.category == selectedCategory) &&
                    (keyword.isEmpty() || label.contains(keyword, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = {
            addedCount = 0
            onDismissRequest()
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(all = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.control_preset_catalog_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                addedCount = 0
                                onDismissRequest()
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.generic_close)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.control_preset_catalog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = onCardColor().copy(alpha = 0.7f)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            onAddStarterPack()
                            addedCount += StarterPackPlacements.size
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.control_preset_starter_pack),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.control_preset_starter_pack_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        placeholder = {
                            Text(text = stringResource(R.string.control_preset_search_hint))
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = null
                            )
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = {
                                Text(text = stringResource(R.string.control_preset_category_all))
                            }
                        )
                        PresetCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory =
                                        category.takeIf { selectedCategory != category }
                                },
                                label = {
                                    Text(text = stringResource(category.labelRes))
                                }
                            )
                        }
                    }

                    if (filtered.isEmpty()) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            text = stringResource(R.string.control_preset_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onCardColor().copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            state = rememberLazyListState(),
                            contentPadding = PaddingValues(vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filtered.chunked(3),
                                key = { row -> row.joinToString("|") { it.first.id } }
                            ) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { (preset, label) ->
                                        PresetButton(
                                            modifier = Modifier.weight(1f),
                                            label = label,
                                            keyLabel = preset.keyLabel,
                                            onClick = {
                                                onAddPreset(preset, label)
                                                addedCount++
                                            }
                                        )
                                    }
                                    repeat(3 - row.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.control_preset_added, addedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = onCardColor().copy(alpha = 0.7f)
                        )
                        FilledTonalButton(
                            onClick = {
                                addedCount = 0
                                onDismissRequest()
                            }
                        ) {
                            Text(text = stringResource(R.string.generic_done))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    keyLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "presetPressScale"
    )

    Surface(
        modifier = modifier.graphicsLayer(
            scaleX = pressScale,
            scaleY = pressScale
        ),
        shape = MaterialTheme.shapes.medium,
        color = itemColor(false),
        contentColor = onItemColor(),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (keyLabel != null) {
                Text(
                    text = keyLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = onItemColor().copy(alpha = 0.65f),
                    maxLines = 1
                )
            }
        }
    }
}
