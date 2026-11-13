package com.mimo.island.source

import android.graphics.Bitmap

/** 音乐信息。 */
data class MusicInfo(
    val title: String,
    val artist: String,
    val album: String,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val artwork: Bitmap?,
    val packageName: String?,
    val canControl: Boolean
)

/** 导航信息。 */
data class NaviInfo(
    val maneuver: String,
    val road: String,
    val distanceMeters: Int,
    val remainSeconds: Int,
    val speedLimitKmh: Int?
)

/** 支付信息。 */
data class PayInfo(
    val amountCents: Long,
    val payer: String?,
    val orderId: String?,
    val success: Boolean
)
