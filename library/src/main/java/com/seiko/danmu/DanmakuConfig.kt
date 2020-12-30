package com.seiko.danmu

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.IntDef

/**
 * 弹幕公共配置
 */
class DanmakuConfig internal constructor(
    builder: Builder
) {
    companion object {
        const val DEFAULT = 100
        const val SHADOW = 200
    }

    @IntDef(DEFAULT, SHADOW)
    @Retention(AnnotationRetention.SOURCE)
    annotation class DrawMode

    /**
     * 字体
     */
    var typeface: Typeface = builder.typeface

    /**
     * 持续时间系数
     */
    var durationCoefficient: Float = builder.durationCoefficient

    /**
     * 字体大小系数
     */
    var textSizeCoefficient: Float = builder.textSizeCoefficient

    /**
     * 行高（像素）
     */
    var lineHeight: Int = builder.lineHeight

    /**
     * 上边距
     */
    var marginTop: Int = builder.marginTop

    /**
     * 下边距
     */
    var marginBottom: Int = builder.marginBottom

    /**
     * 绘制模式
     */
    @DrawMode
    val drawMode: Int = builder.drawMode

    /**
     * 阴影半径
     */
    val shadowRadius: Float = builder.shadowRadius

    /**
     * 阴影 X 偏移
     */
    var shadowDx: Float = builder.shadowDx

    /**
     * 阴影 X 偏移
     */
    var shadowDy: Float = builder.shadowDy

    /**
     * 阴影颜色
     */
    @ColorInt
    val shadowColor: Int = builder.shadowColor

    /**
     * 是否允许覆盖
     */
    var isAllowCovering: Boolean = builder.isAllowCovering

    /**
     * 屏蔽器
     */
    val blockers: MutableList<DanmakuBlocker> = builder.blockers

    fun newBuilder(): Builder = Builder(this)

    class Builder constructor() {
        internal var typeface: Typeface = Typeface.DEFAULT
        internal var durationCoefficient: Float = 1f
        internal var textSizeCoefficient: Float = 1f
        internal var lineHeight: Int = 40
        internal var marginTop: Int = 0
        internal var marginBottom: Int = 0
        @DrawMode internal var drawMode: Int = DEFAULT
        internal var shadowRadius: Float = 5f
        internal var shadowDx: Float = 0f
        internal var shadowDy: Float = 0f
        @ColorInt internal var shadowColor: Int = Color.DKGRAY
        internal var isAllowCovering: Boolean = false
        internal var blockers: MutableList<DanmakuBlocker> = mutableListOf()

        constructor(config: DanmakuConfig): this() {
            this.typeface = config.typeface
            this.durationCoefficient = config.durationCoefficient
            this.textSizeCoefficient = config.textSizeCoefficient
            this.lineHeight = config.lineHeight
            this.marginTop = config.marginTop
            this.marginBottom = config.marginBottom
            this.drawMode = config.drawMode
            this.shadowRadius = config.shadowRadius
            this.shadowDx = config.shadowDx
            this.shadowDy = config.shadowDy
            this.shadowColor = config.shadowColor
            this.isAllowCovering = config.isAllowCovering
            this.blockers = config.blockers
        }

        fun build(): DanmakuConfig {
            return DanmakuConfig(this)
        }
    }

}