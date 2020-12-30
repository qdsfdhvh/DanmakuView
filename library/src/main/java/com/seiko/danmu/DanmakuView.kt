package com.seiko.danmu

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.seiko.danmu.danmaku.CacheDanmaku
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
        private const val TAG = "DanmakuView"
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
     * 是否正在绘制
     */
    private var isRunning: Boolean = false

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
     * 进行时间纳秒 (不准确)
     */
    val conductedTimeMs: Long get() = conductedTimeNs / 1_000_000

    /**
     * 弹幕集合
     */
    var danmakus: Danmakus = ArrayList(1000)

    /**
     * 正在显示的弹幕相关信息集合
     */
    var showingDanmakus: Collection<ShowingDanmakuInfo> = emptySet()
        private set

    /**
     * 播放速度
     */
    var speed: Float = 1f

    /**
     * 是否调试
     */
    var isDebug: Boolean = false

    var danmakuConfig: DanmakuConfig? = null
        set(value) {
            field = value
            if (isRunning && value != null) {
                startDrawDanmu(value)
            }
        }

    /**
     * 构建缓存
     * @param refresh 刷新已有缓存
     */
    fun buildCache(config: DanmakuConfig, refresh: Boolean = false) {
        launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            val count = danmakus.asSequence()
                .filterIsInstance(CacheDanmaku::class.java)
                .filter { it.cache == null || refresh }
                .sumBy { danmaku ->
                    danmaku.onBuildCache(config)
                    if (danmaku.cache != null) 1 else 0
                }
            val deltaTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "buildCache: $count of ${danmakus.size} danmakus in $deltaTime ms")
        }
    }

    /**
     * 清空缓存
     */
    fun clearCache() {
        launch(Dispatchers.Default + NonCancellable) {
            danmakus.asSequence()
                .filterIsInstance(CacheDanmaku::class.java)
                .forEach {
                    it.cache?.recycle()
                    it.cache = null
                }
        }
    }

    suspend fun parse(parser: DanmakuParser) {
        withContext(Dispatchers.Default) {
            danmakus = parser.parse()
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

    fun add(list: Collection<Danmaku>) {
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
        isPaused = true
    }

    /**
     * 继续
     */
    fun resume() {
        if (isPaused) {
            isPaused = false
            launch(Dispatchers.Default) {
                while (!isDestroyed && !isPaused && isActive) {
                    val startTime = System.nanoTime()
                    delay(16)
                    conductedTimeNs += ((System.nanoTime() - startTime) * speed).toLong()
                }
            }
        }
    }

    /**
     * 开始
     */
    fun start(offsetMs: Long = 0) {
        conductedTimeNs = offsetMs * 1_000_000
        resume()
    }

    /**
     * 注销
     */
    fun destroy() {
        isDestroyed = true
        holder.removeCallback(this)
        clearCache()
        cancel()
    }

    fun seekTo(timeMs: Long) {
        conductedTimeNs = timeMs * 1_000_000
        isOnceResume = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        startDrawDanmu(danmakuConfig ?: return)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        drawWidth = width
        drawHeight = height
        isOnceResume = true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        destroy()
    }

    private var startJob: Job? = null

    fun startDrawDanmu(config: DanmakuConfig) {
        startJob?.cancel()
        startJob = launch(Dispatchers.Default) {
            while (isRunning) {

                // 绘制弹幕
                val startTime = System.currentTimeMillis()
                onDrawDanmakus(config)
                val deltaTime = System.currentTimeMillis() - startTime

                // 如果绘制过快，适当延时
                if (deltaTime < 16) {
                    delay(20 - deltaTime)
                }

                while (isPaused && isRunning && !isOnceResume) {
                    delay(1)
                }

                if (isOnceResume) {
                    isOnceResume = false
                }
            }
        }
    }

    private fun onDrawDanmakus(config: DanmakuConfig) {
        val canvas: Canvas = try {
            holder.lockHardwareCanvas()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            holder.lockCanvas()
        } ?: return
        // 清空画布
        canvas.clear()
        // 绘制弹幕
        drawDanmakus(canvas, config)
        // 绘制调试文字
        if (isDebug) {
            drawDebugText(canvas)
        }
        holder.unlockCanvasAndPost(canvas)
    }

    /**
     * 绘制弹幕
     */
    private fun drawDanmakus(canvas: Canvas, config: DanmakuConfig) {
        // 计算出可以绘制多少行弹幕
        val maxLine = (drawHeight - config.marginTop - config.marginBottom) / config.lineHeight
        if (maxLine < 1) return

        val oldDanmakus = showingDanmakus
        val conductedTime = conductedTimeMs

        val willShowDanmakus = mutableSetOf<ShowingDanmakuInfo>()
        danmakus.asSequence()
            // 弹幕显示
            .filter { it.visibility }
            // 弹幕没有被拦截
            .filter { !config.blockers.showBlock(it) }
            // 在时间段内
            .filter { danmaku ->
                val duration = danmaku.duration * config.durationCoefficient
                val start = danmaku.offset
                val end = (danmaku.offset + duration).toLong()
                conductedTime in start..end
            }
            .forEach { danmaku ->
                val duration = danmaku.duration * config.durationCoefficient
                val start = danmaku.offset
                val progress = (conductedTime - start) / duration

                val info = oldDanmakus.find { it.danmaku == danmaku }
                if (info != null) {
                    drawDanmaku(
                        canvas = canvas,
                        config = config,
                        danmaku = danmaku,
                        maxLine = maxLine,
                        progress = progress,
                        line = info.line,
                        willShowDanmakus = willShowDanmakus
                    )
                    return@forEach
                }

                if (danmaku is LineDanmaku) {
                    var line = 1
                    var moved: Boolean
                    do {
                        moved = false
                        for (info in willShowDanmakus) {
                            if (line == info.line && danmaku.javaClass == info.danmaku.javaClass) {
                                if (danmaku.willHit(
                                        config,
                                        info.danmaku as LineDanmaku,
                                        drawWidth,
                                        drawHeight
                                    )
                                ) {
                                    line++
                                    moved = true
                                    break
                                }
                            }
                        }
                    } while (moved)
                    drawDanmaku(
                        canvas = canvas,
                        config = config,
                        danmaku = danmaku,
                        maxLine = maxLine,
                        progress = progress,
                        line = line,
                        willShowDanmakus = willShowDanmakus
                    )
                    return@forEach
                }

                drawDanmaku(
                    canvas = canvas,
                    config = config,
                    maxLine = 0,
                    danmaku = danmaku,
                    progress = progress,
                    line = 0,
                    willShowDanmakus = willShowDanmakus
                )
            }
        showingDanmakus = willShowDanmakus
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
        willShowDanmakus: MutableCollection<ShowingDanmakuInfo>
    ) {
        if (line == 0) {
            danmaku.onDraw(
                canvas,
                drawWidth, drawHeight,
                progress, config,
                0
            )?.let {
                willShowDanmakus.add(
                    ShowingDanmakuInfo(
                        danmaku, it, 0, progress
                    )
                )
            }
            return
        }

        // TODO 不了解
        if (line < maxLine || config.isAllowCovering) {
            var drawLine = line % maxLine
            if (drawLine == 0) drawLine = maxLine

            danmaku.onDraw(
                canvas,
                drawWidth, drawHeight,
                progress, config,
                drawLine
            )?.let {
                willShowDanmakus.add(
                    ShowingDanmakuInfo(
                        danmaku, it, line, progress
                    )
                )
            }
        }
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
            "showingCount = ${showingDanmakus.size}," +
                    " count = ${danmakus.size}",
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
        val rect: RectF,
        val line: Int,
        val progress: Float
    )
}