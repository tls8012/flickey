package com.example.flickey

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout

class FlickeyLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var keyboardState = KeyboardState()
    private var actionListener: FlickeyActionListener? = null

    // UI Components
    private val btnLayerToggle: Button
    private val btnLayerCustom: Button
    private val btnLayerSpecial: Button
    private val btnCursorLeft: Button
    private val btnCursorRight: Button
    
    private val knobConsonant: FlickKnob
    private val knobVowel: FlickKnob
    private val knobPunctuation: FlickKnob
    private val keyboardRoot: LinearLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_flickey, this, true)
        
        keyboardRoot = findViewById(R.id.keyboard_root)
        btnLayerToggle = findViewById(R.id.btn_layer_toggle)
        btnLayerCustom = findViewById(R.id.btn_layer_custom)
        btnLayerSpecial = findViewById(R.id.btn_layer_special)
        btnCursorLeft = findViewById(R.id.btn_cursor_left)
        btnCursorRight = findViewById(R.id.btn_cursor_right)
        
        knobConsonant = findViewById(R.id.knob_consonant)
        knobVowel = findViewById(R.id.knob_vowel)
        knobPunctuation = findViewById(R.id.knob_punctuation)

        setupListeners()
        updateUI()
    }

    fun setActionListener(listener: FlickeyActionListener) {
        this.actionListener = listener
    }

    private fun setupListeners() {
        // State Change Listener
        keyboardState.onStateChanged = {
            updateUI()
        }

        // Buttons
        btnLayerToggle.setOnClickListener {
            keyboardState.toggleLayer()
        }
        btnLayerSpecial.setOnClickListener {
            keyboardState.setSpecialLayer()
        }
        
        // Cursor
        btnCursorLeft.setOnClickListener { actionListener?.onCursorLeft() }
        btnCursorRight.setOnClickListener { actionListener?.onCursorRight() }

        // Knobs - Flicks
        knobConsonant.onFlick = { text -> actionListener?.onTextInput(text) }
        knobVowel.onFlick = { text -> actionListener?.onTextInput(text) }
        knobPunctuation.onFlick = { text -> handlePunctuation(text) }

        // Knobs - Taps
        knobConsonant.onTap = {
            keyboardState.toggleConsonantPage()
        }
        knobVowel.onTap = {
            keyboardState.toggleVowelPage()
        }
        knobPunctuation.onTap = {
            // User requested: One tap on punctuation = Backspace or Delete
            actionListener?.onDelete()
        }
    }

    private fun handlePunctuation(text: String) {
        when (text) {
            "SPACE" -> actionListener?.onTextInput(" ")
            "ENTER" -> actionListener?.onEnter()
            "BACKSPACE" -> actionListener?.onDelete()
            "SHIFT" -> actionListener?.onShift()
            else -> actionListener?.onTextInput(text)
        }
    }

    private fun updateUI() {
        // Update Knobs
        knobConsonant.setConfig(keyboardState.getConsonantConfig())
        knobVowel.setConfig(keyboardState.getVowelConfig())
        knobPunctuation.setConfig(KeyMappings.PUNCTUATION)

        // Update Colors based on Layer
        val bgColor = when (keyboardState.currentLayer) {
            Layer.KR -> Color.parseColor("#E0F7FA") // Light Cyan
            Layer.EN -> Color.parseColor("#F3E5F5") // Light Purple
            Layer.SP -> Color.parseColor("#FFF3E0") // Light Orange
        }
        keyboardRoot.setBackgroundColor(bgColor)

        // Update Button Texts
        btnLayerToggle.text = if (keyboardState.currentLayer == Layer.KR) "한/영 (KR)" else "한/영 (EN)"
    }
}
