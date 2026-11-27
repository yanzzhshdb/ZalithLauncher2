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

package com.movtery.zalithlauncher.event

import com.movtery.zalithlauncher.game.session.PlaytimeTracker
import com.movtery.zalithlauncher.setting.launcherMMKV
import java.util.concurrent.TimeUnit

/**
 * First Release 活动
 *
 * 活动开放 6 天，期间可以免费使用专属 UI（Aurora 主题与主页横幅）。
 * 累计游玩（含挂机）满 15 分钟即可永久保留，之后即使活动结束也不会失效。
 */
object FirstReleaseEvent {
    /** 活动持续时长 */
    private val DURATION_MILLIS = TimeUnit.DAYS.toMillis(6L)

    /** 永久保留所需的游玩时长 */
    const val GOAL_MILLIS = 15L * 60L * 1000L

    /** 活动版本标签，展示用 */
    const val VERSION_LABEL = "1.6.0"

    private const val KEY_STARTED_AT = "event.firstRelease.startedAt"
    private const val KEY_BASELINE = "event.firstRelease.baseline"
    private const val KEY_UNLOCKED = "event.firstRelease.unlocked"
    private const val KEY_CLAIMED_AT = "event.firstRelease.claimedAt"
    private const val KEY_GOAL_REACHED = "event.firstRelease.goalReached"

    private val mmkv get() = launcherMMKV()

    data class State(
        val startedAt: Long,
        val endsAt: Long,
        val progressMillis: Long,
        val permanent: Boolean,
        val claimedAt: Long,
        val timeLeftMillis: Long
    ) {
        val goalMillis: Long get() = GOAL_MILLIS
        val expired: Boolean get() = timeLeftMillis <= 0L && !permanent
        val goalReached: Boolean get() = progressMillis >= GOAL_MILLIS
        val canClaim: Boolean get() = goalReached && !permanent
        val ratio: Float get() = (progressMillis.toFloat() / GOAL_MILLIS).coerceIn(0f, 1f)
    }

    /**
     * 首次启动时记录活动开始时间与游玩时长基线，只执行一次
     */
    fun ensureStarted() {
        val store = mmkv
        if (store.getLong(KEY_STARTED_AT, 0L) > 0L) return
        store.putLong(KEY_STARTED_AT, System.currentTimeMillis())
        store.putLong(KEY_BASELINE, PlaytimeTracker.snapshot().totalMillis)
        store.apply()
    }

    fun state(): State {
        val store = mmkv
        val startedAt = store.getLong(KEY_STARTED_AT, 0L).takeIf { it > 0L }
            ?: System.currentTimeMillis().also { ensureStarted() }
        val baseline = store.getLong(KEY_BASELINE, 0L)
        val permanent = store.getBoolean(KEY_UNLOCKED, false)
        val claimedAt = store.getLong(KEY_CLAIMED_AT, 0L)
        val endsAt = startedAt + DURATION_MILLIS

        val played = (PlaytimeTracker.snapshot().totalMillis - baseline).coerceAtLeast(0L) +
            PlaytimeTracker.pendingSessionMillis()
        if (played >= GOAL_MILLIS && !store.getBoolean(KEY_GOAL_REACHED, false)) {
            store.putBoolean(KEY_GOAL_REACHED, true)
            store.apply()
        }

        return State(
            startedAt = startedAt,
            endsAt = endsAt,
            progressMillis = played,
            permanent = permanent,
            claimedAt = claimedAt,
            timeLeftMillis = (endsAt - System.currentTimeMillis()).coerceAtLeast(0L)
        )
    }

    /**
     * 活动是否仍在进行
     *
     * 只读取时间戳，不统计游玩时长，可以在组合期间频繁调用
     */
    fun isActive(): Boolean {
        val startedAt = mmkv.getLong(KEY_STARTED_AT, 0L)
        if (startedAt <= 0L) return true
        return System.currentTimeMillis() < startedAt + DURATION_MILLIS
    }

    /**
     * 专属 UI 是否可用：活动期间可用，或已经永久解锁
     */
    fun isUiAvailable(): Boolean = isPermanent() || isGoalReached() || isActive()

    /** 是否已经达成过游玩时长目标（即使还没领取） */
    fun isGoalReached(): Boolean = mmkv.getBoolean(KEY_GOAL_REACHED, false)

    fun isPermanent(): Boolean = mmkv.getBoolean(KEY_UNLOCKED, false)

    /**
     * 领取永久资格。只要游玩时长达标，活动结束后也允许补领
     */
    fun claim(): Boolean {
        val current = state()
        if (!current.canClaim) return false
        mmkv.putBoolean(KEY_UNLOCKED, true)
        mmkv.putLong(KEY_CLAIMED_AT, System.currentTimeMillis())
        mmkv.apply()
        return true
    }
}
