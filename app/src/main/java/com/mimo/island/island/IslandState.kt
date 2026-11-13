package com.mimo.island.island

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局状态总线：持有各来源的最新卡片，按优先级仲裁出当前该显示哪张。
 */
object IslandState {

    private val _current = MutableStateFlow<IslandCard?>(null)
    val current: StateFlow<IslandCard?> = _current.asStateFlow()

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded.asStateFlow()

    /** 展开态变化时通知渲染层重绘。 */
    @Volatile
    var expandedListener: ((Boolean) -> Unit)? = null

    /** 各来源上报的最新卡片，key 为来源标识。 */
    private val cards = mutableMapOf<String, IslandCard>()

    /** 支付播报有有效期，到期后自动降级回其它卡片。 */
    private var paymentExpiry: Long = 0L

    private val listeners = mutableSetOf<(IslandCard?) -> Unit>()

    fun onChange(l: (IslandCard?) -> Unit) {
        listeners += l
        l(_current.value)
    }

    fun offChange(l: (IslandCard?) -> Unit) {
        listeners -= l
    }

    fun submit(source: String, card: IslandCard?) {
        if (card == null) cards.remove(source) else cards[source] = card

        if (card is IslandCard.Payment) {
            paymentExpiry = System.currentTimeMillis() + PAYMENT_VISIBLE_MS
        }
        recompute()
    }

    fun setExpanded(v: Boolean) {
        if (_expanded.value == v) return
        _expanded.value = v
        expandedListener?.invoke(v)
    }

    fun toggleExpanded() {
        setExpanded(!_expanded.value)
    }

    private fun recompute() {
        val now = System.currentTimeMillis()
        val effective = cards.values.filter {
            // 支付过期后不再占据最高位
            if (it is IslandCard.Payment && now > paymentExpiry) false else true
        }
        val next = effective.maxByOrNull { it.priority }
        if (next != _current.value) {
            _current.value = next
            listeners.toList().forEach { it(next) }
        }
    }

    /** 由定时器驱动：支付播报到期后重算一次。 */
    fun tick() {
        if (System.currentTimeMillis() > paymentExpiry && cards.values.any { it is IslandCard.Payment }) {
            recompute()
        }
    }

    const val PAYMENT_VISIBLE_MS = 4000L
}
