package com.mimo.island.source

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 通知监听：用于读取导航 App 写入通知栏的转向文本。
 *
 * 支付通知的解析不在这里做 —— 见 README 的合规说明：
 * 支付金额只从自有服务端获取，避免触碰资金数据合规红线。
 */
class NotifListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        if (!isNaviPackage(n.packageName)) return
        val text = extractText(n) ?: return
        NaviEvents.emit(n.packageName, text)
    }

    private fun isNaviPackage(pkg: String?): Boolean {
        if (pkg == null) return false
        return NAVI_PACKAGES.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun extractText(sbn: StatusBarNotification): String? {
        val extras = sbn.notification?.extras ?: return null
        val parts = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it.toString()) }
        return parts.joinToString(" ").takeIf { it.isNotBlank() }
    }

    companion object {
        private val NAVI_PACKAGES = listOf(
            "autonavi", "amap", "baidu", "map.baidu", "tencent.map"
        )
    }
}

/** 导航文本的事件总线，NotifListener 与轮询器通过它汇合。 */
object NaviEvents {
    private val listeners = mutableSetOf<(String, String) -> Unit>()

    fun onText(l: (String, String) -> Unit) {
        listeners += l
    }

    fun offText(l: (String, String) -> Unit) {
        listeners -= l
    }

    fun emit(pkg: String, text: String) {
        listeners.toList().forEach { it(pkg, text) }
    }
}
