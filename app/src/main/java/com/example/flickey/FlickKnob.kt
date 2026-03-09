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
import kotlin.math.atan2
import kotlin.math.min

class FlickKnob(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {
    private var isDragging = false
    private var flickStartX = 0f
    private var flickStartY = 0f
    private var isTapCandidate = false

    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var currentConfig: KnobConfig? = null
    
    // Configurable radius
    var knobRadius: Float = 100f

    var onFlick: ((String) -> Unit)? = null
    var onTap: (() -> Unit)? = null

    // Paint objects
    private val circlePaint = Paint().apply {
        color = Color.parseColor("#DDDDDD") // Default Light Gray
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun setConfig(config: KnobConfig) {
        currentConfig = config
        invalidate()
    }

    fun setKnobColor(color: Int) {
        circlePaint.color = color
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        // Dynamic radius based on size, keeping some padding
        knobRadius = (min(w, h) / 2f) * 0.8f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                if (dx * dx + dy * dy <= knobRadius * knobRadius) {
                    isDragging = true
                    isTapCandidate = true
                    flickStartX = event.x
                    flickStartY = event.y
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - flickStartX
                    val dy = event.y - flickStartY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble())
                    
                    // If moved beyond a threshold, it's not a tap
                    if (dist > 20) {
                        isTapCandidate = false
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    val dx = event.x - flickStartX
                    val dy = flickStartY - event.y // Y is inverted on screen
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble())

                    if (isTapCandidate && dist < 20) {
                        // It's a Tap
                        onTap?.invoke()
                        performClick()
                    } else {
                        // It's a Flick
                        val angle = Math.toDegrees(atan2(dy, dx).toDouble()).let {
                            if (it < 0) it + 360 else it
                        }
                        // Map angle to 0-7
                        val direction = ((angle + 22.5) / 45).toInt() % 8
                        
                        currentConfig?.directions?.getOrNull(direction)?.let { output ->
                            onFlick?.invoke(output)
                        }
                    }
                    
                    isDragging = false
                    invalidate()
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw Background Circle
        canvas.drawCircle(centerX, centerY, knobRadius, circlePaint)
        
        // Draw Center Label
        currentConfig?.centerLabel?.let { label ->
            // Adjust text position to center vertically
            val textY = centerY - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(label, centerX, textY, textPaint)
        }

        // Draw Direction Labels (Small)
        currentConfig?.directions?.forEachIndexed { index, label ->
            if (label != null) {
                // Angle for index: 0 = Right, 1 = UR ... (CCW)
                // My KeyMappings are CCW starting from Right.
                val angleRad = Math.toRadians((index * 45).toDouble())
                // Canvas Y is down-positive.
                // Right (0): x+, y0.
                // Up (90): x0, y-.
                // Math: x = cos, y = -sin (because y is inverted)
                
                val labelX = centerX + Math.cos(angleRad).toFloat() * knobRadius * 0.7f
                val labelY = centerY - Math.sin(angleRad).toFloat() * knobRadius * 0.7f - ((textPaint.descent() + textPaint.ascent()) / 2)
                
                val originalSize = textPaint.textSize
                textPaint.textSize = 25f
                canvas.drawText(label, labelX, labelY, textPaint)
                textPaint.textSize = originalSize
            }
        }
    }
}

class FlickIME: InputMethodService(), FlickeyActionListener{
    override fun onCreate() {
        super.onCreate()
        Log.e("FlickIME", "starting service")
    }
    override fun onCreateInputView(): View {
        Log.e("FlickIME", "onCreateInputView 진입")
        val view = FlickeyLayout(this, null)
        view.setActionListener(this)
        Log.e("FlickIME", "FlickeyLayout 인스턴스 생성 완료")
        return view
    }
    override fun onTextInput(text: String?) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    override fun onShift() {
        // Toggle caps lock or shift state if needed
    }

    override fun onEnter() {
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
    }

    override fun onCursorLeft() {
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_LEFT))
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_LEFT))
    }

    override fun onCursorRight() {
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
        currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
    }
}