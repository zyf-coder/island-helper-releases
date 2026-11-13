package com.mimo.island.island

import android.graphics.Bitmap

/**
 * 统一卡片模型。采集层只产出这几种结构，渲染层只认它们。
 */
sealed class IslandCard {
    abstract val priority: Int

    data class Music(
        val title: String,
        val artist: String,
        val album: String,
        val playing: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val artwork: Bitmap?,
        val packageName: String?,
        val canControl: Boolean,
        override val priority: Int = 10
    ) : IslandCard()

    data class Navigation(
        val maneuver: String,
        val road: String,
        val distanceMeters: Int,
        val remainSeconds: Int,
        val speedLimitKmh: Int?,
        val isActive: Boolean,
        override val priority: Int = 30
    ) : IslandCard()

    data class Payment(
        val amountCents: Long,
        val payer: String?,
        val orderId: String?,
        val success: Boolean,
        override val priority: Int = 20
    ) : IslandCard()
}
