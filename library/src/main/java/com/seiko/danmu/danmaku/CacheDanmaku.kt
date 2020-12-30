package com.seiko.danmu.danmaku

import android.graphics.Bitmap
import com.seiko.danmu.Danmaku
import com.seiko.danmu.DanmakuConfig

/**
 * 可缓存的弹幕
 */
abstract class CacheDanmaku : Danmaku() {

    /**
     * 绘制用缓存
     */
    var cache: Bitmap? = null

    /**
     * 绘制缓存
     */
    abstract fun onBuildCache(config: DanmakuConfig)

}