package com.seiko.danmu.danmaku

import android.graphics.*
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import com.seiko.danmu.DanmakuConfig
import com.seiko.danmu.createPaint

private typealias FrameModel = Triple<PointF, Float, Int>

class SpecialDanmaku : CacheDanmaku() {

    companion object {
        private val DEFAULT_START_FRAME: FrameModel = Triple(PointF(0f, 0f), 0f, 255)
        private val DEFAULT_END_FRAME: FrameModel = Triple(PointF(1f, 1f), 0f, 255)
    }

    private var lines: Array<String>? = null

    var keyframes = mutableMapOf<Float, FrameModel>()

    private val bitmapPaint by lazy(LazyThreadSafetyMode.NONE) { Paint() }

    override fun onBuildCache(config: DanmakuConfig) {
        var paint = createPaint(config)
        paint.alpha = 255

        val textArray = lines ?: arrayOf(text)

        val boundsList = mutableListOf<Rect>()
        var width = 0; var height = 0
        textArray.forEach { s ->
            val bounds = Rect()
            paint.getTextBounds(s, 0, s.length, bounds)
            boundsList.add(bounds)
            if (width < bounds.width()) {
                width = bounds.width()
            }
            height += bounds.height()
        }
        width += (textSize / 3).toInt()
        height += (textSize / 3).toInt()
        if (width == 0 || height == 0) return

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        var nextY = 0f
        textArray.forEachIndexed { i, s ->
            nextY += boundsList[i].height()
            canvas.drawText(s, 0f, nextY, paint)
        }

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

    override fun onDraw(
        canvas: Canvas,
        drawWidth: Int,
        drawHeight: Int,
        progress: Float,
        config: DanmakuConfig,
        line: Int
    ): RectF? {
        val bitmap = tryBuildCache(config) ?: return null

        var lastKeyframeP = 0f
        var nextKeyframeP = 1f
        var lastFrame = DEFAULT_START_FRAME
        var nextFrame = DEFAULT_END_FRAME

        keyframes.forEach { (p, frame) ->
            if (p in lastKeyframeP..progress) {
                lastKeyframeP = p
                lastFrame = frame
            }
            if (p in progress..nextKeyframeP) {
                nextKeyframeP = p
                nextFrame = frame
            }
        }

        val (lastPoint, lastZR, lastAlpha) = lastFrame
        val (nextPoint, nextZR, nextAlpha) = nextFrame
        val (lastX, lastY) = lastPoint
        val (nextX, nextY) = nextPoint

        val fraction = (progress - lastKeyframeP) / (nextKeyframeP - lastKeyframeP)
        val x = lastX + (nextX - lastX) * fraction
        val y = lastY + (nextY - lastY) * fraction
        val zr = lastZR + (nextZR - lastZR) * fraction
        val alpha = (lastAlpha + (nextAlpha - lastAlpha) * fraction).toInt()

        val drawX = x + drawWidth
        val drawY = y + drawHeight

        bitmapPaint.alpha = alpha
        if (zr == 0f) {
            canvas.drawBitmap(bitmap, drawX, drawY, bitmapPaint)
            return RectF(drawX, drawY, drawX + bitmap.width, drawY + bitmap.height)
        }

        val matrix = Matrix()
        matrix.postRotate(zr)
        matrix.postTranslate(drawX, drawY)
        canvas.drawBitmap(bitmap, matrix, bitmapPaint)
        val rect = RectF()
        matrix.mapRect(rect)
        return rect
    }

    fun fillText() {
        if (text.isNotEmpty() && text.contains("\n")) {
            lines = text.split("\n").toTypedArray()
        }
    }

}