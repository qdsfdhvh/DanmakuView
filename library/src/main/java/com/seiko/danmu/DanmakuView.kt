package com.seiko.danmu

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.seiko.danmu.danmaku.LineDanmaku
import kotlinx.coroutines.*

class DanmakuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr),
    SurfaceHolder.Callback,
    CoroutineScope by MainScope() {

    companion object {
        private const val SPLIT_TIME = 1000L / 60
    }

    init {
        if (isInEditMode) {
            setBackgroundColor(Color.TRANSPARENT)
        } else {
            holder.addCallback(this)
            holder.setFormat(PixelFormat.TRANSLUCENT)
        }
    }

    private var drawWidth: Int = 0
    private var drawHeight: Int = 0

    /**
     * Surface是否创建
     */
    private var isSurfaceCreated: Boolean = false

    private var isPaused: Boolean = true
    private var isOnceResume: Boolean = false

    /**
     * 是否已经注销
     */
    var isDestroyed = false
        private set

    /**
     * 进行时间纳秒 (不准确)
     */
    var conductedTimeNs: Long = 0L
        private set

    /**
     * 进行时间毫秒
     */
    val conductedTimeMs: Long get() = conductedTimeNs / 1_000_000

    /**
     * 弹幕集合
     */
    private val danmakus: MutableCollection<Danmaku> = ArrayList(1000)

    /**
     * 正在显示的弹幕相关信息集合
     */
    private var showingDanmakus: Collection<ShowingDanmakuInfo> = emptySet()

    /**
     * 播放速度
     */
    var speed: Float = 1f

    /**
     * 是否调试
     */
    var isDebug: Boolean = false

    suspend fun parse(parser: DanmakuParser) {
        withContext(Dispatchers.Default) {
            danmakus.clear()
            danmakus.addAll(parser.parse())
            isPaused = true
            conductedTimeNs = 0
            isOnceResume = true
        }
    }

    fun drawOnce() {
        isOnceResume = true
    }

    fun add(danmaku: Danmaku) {
        danmakus.add(danmaku)
    }

    fun add(list: Danmakus) {
        danmakus.addAll(list)
    }

    fun remove(danmaku: Danmaku) {
        danmakus.remove(danmaku)
    }

    fun removeAll() {
        danmakus.clear()
    }

    /**
     * 暂停
     */
    fun pause() {
        if (!isPaused) {
            isPaused = true
            stopTimeKeeping()
        }
    }

    /**
     * 继续
     */
    fun resume() {
        if (isPaused) {
            isPaused = false
            startTimeKeeping()
        }
    }

    /**
     * 开始
     */
    fun start(config: DanmakuConfig, offsetMs: Long = 0) {
        conductedTimeNs = offsetMs * 1_000_000
        startDrawingDanmu(config)
        resume()
    }

    /**
     * 停止
     */
    fun stop() {
        pause()
        stopDrawingDanmu()
    }

    /**
     * 注销
     */
    fun destroy() {
        isDestroyed = true
        holder.removeCallback(this)
        cancel()
    }

    fun seekTo(timeMs: Long) {
        conductedTimeNs = timeMs * 1_000_000
        isOnceResume = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceCreated = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        drawWidth = width
        drawHeight = height
        isOnceResume = true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceCreated = false
    }

    private var timeJob: Job? = null
    private fun startTimeKeeping() {
        stopTimeKeeping()
        timeJob = launch(Dispatchers.Default) {
            while (!isDestroyed && !isPaused && isActive) {
                val startTime = System.nanoTime()
                delay(SPLIT_TIME)
                conductedTimeNs += ((System.nanoTime() - startTime) * speed).toLong()
            }
        }
    }

    private fun stopTimeKeeping() {
        timeJob?.cancel()
    }

    private var startJob: Job? = null
    private fun startDrawingDanmu(config: DanmakuConfig) {
        stopDrawingDanmu()
        startJob = launch(Dispatchers.Default) {
            while (!isDestroyed && isActive) {

                // 界面不显示
                if (!isSurfaceCreated) {
                    delay(200)
                    continue
                }

                // 绘制弹幕
                val startTime = System.currentTimeMillis()
                onDrawDanmakus(config)
                val deltaTime = System.currentTimeMillis() - startTime

                // 如果绘制过快，适当延时
                if (deltaTime < SPLIT_TIME) {
                    delay(SPLIT_TIME - deltaTime)
                }

                while (isPaused && !isOnceResume) {
                    delay(1)
                }

                if (isOnceResume) {
                    isOnceResume = false
                }
            }
        }
    }

    private fun stopDrawingDanmu() {
        startJob?.cancel()
    }

    private fun onDrawDanmakus(config: DanmakuConfig) {
        withCanvas {
            // 清空画布
            clear()
            // 绘制弹幕
            drawDanmakus(this, config)
            // 绘制调试文字
            if (isDebug) {
                drawDebugText(this)
            }
        }
    }

    /**
     * 绘制弹幕
     */
    private fun drawDanmakus(canvas: Canvas, config: DanmakuConfig) {
        // 计算出可以绘制多少行弹幕
        val maxLine = (drawHeight - config.marginTop - config.marginBottom) / config.lineHeight
        if (maxLine < 1) return

        val conductedTime = conductedTimeMs

        // 已经显示的弹幕
        val oldShowingDanmakus = showingDanmakus
        val newShowingDanmakus = mutableSetOf<ShowingDanmakuInfo>()

        // 准备绘制的弹幕，也是新增加的弹幕
        val willDrawDanmakus = mutableSetOf<Pair<Danmaku, Float>>()

        fun drawAndAdd(
            danmaku: Danmaku,
            line: Int, progress: Float,
            info: ShowingDanmakuInfo? = null
        ) {
            // 绘制弹幕
            val rect = drawDanmaku(
                canvas = canvas,
                config = config,
                danmaku = danmaku,
                maxLine = maxLine,
                progress = progress,
                line = line,
            )
            // 绘制成功后，记录信息
            if (rect != null) {
                if (info != null) {
                    info.rect = rect
                    info.progress = progress
                    newShowingDanmakus.add(info)
                } else {
                    newShowingDanmakus.add(
                        ShowingDanmakuInfo(danmaku, line, rect, progress)
                    )
                }
            }
        }

        // Will java.util.ConcurrentModificationException
        ArrayList(danmakus).asSequence()
            // 弹幕显示
            .filter { it.visibility }
            // 弹幕没有被拦截
            .filter { !config.blockers.shouldBlock(it) }
            .forEach { danmaku ->
                val duration = danmaku.duration * config.durationCoefficient
                val start = danmaku.offset
                val end = (danmaku.offset + duration).toLong()
                val progress = (conductedTime - start) / duration

                // 过滤不在时间段内
                if (conductedTime !in start..end) return@forEach

                // 直接绘制旧弹幕
                val info = oldShowingDanmakus.find { it.danmaku == danmaku }
                if (info != null) {
                    drawAndAdd(danmaku, info.line, progress, info)
                    return@forEach
                }

                // 放入待绘制列表
                willDrawDanmakus.add(danmaku to progress)
            }

        // 将已经显示的弹幕先按行数
        val lineDanmakuInfoList = newShowingDanmakus.filter { it.danmaku is LineDanmaku }
            .sortedBy { it.line }

        // 开始绘制新弹幕
        willDrawDanmakus.forEach { (danmaku, progress) ->
            if (danmaku is LineDanmaku) {
                // 这里计算行数不严谨，后续待调整
                var line = 1
                for (info in lineDanmakuInfoList) {
                    if (info.line > line) {
                        break
                    }
                    if (info.line == line) {
                        if (isLineDanmakuHit(config, danmaku, info.danmaku as LineDanmaku)) {
                            line++
                        }
                    }
                }
                drawAndAdd(danmaku, line, progress)
            } else {
                drawAndAdd(danmaku, 0, progress)
            }
        }

        showingDanmakus = newShowingDanmakus
    }

    /**
     * 弹幕是否会碰撞(重叠)
     */
    private fun isLineDanmakuHit(
        config: DanmakuConfig,
        danmaku1: LineDanmaku,
        danmaku2: LineDanmaku
    ): Boolean {
        // 只比较同类
        if (danmaku1.javaClass != danmaku2.javaClass) {
            return false
        }
        return danmaku1.willHit(config, danmaku2, drawWidth, drawHeight)
    }

    /**
     * 绘制弹幕
     */
    private fun drawDanmaku(
        canvas: Canvas,
        config: DanmakuConfig,
        danmaku: Danmaku,
        maxLine: Int,
        progress: Float,
        line: Int,
    ): RectF? {
        // 最大行数大于1时，绘制行不能比最大行大
        if (maxLine in 1 until line) return null
        // 绘制成功后，返回位置
        return danmaku.onDraw(
            canvas = canvas,
            config = config,
            drawWidth = drawWidth,
            drawHeight = drawHeight,
            progress = progress,
            line = line
        )
    }

    /**
     * 绘制调试文字
     */
    private fun drawDebugText(canvas: Canvas) {
        canvas.drawText(
            "conductedTimeNs = $conductedTimeNs, " +
                    "speed = $speed, " +
                    "size = ${drawWidth}x$drawHeight",
            20F, drawHeight - 100F,
            debugPaint
        )
        canvas.drawText(
            "showingCount = ${showingDanmakus.size}, " +
                    "count = ${danmakus.size}",
            20F, drawHeight - 50F,
            debugPaint
        )
    }

    private val debugPaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply {
            color = Color.WHITE
            textSize = 40f
        }
    }

    /**
     * 正在显示的弹幕相关信息
     */
    data class ShowingDanmakuInfo(
        val danmaku: Danmaku,
        val line: Int,
        var rect: RectF,
        var progress: Float
    )
}