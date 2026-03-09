package com.example.flickey

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2

interface FlickeyActionListener {
    fun onTextInput(text: String?)
    fun onDelete()
    fun onShift()
    fun onEnter()
    fun onCursorLeft()
    fun onCursorRight()
}

class Flickey(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var startX = 0f
    private var startY = 0f

    var onFlickDetected: ((String) -> Unit)? = null

    private val directionMap = mapOf(
        0 to "a",  // ↑
        1 to "b",  // ↗
        2 to "c",  // →
        3 to "d",  // ↘
        4 to "e",  // ↓
        5 to "f",  // ↙
        6 to "g",  // ←
        7 to "s"   // ↖
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - startX
                val dy = startY - event.y 
                val angle = Math.toDegrees(atan2(dy, dx).toDouble()).let {
                    if (it < 0) it + 360 else it
                }

                val direction = ((angle + 22.5) / 45).toInt() % 8
                val output = directionMap[direction]
                (context as? FlickeyActionListener)?.onTextInput(output)
                output?.let { onFlickDetected?.invoke(it) }
                performClick()
            }
        }
        return true
    }
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
    private var paint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(120F, 120F, 100f, paint)
        paint.textSize = 50f
        canvas.drawText("Flickey View", width / 2f - 100, height / 2f, paint)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = MeasureSpec.getSize(widthMeasureSpec)
        val maxHeight = 300 * resources.displayMetrics.density // 400dp
        setMeasuredDimension(minWidth, maxHeight.toInt())
    }
}