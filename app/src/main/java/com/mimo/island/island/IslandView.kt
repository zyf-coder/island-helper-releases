package com.mimo.island.island

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * 屏幕顶部的灵动岛胶囊。收起态是一条药丸，展开态是一张控制面板。
 * 纯代码构建，不依赖 XML 布局。
 */
class IslandView(context: Context) : FrameLayout(context) {

    private val collapsed: LinearLayout
    private val expanded: LinearLayout
    private val cIcon: TextView
    private val cTitle: TextView
    private val cSub: TextView
    private val btnPrev: TextView
    private val btnPlay: TextView
    private val btnNext: TextView
    private val eTitle: TextView
    private val eSub: TextView
    private val eProgress: ProgressBar
    private val eTime: TextView
    private val eArt: ImageView
    private val ePrev: TextView
    private val ePlay: TextView
    private val eNext: TextView

    var onToggle: (() -> Unit)? = null
    var onCollapse: (() -> Unit)? = null
    var onPlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrev: (() -> Unit)? = null

    init {
        val pad = dp(6f)
        val gap = dp(6f)

        // ---------- 收起态 ----------
        collapsed = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), 0, dp(10f), 0)
            background = pill(Color.parseColor("#ECEDEF").toInt() and 0xFFFFFFFF.toInt(), 18f)
        }
        cIcon = TextView(context).apply { text = "♪"; setTextColor(Color.parseColor("#1B1D21")) }
        cTitle = TextView(context).apply {
            setTextColor(Color.parseColor("#1B1D21"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        cSub = TextView(context).apply { setTextColor(Color.parseColor("#6B7280")); maxLines = 1 }
        btnPrev = iconBtn("⏮")
        btnPlay = iconBtn("⏸")
        btnNext = iconBtn("⏭")
        btnPrev.setOnClickListener { onPrev?.invoke() }
        btnPlay.setOnClickListener { onPlayPause?.invoke() }
        btnNext.setOnClickListener { onNext?.invoke() }

        collapsed.addView(cIcon, lp(wrap, wrap, gap))
        collapsed.addView(cTitle, lp(0, wrap, gap, 1f))
        collapsed.addView(cSub, lp(0, wrap, gap, 1f))
        collapsed.addView(btnPrev, lp(wrap, wrap, gap))
        collapsed.addView(btnPlay, lp(wrap, wrap, gap))
        collapsed.addView(btnNext, lp(wrap, wrap, 0))
        collapsed.setOnClickListener { onToggle?.invoke() }

        // ---------- 展开态 ----------
        expanded = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
            background = pill(Color.parseColor("#F7F8FA").toInt(), 22f)
        }
        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        eArt = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#E3E5EA"))
        }
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), 0, 0, 0)
        }
        eTitle = TextView(context).apply {
            setTextColor(Color.parseColor("#1B1D21"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        eSub = TextView(context).apply {
            setTextColor(Color.parseColor("#6B7280"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 1
        }
        eProgress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
        }
        eTime = TextView(context).apply {
            setTextColor(Color.parseColor("#9AA0A8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        texts.addView(eTitle, lp(0, wrap, 0))
        texts.addView(eSub, lp(0, wrap, dp(6f)))
        texts.addView(eProgress, lp(0, dp(4f), dp(6f)))
        texts.addView(eTime, lp(0, wrap, 0))
        top.addView(eArt, lp(dp(56f), dp(56f), 0))
        top.addView(texts, lp(0, wrap, 0, 1f))

        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        ePrev = iconBtn("⏮", 20f)
        ePlay = iconBtn("⏸", 24f)
        eNext = iconBtn("⏭", 20f)
        val eClose = iconBtn("✕", 16f)
        ePrev.setOnClickListener { onPrev?.invoke() }
        ePlay.setOnClickListener { onPlayPause?.invoke() }
        eNext.setOnClickListener { onNext?.invoke() }
        eClose.setOnClickListener { onCollapse?.invoke() }
        controls.addView(ePrev, lp(wrap, wrap, dp(18f)))
        controls.addView(ePlay, lp(wrap, wrap, dp(18f)))
        controls.addView(eNext, lp(wrap, wrap, dp(18f)))
        controls.addView(eClose, lp(wrap, wrap, 0))

        expanded.addView(top, lp(0, wrap, 0))
        expanded.addView(controls, lp(0, wrap, 0).apply { topMargin = dp(14f) })
        expanded.setOnClickListener { }

        addView(collapsed, frameLp(wrap, dp(36f)))
        addView(expanded, frameLp(wrap, wrap))
        expanded.visibility = View.GONE
    }

    /** FrameLayout 参数，用于把胶囊居中放在顶部。 */
    private fun frameLp(w: Int, h: Int) = LayoutParams(w, h).apply {
        gravity = Gravity.CENTER
    }

    private fun iconBtn(glyph: String, sizeSp: Float = 15f) = TextView(context).apply {
        text = glyph
        setTextColor(Color.parseColor("#1B1D21"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        gravity = Gravity.CENTER
        setPadding(dp(6f), dp(4f), dp(6f), dp(4f))
    }

    private fun pill(color: Int, radiusDp: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
        setStroke(dp(0.5f), Color.parseColor("#1A000000"))
    }

    private fun dp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    private val wrap get() = ViewGroup.LayoutParams.WRAP_CONTENT
    private val match get() = ViewGroup.LayoutParams.MATCH_PARENT

    /** 通用 LinearLayout 参数，weight 仅在线性容器中有效。 */
    private fun lp(w: Int, h: Int, margin: Int, weight: Float = 0f) =
        LinearLayout.LayoutParams(w, h).apply {
            if (weight > 0f) this.weight = weight
            if (margin > 0) setMargins(0, 0, margin, 0)
        }

    /** 根据当前卡片刷新界面。 */
    fun render(card: IslandCard?, expandedNow: Boolean) {
        collapsed.visibility = if (expandedNow) View.GONE else View.VISIBLE
        expanded.visibility = if (expandedNow) View.VISIBLE else View.GONE
        if (card == null) {
            collapsed.visibility = View.GONE
            expanded.visibility = View.GONE
            return
        }
        when (card) {
            is IslandCard.Music -> renderMusic(card, expandedNow)
            is IslandCard.Navigation -> renderNav(card, expandedNow)
            is IslandCard.Payment -> renderPay(card, expandedNow)
        }
    }

    private fun renderMusic(c: IslandCard.Music, expandedNow: Boolean) {
        cIcon.text = "♪"
        cTitle.text = c.title
        cSub.text = c.artist
        btnPlay.text = if (c.playing) "⏸" else "▶"
        btnPrev.visibility = if (c.canControl) View.VISIBLE else View.GONE
        btnNext.visibility = if (c.canControl) View.VISIBLE else View.GONE
        btnPrev.isEnabled = c.canControl
        btnNext.isEnabled = c.canControl

        eTitle.text = c.title
        eSub.text = listOfNotNull(c.artist, c.album.takeIf { it.isNotBlank() }).joinToString(" · ")
        ePlay.text = if (c.playing) "⏸" else "▶"
        eProgress.progress = if (c.durationMs > 0)
            ((c.positionMs * 1000 / c.durationMs).toInt()).coerceIn(0, 1000) else 0
        eTime.text = if (c.durationMs > 0)
            "${fmt(c.positionMs)} / ${fmt(c.durationMs)}" else ""
        c.artwork?.let { eArt.setImageBitmap(it) } ?: eArt.setImageDrawable(null)
    }

    private fun renderNav(c: IslandCard.Navigation, expandedNow: Boolean) {
        cIcon.text = "↱"
        cTitle.text = c.maneuver
        cSub.text = if (c.distanceMeters > 0) "${c.distanceMeters}m" else ""
        btnPrev.visibility = View.GONE
        btnNext.visibility = View.GONE
        btnPlay.visibility = View.GONE

        eTitle.text = c.maneuver
        eSub.text = listOfNotNull(
            c.road.takeIf { it.isNotBlank() },
            if (c.distanceMeters > 0) "剩余 ${c.distanceMeters} 米" else null,
            "剩余 ${c.remainSeconds / 60} 分 ${c.remainSeconds % 60} 秒",
            c.speedLimitKmh?.let { "限速 $it" }
        ).joinToString(" · ")
        eProgress.progress = 0
        eTime.text = ""
        eArt.setImageDrawable(null)
    }

    private fun renderPay(c: IslandCard.Payment, expandedNow: Boolean) {
        cIcon.text = if (c.success) "✓" else "!"
        cTitle.text = if (c.success) "收款成功" else "支付失败"
        cSub.text = "¥${"%.2f".format(c.amountCents / 100.0)}"
        btnPrev.visibility = View.GONE
        btnNext.visibility = View.GONE
        btnPlay.visibility = View.GONE

        eTitle.text = if (c.success) "收款成功" else "支付失败"
        eSub.text = listOfNotNull(
            "¥${"%.2f".format(c.amountCents / 100.0)}",
            c.payer?.takeIf { it.isNotBlank() },
            c.orderId?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        eProgress.progress = 0
        eTime.text = ""
        eArt.setImageDrawable(null)
    }

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }
}
