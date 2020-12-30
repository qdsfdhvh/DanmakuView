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
    forEach { blocker ->
        if (blocker.block(danmaku)) {
            return true
        }
    }
    return false
}

/**
 * 为弹幕创建画笔
 */
fun Danmaku.createPaint(config: DanmakuConfig): Paint {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = textColor
    paint.alpha = alpha
    paint.textSize = textSize * config.textSizeCoefficient
    paint.typeface = config.typeface
    paint.isUnderlineText = underLine
    when(config.drawMode) {
        DanmakuConfig.DEFAULT -> {}
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