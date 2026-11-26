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
import com.movtery.zalithlauncher.notification.GameSessionNotification
import com.movtery.zalithlauncher.setting.launcherMMKV
import com.movtery.zalithlauncher.utils.logging.Logger
import java.util.concurrent.TimeUnit

private const val TAG = "PlaytimeTracker"

/**
 * 记录每个版本的游玩时长
 *
 * 时长以墙钟时间计算，写入 MMKV：总时长、会话次数、最近游玩时间与每日时长桶。
 * 数据供主页的“继续游玩”卡片与设置中的游玩统计页面使用。
 */
object PlaytimeTracker {
    /** 短于该时长的会话不计入统计（启动失败、立刻退出） */
    private const val MIN_SESSION_MILLIS = 10_000L
    /** 达到该时长才推送会话结束通知 */
    private const val NOTIFY_SESSION_MILLIS = 60_000L
    /** 超过该时长的未结束会话视为异常残留 */
    private const val STALE_SESSION_MILLIS = TimeUnit.HOURS.toMillis(12L)

    private const val KEY_PENDING_VERSION = "playtime.pending.version"
    private const val KEY_PENDING_HOME = "playtime.pending.home"
    private const val KEY_PENDING_START = "playtime.pending.start"
    private const val KEY_TOTAL = "playtime.total."
    private const val KEY_SESSIONS = "playtime.sessions."
    private const val KEY_LAST = "playtime.last."
    private const val KEY_HOME = "playtime.home."
    private const val KEY_DAILY = "playtime.daily."

    /** 统计页展示的天数 */
    const val RECENT_DAYS = 7

    private val mmkv get() = launcherMMKV()

    data class VersionPlaytime(
        val versionName: String,
        val gameHome: String?,
        val totalMillis: Long,
        val sessions: Int,
        val lastPlayed: Long
    )

    data class DailyPlaytime(val epochDay: Long, val millis: Long)

    data class Snapshot(
        val totalMillis: Long,
        val sessions: Int,
        val versions: List<VersionPlaytime>,
        val recentDays: List<DailyPlaytime>
    ) {
        /** 最近一次游玩的版本 */
        val latest: VersionPlaytime?
            get() = versions.maxByOrNull { it.lastPlayed }

        val isEmpty: Boolean
            get() = sessions == 0
    }

    /**
     * 游戏开始启动时登记会话
     */
    fun start(versionName: String, gameHome: String) {
        mmkv.putString(KEY_PENDING_VERSION, versionName).apply()
        mmkv.putString(KEY_PENDING_HOME, gameHome).apply()
        mmkv.putLong(KEY_PENDING_START, System.currentTimeMillis()).apply()
    }

    /**
     * 游戏进程退出时结算会话
     * @param context 用于推送会话结束通知，可为空
     */
    fun finish(context: Context?, exitCode: Int) {
        val versionName = mmkv.getString(KEY_PENDING_VERSION, null)
        val gameHome = mmkv.getString(KEY_PENDING_HOME, null)
        val start = mmkv.getLong(KEY_PENDING_START, 0L)
        clearPending()
        if (versionName.isNullOrEmpty() || start <= 0L) return

        val duration = System.currentTimeMillis() - start
        //进程被系统回收后补算的无效时长，直接丢弃
        if (duration <= 0L || duration > STALE_SESSION_MILLIS) return
        if (duration < MIN_SESSION_MILLIS) return

        record(versionName, gameHome, duration)

        if (context != null && duration >= NOTIFY_SESSION_MILLIS) {
            GameSessionNotification.show(context, versionName, duration, exitCode)
        }
    }

    /**
     * 启动器重新创建时清理未结算的会话，避免把异常退出的时长算进去
     */
    fun clearStaleSession() {
        if (mmkv.getLong(KEY_PENDING_START, 0L) > 0L) {
            Logger.info(TAG, "Dropping unfinished play session")
            clearPending()
        }
    }

    fun snapshot(): Snapshot {
        val store = mmkv
        val names = store.allKeys()
            ?.mapNotNull { key -> key.takeIf { it.startsWith(KEY_TOTAL) }?.removePrefix(KEY_TOTAL) }
            ?.distinct()
            ?: emptyList()

        val versions = names.map { name ->
            VersionPlaytime(
                versionName = name,
                gameHome = store.getString(KEY_HOME + name, null),
                totalMillis = store.getLong(KEY_TOTAL + name, 0L),
                sessions = store.getInt(KEY_SESSIONS + name, 0),
                lastPlayed = store.getLong(KEY_LAST + name, 0L)
            )
        }.sortedByDescending { it.totalMillis }

        val today = currentEpochDay()
        val recentDays = (RECENT_DAYS - 1 downTo 0).map { offset ->
            val day = today - offset
            DailyPlaytime(day, store.getLong(KEY_DAILY + day, 0L))
        }

        return Snapshot(
            totalMillis = versions.sumOf { it.totalMillis },
            sessions = versions.sumOf { it.sessions },
            versions = versions,
            recentDays = recentDays
        )
    }

    fun reset() {
        val store = mmkv
        store.allKeys()
            ?.filter { it.startsWith("playtime.") }
            ?.forEach { store.remove(it) }
        clearPending()
    }

    private fun record(versionName: String, gameHome: String?, durationMillis: Long) {
        val store = mmkv
        val total = store.getLong(KEY_TOTAL + versionName, 0L) + durationMillis
        val sessions = store.getInt(KEY_SESSIONS + versionName, 0) + 1
        store.putLong(KEY_TOTAL + versionName, total)
        store.putInt(KEY_SESSIONS + versionName, sessions)
        store.putLong(KEY_LAST + versionName, System.currentTimeMillis())
        if (!gameHome.isNullOrEmpty()) {
            store.putString(KEY_HOME + versionName, gameHome)
        }

        val dailyKey = KEY_DAILY + currentEpochDay()
        store.putLong(dailyKey, store.getLong(dailyKey, 0L) + durationMillis)
        store.apply()
    }

    private fun clearPending() {
        mmkv.remove(KEY_PENDING_VERSION)
        mmkv.remove(KEY_PENDING_HOME)
        mmkv.remove(KEY_PENDING_START)
    }

    private fun currentEpochDay(): Long = System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1L)
}
