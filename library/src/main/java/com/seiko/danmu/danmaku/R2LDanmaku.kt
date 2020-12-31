package com.seiko.danmu.danmaku

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.seiko.danmu.DanmakuConfig

class R2LDanmaku : LineDanmaku() {

    override var duration: Long = 10000

    override fun onDraw(
        canvas: Canvas,
        config: DanmakuConfig,
        drawWidth: Int,
        drawHeight: Int,
        progress: Float,
        line: Int
    ): RectF {
        val (width, height) = getSize(config)
        val x = (drawWidth + width) * (1 - progress) - width
        val y = (config.lineHeight * (line - 1)).toFloat() + config.marginTop

        canvas.drawText(text, x, y + height.textHeight, getPaint(config))

        val rectF = RectF(x, y, x + width, y + height)
        // 绘制边框
        if (borderColor != Color.TRANSPARENT) {
            canvas.drawRect(rectF, borderPaint)
        }
        return rectF
    }

    override fun willHit(
        config: DanmakuConfig,
        other: LineDanmaku,
        drawWidth: Int,
        drawHeight: Int
    ): Boolean {
        return checkScrollLineDanmakuHit(config, other, drawWidth)
    }

}