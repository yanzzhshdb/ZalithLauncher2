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

package com.movtery.zalithlauncher.game.session

import android.content.Context
import com.movtery.zalithlauncher.R
import java.util.concurrent.TimeUnit

/**
 * 游玩时长的展示格式
 */
object PlaytimeFormat {
    /**
     * “1 j 23 m”式的可读时长
     */
    fun duration(context: Context, millis: Long): String {
        val minutes = millis / TimeUnit.MINUTES.toMillis(1L)
        if (minutes < 1L) return context.getString(R.string.playtime_duration_less_min)
        val hours = minutes / 60L
        val restMinutes = minutes % 60L
        return if (hours > 0L) {
            context.getString(R.string.playtime_duration_hour_min, hours, restMinutes)
        } else {
            context.getString(R.string.playtime_duration_min, restMinutes)
        }
    }

    /**
     * “2 jam lalu”式的相对时间
     */
    fun relative(context: Context, timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - timestamp).coerceAtLeast(0L)
        return when {
            diff < TimeUnit.MINUTES.toMillis(1L) ->
                context.getString(R.string.playtime_relative_now)
            diff < TimeUnit.HOURS.toMillis(1L) ->
                context.getString(R.string.playtime_relative_minutes, diff / TimeUnit.MINUTES.toMillis(1L))
            diff < TimeUnit.DAYS.toMillis(1L) ->
                context.getString(R.string.playtime_relative_hours, diff / TimeUnit.HOURS.toMillis(1L))
            else ->
                context.getString(R.string.playtime_relative_days, diff / TimeUnit.DAYS.toMillis(1L))
        }
    }

    /**
     * 图表柱状高度用的紧凑时长，只保留最大的单位
     */
    fun compact(context: Context, millis: Long): String {
        val minutes = millis / TimeUnit.MINUTES.toMillis(1L)
        if (minutes <= 0L) return ""
        val hours = minutes / 60L
        return if (hours > 0L) {
            context.getString(R.string.playtime_compact_hour, hours)
        } else {
            context.getString(R.string.playtime_compact_min, minutes)
        }
    }
}
