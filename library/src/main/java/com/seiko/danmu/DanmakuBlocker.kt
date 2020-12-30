package com.seiko.danmu

/**
 * 弹幕拦截器
 */
interface DanmakuBlocker {

    /**
     * 是否需要拦截
     */
    fun block(danmaku: Danmaku): Boolean

}