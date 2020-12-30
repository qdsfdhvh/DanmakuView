package com.seiko.danmu.danmaku

import com.seiko.danmu.DanmakuConfig

internal fun LineDanmaku.checkStaticLineDanmakuHit(
    config: DanmakuConfig,
    other: LineDanmaku,
): Boolean {
    val thisDanmakuStart = offset
    val thisDanmakuEnd = offset + (duration * config.durationCoefficient).toLong()
    val otherDanmakuStart = other.offset
    val otherDanmakuEnd = other.offset + (other.duration * config.durationCoefficient).toLong()
    return thisDanmakuStart in otherDanmakuStart..otherDanmakuEnd
            || otherDanmakuStart in thisDanmakuStart..thisDanmakuEnd
}

internal fun LineDanmaku.checkScrollLineDanmakuHit(
    config: DanmakuConfig,
    other: LineDanmaku,
    drawWidth: Int,
): Boolean {
    if (this.offset == other.offset) {
        return true
    }

    if (this.cache == null) this.onBuildCache(config)
    val otherCache = other.cache ?: return false

    val otherSpeed = (drawWidth + otherCache.width).toDouble() /
            (other.duration * config.durationCoefficient)
    val otherFullShowTime = other.offset + (otherCache.width / otherSpeed).toLong()
    if (this.offset in other.offset..otherFullShowTime) {
        return true
    }

    if (other.cache == null) other.onBuildCache(config)
    val thisCache = cache ?: return false

    val thisSpeed = (drawWidth + thisCache.width).toDouble() /
            (this.duration * config.durationCoefficient)
    val thisFullShowTime = this.offset + (thisCache.width / thisSpeed).toLong()
    if (other.offset in this.offset..thisFullShowTime) return true

    if (thisSpeed == otherSpeed) return false

    val x1 = otherSpeed * (this.offset - other.offset) - otherCache.width
    if (x1 > 0) {
        val t1 = x1 / (thisSpeed - otherSpeed)
        if (t1 in 0.0..(drawWidth / thisSpeed)) return true
    }

    val x2 = thisSpeed * (other.offset - this.offset) - thisCache.width
    if (x2 > 0) {
        val t2 = x2 / (otherSpeed - thisSpeed)
        if (t2 in 0.0..(drawWidth / otherSpeed)) return true
    }

    return false //不会碰撞
}