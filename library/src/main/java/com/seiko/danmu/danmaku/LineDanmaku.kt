package com.seiko.danmu.danmaku

import android.graphics.*
import com.seiko.danmu.DanmakuConfig
import com.seiko.danmu.createPaint

abstract class LineDanmaku : CacheDanmaku() {

    override fun onBuildCache(config: DanmakuConfig) {
        var paint = createPaint(config)

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val width = (bounds.width() + textSize / 3).toInt()
        val height = (bounds.height() + textSize / 3).toInt()
        if (width == 0 || height == 0) return

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        canvas.drawText(text, 0f, bounds.height().toFloat(), paint)

        // 绘制边框
        if (borderColor != Color.TRANSPARENT) {
            paint = Paint()
            paint.color = borderColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        cache = bitmap
    }

    /**
     * 同行同类弹幕是否碰撞
     */
    abstract fun willHit(
        config: DanmakuConfig,
        other: LineDanmaku,
        drawWidth: Int, drawHeight: Int
    ): Boolean
}