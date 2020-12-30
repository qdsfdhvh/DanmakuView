package com.seiko.danmu.danmaku

import android.graphics.Canvas
import android.graphics.RectF
import com.seiko.danmu.DanmakuConfig

class L2RDanmaku : LineDanmaku() {

    override var duration: Long = 10000

    override fun onDraw(
        canvas: Canvas,
        drawWidth: Int,
        drawHeight: Int,
        progress: Float,
        config: DanmakuConfig,
        line: Int
    ): RectF? {
        val bitmap = tryBuildCache(config) ?: return null
        val x = (drawWidth + bitmap.width) * progress - bitmap.width
        val y = (config.lineHeight * (line - 1)).toFloat() + config.marginTop
        canvas.drawBitmap(bitmap, x, y, null)
        return RectF(x, y, x + bitmap.width, y + bitmap.height)
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