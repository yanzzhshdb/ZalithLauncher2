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

package com.movtery.zalithlauncher.ui.screens.content.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.tools.InstanceNotes
import com.movtery.zalithlauncher.tools.InstanceTools
import com.movtery.zalithlauncher.tools.LogTools
import com.movtery.zalithlauncher.tools.RamTuner
import com.movtery.zalithlauncher.tools.ReportBundler
import com.movtery.zalithlauncher.tools.SettingsBackup
import com.movtery.zalithlauncher.tools.ToolsStorage
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedContext
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "ToolsScreen"
private const val KEEP_LAUNCHER_LOGS = 5

private data class ToolsSnapshot(
    val cacheBytes: Long = 0L,
    val imageBytes: Long = 0L,
    val logBytes: Long = 0L,
    val logCount: Int = 0,
    val instances: List<InstanceTools.InstanceSize> = emptyList(),
    val bigFiles: List<File> = emptyList(),
    val ram: RamTuner.Recommendation? = null,
    val freeSpace: Long = 0L,
    val totalSpace: Long = 0L,
    val modsCount: Int = 0
) {
    val instanceBytes: Long get() = instances.sumOf { it.bytes }
}

private sealed interface ConfirmAction {
    data object CleanCaches : ConfirmAction
    data object CleanLogs : ConfirmAction
    data object ImportSettings : ConfirmAction
    data class DeleteFile(val file: File) : ConfirmAction
}

private data class ResultDialog(val title: String, val text: String)

@Composable
fun ToolsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Tools, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var refreshKey by remember { mutableIntStateOf(0) }
        var snapshot by remember { mutableStateOf(ToolsSnapshot()) }
        var busy by remember { mutableStateOf(false) }
        var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
        var resultDialog by remember { mutableStateOf<ResultDialog?>(null) }
        var noteTarget by remember { mutableStateOf<String?>(null) }
        var noteDraft by remember { mutableStateOf("") }

        LaunchedEffect(refreshKey) {
            snapshot = withContext(Dispatchers.IO) { loadSnapshot(context) }
        }

        fun toast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                scope.launch {
                    busy = true
                    runCatching { SettingsBackup.exportTo(context, uri) }
                        .onSuccess {
                            resultDialog = ResultDialog(
                                context.getString(R.string.tools_backup_title),
                                context.getString(R.string.tools_backup_export_done)
                            )
                        }
                        .onFailure {
                            Logger.warning(TAG, "Failed to export settings", it)
                            resultDialog = ResultDialog(
                                context.getString(R.string.tools_backup_title),
                                context.getString(R.string.tools_backup_failed, it.message.orEmpty())
                            )
                        }
                    busy = false
                }
            }
        }

        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                scope.launch {
                    busy = true
                    runCatching { SettingsBackup.importFrom(context, uri) }
                        .onSuccess { count ->
                            resultDialog = ResultDialog(
                                context.getString(R.string.tools_backup_title),
                                context.getString(R.string.tools_backup_import_done, count)
                            )
                        }
                        .onFailure {
                            Logger.warning(TAG, "Failed to import settings", it)
                            resultDialog = ResultDialog(
                                context.getString(R.string.tools_backup_title),
                                context.getString(R.string.tools_backup_failed, it.message.orEmpty())
                            )
                        }
                    busy = false
                    refreshKey++
                }
            }
        }

        AnimatedLazyColumn(
            modifier = Modifier.fillMaxSize(),
            isVisible = isVisible,
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            animatedItem { ctx ->
                SummaryCard(ctx, snapshot) {
                    refreshKey++
                }
            }

            animatedItem { ctx ->
                RamCard(
                    ctx = ctx,
                    snapshot = snapshot,
                    enabled = !busy,
                    onApplyGlobal = {
                        val ram = snapshot.ram ?: return@RamCard
                        RamTuner.applyGlobal(ram)
                        toast(context.getString(R.string.tools_ram_applied_global, ram.recommendedMb))
                        refreshKey++
                    },
                    onApplyInstances = {
                        val ram = snapshot.ram ?: return@RamCard
                        scope.launch {
                            busy = true
                            val count = withContext(Dispatchers.IO) { RamTuner.applyToInstances(ram) }
                            busy = false
                            toast(context.getString(R.string.tools_ram_applied_instances, count))
                            refreshKey++
                        }
                    }
                )
            }

            animatedItem { ctx ->
                CleanCard(
                    ctx = ctx,
                    snapshot = snapshot,
                    enabled = !busy,
                    onClean = { confirmAction = ConfirmAction.CleanCaches }
                )
            }

            animatedItem { ctx ->
                InstanceSizeCard(ctx, snapshot)
            }

            animatedItem { ctx ->
                BigFilesCard(
                    ctx = ctx,
                    snapshot = snapshot,
                    enabled = !busy,
                    onDelete = { file -> confirmAction = ConfirmAction.DeleteFile(file) }
                )
            }

            animatedItem { ctx ->
                LogCard(
                    ctx = ctx,
                    snapshot = snapshot,
                    enabled = !busy,
                    onClean = { confirmAction = ConfirmAction.CleanLogs }
                )
            }

            animatedItem { ctx ->
                HealthCard(
                    ctx = ctx,
                    enabled = !busy,
                    onCheck = {
                        scope.launch {
                            busy = true
                            val reports = withContext(Dispatchers.IO) { InstanceTools.healthCheck() }
                            busy = false
                            resultDialog = ResultDialog(
                                context.getString(R.string.tools_health_title),
                                formatHealthReports(context, reports)
                            )
                        }
                    }
                )
            }

            animatedItem { ctx ->
                DuplicateModsCard(
                    ctx = ctx,
                    enabled = !busy,
                    onScan = {
                        scope.launch {
                            busy = true
                            val groups = withContext(Dispatchers.IO) { InstanceTools.duplicateMods() }
                            busy = false
                            resultDialog = ResultDialog(
                                context.getString(R.string.tools_mods_title),
                                formatDuplicateGroups(context, groups)
                            )
                        }
                    }
                )
            }

            animatedItem { ctx ->
                BackupCard(
                    ctx = ctx,
                    enabled = !busy,
                    onExport = {
                        exportLauncher.launch(
                            Intent(Intent.ACTION_CREATE_DOCUMENT)
                                .addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("application/json")
                                .putExtra(Intent.EXTRA_TITLE, "norxlauncher-settings.json")
                        )
                    },
                    onImport = { confirmAction = ConfirmAction.ImportSettings }
                )
            }

            animatedItem { ctx ->
                ReportCard(
                    ctx = ctx,
                    enabled = !busy,
                    onCreate = {
                        scope.launch {
                            busy = true
                            runCatching { withContext(Dispatchers.IO) { ReportBundler.create(context) } }
                                .onSuccess { file ->
                                    busy = false
                                    shareFile(context, file)
                                }
                                .onFailure {
                                    busy = false
                                    Logger.warning(TAG, "Failed to create report", it)
                                    toast(context.getString(R.string.tools_report_failed, it.message.orEmpty()))
                                }
                        }
                    }
                )
            }

            animatedItem { ctx ->
                NotesCard(
                    ctx = ctx,
                    snapshot = snapshot,
                    onEdit = { versionName ->
                        noteTarget = versionName
                        noteDraft = InstanceNotes.noteOf(versionName)
                    }
                )
            }
        }

        confirmAction?.let { action ->
            val (title, message) = when (action) {
                is ConfirmAction.CleanCaches -> stringResource(R.string.tools_clean_confirm_title) to
                    stringResource(
                        R.string.tools_clean_confirm_message,
                        ToolsStorage.formatSize(snapshot.cacheBytes + snapshot.imageBytes)
                    )
                is ConfirmAction.CleanLogs -> stringResource(R.string.tools_logs_clean_title) to
                    stringResource(R.string.tools_logs_clean_message, KEEP_LAUNCHER_LOGS)
                is ConfirmAction.ImportSettings -> stringResource(R.string.tools_backup_import_confirm_title) to
                    stringResource(R.string.tools_backup_import_confirm_message)
                is ConfirmAction.DeleteFile -> stringResource(R.string.tools_big_delete_title) to
                    stringResource(R.string.tools_big_delete_message, action.file.name)
            }

            SimpleAlertDialog(
                title = title,
                text = message,
                onConfirm = {
                    confirmAction = null
                    when (action) {
                        is ConfirmAction.CleanCaches -> scope.launch {
                            busy = true
                            val freed = withContext(Dispatchers.IO) {
                                ToolsStorage.clearDirs(
                                    listOf(
                                        PathManager.DIR_CACHE_GAME_DOWNLOADER,
                                        PathManager.DIR_CACHE_MODPACK_DOWNLOADER,
                                        PathManager.DIR_CACHE_MODPACK_EXPORTER,
                                        PathManager.DIR_CACHE_SAVE_EXPORTER,
                                        PathManager.DIR_CACHE_MOD_UPDATER,
                                        PathManager.DIR_IMAGE_CACHE,
                                        PathManager.DIR_CACHE_APP_ICON,
                                        PathManager.DIR_CACHE_HOME_PAGE
                                    )
                                )
                            }
                            busy = false
                            toast(context.getString(R.string.tools_clean_done, ToolsStorage.formatSize(freed)))
                            refreshKey++
                        }
                        is ConfirmAction.CleanLogs -> scope.launch {
                            busy = true
                            val (count, freed) = withContext(Dispatchers.IO) {
                                LogTools.deleteOldLauncherLogs(KEEP_LAUNCHER_LOGS)
                            }
                            busy = false
                            toast(
                                context.getString(
                                    R.string.tools_logs_clean_done,
                                    count,
                                    ToolsStorage.formatSize(freed)
                                )
                            )
                            refreshKey++
                        }
                        is ConfirmAction.ImportSettings -> importLauncher.launch(
                            Intent(Intent.ACTION_OPEN_DOCUMENT)
                                .addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("*/*")
                        )
                        is ConfirmAction.DeleteFile -> scope.launch {
                            val deleted = withContext(Dispatchers.IO) {
                                ToolsStorage.deleteFile(action.file)
                            }
                            toast(
                                context.getString(
                                    if (deleted) R.string.tools_big_deleted else R.string.tools_big_failed
                                )
                            )
                            refreshKey++
                        }
                    }
                },
                onDismiss = { confirmAction = null }
            )
        }

        resultDialog?.let { dialog ->
            SimpleAlertDialog(
                title = dialog.title,
                text = dialog.text,
                onDismiss = { resultDialog = null }
            )
        }

        noteTarget?.let { versionName ->
            SimpleEditDialog(
                title = stringResource(R.string.tools_notes_edit_title, versionName),
                value = noteDraft,
                onValueChange = { noteDraft = it },
                singleLine = false,
                maxLines = 4,
                onDismissRequest = { noteTarget = null },
                onConfirm = {
                    InstanceNotes.save(versionName, noteDraft)
                    noteTarget = null
                    toast(context.getString(R.string.tools_notes_saved))
                    refreshKey++
                }
            )
        }
    }
}

private fun loadSnapshot(context: Context): ToolsSnapshot {
    val cacheDirs = listOf(
        PathManager.DIR_CACHE_GAME_DOWNLOADER,
        PathManager.DIR_CACHE_MODPACK_DOWNLOADER,
        PathManager.DIR_CACHE_MODPACK_EXPORTER,
        PathManager.DIR_CACHE_SAVE_EXPORTER,
        PathManager.DIR_CACHE_MOD_UPDATER
    )
    val imageDirs = listOf(
        PathManager.DIR_IMAGE_CACHE,
        PathManager.DIR_CACHE_APP_ICON,
        PathManager.DIR_CACHE_HOME_PAGE
    )
    val logs = LogTools.collect()
    return ToolsSnapshot(
        cacheBytes = cacheDirs.sumOf { ToolsStorage.sizeOf(it) },
        imageBytes = imageDirs.sumOf { ToolsStorage.sizeOf(it) },
        logBytes = logs.sumOf { it.file.length() },
        logCount = logs.size,
        instances = InstanceTools.instanceSizes(),
        bigFiles = InstanceTools.bigFiles(),
        ram = runCatching { RamTuner.recommend(context) }.getOrNull(),
        freeSpace = context.filesDir.usableSpace,
        totalSpace = context.filesDir.totalSpace,
        modsCount = RamTuner.totalModsCount()
    )
}

private fun formatHealthReports(
    context: Context,
    reports: List<InstanceTools.HealthReport>
): String {
    val withIssues = reports.filter { it.issues.isNotEmpty() }
    if (withIssues.isEmpty()) {
        return context.getString(R.string.tools_health_ok, reports.size)
    }
    return buildString {
        appendLine(context.getString(R.string.tools_health_found, withIssues.size))
        withIssues.forEach { report ->
            appendLine()
            appendLine("• ${report.versionName}")
            report.issues.forEach { issue ->
                val text = when (issue.type) {
                    InstanceTools.IssueType.MISSING_VERSION_JSON ->
                        context.getString(R.string.tools_health_issue_json)
                    InstanceTools.IssueType.MISSING_CLIENT_JAR ->
                        context.getString(R.string.tools_health_issue_jar)
                    InstanceTools.IssueType.MISSING_ASSETS ->
                        context.getString(R.string.tools_health_issue_assets)
                    InstanceTools.IssueType.MISSING_JAVA_RUNTIME ->
                        context.getString(R.string.tools_health_issue_java)
                    InstanceTools.IssueType.INVALID_CONFIG ->
                        context.getString(R.string.tools_health_issue_config)
                    InstanceTools.IssueType.EMPTY_INSTANCE ->
                        context.getString(R.string.tools_health_issue_empty)
                }
                appendLine("   - $text (${issue.detail})")
            }
        }
    }.trim()
}

private fun formatDuplicateGroups(
    context: Context,
    groups: List<InstanceTools.DuplicateGroup>
): String {
    if (groups.isEmpty()) {
        return context.getString(R.string.tools_mods_none)
    }
    return buildString {
        appendLine(context.getString(R.string.tools_mods_found, groups.size))
        groups.forEach { group ->
            appendLine()
            appendLine("• ${group.versionName} — ${group.modId}")
            group.files.forEach { file ->
                appendLine("   - ${file.name} (${ToolsStorage.formatSize(file.length())})")
            }
        }
    }.trim()
}

private fun shareFile(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/zip")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.tools_report_share_title))
        )
    }.onFailure {
        Logger.warning(TAG, "Failed to share report", it)
        Toast.makeText(
            context,
            context.getString(R.string.tools_report_failed, it.message.orEmpty()),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun ToolsCard(
    ctx: AnimatedContext,
    title: String,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsCard(
        modifier = Modifier.animated(ctx),
        position = CardPosition.Single
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryCard(
    ctx: AnimatedContext,
    snapshot: ToolsSnapshot,
    onRefresh: () -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_summary_title),
        summary = stringResource(R.string.tools_summary_hint)
    ) {
        val ram = snapshot.ram
        InfoRow(
            stringResource(R.string.tools_summary_ram),
            if (ram != null) stringResource(R.string.tools_summary_ram_value, ram.deviceRamMb) else "—"
        )
        InfoRow(
            stringResource(R.string.tools_summary_storage),
            stringResource(
                R.string.tools_summary_storage_value,
                ToolsStorage.formatSize(snapshot.freeSpace),
                ToolsStorage.formatSize(snapshot.totalSpace)
            )
        )
        InfoRow(
            stringResource(R.string.tools_summary_instances),
            stringResource(R.string.tools_summary_count, snapshot.instances.size)
        )
        InfoRow(
            stringResource(R.string.tools_summary_mods),
            stringResource(R.string.tools_summary_count, snapshot.modsCount)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRefresh) {
                Icon(
                    modifier = Modifier.height(18.dp),
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = stringResource(R.string.generic_refresh))
            }
        }
    }
}

@Composable
private fun RamCard(
    ctx: AnimatedContext,
    snapshot: ToolsSnapshot,
    enabled: Boolean,
    onApplyGlobal: () -> Unit,
    onApplyInstances: () -> Unit
) {
    val ram = snapshot.ram
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_ram_title),
        summary = stringResource(R.string.tools_ram_summary)
    ) {
        if (ram == null) {
            Text(
                text = stringResource(R.string.generic_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            InfoRow(
                stringResource(R.string.tools_ram_recommend),
                stringResource(R.string.tools_ram_recommend_value, ram.recommendedMb, ram.maxRamMb)
            )
            Text(
                text = stringResource(R.string.tools_ram_args, ram.jvmArgs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(enabled = enabled, onClick = onApplyGlobal) {
                    Text(text = stringResource(R.string.tools_ram_apply_global))
                }
                FilledTonalButton(enabled = enabled, onClick = onApplyInstances) {
                    Text(text = stringResource(R.string.tools_ram_apply_instances))
                }
            }
        }
    }
}

@Composable
private fun CleanCard(
    ctx: AnimatedContext,
    snapshot: ToolsSnapshot,
    enabled: Boolean,
    onClean: () -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_clean_title),
        summary = stringResource(R.string.tools_clean_summary)
    ) {
        InfoRow(
            stringResource(R.string.tools_clean_cache),
            ToolsStorage.formatSize(snapshot.cacheBytes)
        )
        InfoRow(
            stringResource(R.string.tools_clean_images),
            ToolsStorage.formatSize(snapshot.imageBytes)
        )
        InfoRow(
            stringResource(R.string.tools_clean_logs),
            ToolsStorage.formatSize(snapshot.logBytes)
        )
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(
                    R.string.tools_clean_total,
                    ToolsStorage.formatSize(snapshot.cacheBytes + snapshot.imageBytes + snapshot.logBytes)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Button(enabled = enabled, onClick = onClean) {
                Text(text = stringResource(R.string.tools_clean_button))
            }
        }
    }
}

@Composable
private fun InstanceSizeCard(ctx: AnimatedContext, snapshot: ToolsSnapshot) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_storage_title),
        summary = stringResource(R.string.tools_storage_summary)
    ) {
        if (snapshot.instances.isEmpty()) {
            Text(
                text = stringResource(R.string.tools_storage_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            snapshot.instances.take(10).forEach { instance ->
                InfoRow(
                    instance.versionName,
                    ToolsStorage.formatSize(instance.bytes)
                )
            }
            HorizontalDivider()
            InfoRow(
                stringResource(R.string.tools_storage_total),
                ToolsStorage.formatSize(snapshot.instanceBytes)
            )
        }
    }
}

@Composable
private fun BigFilesCard(
    ctx: AnimatedContext,
    snapshot: ToolsSnapshot,
    enabled: Boolean,
    onDelete: (File) -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_big_title),
        summary = stringResource(R.string.tools_big_summary)
    ) {
        if (snapshot.bigFiles.isEmpty()) {
            Text(
                text = stringResource(R.string.tools_big_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            snapshot.bigFiles.take(10).forEach { file ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = file.name, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = ToolsStorage.formatSize(file.length()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(enabled = enabled, onClick = { onDelete(file) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_outlined),
                            contentDescription = stringResource(R.string.generic_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogCard(
    ctx: AnimatedContext,
    snapshot: ToolsSnapshot,
    enabled: Boolean,
    onClean: () -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_logs_title),
        summary = stringResource(R.string.tools_logs_summary)
    ) {
        InfoRow(
            stringResource(R.string.tools_logs_count),
            stringResource(R.string.tools_summary_count, snapshot.logCount)
        )
        InfoRow(
            stringResource(R.string.generic_file_size),
            ToolsStorage.formatSize(snapshot.logBytes)
        )
        Row {
            OutlinedButton(enabled = enabled && snapshot.logCount > KEEP_LAUNCHER_LOGS, onClick = onClean) {
                Text(text = stringResource(R.string.tools_logs_clean_button, KEEP_LAUNCHER_LOGS))
            }
        }
    }
}

@Composable
private fun HealthCard(
    ctx: AnimatedContext,
    enabled: Boolean,
    onCheck: () -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_health_title),
        summary = stringResource(R.string.tools_health_summary)
    ) {
        Row {
            FilledTonalButton(enabled = enabled, onClick = onCheck) {
                Text(text = stringResource(R.string.tools_health_button))
            }
        }
    }
}

@Composable
private fun DuplicateModsCard(
    ctx: AnimatedContext,
    enabled: Boolean,
    onScan: () -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_mods_title),
        summary = stringResource(R.string.tools_mods_summary)
    ) {
        Row {
            FilledTonalButton(enabled = enabled, onClick = onScan) {
                Text(text = stringResource(R.string.tools_mods_button))
            }
        }
    }
}

@Composable
private fun BackupCard(
    ctx: AnimatedContext,
    enabled: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_backup_title),
        summary = stringResource(R.string.tools_backup_summary)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = enabled, onClick = onExport) {
                Text(text = stringResource(R.string.tools_backup_export))
            }
            OutlinedButton(enabled = enabled, onClick = onImport) {
                Text(text = stringResource(R.string.tools_backup_import))
            }
        }
    }
}

@Composable
private fun ReportCard(
    ctx: AnimatedContext,
    enabled: Boolean,
    onCreate: () -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_report_title),
        summary = stringResource(R.string.tools_report_summary)
    ) {
        Row {
            Button(enabled = enabled, onClick = onCreate) {
                Text(text = stringResource(R.string.tools_report_button))
            }
        }
    }
}

@Composable
private fun NotesCard(
    ctx: AnimatedContext,
    snapshot: ToolsSnapshot,
    onEdit: (String) -> Unit
) {
    ToolsCard(
        ctx = ctx,
        title = stringResource(R.string.tools_notes_title),
        summary = stringResource(R.string.tools_notes_summary)
    ) {
        if (snapshot.instances.isEmpty()) {
            Text(
                text = stringResource(R.string.tools_notes_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            snapshot.instances.forEach { instance ->
                val note = InstanceNotes.noteOf(instance.versionName)
                val noteText = if (note.isBlank()) {
                    stringResource(R.string.tools_notes_none)
                } else {
                    note
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = instance.versionName, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = noteText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onEdit(instance.versionName) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_build_outlined),
                            contentDescription = stringResource(R.string.tools_notes_edit)
                        )
                    }
                }
            }
        }
    }
}
