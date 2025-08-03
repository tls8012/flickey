package com.example.flickey

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.graphics.Paint
import android.inputmethodservice.InputMethodService
import android.util.Log
import kotlin.math.atan2

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
                val dy = startY - event.y // y 좌표는 위로 갈수록 작아지므로 반대로 계산
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
        // 혹시 클릭 처리 로직 따로 하고 싶으면 여기에
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

interface FlickeyActionListener {
    fun onTextInput(text: String?)
    fun onDelete()
    fun onShift()
    fun onEnter()
    // 필요한 만큼 계속 추가 가능!
}

class FlickIMEPP : InputMethodService(), FlickeyActionListener {
    override fun onCreate() {
        super.onCreate()
        Log.e("FlickIME", "starting service")
    }
    override fun onCreateInputView(): View {
        Log.e("FlickIME", "onCreateInputView 진입")
        val view = Flickey(this, null)
        Log.e("FlickIME", "Flickey 인스턴스 생성 완료")
        return view
    }

    override fun onTextInput(text: String?) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    override fun onShift() {
        return
    }

    override fun onEnter() {
        return
    }

}