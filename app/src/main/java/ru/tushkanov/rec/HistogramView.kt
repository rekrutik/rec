package ru.tushkanov.rec

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class HistogramView : View {

    sealed class DrawState(val levels: List<Float>) {
        class Recording(levels: List<Float>) : DrawState(levels)
        class Narrow(levels: List<Float>, val preTransition: Float = 0f, val postTransition: Float  = 0f, val progress: Float  = 0f) : DrawState(levels)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var state: DrawState? = DrawState.Recording(emptyList())
        private set

    private val histogramWidth: Float by lazy { 4f.dip(context) }
    private val offset: Float by lazy { 4f.dip(context) }
    private val cornerRadius: Float by lazy { 2f.dip(context) }
    private val rect: RectF by lazy { RectF() }
    private var computedLevels = mutableListOf<Float>()
    private val evaluator = ArgbEvaluator()
    private val histogramColor: Int by lazy { ContextCompat.getColor(context, R.color.fg_histogram) }
    private val histogramInactiveColor: Int by lazy { ContextCompat.getColor(context, R.color.fg_histogram_inactive) }
    private val paint: Paint by lazy {
        Paint().apply {
            color = ContextCompat.getColor(context, R.color.fg_histogram)
            style = Paint.Style.FILL
            flags = Paint.ANTI_ALIAS_FLAG
        }
    }

    fun update(state: DrawState) {
        this.state = state
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return
        state?.let {
            val size = histogramWidth + offset
            val count = ceil(width / size).toInt()
            if (computedLevels.size < count) {
                computedLevels = MutableList(count) { 0f }
            }
            var offset = 0
            for (i in 0 until count - it.levels.size) {
                computedLevels[offset] = 0f
                offset += 1
            }
            for (i in max(0, it.levels.size - count) until it.levels.size) {
                computedLevels[offset] = it.levels[i]
                offset += 1
            }
            var inactive = histogramColor
            var activeBound = 1f
            when (it) {
                is DrawState.Recording -> {}
                is DrawState.Narrow -> {
                    for (i in 0 until count) {
                        val from = floor(i * it.levels.size / count.toFloat()).toInt()
                        val to = max(from + 1, ((i + 1) * it.levels.size / count.toFloat()).toInt())
                        var sum = 0f
                        for (j in from until to) {
                            sum += it.levels[j]
                        }
                        sum /= (to - from).toFloat()
                        computedLevels[i] = computedLevels[i] + (sum - computedLevels[i]) * it.preTransition
                        computedLevels[i] = computedLevels[i] * (1f - it.postTransition)
                    }
                    activeBound = it.progress
                    inactive = evaluator.evaluate(it.preTransition * (1f - it.postTransition), histogramColor, histogramInactiveColor) as Int
                }
            }
            computedLevels.forEachIndexed { i, v ->
                rect.set(
                    size * i,
                    height / 2f - cornerRadius - height / 2f * v,
                    size * i + histogramWidth,
                    height / 2f + cornerRadius + height / 2f * v
                )
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint.apply {
                    color = if (i < count * activeBound) {
                        histogramColor
                    } else {
                        inactive
                    }
                })
            }
        }
    }
}