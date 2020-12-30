package com.seiko.danmu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.annotation.IntRange

abstract class Danmaku {

    /**
     * 弹幕文本
     */
    var text: String = ""

    /**
     * 字体大小
     */
    open var textSize: Float = 25f

    /**
     * 字体颜色
     */
    @ColorInt
    open var textColor: Int = Color.WHITE

    /**
     * 字体阴影/描边颜色
     */
    @ColorInt
    open var textShadowColor: Int = Color.TRANSPARENT

    /**
     * 下划线
     */
    open var underLine: Boolean = false

    /**
     * 透明度
     */
    @IntRange(from = 0, to = 255)
    open var alpha = 255

    /**
     * 偏移时间
     */
    open var offset: Long = 0L

    /**
     * 边框颜色
     */
    @ColorInt
    var borderColor: Int = Color.TRANSPARENT

    /**
     * 存货时间ms
     */
    open var duration: Long = 5000L

    /**
     * 是否可见
     */
    var visibility: Boolean = true

    /**
     * 标签
     */
    open var tag: Any? = null

    /**
     * 绘制弹幕
     * @param drawWidth  画布宽度
     * @param drawHeight 画布高度
     * @param progress 进度[0, 1]
     * @param line 行弹幕绘制在第几行, 从1开始计数, 对于非行弹幕常为 0
     * @return 画到哪了 null:没画
     */
    abstract fun onDraw(
        canvas: Canvas,
        drawWidth: Int,
        drawHeight: Int,
        progress: Float,
        config: DanmakuConfig,
        line: Int
    ): RectF?

    override fun toString(): String = text
}