package com.seiko.danmu

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

private val cleanPaint by lazy(LazyThreadSafetyMode.NONE) {
    Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
}

/**
 * 清空画布
 */
fun Canvas.clear() {
    drawPaint(cleanPaint)
}

/**
 * 是否需要拦截弹幕
 * @param danmaku 弹幕
 */
fun Collection<DanmakuBlocker>.showBlock(danmaku: Danmaku): Boolean {
    return find { it.block(danmaku) } != null
}