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

package com.movtery.zalithlauncher.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.launcherName
import com.movtery.zalithlauncher.upgrade.LauncherReleaseChecker
import com.movtery.zalithlauncher.upgrade.UpgradePrefs

/**
 * 有新版本时推送系统通知，点击通知直达下载页
 */
object LauncherUpdateNotification {
    private const val MAX_NOTE_LENGTH = 400

    fun show(context: Context, release: LauncherReleaseChecker.LatestRelease) {
        if (!NotificationManager.checkNotificationEnabled(context)) return

        val channel = NotificationChannelData.LAUNCHER_UPDATE_CHANNEL
        val name = launcherName()
        val summary = context.getString(R.string.notification_launcher_update_text, name, release.version)
        val detail = release.notes?.let { notes ->
            context.getString(
                R.string.notification_launcher_update_detail,
                summary,
                notes.take(MAX_NOTE_LENGTH)
            )
        } ?: summary

        val openDownload = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_LAUNCHER_UPDATE,
            Intent(Intent.ACTION_VIEW, Uri.parse(release.url)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel.channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_launcher_update_title, name, release.version))
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(openDownload)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_LAUNCHER_UPDATE, notification)
        }
        UpgradePrefs.notifiedReleaseTag = release.tag
    }

    /**
     * 用户已经升级或不再需要提示时，清除通知
     */
    fun cancel(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_LAUNCHER_UPDATE)
        }
    }
}
