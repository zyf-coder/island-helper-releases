package com.mimo.island.source

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 支付数据源。
 *
 * 合规优先：**支付真相永远来自我们自己的服务端**。
 * 监听支付宝 / 微信通知（[ListenPaySource]）只作为用户主动开启的辅助，
 * 默认关闭，且只用于展示、不做记账。
 */
class PaySource(private val context: Context) {

    /** 服务端地址；为空时不做任何轮询。 */
    var endpoint: String? = null

    /** 轮询间隔（毫秒）。 */
    var pollIntervalMs: Long = 15_000L

    @Volatile
    var lastSeenAt: Long = 0L

    /**
     * 拉取最新一笔收款记录。
     * 约定服务端返回 JSON：{"amountCents":8800,"payer":"张三","orderId":"xxx","success":true}
     */
    fun fetchLatest(): PayInfo? {
        val url = endpoint?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            conn.disconnect()
            val json = JSONObject(body)
            PayInfo(
                amountCents = json.optLong("amountCents", 0L),
                payer = json.optString("payer").takeIf { it.isNotBlank() },
                orderId = json.optString("orderId").takeIf { it.isNotBlank() },
                success = json.optBoolean("success", true)
            ).also { lastSeenAt = System.currentTimeMillis() }
        } catch (e: Exception) {
            null
        }
    }
}
