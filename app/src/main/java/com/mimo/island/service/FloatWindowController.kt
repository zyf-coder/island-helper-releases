package com.mimo.island.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import com.mimo.island.island.IslandState
import com.mimo.island.island.IslandView

/**
 * 悬浮窗控制器：在屏幕顶部放置灵动岛胶囊。
 * 使用 TYPE_APPLICATION_OVERLAY，不抢占触摸，展开面板内才接收事件。
 */
class FloatWindowController(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var container: FrameLayout? = null
    private var islandView: IslandView? = null

    /** 由 IslandService 注入，负责实际的播放控制与展开行为。 */
    var callbacks: Callbacks? = null

    interface Callbacks {
        fun onToggleExpanded()
        fun onCollapse()
        fun onPlayPause(): Boolean
        fun onNext(): Boolean
        fun onPrevious(): Boolean
    }

    private val listener: (com.mimo.island.island.IslandCard?) -> Unit = { card ->
        islandView?.render(card, IslandState.expanded.value)
    }

    private val expandedListener: (Boolean) -> Unit = { expanded ->
        islandView?.render(IslandState.current.value, expanded)
    }

    fun show() {
        if (container != null) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val root = FrameLayout(context)
        val view = IslandView(context)
        view.onToggle = { callbacks?.onToggleExpanded() }
        view.onCollapse = { callbacks?.onCollapse() }
        view.onPlayPause = { callbacks?.onPlayPause() }
        view.onNext = { callbacks?.onNext() }
        view.onPrev = { callbacks?.onPrevious() }
        islandView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }

        root.addView(view, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        container = root

        try {
            wm.addView(root, params)
        } catch (e: Exception) {
            container = null
            islandView = null
            return
        }
        IslandState.onChange(listener)
        IslandState.expandedListener = expandedListener
        listener(IslandState.current.value)
    }

    fun hide() {
        IslandState.offChange(listener)
        IslandState.expandedListener = null
        container?.let { c ->
            try {
                windowManager?.removeView(c)
            } catch (e: Exception) {
            }
        }
        container = null
        islandView = null
    }

    val isShowing: Boolean get() = container != null
}
