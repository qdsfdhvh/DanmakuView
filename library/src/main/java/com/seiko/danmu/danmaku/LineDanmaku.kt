package com.seiko.danmu.danmaku

import android.graphics.Paint
import android.graphics.Rect
import com.seiko.danmu.Danmaku
import com.seiko.danmu.DanmakuConfig

abstract class LineDanmaku : Danmaku() {

    private var _paint: Paint? = null
    private var _size: Pair<Int, Int>? = null

    protected val borderPaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
    }

    fun getPaint(config: DanmakuConfig): Paint {
        var paint = _paint
        if (paint == null) {
            paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = textColor
            paint.alpha = alpha
            paint.isUnderlineText = underLine
            _paint = paint
        }
        paint.textSize = textSize * config.textSizeCoefficient
        paint.typeface = config.typeface
        when (config.drawMode) {
            DanmakuConfig.DEFAULT -> {
                paint.clearShadowLayer()
            }
            DanmakuConfig.SHADOW -> {
                paint.setShadowLayer(
                    config.shadowRadius,
                    config.shadowDx,
                    config.shadowDy,
                    config.shadowColor
                )
            }
        }
        return paint
    }

    /**
     * 绘制的编剧
     * @param canReadCached 是否可以读取缓存
     */
    fun getSize(config: DanmakuConfig, canReadCached: Boolean = false) =
        getSize(getPaint(config), canReadCached)

    fun getSize(paint: Paint, canReadCached: Boolean = false): Pair<Int, Int> {
        if (canReadCached && _size != null) {
            return _size!!
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val width = (bounds.width() + textSize / 3).toInt()
        val height = (bounds.height() + textSize / 3).toInt()
        _size = width to height
        return _size!!
    }

    /**
     * 字体的绘制高度
     */
    protected val Int.textHeight: Float
        get() = this - textSize / 3

    /**
     * 同行同类弹幕是否碰撞
     */
    abstract fun willHit(
        config: DanmakuConfig,
        other: LineDanmaku,
        drawWidth: Int, drawHeight: Int
    ): Boolean
}