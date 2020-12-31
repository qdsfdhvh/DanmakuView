package com.seiko.danmu.danmaku

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.seiko.danmu.DanmakuConfig

class BottomDanmaku : LineDanmaku() {

    override fun onDraw(
        canvas: Canvas,
        config: DanmakuConfig,
        drawWidth: Int,
        drawHeight: Int,
        progress: Float,
        line: Int
    ): RectF {
        val (width, height) = getSize(config)
        val x = (drawWidth - width) / 2f
        val y = (drawHeight - config.lineHeight * line - config.marginBottom).toFloat()
        canvas.drawText(text, x, y + height, getPaint(config))

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
        return checkStaticLineDanmakuHit(config, other)
    }
}