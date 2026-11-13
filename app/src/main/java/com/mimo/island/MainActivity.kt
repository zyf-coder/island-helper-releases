package com.mimo.island

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mimo.island.service.IslandService
import com.mimo.island.service.Prefs

/**
 * 单页设置界面：权限引导 + 运行开关 + 支付服务端地址。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var overlayBtn: Button
    private lateinit var notifBtn: Button
    private lateinit var batteryBtn: Button
    private lateinit var runSwitch: Switch
    private lateinit var autoSwitch: Switch
    private lateinit var endpointInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 56, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "灵动助手"
            textSize = 26f
            setTextColor(0xFF1B1D21.toInt())
        })
        root.addView(TextView(this).apply {
            text = "为魅族 22 的实况通知补充导航 / 音乐 / 支付信息"
            textSize = 13f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, 12, 0, 32)
        })

        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFE0892B.toInt())
            setPadding(0, 0, 0, 24)
        }
        root.addView(statusText)

        // ---- 权限引导 ----
        root.addView(sectionTitle("一、权限设置"))

        overlayBtn = Button(this).apply { text = "① 允许悬浮窗" }
        overlayBtn.setOnClickListener { openOverlaySettings() }
        root.addView(overlayBtn)

        notifBtn = Button(this).apply { text = "② 允许通知使用权" }
        notifBtn.setOnClickListener { openNotificationAccessSettings() }
        root.addView(notifBtn)

        batteryBtn = Button(this).apply { text = "③ 忽略电池优化" }
        batteryBtn.setOnClickListener { openBatterySettings() }
        root.addView(batteryBtn)

        root.addView(hint(
            "提示：Flyme 自带的「实况通知」在 系统设置 → 通知和状态栏 → 实况通知 里，" +
                "可以把高德、百度地图等也加进去，那是系统原生卡片。"
        ))

        // ---- 运行控制 ----
        root.addView(sectionTitle("二、运行控制"))

        runSwitch = Switch(this).apply {
            text = "启动灵动岛服务"
            textSize = 15f
            setTextColor(0xFF1B1D21.toInt())
            setOnCheckedChangeListener { _, on ->
                if (on) {
                    if (!hasOverlayPermission()) {
                        this@MainActivity.runSwitch.isChecked = false
                        toast("请先允许悬浮窗权限")
                        return@setOnCheckedChangeListener
                    }
                    IslandService.start(this@MainActivity)
                    Prefs.setServiceRunning(this@MainActivity, true)
                } else {
                    IslandService.stop(this@MainActivity)
                    Prefs.setServiceRunning(this@MainActivity, false)
                }
            }
        }
        root.addView(runSwitch)

        autoSwitch = Switch(this).apply {
            text = "开机自动启动"
            textSize = 15f
            setTextColor(0xFF1B1D21.toInt())
            isChecked = Prefs.autoStartEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, on ->
                Prefs.setAutoStart(this@MainActivity, on)
            }
        }
        root.addView(autoSwitch)

        // ---- 支付服务端 ----
        root.addView(sectionTitle("三、支付来源（可选）"))
        root.addView(hint(
            "填写你自己的收款服务端地址，留空则不接入支付播报。" +
                "App 只展示服务端下发的结果，不解析微信/支付宝通知里的金额。"
        ))
        endpointInput = EditText(this).apply {
            hint = "https://your-server/api/latest-payment"
            textSize = 13f
            setText(Prefs.payEndpoint(this@MainActivity))
        }
        root.addView(endpointInput)
        root.addView(Button(this).apply {
            text = "保存支付地址"
            setOnClickListener {
                Prefs.setPayEndpoint(this@MainActivity, endpointInput.text.toString().trim())
                toast("已保存")
            }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun sectionTitle(t: String) = TextView(this).apply {
        text = t
        textSize = 16f
        setTextColor(0xFF1B1D21.toInt())
        setPadding(0, 40, 0, 16)
    }

    private fun hint(t: String) = TextView(this).apply {
        text = t
        textSize = 12f
        setTextColor(0xFF6B7280.toInt())
        setPadding(0, 16, 0, 8)
    }

    private fun hasOverlayPermission() =
        Settings.canDrawOverlays(this)

    private fun openOverlaySettings() {
        val i = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        try {
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    private fun openNotificationAccessSettings() {
        try {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        } catch (e: Exception) {
            toast("请到 设置 → 应用 → 特殊应用权限 → 通知使用权 手动开启")
        }
    }

    private fun openBatterySettings() {
        val i = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        try {
            startActivity(i)
        } catch (e: Exception) {
            toast("请到 设置 → 电池 → 后台活动管理 里关闭限制")
        }
    }

    private fun refresh() {
        val overlay = hasOverlayPermission()
        val notif = Prefs.notificationAccessGranted(this)
        val battery = isIgnoringBatteryOptimizations()

        statusText.text = buildString {
            append("悬浮窗 ").append(ok(overlay))
            append("　通知使用权 ").append(ok(notif))
            append("　电池白名单 ").append(ok(battery))
        }
        overlayBtn.text = if (overlay) "① 悬浮窗 ✓" else "① 允许悬浮窗"
        notifBtn.text = if (notif) "② 通知使用权 ✓" else "② 允许通知使用权"
        batteryBtn.text = if (battery) "③ 电池白名单 ✓" else "③ 忽略电池优化"

        val running = Prefs.serviceRunning(this)
        if (runSwitch.isChecked != running) {
            runSwitch.setOnCheckedChangeListener(null)
            runSwitch.isChecked = running
            runSwitch.setOnCheckedChangeListener { _, on ->
                if (on) {
                    if (!hasOverlayPermission()) {
                        runSwitch.isChecked = false
                        toast("请先允许悬浮窗权限")
                        return@setOnCheckedChangeListener
                    }
                    IslandService.start(this@MainActivity)
                    Prefs.setServiceRunning(this@MainActivity, true)
                } else {
                    IslandService.stop(this@MainActivity)
                    Prefs.setServiceRunning(this@MainActivity, false)
                }
            }
        }
    }

    private fun ok(b: Boolean) = if (b) "✓" else "✗"

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(android.os.PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(packageName) == true
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
