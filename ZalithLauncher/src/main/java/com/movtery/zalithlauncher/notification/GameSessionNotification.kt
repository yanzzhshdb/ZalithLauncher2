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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.session.PlaytimeFormat
import com.movtery.zalithlauncher.ui.activities.MainActivity

/**
 * 游戏结束后推送本次游玩的时长小结
 */
object GameSessionNotification {
    fun show(context: Context, versionName: String, durationMillis: Long, exitCode: Int) {
        if (!NotificationManager.checkNotificationEnabled(context)) return

        val channel = NotificationChannelData.GAME_SESSION_CHANNEL
        val duration = PlaytimeFormat.duration(context, durationMillis)
        val text = if (exitCode == 0) {
            context.getString(R.string.notification_game_session_text, versionName, duration)
        } else {
            context.getString(R.string.notification_game_session_text_error, versionName, duration)
        }

        val openLauncher = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_GAME_SESSION,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel.channelId)
            .setSmallIcon(R.drawable.ic_schedule_outlined)
            .setContentTitle(context.getString(R.string.notification_game_session_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openLauncher)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GAME_SESSION, notification)
        }
    }
}
