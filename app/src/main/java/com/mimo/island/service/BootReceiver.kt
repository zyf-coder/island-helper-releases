package com.mimo.island.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 开机自启。用户在设置页开启后生效。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs.autoStartEnabled(context)) {
            IslandService.start(context)
        }
    }
}

/** 简单偏好存储。 */
object Prefs {
    private const val FILE = "island_prefs"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun autoStartEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean("auto_start", false)

    fun setAutoStart(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("auto_start", on).apply()
    }

    fun serviceRunning(ctx: Context): Boolean =
        prefs(ctx).getBoolean("service_running", false)

    fun setServiceRunning(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("service_running", on).apply()
    }

    fun payEndpoint(ctx: Context): String =
        prefs(ctx).getString("pay_endpoint", "").orEmpty()

    fun setPayEndpoint(ctx: Context, v: String) {
        prefs(ctx).edit().putString("pay_endpoint", v).apply()
    }

    /** 判断通知使用权是否已授予。 */
    fun notificationAccessGranted(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(
            ctx.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(ctx.packageName)
    }
}
