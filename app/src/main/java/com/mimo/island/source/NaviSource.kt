package com.mimo.island.source

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 导航数据源。
 *
 * 高德 / 百度都没有向第三方开放「读取其他 App 正在进行的导航会话」的接口，
 * 所以这里走两条可落地的路：
 *
 *  1. [launchNavi] —— 由本 App 发起导航（高德 / 百度 URI API）。
 *     之后高德会把转向信息写进通知栏，由 [parse] 解析。
 *  2. 用户若已在他处发起导航，同样通过通知栏文本解析。
 *
 * 文本格式随 App 版本变化，因此解析是容错的：解析不出具体转向就降级为「导航中」。
 */
class NaviSource(private val context: Context) {

    /** 用高德地图发起导航；未安装则返回 false。 */
    fun launchNavi(dest: String): Boolean {
        val uri = Uri.parse(
            "amapuri://route/plan/?sourceApplication=islandhelper&dname=$dest&dev=0&t=0"
        )
        return launch(uri)
    }

    /** 用百度地图发起导航。 */
    fun launchBaiduNavi(dest: String): Boolean {
        val uri = Uri.parse("baidumap://map/dir?destination=$dest&mode=driving")
        return launch(uri)
    }

    private fun launch(uri: Uri) = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    /**
     * 从通知栏文本里解析导航信息。
     * 典型文本：「剩余3.2公里 右前方沿XX路行驶」「前方500米右转进入XX大道」
     */
    fun parse(text: String): NaviInfo? {
        val t = text.replace(" ", "").replace("　", "")
        if (!hasNaviSignal(t)) return null

        val remainMeters = Regex("(\\d+(?:\\.\\d+)?)(公里|千米|米)").find(t)?.let {
            val v = it.groupValues[1].toDoubleOrNull() ?: return@let null
            val unit = it.groupValues[2]
            (v * if (unit == "米") 1 else 1000).toInt()
        }

        val remainMinutes = Regex("(\\d+)分钟").find(t)?.groupValues?.get(1)?.toIntOrNull()
        val remainSeconds = Regex("(\\d+)秒").find(t)?.groupValues?.get(1)?.toIntOrNull()

        val maneuver = when {
            t.contains("右前方") -> "右前方"
            t.contains("左前方") -> "左前方"
            t.contains("右转") || t.contains("靠右") -> "右转"
            t.contains("左转") || t.contains("靠左") -> "左转"
            t.contains("直行") -> "直行"
            t.contains("掉头") -> "掉头"
            t.contains("环岛") -> "进入环岛"
            t.contains("匝道") -> "上匝道"
            t.contains("出口") -> "驶出出口"
            else -> "导航中"
        }

        val road = Regex("进入([^，,。；;]{2,15})").find(t)?.groupValues?.get(1)
            ?: Regex("沿([^，,。；;]{2,15})").find(t)?.groupValues?.get(1)

        val speedLimit = Regex("限速(\\d+)").find(t)?.groupValues?.get(1)?.toIntOrNull()

        return NaviInfo(
            maneuver = maneuver,
            road = road.orEmpty(),
            distanceMeters = remainMeters ?: 0,
            remainSeconds = (remainMinutes ?: 0) * 60 + (remainSeconds ?: 0),
            speedLimitKmh = speedLimit
        )
    }

    private fun hasNaviSignal(t: String): Boolean {
        val keys = listOf(
            "米右转", "米左转", "米直行", "公里", "后右转", "后左转", "路口",
            "沿路行驶", "匝道", "环岛", "限速", "掉头", "到达目的地", "前方"
        )
        return keys.any { t.contains(it) }
    }
}
