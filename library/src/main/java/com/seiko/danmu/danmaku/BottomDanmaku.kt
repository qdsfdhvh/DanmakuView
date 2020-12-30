package com.seiko.danmu.danmaku

import android.graphics.Canvas
import android.graphics.RectF
import com.seiko.danmu.DanmakuConfig

class BottomDanmaku : LineDanmaku() {

    override fun onDraw(
        canvas: Canvas,
        drawWidth: Int,
        drawHeight: Int,
        progress: Float,
        config: DanmakuConfig,
        line: Int
    ): RectF? {
        val bitmap = tryBuildCache(config) ?: return null
        val x = (drawWidth - bitmap.width) / 2f
        val y = (drawHeight - config.lineHeight * line - config.marginBottom).toFloat()
        canvas.drawBitmap(bitmap, x, y, null)
        return RectF(x, y, x + bitmap.width, y + bitmap.height)
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