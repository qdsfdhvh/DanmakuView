package com.seiko.danmu

interface DanmakuParser {

    fun parse(): Danmakus

    companion object {

        @JvmField
        val EMPTY: DanmakuParser = EmptyDanmakuParser()

        private class EmptyDanmakuParser : DanmakuParser {
            override fun parse(): Danmakus = ArrayList(0)
        }
    }
}