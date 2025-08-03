package com.example.flickey

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.inputmethodservice.InputMethodService
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class FlickKnob(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {
    private var isDragging = false
    private var flickStartX = 0f
    private var flickStartY = 0f

    var centerX: Float = (width/2).toFloat()
    var centerY: Float = (height/2).toFloat()
    var directionMap = listOf<String>("a","b","c","d","e","f","g","h")
    val radius: Float = 100f

    fun setMap(map: List<String>) {
        directionMap = map
        return
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                if (dx*dx + dy*dy <= radius*radius) {
                    isDragging = true
                    flickStartX = event.x
                    flickStartY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    centerX = min(max(event.x, radius), width - radius)
                    centerY = min(max(event.y, radius), height - radius)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    val dx = event.x - flickStartX
                    val dy = flickStartY - event.y
                    val angle = Math.toDegrees(atan2(dy, dx).toDouble()).let {
                        if (it < 0) it + 360 else it
                    }
                    val direction = ((angle + 22.5) / 45).toInt() % 8
                    isDragging = false
                    val output = directionMap[direction]
                    (context as? FlickeyActionListener)?.onTextInput(output)
                    centerX = (width/2).toFloat()
                    centerY = (height/2).toFloat()
                    performClick()
                }
            }
        }
        return true
    }
    override fun performClick(): Boolean {
        super.performClick()
        // 혹시 클릭 처리 로직 따로 하고 싶으면 여기에
        return true
    }

    val paint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, radius, paint)
    }
}

class FlickeyView2(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private val knob1 = FlickKnob(context, attrs)
    private val knob2 = FlickKnob(context, attrs)

    init {
        addView(knob1)
        addView(knob2)
        knob1.setMap("ijklmnop".map { it.toString() })
        knob2.setMap("qrstuvwx".map { it.toString() }) // to load from preferences
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // 예: knob1은 오른쪽 아래
        val size = 120 * resources.displayMetrics.density.toInt() // 400dp
        knob1.layout(width - size, height - size, width, height)
        // knob2는 바로 위쪽
        knob2.layout(width - size, height - 2*size - 20, width, height - size - 20)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = MeasureSpec.getSize(widthMeasureSpec)
        val maxHeight = 300 * resources.displayMetrics.density // 400dp
        // knob1, knob2 모두 크기 측정
        for (i in 0 until childCount) {
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec)
        }
        setMeasuredDimension(minWidth, maxHeight.toInt())
    }
}

class FlickIME: InputMethodService(), FlickeyActionListener{
    override fun onCreate() {
        super.onCreate()
        Log.e("FlickIME", "starting service")
    }
    override fun onCreateInputView(): View {
        Log.e("FlickIME", "onCreateInputView 진입")
        val view = FlickeyView2(this, null)
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