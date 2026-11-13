package com.mimo.island.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mimo.island.MainActivity
import com.mimo.island.R
import com.mimo.island.island.IslandCard
import com.mimo.island.island.IslandState
import com.mimo.island.source.MediaSource
import com.mimo.island.source.NaviEvents
import com.mimo.island.source.NaviSource
import com.mimo.island.source.PaySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 前台服务：持有悬浮窗，并驱动三个数据源的轮询。
 */
class IslandService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null
    private lateinit var float: FloatWindowController
    private lateinit var media: MediaSource
    private lateinit var navi: NaviSource
    private lateinit var pay: PaySource

    override fun onCreate() {
        super.onCreate()
        float = FloatWindowController(this)
        media = MediaSource(this)
        navi = NaviSource(this)
        pay = PaySource(this)

        float.show()
        float.callbacks = object : FloatWindowController.Callbacks {
            override fun onToggleExpanded() = IslandState.toggleExpanded()
            override fun onCollapse() = IslandState.setExpanded(false)
            override fun onPlayPause() = media.togglePlayPause()
            override fun onNext() = media.next()
            override fun onPrevious() = media.previous()
        }
        startForeground(NOTIF_ID, buildNotification())
        startPolling()

        NaviEvents.onText { pkg, text ->
            navi.parse(text)?.let {
                IslandState.submit("navi:$pkg", IslandCard.Navigation(
                    maneuver = it.maneuver,
                    road = it.road,
                    distanceMeters = it.distanceMeters,
                    remainSeconds = it.remainSeconds,
                    speedLimitKmh = it.speedLimitKmh,
                    isActive = true
                ))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                // 音乐
                media.read()?.let {
                    IslandState.submit("media", IslandCard.Music(
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        playing = it.playing,
                        positionMs = it.positionMs,
                        durationMs = it.durationMs,
                        artwork = it.artwork,
                        packageName = it.packageName,
                        canControl = it.canControl
                    ))
                }

                // 支付（仅当配置了服务端地址）
                pay.fetchLatest()?.let {
                    IslandState.submit("pay", IslandCard.Payment(
                        amountCents = it.amountCents,
                        payer = it.payer,
                        orderId = it.orderId,
                        success = it.success
                    ))
                }

                IslandState.tick()
                delay(1000)
            }
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CH_ID, "灵动助手运行状态", NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CH_ID)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle("灵动助手运行中")
            .setContentText("点击返回设置页")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    override fun onDestroy() {
        pollJob?.cancel()
        float.hide()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CH_ID = "island_running"
        private const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.mimo.island.action.STOP"

        fun start(ctx: android.content.Context) {
            val i = Intent(ctx, IslandService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: android.content.Context) {
            ctx.startService(Intent(ctx, IslandService::class.java).setAction(ACTION_STOP))
        }
    }
}
