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

package com.movtery.zalithlauncher.music

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.database.AppDatabase
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.ensureDirectorySilently
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val TAG = "MusicRepository"
private const val MAX_EXTENSION_LENGTH = 5

/**
 * 音乐库：负责将用户选择的音频文件复制进启动器私有目录，并维护歌曲信息
 */
object MusicRepository {
    private val dao: MusicTrackDao by lazy {
        AppDatabase.getInstance(GlobalContext).musicTrackDao()
    }

    val tracks: Flow<List<MusicTrack>>
        get() = dao.observeAll()

    fun fileOf(track: MusicTrack): File = File(PathManager.DIR_MUSIC, track.fileName)

    suspend fun import(
        uris: List<Uri>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): MusicImportResult = withContext(Dispatchers.IO) {
        var imported = 0
        var failed = 0

        uris.forEachIndexed { index, uri ->
            val success = runCatching { importSingle(uri) }
                .onFailure { Logger.error(TAG, "Failed to import music: $uri", it) }
                .getOrDefault(false)

            if (success) imported++ else failed++
            onProgress(index + 1, uris.size)
        }

        MusicImportResult(imported = imported, failed = failed)
    }

    suspend fun delete(track: MusicTrack) = withContext(Dispatchers.IO) {
        dao.delete(track)
        val file = fileOf(track)
        if (file.exists() && !file.delete()) {
            Logger.warning(TAG, "Failed to delete music file: ${file.absolutePath}")
        }
    }

    suspend fun rename(track: MusicTrack, title: String, artist: String?) = withContext(Dispatchers.IO) {
        dao.updateInfo(
            id = track.id,
            title = title.trim().ifEmpty { track.title },
            artist = artist?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    private suspend fun importSingle(uri: Uri): Boolean {
        val resolver = GlobalContext.contentResolver
        val displayName = queryDisplayName(uri)
        val extension = resolveExtension(uri, displayName)

        val directory = PathManager.DIR_MUSIC
        if (!directory.ensureDirectorySilently()) {
            Logger.warning(TAG, "Music directory is not writable: ${directory.absolutePath}")
            return false
        }

        val target = File(directory, "${UUID.randomUUID()}.$extension")
        val copied = resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false

        if (!copied || target.length() <= 0L) {
            target.delete()
            return false
        }

        val metadata = readMetadata(target)
        dao.insert(
            MusicTrack(
                title = metadata.title ?: displayName?.substringBeforeLast('.') ?: target.nameWithoutExtension,
                artist = metadata.artist,
                fileName = target.name,
                durationMs = metadata.durationMs,
                sizeBytes = target.length(),
                addedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        GlobalContext.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    private fun resolveExtension(uri: Uri, displayName: String?): String {
        val fromName = displayName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_EXTENSION_LENGTH && it.all(Char::isLetterOrDigit) }
        if (fromName != null) return fromName

        val fromMimeType = GlobalContext.contentResolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return fromMimeType ?: "mp3"
    }

    private fun readMetadata(file: File): TrackMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            TrackMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.trim()?.takeIf { it.isNotEmpty() },
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.trim()?.takeIf { it.isNotEmpty() },
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            )
        } catch (t: Throwable) {
            Logger.warning(TAG, "Failed to read music metadata: ${file.name}", t)
            TrackMetadata(title = null, artist = null, durationMs = 0L)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private data class TrackMetadata(
        val title: String?,
        val artist: String?,
        val durationMs: Long
    )
}

data class MusicImportResult(
    val imported: Int,
    val failed: Int
) {
    val total: Int get() = imported + failed
}
