package com.seiko.danmu

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.SurfaceView

private val cleanPaint by lazy(LazyThreadSafetyMode.NONE) {
    Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
}

/**
 * 清空画布
 */
internal fun Canvas.clear() {
    drawPaint(cleanPaint)
}

/**
 * 是否需要拦截弹幕
 * @param danmaku 弹幕
 */
fun Collection<DanmakuBlocker>.shouldBlock(danmaku: Danmaku): Boolean {
    return find { it.block(danmaku) } != null
}

/**
 * 获取SurfaceView画布并绘制
 */
internal fun SurfaceView.withCanvas(block: Canvas.() -> Unit) {
    val canvas: Canvas = try {
        holder.lockHardwareCanvas()
    } catch (e: IllegalStateException) {
        e.printStackTrace()
        holder.lockCanvas()
    } ?: return
    block(canvas)
    holder.unlockCanvasAndPost(canvas)
}