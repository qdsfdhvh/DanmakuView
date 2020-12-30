package com.seiko.danmu.danmaku

import android.graphics.Bitmap
import com.seiko.danmu.DanmakuConfig

/**
 * 尝试创建缓存
 */
internal fun CacheDanmaku.tryBuildCache(config: DanmakuConfig): Bitmap? {
    if (cache == null) {
        onBuildCache(config)
    }
    return cache
}